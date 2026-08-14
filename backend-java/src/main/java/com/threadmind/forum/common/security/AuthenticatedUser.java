package com.threadmind.forum.common.security;

import java.util.List;

public record AuthenticatedUser(
        Long id,
        String username,
        List<String> roles
) {
}
