package com.threadmind.forum.forum.service;

import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.forum.dto.CategoryDto;
import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.forum.dto.CreatedResourceResponse;
import com.threadmind.forum.forum.dto.ReplyDto;
import com.threadmind.forum.forum.dto.TopicDetailDto;
import com.threadmind.forum.forum.dto.TopicSummaryDto;
import com.threadmind.forum.forum.repository.ForumRepository;
import com.threadmind.forum.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ForumService {

    private final ForumRepository forumRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public ForumService(
            ForumRepository forumRepository,
            NotificationRepository notificationRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.forumRepository = forumRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<CategoryDto> listCategories() {
        return forumRepository.listCategories();
    }

    public PageResponse<TopicSummaryDto> listTopics(Long categoryId, String keyword, String sort, int page, int pageSize) {
        return forumRepository.listTopics(categoryId, keyword, sort, page, pageSize);
    }

    @Transactional
    public CreatedResourceResponse createTopic(CreateTopicRequest request) {
        if (!forumRepository.categoryExists(request.categoryId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        Long topicId = forumRepository.createTopic(request, currentUserProvider.currentUserId());
        notificationRepository.createNotification(
                currentUserProvider.currentUserId(),
                "TOPIC_CREATED",
                "Topic published",
                "Your topic has been saved to the forum."
        );
        return new CreatedResourceResponse(topicId);
    }

    public TopicDetailDto getTopic(Long id) {
        return forumRepository.findTopicDetail(id, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    public List<ReplyDto> listReplies(Long topicId) {
        if (!forumRepository.topicExists(topicId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
        }
        return forumRepository.listReplies(topicId);
    }

    @Transactional
    public CreatedResourceResponse createReply(Long topicId, CreateReplyRequest request) {
        Long topicAuthorId = forumRepository.findTopicAuthorId(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        Long replyId = forumRepository.createReply(topicId, request, currentUserProvider.currentUserId());
        notificationRepository.createNotification(
                topicAuthorId,
                "REPLY_CREATED",
                "New reply",
                "Someone replied to your topic."
        );
        return new CreatedResourceResponse(replyId);
    }

    public List<TopicSummaryDto> searchTopicsForAgent(String keyword, int limit) {
        return forumRepository.searchTopicsForAgent(keyword, limit);
    }

    public TopicDetailDto getTopicContextForAgent(Long topicId) {
        return forumRepository.findTopicDetail(topicId, false)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    public List<ReplyDto> getRepliesForAgent(Long topicId) {
        return listReplies(topicId);
    }
}
