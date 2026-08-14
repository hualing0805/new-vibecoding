package com.threadmind.forum.interaction.service;

import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import com.threadmind.forum.interaction.repository.InteractionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public InteractionService(InteractionRepository interactionRepository, CurrentUserProvider currentUserProvider) {
        this.interactionRepository = interactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public InteractionStatusResponse likeTopic(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.likeTopic(currentUserProvider.currentUserId(), topicId);
    }

    @Transactional
    public InteractionStatusResponse unlikeTopic(Long topicId) {
        requireTopic(topicId);
        return interactionRepository.unlikeTopic(currentUserProvider.currentUserId(), topicId);
    }

    @Transactional
    public InteractionStatusResponse likeReply(Long replyId) {
        requireReply(replyId);
        return interactionRepository.likeReply(currentUserProvider.currentUserId(), replyId);
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
}
