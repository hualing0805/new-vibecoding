package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.dto.AgentDtos.ToolCallDto;
import com.threadmind.forum.agent.repository.AgentAuditRepository;
import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.common.web.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditService.class);
    private static final int MESSAGE_LIMIT = 2000;
    private static final int JSON_SUMMARY_LIMIT = 1000;

    private final AgentAuditRepository agentAuditRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public AgentAuditService(
            AgentAuditRepository agentAuditRepository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.agentAuditRepository = agentAuditRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    public AuditSession begin(String scene, String requestedSessionId) {
        try {
            Long userId = currentUserProvider.currentUserId();
            Long sessionId = parseSessionId(requestedSessionId);
            if (sessionId != null && agentAuditRepository.findOwnedSession(userId, sessionId).isPresent()) {
                return new AuditSession(sessionId, scene, true);
            }
            Long createdSessionId = agentAuditRepository.createSession(
                    userId,
                    scene,
                    "OPEN",
                    TraceContext.currentTraceId()
            );
            return new AuditSession(createdSessionId, scene, true);
        } catch (RuntimeException ex) {
            log.warn("AI audit session creation failed for scene {}", scene, ex);
            return AuditSession.disabled(scene);
        }
    }

    public Long recordUserMessage(AuditSession session, String content) {
        return recordMessage(session, "USER", content);
    }

    public Long recordAssistantMessage(AuditSession session, String content) {
        return recordMessage(session, "ASSISTANT", content);
    }

    public void recordToolCalls(
            AuditSession session,
            Long messageId,
            List<ToolCallDto> toolCalls,
            String inputSummary,
            String outputSummary
    ) {
        if (!session.enabled() || messageId == null) {
            return;
        }
        List<ToolCallDto> safeToolCalls = toolCalls == null || toolCalls.isEmpty()
                ? List.of(new ToolCallDto("agent-python", "OK"))
                : toolCalls;
        for (ToolCallDto toolCall : safeToolCalls) {
            recordToolCall(session, messageId, toolCall, inputSummary, outputSummary);
        }
    }

    public void recordToolFailure(
            AuditSession session,
            Long messageId,
            String toolName,
            String inputSummary,
            String error,
            Integer elapsedMs
    ) {
        if (!session.enabled() || messageId == null) {
            return;
        }
        recordToolCall(
                session,
                messageId,
                new ToolCallDto(toolName, "FAILED", elapsedMs, null, sanitize(error, JSON_SUMMARY_LIMIT)),
                inputSummary,
                error
        );
    }

    public void finish(AuditSession session, String status) {
        if (!session.enabled()) {
            return;
        }
        try {
            agentAuditRepository.updateSessionStatus(session.id(), status);
        } catch (RuntimeException ex) {
            log.warn("AI audit session status update failed for session {}", session.id(), ex);
        }
    }

    private Long recordMessage(AuditSession session, String role, String content) {
        if (!session.enabled()) {
            return null;
        }
        try {
            return agentAuditRepository.createMessage(session.id(), role, sanitize(content, MESSAGE_LIMIT));
        } catch (RuntimeException ex) {
            log.warn("AI audit message write failed for session {}", session.id(), ex);
            return null;
        }
    }

    private void recordToolCall(
            AuditSession session,
            Long messageId,
            ToolCallDto toolCall,
            String inputSummary,
            String outputSummary
    ) {
        try {
            agentAuditRepository.createToolCall(
                    messageId,
                    sanitize(toolCall.toolName(), 64),
                    sanitize(toolCall.status(), 24),
                    toJson(Map.of(
                            "scene", session.scene(),
                            "summary", sanitize(inputSummary, JSON_SUMMARY_LIMIT)
                    )),
                    toJson(toolOutput(toolCall, outputSummary)),
                    toolCall.elapsedMs()
            );
        } catch (RuntimeException ex) {
            log.warn("AI audit tool call write failed for message {}", messageId, ex);
        }
    }

    private Map<String, Object> toolOutput(ToolCallDto toolCall, String outputSummary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", sanitize(outputSummary, JSON_SUMMARY_LIMIT));
        payload.put("toolSummary", sanitize(toolCall.summary(), JSON_SUMMARY_LIMIT));
        payload.put("error", sanitize(toolCall.error(), JSON_SUMMARY_LIMIT));
        return payload;
    }

    private String toJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private Long parseSessionId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String sanitize(String value, int limit) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= limit) {
            return compact;
        }
        return compact.substring(0, limit) + "...";
    }

    public record AuditSession(Long id, String scene, boolean enabled) {
        static AuditSession disabled(String scene) {
            return new AuditSession(null, scene, false);
        }
    }
}
