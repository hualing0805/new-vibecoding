package com.threadmind.forum.agent.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AgentProtectionService {

    private static final Logger log = LoggerFactory.getLogger(AgentProtectionService.class);
    private static final int MAX_CALLS_PER_WINDOW = 8;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final Map<String, ArrayDeque<Instant>> callWindows = new HashMap<>();
    private final Map<String, CircuitState> circuits = new HashMap<>();

    @Autowired
    public AgentProtectionService(CurrentUserProvider currentUserProvider) {
        this(currentUserProvider, Clock.systemUTC());
    }

    AgentProtectionService(CurrentUserProvider currentUserProvider, Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public synchronized void assertAllowed(String scene) {
        Long userId = currentUserProvider.currentUserId();
        String key = userId + ":" + normalize(scene);
        Instant now = clock.instant();
        ArrayDeque<Instant> calls = callWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!calls.isEmpty() && calls.peekFirst().plus(RATE_WINDOW).isBefore(now)) {
            calls.removeFirst();
        }
        if (calls.size() >= MAX_CALLS_PER_WINDOW) {
            log.warn("AI rate limit exceeded for {}", key);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI 调用过于频繁，请稍后再试");
        }
        calls.addLast(now);
    }

    public synchronized boolean isCircuitOpen(String scene) {
        CircuitState state = circuits.get(normalize(scene));
        if (state == null || state.openUntil == null) {
            return false;
        }
        if (state.openUntil.isAfter(clock.instant())) {
            return true;
        }
        circuits.remove(normalize(scene));
        return false;
    }

    public synchronized void recordSuccess(String scene) {
        circuits.remove(normalize(scene));
    }

    public synchronized void recordFailure(String scene) {
        String key = normalize(scene);
        CircuitState state = circuits.computeIfAbsent(key, ignored -> new CircuitState());
        state.consecutiveFailures++;
        if (state.consecutiveFailures >= FAILURE_THRESHOLD) {
            state.openUntil = clock.instant().plus(CIRCUIT_OPEN_DURATION);
            log.warn("AI circuit opened for scene {} until {}", key, state.openUntil);
        }
    }

    private String normalize(String scene) {
        return scene == null ? "UNKNOWN" : scene.trim().toUpperCase(Locale.ROOT);
    }

    private static final class CircuitState {
        private int consecutiveFailures;
        private Instant openUntil;
    }
}
