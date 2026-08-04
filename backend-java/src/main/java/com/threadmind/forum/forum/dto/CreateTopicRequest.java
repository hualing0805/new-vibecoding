package com.threadmind.forum.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTopicRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 20000) String content,
        List<String> tagNames
) {
}

