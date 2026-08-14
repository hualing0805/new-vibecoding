package com.threadmind.forum.notification.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.notification.dto.NotificationDto;
import com.threadmind.forum.notification.dto.NotificationUnreadCountResponse;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void unreadCountUsesCurrentUser() {
        NotificationService service = new NotificationService(notificationRepository, currentUserProvider);

        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(notificationRepository.countUnreadByUser(7L)).thenReturn(3L);

        NotificationUnreadCountResponse response = service.unreadCount();

        assertEquals(3L, response.unreadCount());
    }

    @Test
    void markReadUsesCurrentUser() {
        NotificationService service = new NotificationService(notificationRepository, currentUserProvider);
        NotificationDto notification = new NotificationDto(
                301L,
                "SYSTEM",
                "系统通知",
                "已经标记已读。",
                true,
                Instant.parse("2026-08-14T00:00:00Z")
        );

        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(notificationRepository.markRead(7L, 301L)).thenReturn(Optional.of(notification));

        NotificationDto response = service.markRead(301L);

        assertEquals(301L, response.id());
        assertEquals(true, response.read());
    }
}
