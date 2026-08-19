package com.threadmind.forum.agent.controller;

import com.threadmind.forum.agent.dto.AgentDtos.AgentChatRequest;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatResponse;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckRequest;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckResponse;
import com.threadmind.forum.agent.dto.AgentDtos.PolishRequest;
import com.threadmind.forum.agent.dto.AgentDtos.PolishResponse;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestResponse;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import com.threadmind.forum.agent.service.AgentService;
import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.ok(agentService.chat(request), TraceContext.currentTraceId());
    }

    @PostMapping("/polish")
    public ApiResponse<PolishResponse> polish(@Valid @RequestBody PolishRequest request) {
        return ApiResponse.ok(agentService.polish(request), TraceContext.currentTraceId());
    }

    @PostMapping("/suggest-tags")
    public ApiResponse<TagSuggestResponse> suggestTags(@Valid @RequestBody TagSuggestRequest request) {
        return ApiResponse.ok(agentService.suggestTags(request), TraceContext.currentTraceId());
    }

    @PostMapping("/summarize-topic")
    public ApiResponse<TopicSummaryResponse> summarizeTopic(@Valid @RequestBody TopicSummaryRequest request) {
        return ApiResponse.ok(agentService.summarizeTopic(request), TraceContext.currentTraceId());
    }

    @PostMapping("/summarize-topic/refresh")
    public ApiResponse<TopicSummaryResponse> refreshTopicSummary(@Valid @RequestBody TopicSummaryRequest request) {
        return ApiResponse.ok(agentService.summarizeTopic(request, true), TraceContext.currentTraceId());
    }

    @PostMapping("/moderation-check")
    public ApiResponse<ModerationCheckResponse> moderationCheck(@Valid @RequestBody ModerationCheckRequest request) {
        return ApiResponse.ok(agentService.moderationCheck(request), TraceContext.currentTraceId());
    }
}
