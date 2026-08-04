package com.threadmind.forum.interaction.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.service.ForumDemoService;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InteractionController {

    private final ForumDemoService forumDemoService;

    public InteractionController(ForumDemoService forumDemoService) {
        this.forumDemoService = forumDemoService;
    }

    @PostMapping("/topics/{id}/like")
    public ApiResponse<InteractionStatusResponse> likeTopic(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.likeTopic(id), TraceContext.currentTraceId());
    }

    @DeleteMapping("/topics/{id}/like")
    public ApiResponse<InteractionStatusResponse> unlikeTopic(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.unlikeTopic(id), TraceContext.currentTraceId());
    }

    @PostMapping("/replies/{id}/like")
    public ApiResponse<InteractionStatusResponse> likeReply(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.likeReply(id), TraceContext.currentTraceId());
    }

    @DeleteMapping("/replies/{id}/like")
    public ApiResponse<InteractionStatusResponse> unlikeReply(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.unlikeReply(id), TraceContext.currentTraceId());
    }

    @PostMapping("/topics/{id}/favorite")
    public ApiResponse<InteractionStatusResponse> favoriteTopic(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.favoriteTopic(id), TraceContext.currentTraceId());
    }

    @DeleteMapping("/topics/{id}/favorite")
    public ApiResponse<InteractionStatusResponse> unfavoriteTopic(@PathVariable Long id) {
        return ApiResponse.ok(forumDemoService.unfavoriteTopic(id), TraceContext.currentTraceId());
    }
}

