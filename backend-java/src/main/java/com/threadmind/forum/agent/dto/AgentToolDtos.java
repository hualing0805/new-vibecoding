package com.threadmind.forum.agent.dto;

import com.threadmind.forum.forum.dto.ReplyDto;
import com.threadmind.forum.forum.dto.TopicDetailDto;
import com.threadmind.forum.forum.dto.TopicSummaryDto;

import java.util.List;

public final class AgentToolDtos {

    private AgentToolDtos() {
    }

    public record AgentTopicSearchResponse(
            List<TopicSummaryDto> topics
    ) {
    }

    public record AgentTopicContextResponse(
            TopicDetailDto topic,
            List<ReplyDto> replies
    ) {
    }
}

