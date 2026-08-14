package com.threadmind.forum.auth.service;

import com.threadmind.forum.auth.dto.AuthResponse;
import com.threadmind.forum.auth.dto.LoginRequest;
import com.threadmind.forum.auth.repository.UserRepository;
import com.threadmind.forum.auth.repository.UserRepository.UserRecord;
import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private JwtService jwtService;

    @Test
    void loginReturnsJwtTokenAndUserSummary() {
        UserRecord user = new UserRecord(
                1L,
                "demo",
                "demo@threadmind.local",
                passwordEncoder.encode("ThreadMind@123"),
                "Demo User",
                "",
                "ACTIVE",
                Instant.parse("2026-08-14T00:00:00Z")
        );
        when(userRepository.findActiveByLogin("demo")).thenReturn(Optional.of(user));
        when(userRepository.findRoleCodes(1L)).thenReturn(List.of("ADMIN", "USER"));
        when(jwtService.issueToken(1L, "demo", List.of("ADMIN", "USER"))).thenReturn("jwt-token");

        AuthService authService = new AuthService(passwordEncoder, userRepository, currentUserProvider, jwtService);
        AuthResponse response = authService.login(new LoginRequest("demo", "ThreadMind@123"));

        assertEquals("jwt-token", response.token());
        assertEquals(1L, response.user().id());
        assertEquals("demo", response.user().username());
        assertEquals(List.of("ADMIN", "USER"), response.user().roles());
    }
}
