package com.threadmind.forum.moderation.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.repository.ForumRepository;
import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationRecordDto;
import com.threadmind.forum.moderation.dto.ModerationResultResponse;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import com.threadmind.forum.moderation.repository.ModerationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ModerationService {

    private final ModerationRepository moderationRepository;
    private final ForumRepository forumRepository;
    private final CurrentUserProvider currentUserProvider;

    public ModerationService(
            ModerationRepository moderationRepository,
            ForumRepository forumRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.moderationRepository = moderationRepository;
        this.forumRepository = forumRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ReportDto createReport(ReportRequest request) {
        forumRepository.findTarget(request.targetType(), request.targetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reported target not found"));
        return moderationRepository.createReport(currentUserProvider.currentUserId(), request);
    }

    public List<ReportDto> listReports(String status) {
        return moderationRepository.listReports(status);
    }

    @Transactional
    public ModerationResultResponse handleReport(Long reportId, HandleReportRequest request) {
        ReportDto report = moderationRepository.findReport(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        String action = request.action().trim().toUpperCase(Locale.ROOT);
        String finalStatus = "IGNORE".equals(action) ? "REJECTED" : "ACCEPTED";
        moderationRepository.updateReportStatus(reportId, finalStatus);
        if (!"IGNORE".equals(action)) {
            forumRepository.applyModerationAction(report.targetType(), report.targetId(), action);
        }
        ModerationRecordDto record = moderationRepository.createModerationRecord(
                reportId,
                currentUserProvider.currentUserId(),
                report.targetType(),
                report.targetId(),
                request,
                TraceContext.currentTraceId()
        );
        ReportDto updated = moderationRepository.findReport(reportId).orElseThrow();
        return new ModerationResultResponse(updated, record);
    }
}
