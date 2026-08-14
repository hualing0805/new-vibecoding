package com.threadmind.forum.notification.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.notification.dto.NotificationDto;
import com.threadmind.forum.notification.dto.NotificationUnreadCountResponse;
import com.threadmind.forum.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDto>> listNotifications() {
        return ApiResponse.ok(notificationService.listNotifications(), TraceContext.currentTraceId());
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> unreadCount() {
        return ApiResponse.ok(notificationService.unreadCount(), TraceContext.currentTraceId());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id), TraceContext.currentTraceId());
    }
}
