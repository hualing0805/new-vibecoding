package com.threadmind.forum.forum.service;

import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationResultResponse;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumDemoServiceTest {

    private final ForumDemoService service = new ForumDemoService();

    @Test
    void createTopicThenReply() {
        Long topicId = service.createTopic(new CreateTopicRequest(1L, "模块边界说明", "正文内容需要可以被回复。", List.of("Java"))).id();

        service.createReply(topicId, new CreateReplyRequest("这是一条回复。"));

        assertEquals(1, service.listReplies(topicId).size());
        assertEquals(1, service.getTopic(topicId).replyCount());
    }

    @Test
    void topicLikeAndFavoriteAreStateful() {
        InteractionStatusResponse liked = service.likeTopic(101L);
        InteractionStatusResponse favorited = service.favoriteTopic(101L);

        assertTrue(liked.liked());
        assertTrue(favorited.favorited());
    }

    @Test
    void reportCanBeHandledWithAuditRecord() {
        ReportDto report = service.createReport(new ReportRequest("TOPIC", 101L, "标题不清晰", "需要管理员复核。"));

        ModerationResultResponse result = service.handleReport(report.id(), new HandleReportRequest("HIDE", "演示处理"));

        assertEquals("ACCEPTED", result.report().status());
        assertEquals("HIDE", result.record().action());
    }
}

