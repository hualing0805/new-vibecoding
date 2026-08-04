package com.threadmind.forum.moderation.dto;

import java.time.Instant;

public record ModerationRecordDto(
        Long id,
        Long reportId,
        Long operatorId,
        String targetType,
        Long targetId,
        String action,
        String note,
        String traceId,
        Instant createdAt
) {
}

