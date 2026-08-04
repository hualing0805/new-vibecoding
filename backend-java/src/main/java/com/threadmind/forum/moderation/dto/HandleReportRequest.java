package com.threadmind.forum.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HandleReportRequest(
        @NotBlank String action,
        @Size(max = 1000) String note
) {
}

