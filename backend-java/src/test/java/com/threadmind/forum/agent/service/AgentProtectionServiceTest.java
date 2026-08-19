package com.threadmind.forum.agent.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentProtectionServiceTest {

    @Test
    void rejectsCallsBeyondShortWindow() {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        AgentProtectionService service = new AgentProtectionService(currentUserProvider, clock);

        for (int i = 0; i < 8; i++) {
            service.assertAllowed("CHAT");
        }

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.assertAllowed("CHAT"));
        assertEquals(429, ex.getStatusCode().value());

        clock.advance(Duration.ofSeconds(61));
        service.assertAllowed("CHAT");
    }

    @Test
    void opensAndResetsCircuitAfterFailures() {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        AgentProtectionService service = new AgentProtectionService(currentUserProvider, clock);

        service.recordFailure("SUMMARY");
        service.recordFailure("SUMMARY");
        assertFalse(service.isCircuitOpen("SUMMARY"));

        service.recordFailure("SUMMARY");
        assertTrue(service.isCircuitOpen("SUMMARY"));

        service.recordSuccess("SUMMARY");
        assertFalse(service.isCircuitOpen("SUMMARY"));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
