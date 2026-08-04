package com.threadmind.forum.forum.dto;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        int topicCount
) {
}

