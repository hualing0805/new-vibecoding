package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.client.AgentClient;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository.CachedTopicSummary;
import com.threadmind.forum.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicSummaryServiceTest {

    @Mock
    private AgentClient agentClient;

    @Mock
    private AgentAuditService auditService;

    @Mock
    private AgentProtectionService protectionService;

    @Mock
    private TopicSummaryCacheRepository cacheRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void usesFreshCacheWithoutCallingAgent() {
        TopicSummaryService service = service();
        CachedTopicSummary cache = cachedSummary();
        when(cacheRepository.findFreshByTopicId(101L)).thenReturn(Optional.of(cache));

        TopicSummaryResponse response = service.summarize(new TopicSummaryRequest(101L), false);

        assertEquals("cached summary", response.summary());
        assertEquals(List.of("point"), response.points());
        assertEquals(77L, response.sessionId());
        assertEquals("summary-cache", response.toolCalls().get(0).toolName());
        verify(protectionService, never()).assertAllowed("SUMMARY");
        verifyNoInteractions(agentClient, auditService);
    }

    @Test
    void refreshBypassesFreshCacheLookup() {
        TopicSummaryService service = service();
        AgentAuditService.AuditSession session = new AgentAuditService.AuditSession(78L, "SUMMARY", true);
        TopicSummaryRequest request = new TopicSummaryRequest(101L);
        when(auditService.begin("SUMMARY", null)).thenReturn(session);
        when(agentClient.post("/agent/summarize-topic", request, TopicSummaryResponse.class))
                .thenReturn(new TopicSummaryResponse("new summary", null, null, null, null, null));
        when(currentUserProvider.currentUserId()).thenReturn(7L);

        TopicSummaryResponse response = service.summarize(request, true);

        assertEquals("new summary", response.summary());
        assertEquals(List.of(), response.points());
        assertEquals(78L, response.sessionId());
        verify(cacheRepository, never()).findFreshByTopicId(101L);
        verify(protectionService).recordSuccess("SUMMARY");
    }

    private TopicSummaryService service() {
        return new TopicSummaryService(
                agentClient,
                auditService,
                protectionService,
                cacheRepository,
                new ObjectMapper(),
                currentUserProvider
        );
    }

    private CachedTopicSummary cachedSummary() {
        return new CachedTopicSummary(
                1L,
                101L,
                "cached summary",
                "[\"point\"]",
                "[\"question\"]",
                77L,
                7L,
                Instant.parse("2026-08-19T00:00:00Z"),
                Instant.parse("2026-08-20T00:00:00Z")
        );
    }
}
