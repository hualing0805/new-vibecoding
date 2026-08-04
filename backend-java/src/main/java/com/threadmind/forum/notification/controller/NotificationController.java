package com.threadmind.forum.notification.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.service.ForumDemoService;
import com.threadmind.forum.notification.dto.NotificationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ForumDemoService forumDemoService;

    public NotificationController(ForumDemoService forumDemoService) {
        this.forumDemoService = forumDemoService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDto>> listNotifications() {
        return ApiResponse.ok(forumDemoService.listNotifications(), TraceContext.currentTraceId());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.markNotificationRead(id), TraceContext.currentTraceId());
    }
}

