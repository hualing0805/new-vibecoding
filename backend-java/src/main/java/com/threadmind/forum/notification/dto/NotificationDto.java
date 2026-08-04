package com.threadmind.forum.notification.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String body,
        boolean read,
        Instant createdAt
) {
}

