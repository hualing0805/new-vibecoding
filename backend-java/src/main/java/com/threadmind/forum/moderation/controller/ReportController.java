package com.threadmind.forum.moderation.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationResultResponse;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import com.threadmind.forum.moderation.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/api/reports")
    public ApiResponse<ReportDto> createReport(@Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(moderationService.createReport(request), TraceContext.currentTraceId());
    }

    @GetMapping("/api/admin/reports")
    public ApiResponse<List<ReportDto>> listReports(@RequestParam(required = false) String status) {
        return ApiResponse.ok(moderationService.listReports(status), TraceContext.currentTraceId());
    }

    @PostMapping("/api/admin/reports/{id}/handle")
    public ApiResponse<ModerationResultResponse> handleReport(
            @PathVariable Long id,
            @Valid @RequestBody HandleReportRequest request
    ) {
        return ApiResponse.ok(moderationService.handleReport(id, request), TraceContext.currentTraceId());
    }
}
