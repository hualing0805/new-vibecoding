package com.threadmind.forum.forum.dto;

import java.time.Instant;

public record ReplyDto(
        Long id,
        Long topicId,
        AuthorDto author,
        String content,
        int likeCount,
        String status,
        Instant createdAt
) {
}

