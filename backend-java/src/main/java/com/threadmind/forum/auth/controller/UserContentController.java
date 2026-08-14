package com.threadmind.forum.auth.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.dto.TopicSummaryDto;
import com.threadmind.forum.forum.service.ForumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserContentController {

    private final ForumService forumService;

    public UserContentController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<TopicSummaryDto>> favoriteTopics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(forumService.listFavoriteTopics(page, pageSize), TraceContext.currentTraceId());
    }
}
