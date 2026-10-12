package com.threadmind.forum.moderation.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationRecordDto;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import com.threadmind.forum.moderation.repository.ModerationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void createReportRejectsMissingTarget() {
        ModerationService service = service();
        ReportRequest request = new ReportRequest("TOPIC", 404L, "spam", "missing");
        when(repository.targetExists("TOPIC", 404L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.createReport(request));

        verify(repository, never()).createReport(7L, request);
    }

    @Test
    void ignoreReportDoesNotChangeTarget() {
        ModerationService service = service();
        ReportDto pending = report("PENDING");
        ReportDto rejected = report("REJECTED");
        HandleReportRequest request = new HandleReportRequest("IGNORE", "not actionable");
        ModerationRecordDto record = record("IGNORE");
        when(repository.findReport(1L)).thenReturn(Optional.of(pending)).thenReturn(Optional.of(rejected));
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(repository.createModerationRecord(1L, 7L, "TOPIC", 101L, request, ""))
                .thenReturn(record);

        var result = service.handleReport(1L, request);

        assertEquals("REJECTED", result.report().status());
        verify(repository).updateReportStatus(1L, "REJECTED");
        verify(repository, never()).applyTargetAction("TOPIC", 101L, "IGNORE");
    }

    @Test
    void acceptedReportAppliesTargetActionAndRecordsAudit() {
        ModerationService service = service();
        ReportDto pending = report("PENDING");
        ReportDto accepted = report("ACCEPTED");
        HandleReportRequest request = new HandleReportRequest("HIDE", "confirmed");
        ModerationRecordDto record = record("HIDE");
        when(repository.findReport(1L)).thenReturn(Optional.of(pending)).thenReturn(Optional.of(accepted));
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(repository.createModerationRecord(1L, 7L, "TOPIC", 101L, request, ""))
                .thenReturn(record);

        var result = service.handleReport(1L, request);

        assertEquals("ACCEPTED", result.report().status());
        verify(repository).applyTargetAction("TOPIC", 101L, "HIDE");
        verify(repository).updateReportStatus(1L, "ACCEPTED");
    }

    private ModerationService service() {
        return new ModerationService(repository, currentUserProvider);
    }

    private ReportDto report(String status) {
        return new ReportDto(1L, "TOPIC", 101L, "Topic", "spam", "detail", status, "MEDIUM", Instant.EPOCH);
    }

    private ModerationRecordDto record(String action) {
        return new ModerationRecordDto(2L, 1L, 7L, "TOPIC", 101L, action, "note", "", Instant.EPOCH);
    }
}
