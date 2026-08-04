package com.threadmind.forum.auth.dto;

import java.util.List;

public record UserSummary(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        List<String> roles
) {
}

