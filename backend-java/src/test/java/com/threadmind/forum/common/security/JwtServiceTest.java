package com.threadmind.forum.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedTokenCanBeParsedBackIntoAuthenticatedUser() {
        JwtService jwtService = new JwtService(new ObjectMapper(), FIXED_CLOCK, "test-secret", 3600);

        String token = jwtService.issueToken(7L, "lin", List.of("USER", "ADMIN"));
        AuthenticatedUser user = jwtService.parseToken(token).orElseThrow();

        assertEquals(7L, user.id());
        assertEquals("lin", user.username());
        assertEquals(List.of("USER", "ADMIN"), user.roles());
    }

    @Test
    void parseTokenRejectsTamperedSignature() {
        JwtService jwtService = new JwtService(new ObjectMapper(), FIXED_CLOCK, "test-secret", 3600);

        String token = jwtService.issueToken(7L, "lin", List.of("USER"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertTrue(jwtService.parseToken(tampered).isEmpty());
    }

    @Test
    void parseTokenRejectsExpiredToken() {
        JwtService jwtService = new JwtService(new ObjectMapper(), FIXED_CLOCK, "test-secret", -1);

        String token = jwtService.issueToken(7L, "lin", List.of("USER"));

        assertTrue(jwtService.parseToken(token).isEmpty());
    }
}
