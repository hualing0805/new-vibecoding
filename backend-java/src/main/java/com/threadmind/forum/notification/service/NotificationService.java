package com.threadmind.forum.notification.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.notification.dto.NotificationDto;
import com.threadmind.forum.notification.dto.NotificationUnreadCountResponse;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<NotificationDto> listNotifications() {
        return notificationRepository.listByUser(currentUserProvider.currentUserId());
    }

    public NotificationUnreadCountResponse unreadCount() {
        return new NotificationUnreadCountResponse(
                notificationRepository.countUnreadByUser(currentUserProvider.currentUserId())
        );
    }

    public NotificationDto markRead(Long id) {
        return notificationRepository.markRead(currentUserProvider.currentUserId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }
}
