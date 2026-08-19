package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.client.AgentClient;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatRequest;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatResponse;
import com.threadmind.forum.agent.dto.AgentDtos.ToolCallDto;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository;
import com.threadmind.forum.agent.repository.TopicSummaryCacheRepository.CachedTopicSummary;
import com.threadmind.forum.agent.service.AgentAuditService.AuditSession;
import com.threadmind.forum.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentClient agentClient;

    @Mock
    private AgentAuditService agentAuditService;

    @Mock
    private AgentProtectionService agentProtectionService;

    @Mock
    private TopicSummaryCacheRepository topicSummaryCacheRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void chatAddsAuditSessionIdAndRecordsSuccess() {
        AgentService service = service();
        AgentChatRequest request = new AgentChatRequest("hello", null, null);
        AuditSession session = new AuditSession(55L, "CHAT", true);

        when(agentAuditService.begin("CHAT", null)).thenReturn(session);
        when(agentAuditService.recordAssistantMessage(same(session), eq("answer"))).thenReturn(92L);
        when(agentClient.post(eq("/agent/chat"), same(request), eq(AgentChatResponse.class)))
                .thenReturn(new AgentChatResponse(
                        "answer",
                        List.of(),
                        List.of(new ToolCallDto("search_topics", "OK", 12, "found 1 topic", null))
                ));

        AgentChatResponse response = service.chat(request);

        assertEquals(55L, response.sessionId());
        assertEquals("answer", response.answer());
        verify(agentProtectionService).assertAllowed("CHAT");
        verify(agentAuditService).recordUserMessage(same(session), eq("message=hello"));
        verify(agentAuditService).recordToolCalls(same(session), eq(92L), any(), eq("message=hello"), eq("answer; citations=0"));
        verify(agentAuditService).finish(same(session), eq("COMPLETED"));
        verify(agentProtectionService).recordSuccess("CHAT");
    }

    @Test
    void chatRecordsFallbackWhenAgentUnavailable() {
        AgentService service = service();
        AgentChatRequest request = new AgentChatRequest("hello", null, null);
        AuditSession session = new AuditSession(56L, "CHAT", true);

        when(agentAuditService.begin("CHAT", null)).thenReturn(session);
        when(agentAuditService.recordAssistantMessage(same(session), any())).thenReturn(93L);
        when(agentClient.post(eq("/agent/chat"), same(request), eq(AgentChatResponse.class)))
                .thenThrow(new RestClientException("down"));

        AgentChatResponse response = service.chat(request);

        assertEquals(56L, response.sessionId());
        assertEquals("UNAVAILABLE", response.toolCalls().get(0).status());
        verify(agentProtectionService).recordFailure("CHAT");
        verify(agentAuditService).recordToolFailure(same(session), eq(93L), eq("agent-python"), eq("message=hello"), any(), any());
        verify(agentAuditService).finish(same(session), eq("FALLBACK"));
    }

    @Test
    void summarizeTopicUsesFreshCacheWithoutCallingAgent() {
        AgentService service = service();
        CachedTopicSummary cache = new CachedTopicSummary(
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
        when(topicSummaryCacheRepository.findFreshByTopicId(101L)).thenReturn(Optional.of(cache));

        TopicSummaryResponse response = service.summarizeTopic(new TopicSummaryRequest(101L));

        assertEquals("cached summary", response.summary());
        assertEquals(List.of("point"), response.points());
        assertEquals(77L, response.sessionId());
        assertEquals("summary-cache", response.toolCalls().get(0).toolName());
        verify(agentProtectionService, never()).assertAllowed("SUMMARY");
        verifyNoInteractions(agentClient);
    }

    private AgentService service() {
        return new AgentService(
                agentClient,
                agentAuditService,
                agentProtectionService,
                topicSummaryCacheRepository,
                new ObjectMapper(),
                currentUserProvider
        );
    }
}
