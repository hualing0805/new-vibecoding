package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.client.AgentClient;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatRequest;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatResponse;
import com.threadmind.forum.agent.dto.AgentDtos.CitationDto;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckRequest;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckResponse;
import com.threadmind.forum.agent.dto.AgentDtos.PolishRequest;
import com.threadmind.forum.agent.dto.AgentDtos.PolishResponse;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestResponse;
import com.threadmind.forum.agent.dto.AgentDtos.ToolCallDto;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository.CachedTopicSummary;
import com.threadmind.forum.agent.service.AgentAuditService.AuditSession;
import com.threadmind.forum.common.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentClient agentClient;
    private final AgentAuditService agentAuditService;
    private final AgentProtectionService agentProtectionService;
    private final TopicSummaryCacheRepository topicSummaryCacheRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;

    public AgentService(
            AgentClient agentClient,
            AgentAuditService agentAuditService,
            AgentProtectionService agentProtectionService,
            TopicSummaryCacheRepository topicSummaryCacheRepository,
            ObjectMapper objectMapper,
            CurrentUserProvider currentUserProvider
    ) {
        this.agentClient = agentClient;
        this.agentAuditService = agentAuditService;
        this.agentProtectionService = agentProtectionService;
        this.topicSummaryCacheRepository = topicSummaryCacheRepository;
        this.objectMapper = objectMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        String scene = "CHAT";
        agentProtectionService.assertAllowed(scene);
        AuditSession session = agentAuditService.begin(scene, request.sessionId());
        String inputSummary = "message=" + request.message();
        agentAuditService.recordUserMessage(session, inputSummary);
        if (agentProtectionService.isCircuitOpen(scene)) {
            return chatFallback(session, inputSummary, "Agent 连续失败，当前进入短暂保护。", "CircuitOpen: scene CHAT");
        }
        long startedAt = System.nanoTime();
        try {
            AgentChatResponse response = requireResponse(agentClient.post("/agent/chat", request, AgentChatResponse.class));
            AgentChatResponse normalized = new AgentChatResponse(
                    response.answer(),
                    safeCitations(response.citations()),
                    safeToolCalls(response.toolCalls()),
                    session.id()
            );
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, normalized.answer());
            agentAuditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    normalized.toolCalls().isEmpty()
                            ? List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "chat completed", null))
                            : normalized.toolCalls(),
                    inputSummary,
                    chatOutputSummary(normalized)
            );
            agentAuditService.finish(session, "COMPLETED");
            agentProtectionService.recordSuccess(scene);
            return normalized;
        } catch (RestClientException ex) {
            agentProtectionService.recordFailure(scene);
            return chatFallback(
                    session,
                    inputSummary,
                    "Agent 服务暂时不可用。论坛基础功能不受影响，可以稍后再试。",
                    exceptionSummary(ex),
                    elapsedMs(startedAt)
            );
        }
    }

    public PolishResponse polish(PolishRequest request) {
        String scene = "POLISH";
        agentProtectionService.assertAllowed(scene);
        AuditSession session = agentAuditService.begin(scene, null);
        String inputSummary = "title=" + request.title() + "; tone=" + request.tone() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
        if (agentProtectionService.isCircuitOpen(scene)) {
            return polishFallback(session, inputSummary, request, "CircuitOpen: scene POLISH", 0);
        }
        long startedAt = System.nanoTime();
        try {
            PolishResponse response = requireResponse(agentClient.post("/agent/polish", request, PolishResponse.class));
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, response.title() + "\n" + response.content());
            agentAuditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "polish completed", null)),
                    inputSummary,
                    response.content()
            );
            agentAuditService.finish(session, "COMPLETED");
            agentProtectionService.recordSuccess(scene);
            return response;
        } catch (RestClientException ex) {
            agentProtectionService.recordFailure(scene);
            return polishFallback(session, inputSummary, request, exceptionSummary(ex), elapsedMs(startedAt));
        }
    }

    public TagSuggestResponse suggestTags(TagSuggestRequest request) {
        String scene = "SUGGEST_TAGS";
        agentProtectionService.assertAllowed(scene);
        AuditSession session = agentAuditService.begin(scene, null);
        String inputSummary = "title=" + request.title() + "; categoryId=" + request.categoryId() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
        if (agentProtectionService.isCircuitOpen(scene)) {
            return tagsFallback(session, inputSummary, "CircuitOpen: scene SUGGEST_TAGS", 0);
        }
        long startedAt = System.nanoTime();
        try {
            TagSuggestResponse response = requireResponse(agentClient.post("/agent/suggest-tags", request, TagSuggestResponse.class));
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, String.join(",", safeStrings(response.tags())));
            agentAuditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "tag suggestion completed", null)),
                    inputSummary,
                    String.join(",", safeStrings(response.tags()))
            );
            agentAuditService.finish(session, "COMPLETED");
            agentProtectionService.recordSuccess(scene);
            return response;
        } catch (RestClientException ex) {
            agentProtectionService.recordFailure(scene);
            return tagsFallback(session, inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
        }
    }

    public TopicSummaryResponse summarizeTopic(TopicSummaryRequest request) {
        return summarizeTopic(request, false);
    }

    public TopicSummaryResponse summarizeTopic(TopicSummaryRequest request, boolean refresh) {
        if (!refresh) {
            return topicSummaryCacheRepository.findFreshByTopicId(request.topicId())
                    .map(cache -> cachedSummary(cache, "HIT", cache.auditSessionId(), List.of()))
                    .orElseGet(() -> generateTopicSummary(request));
        }
        return generateTopicSummary(request);
    }

    public ModerationCheckResponse moderationCheck(ModerationCheckRequest request) {
        String scene = "MODERATION_CHECK";
        agentProtectionService.assertAllowed(scene);
        AuditSession session = agentAuditService.begin(scene, null);
        String inputSummary = "targetType=" + request.targetType() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
        if (agentProtectionService.isCircuitOpen(scene)) {
            return moderationFallback(session, inputSummary, "CircuitOpen: scene MODERATION_CHECK", 0);
        }
        long startedAt = System.nanoTime();
        try {
            ModerationCheckResponse response = requireResponse(agentClient.post("/agent/moderation-check", request, ModerationCheckResponse.class));
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, moderationOutput(response));
            agentAuditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "moderation check completed", null)),
                    inputSummary,
                    moderationOutput(response)
            );
            agentAuditService.finish(session, "COMPLETED");
            agentProtectionService.recordSuccess(scene);
            return response;
        } catch (RestClientException ex) {
            agentProtectionService.recordFailure(scene);
            return moderationFallback(session, inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
        }
    }

    private TopicSummaryResponse generateTopicSummary(TopicSummaryRequest request) {
        String scene = "SUMMARY";
        agentProtectionService.assertAllowed(scene);
        AuditSession session = agentAuditService.begin(scene, null);
        String inputSummary = "topicId=" + request.topicId();
        agentAuditService.recordUserMessage(session, inputSummary);
        if (agentProtectionService.isCircuitOpen(scene)) {
            return summaryFallbackFromCacheOrDefault(session, inputSummary, "CircuitOpen: scene SUMMARY", 0);
        }
        long startedAt = System.nanoTime();
        try {
            TopicSummaryResponse response = requireResponse(agentClient.post("/agent/summarize-topic", request, TopicSummaryResponse.class));
            TopicSummaryResponse normalized = new TopicSummaryResponse(
                    response.summary(),
                    safeStrings(response.points()),
                    safeStrings(response.followUpQuestions()),
                    safeCitations(response.citations()),
                    safeToolCalls(response.toolCalls()),
                    session.id()
            );
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, summaryOutput(normalized));
            agentAuditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    normalized.toolCalls().isEmpty()
                            ? List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "summary completed", null))
                            : normalized.toolCalls(),
                    inputSummary,
                    summaryOutput(normalized)
            );
            agentAuditService.finish(session, "COMPLETED");
            storeTopicSummary(request.topicId(), normalized);
            agentProtectionService.recordSuccess(scene);
            return normalized;
        } catch (RestClientException ex) {
            agentProtectionService.recordFailure(scene);
            return summaryFallbackFromCacheOrDefault(session, inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
        }
    }

    private AgentChatResponse chatFallback(AuditSession session, String inputSummary, String answer, String error) {
        return chatFallback(session, inputSummary, answer, error, 0);
    }

    private AgentChatResponse chatFallback(AuditSession session, String inputSummary, String answer, String error, Integer elapsedMs) {
        AgentChatResponse fallback = new AgentChatResponse(
                answer,
                List.of(),
                List.of(new ToolCallDto("agent-python", fallbackStatus(error), elapsedMs, null, error)),
                session.id()
        );
        Long assistantMessageId = agentAuditService.recordAssistantMessage(session, fallback.answer());
        agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        agentAuditService.finish(session, "FALLBACK");
        return fallback;
    }

    private PolishResponse polishFallback(AuditSession session, String inputSummary, PolishRequest request, String error, Integer elapsedMs) {
        PolishResponse fallback = new PolishResponse(
                request.title(),
                request.content(),
                List.of("Agent 服务暂时不可用，已保留原文。")
        );
        Long assistantMessageId = agentAuditService.recordAssistantMessage(session, fallback.content());
        agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        agentAuditService.finish(session, "FALLBACK");
        return fallback;
    }

    private TagSuggestResponse tagsFallback(AuditSession session, String inputSummary, String error, Integer elapsedMs) {
        TagSuggestResponse fallback = new TagSuggestResponse(List.of("Java", "Vue3", "Agent"));
        Long assistantMessageId = agentAuditService.recordAssistantMessage(session, String.join(",", fallback.tags()));
        agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        agentAuditService.finish(session, "FALLBACK");
        return fallback;
    }

    private TopicSummaryResponse summaryFallbackFromCacheOrDefault(
            AuditSession session,
            String inputSummary,
            String error,
            Integer elapsedMs
    ) {
        Long assistantMessageId = agentAuditService.recordAssistantMessage(session, "保留旧摘要或返回本地 fallback");
        agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        agentAuditService.finish(session, "FALLBACK");
        String topicIdText = inputSummary.replace("topicId=", "");
        Long topicId = parseLong(topicIdText);
        if (topicId != null) {
            return topicSummaryCacheRepository.findLatestByTopicId(topicId)
                    .map(cache -> cachedSummary(
                            cache,
                            "STALE",
                            session.id(),
                            List.of(new ToolCallDto("agent-python", fallbackStatus(error), elapsedMs, null, error))
                    ))
                    .orElseGet(() -> defaultSummaryFallback(session, error, elapsedMs));
        }
        return defaultSummaryFallback(session, error, elapsedMs);
    }

    private TopicSummaryResponse defaultSummaryFallback(AuditSession session, String error, Integer elapsedMs) {
        return new TopicSummaryResponse(
                "Agent 服务暂时不可用，当前显示本地占位摘要。",
                List.of("Java 后端保留业务边界", "Python Agent 负责建议生成"),
                List.of("是否需要给该主题生成可引用摘要？"),
                List.of(),
                List.of(new ToolCallDto("agent-python", fallbackStatus(error), elapsedMs, null, error)),
                session.id()
        );
    }

    private ModerationCheckResponse moderationFallback(AuditSession session, String inputSummary, String error, Integer elapsedMs) {
        ModerationCheckResponse fallback = new ModerationCheckResponse("UNKNOWN", List.of("Agent 服务暂时不可用"), "REVIEW");
        Long assistantMessageId = agentAuditService.recordAssistantMessage(session, moderationOutput(fallback));
        agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        agentAuditService.finish(session, "FALLBACK");
        return fallback;
    }

    private TopicSummaryResponse cachedSummary(
            CachedTopicSummary cache,
            String cacheStatus,
            Long sessionId,
            List<ToolCallDto> extraToolCalls
    ) {
        List<ToolCallDto> toolCalls = new java.util.ArrayList<>();
        toolCalls.add(new ToolCallDto(
                "summary-cache",
                cacheStatus,
                0,
                "topicId=" + cache.topicId() + "; generatedAt=" + cache.generatedAt(),
                null
        ));
        toolCalls.addAll(extraToolCalls);
        return new TopicSummaryResponse(
                cache.summary(),
                readStringList(cache.pointsJson()),
                readStringList(cache.followUpQuestionsJson()),
                List.of(new CitationDto(cache.topicId(), "缓存摘要来源主题")),
                toolCalls,
                sessionId
        );
    }

    private void storeTopicSummary(Long topicId, TopicSummaryResponse response) {
        try {
            topicSummaryCacheRepository.create(
                    topicId,
                    response,
                    objectMapper.writeValueAsString(safeStrings(response.points())),
                    objectMapper.writeValueAsString(safeStrings(response.followUpQuestions())),
                    currentUserProvider.currentUserId()
            );
        } catch (JsonProcessingException ex) {
            log.warn("AI topic summary cache serialization failed for topic {}", topicId, ex);
        } catch (RuntimeException ex) {
            log.warn("AI topic summary cache write failed for topic {}", topicId, ex);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private <T> T requireResponse(T response) {
        if (response == null) {
            throw new RestClientException("Empty agent response");
        }
        return response;
    }

    private List<CitationDto> safeCitations(List<CitationDto> citations) {
        return citations == null ? List.of() : citations;
    }

    private List<ToolCallDto> safeToolCalls(List<ToolCallDto> toolCalls) {
        return toolCalls == null ? List.of() : toolCalls;
    }

    private List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String chatOutputSummary(AgentChatResponse response) {
        return response.answer() + "; citations=" + response.citations().size();
    }

    private String summaryOutput(TopicSummaryResponse response) {
        return response.summary() + "; points=" + String.join(",", safeStrings(response.points()));
    }

    private String moderationOutput(ModerationCheckResponse response) {
        return response.riskLevel() + "; action=" + response.suggestedAction() + "; reasons=" + String.join(",", safeStrings(response.reasons()));
    }

    private Integer elapsedMs(long startedAt) {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, elapsed));
    }

    private String exceptionSummary(RestClientException ex) {
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    private String fallbackStatus(String error) {
        return error != null && error.startsWith("CircuitOpen") ? "CIRCUIT_OPEN" : "UNAVAILABLE";
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
