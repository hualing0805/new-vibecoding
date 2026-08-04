package com.threadmind.forum.auth.service;

import com.threadmind.forum.auth.dto.AuthResponse;
import com.threadmind.forum.auth.dto.LoginRequest;
import com.threadmind.forum.auth.dto.RegisterRequest;
import com.threadmind.forum.auth.dto.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        passwordEncoder.matches(request.password(), passwordEncoder.encode(request.password()));
        return demoAuth(request.username(), "演示用户");
    }

    public AuthResponse register(RegisterRequest request) {
        passwordEncoder.encode(request.password());
        return demoAuth(request.username(), request.nickname());
    }

    public UserSummary currentUser() {
        return new UserSummary(1L, "demo", "演示用户", "", List.of("USER", "ADMIN"));
    }

    private AuthResponse demoAuth(String username, String nickname) {
        UserSummary user = new UserSummary(1L, username, nickname, "", List.of("USER", "ADMIN"));
        String raw = username + ":" + Instant.now().toEpochMilli();
        String token = "demo." + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return new AuthResponse(token, user);
    }
}

