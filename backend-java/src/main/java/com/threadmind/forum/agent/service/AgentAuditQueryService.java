package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiAuditMetricsDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionDetailDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionListItemDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiToolCallDetailDto;
import com.threadmind.forum.agent.repository.AgentAuditQueryRepository;
import com.threadmind.forum.agent.repository.AgentAuditQueryRepository.ToolCallRecord;
import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class AgentAuditQueryService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AgentAuditQueryRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public AgentAuditQueryService(
            AgentAuditQueryRepository repository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    public PageResponse<AiSessionListItemDto> listAdminSessions(String scene, String status, int page, int pageSize) {
        return new PageResponse<>(
                repository.listAdminSessions(scene, status, page, pageSize),
                Math.max(page, 1),
                safePageSize(pageSize),
                repository.countAdminSessions(scene, status)
        );
    }

    public AiSessionDetailDto getAdminSession(Long sessionId) {
        AiSessionListItemDto session = repository.findAdminSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI session not found"));
        return detail(session);
    }

    public PageResponse<AiSessionListItemDto> listCurrentUserSessions(int page, int pageSize) {
        Long userId = currentUserProvider.currentUserId();
        return new PageResponse<>(
                repository.listUserSessions(userId, page, pageSize),
                Math.max(page, 1),
                safePageSize(pageSize),
                repository.countUserSessions(userId)
        );
    }

    public AiSessionDetailDto getCurrentUserSession(Long sessionId) {
        Long userId = currentUserProvider.currentUserId();
        AiSessionListItemDto session = repository.findUserSession(userId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI session not found"));
        return detail(session);
    }

    public AiAuditMetricsDto metricsToday() {
        return repository.metricsToday();
    }

    private AiSessionDetailDto detail(AiSessionListItemDto session) {
        return new AiSessionDetailDto(
                session,
                repository.listMessages(session.sessionId()),
                repository.listToolCalls(session.sessionId()).stream().map(this::toToolCall).toList()
        );
    }

    private AiToolCallDetailDto toToolCall(ToolCallRecord record) {
        Map<String, Object> input = readJson(record.inputJson());
        Map<String, Object> output = readJson(record.outputJson());
        return new AiToolCallDetailDto(
                record.id(),
                record.messageId(),
                record.toolName(),
                record.status(),
                record.elapsedMs(),
                asText(input.get("summary")),
                firstText(output, "summary", "toolSummary"),
                asText(output.get("error")),
                record.createdAt()
        );
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of("summary", "JSON 解析失败");
        }
    }

    private String firstText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = asText(payload.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int safePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 100);
    }
}
