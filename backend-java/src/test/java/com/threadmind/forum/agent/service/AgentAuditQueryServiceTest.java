package com.threadmind.forum.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionDetailDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionListItemDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionMessageDto;
import com.threadmind.forum.agent.repository.AgentAuditQueryRepository;
import com.threadmind.forum.agent.repository.AgentAuditQueryRepository.ToolCallRecord;
import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAuditQueryServiceTest {

    @Mock
    private AgentAuditQueryRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void listCurrentUserSessionsUsesCurrentUserAndClampsResponsePaging() {
        AgentAuditQueryService service = service();
        AiSessionListItemDto item = session(501L, 7L);

        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(repository.listUserSessions(7L, -3, 500)).thenReturn(List.of(item));
        when(repository.countUserSessions(7L)).thenReturn(1L);

        PageResponse<AiSessionListItemDto> response = service.listCurrentUserSessions(-3, 500);

        assertEquals(1, response.page());
        assertEquals(100, response.pageSize());
        assertEquals(1L, response.total());
        assertEquals(501L, response.items().get(0).sessionId());
    }

    @Test
    void getCurrentUserSessionReturnsNotFoundForOtherUsersSession() {
        AgentAuditQueryService service = service();

        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(repository.findUserSession(7L, 900L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getCurrentUserSession(900L));

        assertEquals(404, ex.getStatusCode().value());
        verify(repository, never()).listMessages(900L);
        verify(repository, never()).listToolCalls(900L);
    }

    @Test
    void getAdminSessionMapsMessagesAndToolCallSummaries() {
        AgentAuditQueryService service = service();

        when(repository.findAdminSession(501L)).thenReturn(Optional.of(session(501L, 7L)));
        when(repository.listMessages(501L)).thenReturn(List.of(new AiSessionMessageDto(
                91L,
                "ASSISTANT",
                "answer",
                Instant.parse("2026-08-19T00:00:02Z")
        )));
        when(repository.listToolCalls(501L)).thenReturn(List.of(new ToolCallRecord(
                301L,
                91L,
                "search_topics",
                "OK",
                18,
                "{\"summary\":\"keyword=Agent\"}",
                "{\"toolSummary\":\"found 2 topics\",\"error\":\"\"}",
                Instant.parse("2026-08-19T00:00:03Z")
        )));

        AiSessionDetailDto detail = service.getAdminSession(501L);

        assertEquals(501L, detail.session().sessionId());
        assertEquals("answer", detail.messages().get(0).content());
        assertEquals("keyword=Agent", detail.toolCalls().get(0).inputSummary());
        assertEquals("found 2 topics", detail.toolCalls().get(0).outputSummary());
        assertEquals("", detail.toolCalls().get(0).errorSummary());
    }

    @Test
    void getAdminSessionKeepsInvalidToolJsonAsSafeSummary() {
        AgentAuditQueryService service = service();

        when(repository.findAdminSession(501L)).thenReturn(Optional.of(session(501L, 7L)));
        when(repository.listMessages(501L)).thenReturn(List.of());
        when(repository.listToolCalls(501L)).thenReturn(List.of(new ToolCallRecord(
                302L,
                91L,
                "agent-python",
                "UNAVAILABLE",
                null,
                "{bad-json",
                "{bad-json",
                Instant.parse("2026-08-19T00:00:03Z")
        )));

        AiSessionDetailDto detail = service.getAdminSession(501L);

        assertEquals("JSON 解析失败", detail.toolCalls().get(0).inputSummary());
        assertEquals("JSON 解析失败", detail.toolCalls().get(0).outputSummary());
        assertEquals("", detail.toolCalls().get(0).errorSummary());
    }

    private AgentAuditQueryService service() {
        return new AgentAuditQueryService(repository, currentUserProvider, new ObjectMapper());
    }

    private AiSessionListItemDto session(Long sessionId, Long userId) {
        return new AiSessionListItemDto(
                sessionId,
                userId,
                "演示用户",
                "CHAT",
                "COMPLETED",
                "trace-1",
                Instant.parse("2026-08-19T00:00:00Z"),
                Instant.parse("2026-08-19T00:00:04Z"),
                2,
                1
        );
    }
}
