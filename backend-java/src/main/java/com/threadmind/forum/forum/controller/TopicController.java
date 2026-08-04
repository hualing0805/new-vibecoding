package com.threadmind.forum.forum.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.forum.dto.CreatedResourceResponse;
import com.threadmind.forum.forum.dto.ReplyDto;
import com.threadmind.forum.forum.dto.TopicDetailDto;
import com.threadmind.forum.forum.dto.TopicSummaryDto;
import com.threadmind.forum.forum.service.ForumDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final ForumDemoService forumDemoService;

    public TopicController(ForumDemoService forumDemoService) {
        this.forumDemoService = forumDemoService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TopicSummaryDto>> listTopics(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(
                forumDemoService.listTopics(categoryId, keyword, sort, page, pageSize),
                TraceContext.currentTraceId()
        );
    }

    @PostMapping
    public ApiResponse<CreatedResourceResponse> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.ok(forumDemoService.createTopic(request), TraceContext.currentTraceId());
    }

    @GetMapping("/{id}")
    public ApiResponse<TopicDetailDto> getTopic(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.getTopic(id), TraceContext.currentTraceId());
    }

    @GetMapping("/{id}/replies")
    public ApiResponse<List<ReplyDto>> listReplies(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.listReplies(id), TraceContext.currentTraceId());
    }

    @PostMapping("/{id}/replies")
    public ApiResponse<CreatedResourceResponse> createReply(
            @PathVariable Long id,
            @Valid @RequestBody CreateReplyRequest request
    ) {
        return ApiResponse.ok(forumDemoService.createReply(id, request), TraceContext.currentTraceId());
    }
}

