package com.threadmind.forum.interaction.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import com.threadmind.forum.interaction.repository.InteractionRepository;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationRepository notificationRepository;

    public InteractionService(
            InteractionRepository interactionRepository,
            CurrentUserProvider currentUserProvider,
            NotificationRepository notificationRepository
    ) {
        this.interactionRepository = interactionRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationRepository = notificationRepository;
    }

    public InteractionStatusResponse getTopicStatus(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.getTopicStatus(currentUserProvider.currentUserId(), topicId);
    }

    public InteractionStatusResponse getReplyStatus(Long replyId) {
        requireReply(replyId);
        return interactionRepository.getReplyStatus(currentUserProvider.currentUserId(), replyId);
    }

    @Transactional
    public InteractionStatusResponse likeTopic(Long topicId) {
        requireTopic(topicId);
        Long userId = currentUserProvider.currentUserId();
        boolean alreadyLiked = interactionRepository.getTopicStatus(userId, topicId).liked();
        InteractionStatusResponse status = interactionRepository.likeTopic(userId, topicId);
        if (!alreadyLiked) {
            notifyTopicAuthor(topicId, userId, "TOPIC_LIKED", "收到新的点赞", "有人点赞了你的主题。");
        }
        return status;
    }

    @Transactional
    public InteractionStatusResponse unlikeTopic(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.unlikeTopic(currentUserProvider.currentUserId(), topicId);
    }

    @Transactional
    public InteractionStatusResponse likeReply(Long replyId) {
        requireReply(replyId);
        Long userId = currentUserProvider.currentUserId();
        boolean alreadyLiked = interactionRepository.getReplyStatus(userId, replyId).liked();
        InteractionStatusResponse status = interactionRepository.likeReply(userId, replyId);
        if (!alreadyLiked) {
            notifyReplyAuthor(replyId, userId, "REPLY_LIKED", "收到新的点赞", "有人点赞了你的回复。");
        }
        return status;
    }

    @Transactional
    public InteractionStatusResponse unlikeReply(Long replyId) {
        requireReply(replyId);
        return interactionRepository.unlikeReply(currentUserProvider.currentUserId(), replyId);
    }

    @Transactional
    public InteractionStatusResponse favoriteTopic(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.favoriteTopic(currentUserProvider.currentUserId(), topicId);
    }

    @Transactional
    public InteractionStatusResponse unfavoriteTopic(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.unfavoriteTopic(currentUserProvider.currentUserId(), topicId);
    }

    private void requireTopic(Long topicId) {
        if (!interactionRepository.topicExists(topicId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
        }
    }

    private void requireReply(Long replyId) {
        if (!interactionRepository.replyExists(replyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reply not found");
        }
    }

    private void notifyTopicAuthor(Long topicId, Long actorId, String type, String title, String body) {
        interactionRepository.findTopicAuthorId(topicId)
                .filter(authorId -> !authorId.equals(actorId))
                .ifPresent(authorId -> notificationRepository.createNotification(authorId, type, title, body));
    }

    private void notifyReplyAuthor(Long replyId, Long actorId, String type, String title, String body) {
        interactionRepository.findReplyAuthorId(replyId)
                .filter(authorId -> !authorId.equals(actorId))
                .ifPresent(authorId -> notificationRepository.createNotification(authorId, type, title, body));
    }
}
