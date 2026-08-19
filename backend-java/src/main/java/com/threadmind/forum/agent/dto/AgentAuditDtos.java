package com.threadmind.forum.agent.dto;

import java.time.Instant;
import java.util.List;

public final class AgentAuditDtos {

    private AgentAuditDtos() {
    }

    public record AiSessionListItemDto(
            Long sessionId,
            Long userId,
            String nickname,
            String scene,
            String status,
            String traceId,
            Instant createdAt,
            Instant updatedAt,
            long messageCount,
            long toolCallCount
    ) {
    }

    public record AiSessionMessageDto(
            Long id,
            String role,
            String content,
            Instant createdAt
    ) {
    }

    public record AiToolCallDetailDto(
            Long id,
            Long messageId,
            String toolName,
            String status,
            Integer elapsedMs,
            String inputSummary,
            String outputSummary,
            String errorSummary,
            Instant createdAt
    ) {
    }

    public record AiSessionDetailDto(
            AiSessionListItemDto session,
            List<AiSessionMessageDto> messages,
            List<AiToolCallDetailDto> toolCalls
    ) {
    }

    public record AiAuditMetricsDto(
            long sessionsToday,
            long completedToday,
            long fallbackToday,
            long toolCallsToday,
            long failedToolCallsToday,
            long cachedSummaries,
            long staleSummaries
    ) {
    }
}
