package com.threadmind.forum.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 128) String reason,
        @Size(max = 1000) String detail
) {
}

