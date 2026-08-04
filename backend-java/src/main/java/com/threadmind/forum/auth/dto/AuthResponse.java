package com.threadmind.forum.auth.dto;

public record AuthResponse(
        String token,
        UserSummary user
) {
}

