package com.threadmind.forum.interaction.dto;

public record InteractionStatusResponse(
        String targetType,
        Long targetId,
        boolean liked,
        boolean favorited,
        int likeCount,
        int favoriteCount
) {
}

