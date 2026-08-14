package com.threadmind.forum.agent.service;

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
import com.threadmind.forum.agent.service.AgentAuditService.AuditSession;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class AgentService {

    private final AgentClient agentClient;
    private final AgentAuditService agentAuditService;

    public AgentService(AgentClient agentClient, AgentAuditService agentAuditService) {
        this.agentClient = agentClient;
        this.agentAuditService = agentAuditService;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        AuditSession session = agentAuditService.begin("CHAT", request.sessionId());
        String inputSummary = "message=" + request.message();
        agentAuditService.recordUserMessage(session, inputSummary);
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
            return normalized;
        } catch (RestClientException ex) {
            AgentChatResponse fallback = new AgentChatResponse(
                    "Agent 服务暂时不可用。论坛基础功能不受影响，可以稍后再试。",
                    List.of(),
                    List.of(new ToolCallDto("agent-python", "UNAVAILABLE", elapsedMs(startedAt), null, exceptionSummary(ex))),
                    session.id()
            );
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, fallback.answer());
            agentAuditService.recordToolFailure(
                    session,
                    assistantMessageId,
                    "agent-python",
                    inputSummary,
                    exceptionSummary(ex),
                    elapsedMs(startedAt)
            );
            agentAuditService.finish(session, "FALLBACK");
            return fallback;
        }
    }

    public PolishResponse polish(PolishRequest request) {
        AuditSession session = agentAuditService.begin("POLISH", null);
        String inputSummary = "title=" + request.title() + "; tone=" + request.tone() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
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
            return response;
        } catch (RestClientException ex) {
            PolishResponse fallback = new PolishResponse(
                    request.title(),
                    request.content(),
                    List.of("Agent 服务暂时不可用，已保留原文。")
            );
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, fallback.content());
            agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
            agentAuditService.finish(session, "FALLBACK");
            return fallback;
        }
    }

    public TagSuggestResponse suggestTags(TagSuggestRequest request) {
        AuditSession session = agentAuditService.begin("SUGGEST_TAGS", null);
        String inputSummary = "title=" + request.title() + "; categoryId=" + request.categoryId() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
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
            return response;
        } catch (RestClientException ex) {
            TagSuggestResponse fallback = new TagSuggestResponse(List.of("Java", "Vue3", "Agent"));
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, String.join(",", fallback.tags()));
            agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
            agentAuditService.finish(session, "FALLBACK");
            return fallback;
        }
    }

    public TopicSummaryResponse summarizeTopic(TopicSummaryRequest request) {
        AuditSession session = agentAuditService.begin("SUMMARY", null);
        String inputSummary = "topicId=" + request.topicId();
        agentAuditService.recordUserMessage(session, inputSummary);
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
            return normalized;
        } catch (RestClientException ex) {
            TopicSummaryResponse fallback = new TopicSummaryResponse(
                    "Agent 服务暂时不可用，当前显示本地占位摘要。",
                    List.of("Java 后端保留业务边界", "Python Agent 负责建议生成"),
                    List.of("是否需要给该主题生成可引用摘要？"),
                    List.of(),
                    List.of(new ToolCallDto("agent-python", "UNAVAILABLE", elapsedMs(startedAt), null, exceptionSummary(ex))),
                    session.id()
            );
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, summaryOutput(fallback));
            agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
            agentAuditService.finish(session, "FALLBACK");
            return fallback;
        }
    }

    public ModerationCheckResponse moderationCheck(ModerationCheckRequest request) {
        AuditSession session = agentAuditService.begin("MODERATION_CHECK", null);
        String inputSummary = "targetType=" + request.targetType() + "; content=" + request.content();
        agentAuditService.recordUserMessage(session, inputSummary);
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
            return response;
        } catch (RestClientException ex) {
            ModerationCheckResponse fallback = new ModerationCheckResponse("UNKNOWN", List.of("Agent 服务暂时不可用"), "REVIEW");
            Long assistantMessageId = agentAuditService.recordAssistantMessage(session, moderationOutput(fallback));
            agentAuditService.recordToolFailure(session, assistantMessageId, "agent-python", inputSummary, exceptionSummary(ex), elapsedMs(startedAt));
            agentAuditService.finish(session, "FALLBACK");
            return fallback;
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
}
