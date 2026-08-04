package com.threadmind.forum.agent.service;

import com.threadmind.forum.agent.client.AgentClient;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatRequest;
import com.threadmind.forum.agent.dto.AgentDtos.AgentChatResponse;
import com.threadmind.forum.agent.dto.AgentDtos.CitationDto;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckRequest;
import com.threadmind.forum.agent.dto.AgentDtos.ModerationCheckResponse;
import com.threadmind.forum.agent.dto.AgentDtos.PolishRequest;
import com.threadmind.forum.agent.dto.AgentDtos.PolishResponse;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TagSuggestResponse;
import com.threadmind.forum.agent.dto.AgentDtos.ToolCallDto;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryRequest;
import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class AgentService {

    private final AgentClient agentClient;

    public AgentService(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        try {
            return agentClient.post("/agent/chat", request, AgentChatResponse.class);
        } catch (RestClientException ex) {
            return new AgentChatResponse(
                    "Agent 服务暂时不可用。论坛基础功能不受影响，可以稍后再试。",
                    List.of(),
                    List.of(new ToolCallDto("agent-python", "UNAVAILABLE"))
            );
        }
    }

    public PolishResponse polish(PolishRequest request) {
        try {
            return agentClient.post("/agent/polish", request, PolishResponse.class);
        } catch (RestClientException ex) {
            return new PolishResponse(
                    request.title(),
                    request.content(),
                    List.of("Agent 服务暂时不可用，已保留原文。")
            );
        }
    }

    public TagSuggestResponse suggestTags(TagSuggestRequest request) {
        try {
            return agentClient.post("/agent/suggest-tags", request, TagSuggestResponse.class);
        } catch (RestClientException ex) {
            return new TagSuggestResponse(List.of("Java", "Vue3", "Agent"));
        }
    }

    public TopicSummaryResponse summarizeTopic(TopicSummaryRequest request) {
        try {
            return agentClient.post("/agent/summarize-topic", request, TopicSummaryResponse.class);
        } catch (RestClientException ex) {
            return new TopicSummaryResponse(
                    "Agent 服务暂时不可用，当前显示本地占位摘要。",
                    List.of("Java 后端保留业务边界", "Python Agent 负责建议生成"),
                    List.of("是否需要给该主题生成可引用摘要？")
            );
        }
    }

    public ModerationCheckResponse moderationCheck(ModerationCheckRequest request) {
        try {
            return agentClient.post("/agent/moderation-check", request, ModerationCheckResponse.class);
        } catch (RestClientException ex) {
            return new ModerationCheckResponse("UNKNOWN", List.of("Agent 服务暂时不可用"), "REVIEW");
        }
    }
}

