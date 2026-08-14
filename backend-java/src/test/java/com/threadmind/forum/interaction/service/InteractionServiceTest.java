package com.threadmind.forum.interaction.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import com.threadmind.forum.interaction.repository.InteractionRepository;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void likeTopicNotifiesAuthorOnlyOnFirstLike() {
        InteractionService service = new InteractionService(interactionRepository, currentUserProvider, notificationRepository);

        when(interactionRepository.topicExists(101L)).thenReturn(true);
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(interactionRepository.getTopicStatus(7L, 101L))
                .thenReturn(new InteractionStatusResponse("TOPIC", 101L, false, false, 0, 0));
        when(interactionRepository.likeTopic(7L, 101L))
                .thenReturn(new InteractionStatusResponse("TOPIC", 101L, true, false, 1, 0));
        when(interactionRepository.findTopicAuthorId(101L)).thenReturn(Optional.of(2L));

        InteractionStatusResponse response = service.likeTopic(101L);

        assertEquals(1, response.likeCount());
        verify(notificationRepository).createNotification(
                2L,
                "TOPIC_LIKED",
                "收到新的点赞",
                "有人点赞了你的主题。"
        );
    }

    @Test
    void likeTopicDoesNotNotifySelf() {
        InteractionService service = new InteractionService(interactionRepository, currentUserProvider, notificationRepository);

        when(interactionRepository.topicExists(101L)).thenReturn(true);
        when(currentUserProvider.currentUserId()).thenReturn(7L);
        when(interactionRepository.getTopicStatus(7L, 101L))
                .thenReturn(new InteractionStatusResponse("TOPIC", 101L, false, false, 0, 0));
        when(interactionRepository.likeTopic(7L, 101L))
                .thenReturn(new InteractionStatusResponse("TOPIC", 101L, true, false, 1, 0));
        when(interactionRepository.findTopicAuthorId(101L)).thenReturn(Optional.of(7L));

        service.likeTopic(101L);

        verify(notificationRepository, never()).createNotification(
                7L,
                "TOPIC_LIKED",
                "收到新的点赞",
                "有人点赞了你的主题。"
        );
    }
}
