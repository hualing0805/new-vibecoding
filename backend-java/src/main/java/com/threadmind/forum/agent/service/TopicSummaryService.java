package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.client.AgentClient;
import com.threadmind.forum.agent.dto.AgentDtos.CitationDto;
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

import java.util.ArrayList;
import java.util.List;

@Service
public class TopicSummaryService {

    private static final Logger log = LoggerFactory.getLogger(TopicSummaryService.class);
    private static final String SCENE = "SUMMARY";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentClient agentClient;
    private final AgentAuditService auditService;
    private final AgentProtectionService protectionService;
    private final TopicSummaryCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;

    public TopicSummaryService(
            AgentClient agentClient,
            AgentAuditService auditService,
            AgentProtectionService protectionService,
            TopicSummaryCacheRepository cacheRepository,
            ObjectMapper objectMapper,
            CurrentUserProvider currentUserProvider
    ) {
        this.agentClient = agentClient;
        this.auditService = auditService;
        this.protectionService = protectionService;
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public TopicSummaryResponse summarize(TopicSummaryRequest request, boolean refresh) {
        if (!refresh) {
            return cacheRepository.findFreshByTopicId(request.topicId())
                    .map(cache -> fromCache(cache, "HIT", cache.auditSessionId(), List.of()))
                    .orElseGet(() -> generate(request));
        }
        return generate(request);
    }

    private TopicSummaryResponse generate(TopicSummaryRequest request) {
        protectionService.assertAllowed(SCENE);
        AuditSession session = auditService.begin(SCENE, null);
        String inputSummary = "topicId=" + request.topicId();
        auditService.recordUserMessage(session, inputSummary);
        if (protectionService.isCircuitOpen(SCENE)) {
            return fallback(request.topicId(), session, inputSummary, "CircuitOpen: scene SUMMARY", 0);
        }

        long startedAt = System.nanoTime();
        try {
            TopicSummaryResponse response = requireResponse(
                    agentClient.post("/agent/summarize-topic", request, TopicSummaryResponse.class)
            );
            TopicSummaryResponse normalized = normalize(response, session.id());
            String outputSummary = outputSummary(normalized);
            Long assistantMessageId = auditService.recordAssistantMessage(session, outputSummary);
            auditService.recordToolCalls(
                    session,
                    assistantMessageId,
                    normalized.toolCalls().isEmpty()
                            ? List.of(new ToolCallDto("agent-python", "OK", elapsedMs(startedAt), "summary completed", null))
                            : normalized.toolCalls(),
                    inputSummary,
                    outputSummary
            );
            auditService.finish(session, "COMPLETED");
            store(request.topicId(), normalized);
            protectionService.recordSuccess(SCENE);
            return normalized;
        } catch (RestClientException ex) {
            protectionService.recordFailure(SCENE);
            return fallback(request.topicId(), session, inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
        }
    }

    private TopicSummaryResponse normalize(TopicSummaryResponse response, Long sessionId) {
        return new TopicSummaryResponse(
                response.summary(),
                safeList(response.points()),
                safeList(response.followUpQuestions()),
                response.citations() == null ? List.of() : response.citations(),
                response.toolCalls() == null ? List.of() : response.toolCalls(),
                sessionId
        );
    }

    private TopicSummaryResponse fallback(
            Long topicId,
            AuditSession session,
            String inputSummary,
            String error,
            Integer elapsedMs
    ) {
        Long assistantMessageId = auditService.recordAssistantMessage(session, "保留旧摘要或返回本地 fallback");
        auditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, error, elapsedMs);
        auditService.finish(session, "FALLBACK");
        return cacheRepository.findLatestByTopicId(topicId)
                .map(cache -> fromCache(
                        cache,
                        "STALE",
                        session.id(),
                        List.of(new ToolCallDto("agent-python", fallbackStatus(error), elapsedMs, null, error))
                ))
                .orElseGet(() -> defaultFallback(session.id(), error, elapsedMs));
    }

    private TopicSummaryResponse defaultFallback(Long sessionId, String error, Integer elapsedMs) {
        return new TopicSummaryResponse(
                "Agent 服务暂时不可用，当前显示本地占位摘要。",
                List.of("Java 后端保留业务边界", "Python Agent 负责建议生成"),
                List.of("是否需要给该主题生成可引用摘要？"),
                List.of(),
                List.of(new ToolCallDto("agent-python", fallbackStatus(error), elapsedMs, null, error)),
                sessionId
        );
    }

    private TopicSummaryResponse fromCache(
            CachedTopicSummary cache,
            String cacheStatus,
            Long sessionId,
            List<ToolCallDto> extraToolCalls
    ) {
        List<ToolCallDto> toolCalls = new ArrayList<>();
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

    private void store(Long topicId, TopicSummaryResponse response) {
        try {
            cacheRepository.create(
                    topicId,
                    response,
                    objectMapper.writeValueAsString(safeList(response.points())),
                    objectMapper.writeValueAsString(safeList(response.followUpQuestions())),
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
        } catch (JsonProcessingException ex) {
            log.warn("Ignoring malformed topic summary cache payload", ex);
            return List.of();
        }
    }

    private <T> T requireResponse(T response) {
        if (response == null) {
            throw new RestClientException("Empty agent response");
        }
        return response;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String outputSummary(TopicSummaryResponse response) {
        return response.summary() + "; points=" + String.join(",", safeList(response.points()));
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
}
