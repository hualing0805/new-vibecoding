package com.threadmind.forum.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReplyRequest(
        @NotBlank @Size(max = 10000) String content
) {
}

