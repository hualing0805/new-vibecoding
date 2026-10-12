package com.threadmind.forum.forum.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.forum.dto.CreatedResourceResponse;
import com.threadmind.forum.forum.repository.ForumRepository;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock
    private ForumRepository forumRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void createTopicUsesCurrentUserAndCreatesNotification() {
        ForumService service = new ForumService(forumRepository, notificationRepository, currentUserProvider);
        CreateTopicRequest request = new CreateTopicRequest(1L, "M2.5 验收", "真实发帖闭环", List.of("M2.5"));

        when(forumRepository.categoryExists(1L)).thenReturn(true);
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(forumRepository.createTopic(request, 7L)).thenReturn(101L);

        CreatedResourceResponse response = service.createTopic(request);

        assertEquals(101L, response.id());
        verify(currentUserProvider, times(1)).currentUserId();
        verify(forumRepository).createTopic(request, 7L);
        verify(notificationRepository).createNotification(
                7L,
                "TOPIC_CREATED",
                "Topic published",
                "Your topic has been saved to the forum."
        );
    }

    @Test
    void createTopicRejectsMissingCategory() {
        ForumService service = new ForumService(forumRepository, notificationRepository, currentUserProvider);
        CreateTopicRequest request = new CreateTopicRequest(99L, "M2.5 验收", "不存在的板块", List.of());

        when(forumRepository.categoryExists(99L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.createTopic(request));
        verify(forumRepository, never()).createTopic(request, 7L);
    }

    @Test
    void createReplyUsesCurrentUserAndNotifiesTopicAuthor() {
        ForumService service = new ForumService(forumRepository, notificationRepository, currentUserProvider);
        CreateReplyRequest request = new CreateReplyRequest("验收回复");

        when(forumRepository.findTopicAuthorId(101L)).thenReturn(Optional.of(2L));
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(forumRepository.createReply(101L, request, 7L)).thenReturn(202L);

        CreatedResourceResponse response = service.createReply(101L, request);

        assertEquals(202L, response.id());
        verify(forumRepository).createReply(101L, request, 7L);
        verify(notificationRepository).createNotification(
                2L,
                "REPLY_CREATED",
                "New reply",
                "Someone replied to your topic."
        );
    }

    @Test
    void createReplyDoesNotNotifyAuthorWhenReplyingToOwnTopic() {
        ForumService service = new ForumService(forumRepository, notificationRepository, currentUserProvider);
        CreateReplyRequest request = new CreateReplyRequest("自回复");

        when(forumRepository.findTopicAuthorId(101L)).thenReturn(Optional.of(7L));
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(forumRepository.createReply(101L, request, 7L)).thenReturn(203L);

        CreatedResourceResponse response = service.createReply(101L, request);

        assertEquals(203L, response.id());
        verify(notificationRepository, never()).createNotification(
                7L,
                "REPLY_CREATED",
                "New reply",
                "Someone replied to your topic."
        );
    }
}
