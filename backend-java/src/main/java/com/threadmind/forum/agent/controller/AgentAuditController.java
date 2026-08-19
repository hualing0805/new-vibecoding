package com.threadmind.forum.agent.controller;

import com.threadmind.forum.agent.dto.AgentAuditDtos.AiAuditMetricsDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionDetailDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionListItemDto;
import com.threadmind.forum.agent.service.AgentAuditQueryService;
import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.web.TraceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentAuditController {

    private final AgentAuditQueryService service;

    public AgentAuditController(AgentAuditQueryService service) {
        this.service = service;
    }

    @GetMapping("/admin/ai/sessions")
    public ApiResponse<PageResponse<AiSessionListItemDto>> listAdminSessions(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.listAdminSessions(scene, status, page, pageSize), TraceContext.currentTraceId());
    }

    @GetMapping("/admin/ai/sessions/{id}")
    public ApiResponse<AiSessionDetailDto> getAdminSession(@PathVariable Long id) {
        return ApiResponse.ok(service.getAdminSession(id), TraceContext.currentTraceId());
    }

    @GetMapping("/admin/ai/metrics")
    public ApiResponse<AiAuditMetricsDto> metrics() {
        return ApiResponse.ok(service.metricsToday(), TraceContext.currentTraceId());
    }

    @GetMapping("/users/me/ai/sessions")
    public ApiResponse<PageResponse<AiSessionListItemDto>> listMySessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(service.listCurrentUserSessions(page, pageSize), TraceContext.currentTraceId());
    }

    @GetMapping("/users/me/ai/sessions/{id}")
    public ApiResponse<AiSessionDetailDto> getMySession(@PathVariable Long id) {
        return ApiResponse.ok(service.getCurrentUserSession(id), TraceContext.currentTraceId());
    }
}
