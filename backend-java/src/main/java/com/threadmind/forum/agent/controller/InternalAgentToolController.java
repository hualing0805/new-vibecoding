package com.threadmind.forum.agent.controller;

import com.threadmind.forum.agent.client.AgentProperties;
import com.threadmind.forum.agent.dto.AgentToolDtos.AgentTopicContextResponse;
import com.threadmind.forum.agent.dto.AgentToolDtos.AgentTopicSearchResponse;
import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.service.ForumService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/agent")
public class InternalAgentToolController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ForumService forumService;
    private final AgentProperties agentProperties;

    public InternalAgentToolController(ForumService forumService, AgentProperties agentProperties) {
        this.forumService = forumService;
        this.agentProperties = agentProperties;
    }

    @GetMapping("/topics/search")
    public ApiResponse<AgentTopicSearchResponse> searchTopics(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String token,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int limit
    ) {
        verifyInternalToken(token);
        return ApiResponse.ok(
                new AgentTopicSearchResponse(forumService.searchTopicsForAgent(keyword, limit)),
                TraceContext.currentTraceId()
        );
    }

    @GetMapping("/topics/{id}/context")
    public ApiResponse<AgentTopicContextResponse> topicContext(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String token,
            @PathVariable Long id
    ) {
        verifyInternalToken(token);
        return ApiResponse.ok(
                new AgentTopicContextResponse(
                        forumService.getTopicContextForAgent(id),
                        forumService.getRepliesForAgent(id)
                ),
                TraceContext.currentTraceId()
        );
    }

    private void verifyInternalToken(String token) {
        if (token == null || !token.equals(agentProperties.getInternalToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal tool token");
        }
    }
}
