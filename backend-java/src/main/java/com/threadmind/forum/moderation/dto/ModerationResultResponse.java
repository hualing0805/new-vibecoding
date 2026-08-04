package com.threadmind.forum.moderation.dto;

public record ModerationResultResponse(
        ReportDto report,
        ModerationRecordDto record
) {
}

