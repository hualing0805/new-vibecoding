package com.threadmind.forum.forum.dto;

import java.time.Instant;
import java.util.List;

public record TopicSummaryDto(
        Long id,
        String title,
        String excerpt,
        CategoryDto category,
        AuthorDto author,
        List<String> tags,
        int replyCount,
        int likeCount,
        int favoriteCount,
        int viewCount,
        String status,
        Instant createdAt
) {
}

