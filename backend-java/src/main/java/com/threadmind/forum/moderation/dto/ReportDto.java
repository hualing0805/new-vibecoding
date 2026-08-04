package com.threadmind.forum.moderation.dto;

import java.time.Instant;

public record ReportDto(
        Long id,
        String targetType,
        Long targetId,
        String target,
        String reason,
        String detail,
        String status,
        String risk,
        Instant createdAt
) {
}

