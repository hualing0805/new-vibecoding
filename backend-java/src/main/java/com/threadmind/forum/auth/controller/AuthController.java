package com.threadmind.forum.auth.controller;

import com.threadmind.forum.auth.dto.AuthResponse;
import com.threadmind.forum.auth.dto.LoginRequest;
import com.threadmind.forum.auth.dto.RegisterRequest;
import com.threadmind.forum.auth.dto.UserSummary;
import com.threadmind.forum.auth.service.AuthService;
import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), TraceContext.currentTraceId());
    }

    @PostMapping("/api/auth/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), TraceContext.currentTraceId());
    }

    @GetMapping("/api/users/me")
    public ApiResponse<UserSummary> currentUser() {
        return ApiResponse.ok(authService.currentUser(), TraceContext.currentTraceId());
    }
}

