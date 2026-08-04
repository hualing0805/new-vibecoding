package com.threadmind.forum.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AgentDtos {

    private AgentDtos() {
    }

    public record AgentChatRequest(
            @NotBlank @Size(max = 4000) String message,
            Long categoryId,
            String sessionId
    ) {
    }

    public record CitationDto(
            Long topicId,
            String title
    ) {
    }

    public record ToolCallDto(
            String toolName,
            String status
    ) {
    }

    public record AgentChatResponse(
            String answer,
            List<CitationDto> citations,
            List<ToolCallDto> toolCalls
    ) {
    }

    public record PolishRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 20000) String content,
            String tone
    ) {
    }

    public record PolishResponse(
            String title,
            String content,
            List<String> notes
    ) {
    }

    public record TagSuggestRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 20000) String content,
            Long categoryId
    ) {
    }

    public record TagSuggestResponse(
            List<String> tags
    ) {
    }

    public record TopicSummaryRequest(
            @NotNull Long topicId
    ) {
    }

    public record TopicSummaryResponse(
            String summary,
            List<String> points,
            List<String> followUpQuestions
    ) {
    }

    public record ModerationCheckRequest(
            @NotBlank String targetType,
            @NotBlank @Size(max = 20000) String content
    ) {
    }

    public record ModerationCheckResponse(
            String riskLevel,
            List<String> reasons,
            String suggestedAction
    ) {
    }
}

