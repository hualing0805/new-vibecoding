package com.threadmind.forum.forum.dto;

import java.time.Instant;
import java.util.List;

public record TopicDetailDto(
        Long id,
        String title,
        String content,
        CategoryDto category,
        AuthorDto author,
        List<String> tags,
        int replyCount,
        int likeCount,
        int favoriteCount,
        int viewCount,
        String status,
        String aiSummary,
        Instant createdAt
) {
}

