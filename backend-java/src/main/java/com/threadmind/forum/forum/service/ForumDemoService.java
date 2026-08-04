package com.threadmind.forum.forum.service;

import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.dto.AuthorDto;
import com.threadmind.forum.forum.dto.CategoryDto;
import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.forum.dto.CreatedResourceResponse;
import com.threadmind.forum.forum.dto.ReplyDto;
import com.threadmind.forum.forum.dto.TopicDetailDto;
import com.threadmind.forum.forum.dto.TopicSummaryDto;
import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationRecordDto;
import com.threadmind.forum.moderation.dto.ModerationResultResponse;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import com.threadmind.forum.notification.dto.NotificationDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ForumDemoService {

    private static final String TOPIC = "TOPIC";
    private static final String REPLY = "REPLY";

    private final AtomicLong idSequence = new AtomicLong(1000L);
    private final Object stateLock = new Object();

    private final List<CategoryDto> categories = new ArrayList<>();
    private final Map<Long, TopicState> topics = new LinkedHashMap<>();
    private final Map<Long, List<ReplyDto>> repliesByTopic = new LinkedHashMap<>();
    private final Map<Long, ReplyDto> replies = new LinkedHashMap<>();
    private final Set<Long> likedTopics = new HashSet<>();
    private final Set<Long> likedReplies = new HashSet<>();
    private final Set<Long> favoriteTopics = new HashSet<>();
    private final Map<Long, ReportDto> reports = new LinkedHashMap<>();
    private final List<NotificationDto> notifications = new CopyOnWriteArrayList<>();
    private final List<ModerationRecordDto> moderationRecords = new CopyOnWriteArrayList<>();

    private final AuthorDto currentUser = new AuthorDto(1L, "演示用户", "");
    private final AuthorDto beiqiao = new AuthorDto(9L, "北桥", "");
    private final AuthorDto lin = new AuthorDto(12L, "林同学", "");

    public ForumDemoService() {
        seedCategories();
        seedTopicsAndReplies();
        seedNotificationsAndReports();
    }

    public List<CategoryDto> listCategories() {
        synchronized (stateLock) {
            return List.copyOf(categories);
        }
    }

    public PageResponse<TopicSummaryDto> listTopics(Long categoryId, String keyword, String sort, int page, int pageSize) {
        List<TopicSummaryDto> filtered;
        synchronized (stateLock) {
            filtered = topics.values()
                    .stream()
                    .map(TopicState::summary)
                    .filter(topic -> "PUBLISHED".equals(topic.status()) || "LOCKED".equals(topic.status()))
                    .filter(topic -> categoryId == null || topic.category().id().equals(categoryId))
                    .filter(topic -> matchesKeyword(topic, keyword))
                    .sorted(topicComparator(sort))
                    .toList();
        }
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int from = Math.min((safePage - 1) * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), safePage, safePageSize, filtered.size());
    }

    public TopicDetailDto getTopic(Long id) {
        synchronized (stateLock) {
            TopicState state = requireTopic(id);
            TopicSummaryDto summary = state.summary();
            TopicSummaryDto viewed = copyTopic(summary, summary.replyCount(), summary.likeCount(), summary.favoriteCount(), summary.viewCount() + 1, summary.status());
            topics.put(id, new TopicState(viewed, state.content(), state.aiSummary()));
            return toDetail(topics.get(id));
        }
    }

    public CreatedResourceResponse createTopic(CreateTopicRequest request) {
        synchronized (stateLock) {
            CategoryDto category = requireCategory(request.categoryId());
            Long id = idSequence.incrementAndGet();
            TopicSummaryDto summary = new TopicSummaryDto(
                    id,
                    request.title().trim(),
                    excerpt(request.content()),
                    category,
                    currentUser,
                    normalizeTags(request.tagNames()),
                    0,
                    0,
                    0,
                    0,
                    "PUBLISHED",
                    Instant.now()
            );
            topics.put(id, new TopicState(summary, request.content().trim(), "摘要待生成。"));
            repliesByTopic.put(id, new ArrayList<>());
            notifications.add(new NotificationDto(
                    idSequence.incrementAndGet(),
                    "TOPIC_CREATED",
                    "主题已发布",
                    "《" + summary.title() + "》已经进入 " + category.name() + "。",
                    false,
                    Instant.now()
            ));
            return new CreatedResourceResponse(id);
        }
    }

    public List<ReplyDto> listReplies(Long topicId) {
        synchronized (stateLock) {
            requireTopic(topicId);
            return List.copyOf(repliesByTopic.getOrDefault(topicId, List.of()));
        }
    }

    public CreatedResourceResponse createReply(Long topicId, CreateReplyRequest request) {
        synchronized (stateLock) {
            TopicState state = requireTopic(topicId);
            Long id = idSequence.incrementAndGet();
            ReplyDto reply = new ReplyDto(id, topicId, currentUser, request.content().trim(), 0, "PUBLISHED", Instant.now());
            replies.put(id, reply);
            repliesByTopic.computeIfAbsent(topicId, ignored -> new ArrayList<>()).add(reply);
            TopicSummaryDto summary = state.summary();
            TopicSummaryDto updated = copyTopic(summary, summary.replyCount() + 1, summary.likeCount(), summary.favoriteCount(), summary.viewCount(), summary.status());
            topics.put(topicId, new TopicState(updated, state.content(), state.aiSummary()));
            notifications.add(new NotificationDto(
                    idSequence.incrementAndGet(),
                    "REPLY_CREATED",
                    "收到新回复",
                    currentUser.nickname() + " 回复了《" + summary.title() + "》。",
                    false,
                    Instant.now()
            ));
            return new CreatedResourceResponse(id);
        }
    }

    public InteractionStatusResponse likeTopic(Long topicId) {
        synchronized (stateLock) {
            TopicState state = requireTopic(topicId);
            if (likedTopics.add(topicId)) {
                TopicSummaryDto summary = state.summary();
                topics.put(topicId, new TopicState(
                        copyTopic(summary, summary.replyCount(), summary.likeCount() + 1, summary.favoriteCount(), summary.viewCount(), summary.status()),
                        state.content(),
                        state.aiSummary()
                ));
            }
            return topicInteractionStatus(topicId);
        }
    }

    public InteractionStatusResponse unlikeTopic(Long topicId) {
        synchronized (stateLock) {
            TopicState state = requireTopic(topicId);
            if (likedTopics.remove(topicId)) {
                TopicSummaryDto summary = state.summary();
                topics.put(topicId, new TopicState(
                        copyTopic(summary, summary.replyCount(), Math.max(summary.likeCount() - 1, 0), summary.favoriteCount(), summary.viewCount(), summary.status()),
                        state.content(),
                        state.aiSummary()
                ));
            }
            return topicInteractionStatus(topicId);
        }
    }

    public InteractionStatusResponse likeReply(Long replyId) {
        synchronized (stateLock) {
            ReplyDto reply = requireReply(replyId);
            if (likedReplies.add(replyId)) {
                replaceReply(reply, new ReplyDto(reply.id(), reply.topicId(), reply.author(), reply.content(), reply.likeCount() + 1, reply.status(), reply.createdAt()));
            }
            ReplyDto current = requireReply(replyId);
            return new InteractionStatusResponse(REPLY, replyId, true, false, current.likeCount(), 0);
        }
    }

    public InteractionStatusResponse unlikeReply(Long replyId) {
        synchronized (stateLock) {
            ReplyDto reply = requireReply(replyId);
            if (likedReplies.remove(replyId)) {
                replaceReply(reply, new ReplyDto(reply.id(), reply.topicId(), reply.author(), reply.content(), Math.max(reply.likeCount() - 1, 0), reply.status(), reply.createdAt()));
            }
            ReplyDto current = requireReply(replyId);
            return new InteractionStatusResponse(REPLY, replyId, false, false, current.likeCount(), 0);
        }
    }

    public InteractionStatusResponse favoriteTopic(Long topicId) {
        synchronized (stateLock) {
            TopicState state = requireTopic(topicId);
            if (favoriteTopics.add(topicId)) {
                TopicSummaryDto summary = state.summary();
                topics.put(topicId, new TopicState(
                        copyTopic(summary, summary.replyCount(), summary.likeCount(), summary.favoriteCount() + 1, summary.viewCount(), summary.status()),
                        state.content(),
                        state.aiSummary()
                ));
            }
            return topicInteractionStatus(topicId);
        }
    }

    public InteractionStatusResponse unfavoriteTopic(Long topicId) {
        synchronized (stateLock) {
            TopicState state = requireTopic(topicId);
            if (favoriteTopics.remove(topicId)) {
                TopicSummaryDto summary = state.summary();
                topics.put(topicId, new TopicState(
                        copyTopic(summary, summary.replyCount(), summary.likeCount(), Math.max(summary.favoriteCount() - 1, 0), summary.viewCount(), summary.status()),
                        state.content(),
                        state.aiSummary()
                ));
            }
            return topicInteractionStatus(topicId);
        }
    }

    public List<NotificationDto> listNotifications() {
        return notifications.stream()
                .sorted(Comparator.comparing(NotificationDto::createdAt).reversed())
                .toList();
    }

    public NotificationDto markNotificationRead(Long id) {
        for (int index = 0; index < notifications.size(); index++) {
            NotificationDto notification = notifications.get(index);
            if (notification.id().equals(id)) {
                NotificationDto updated = new NotificationDto(
                        notification.id(),
                        notification.type(),
                        notification.title(),
                        notification.body(),
                        true,
                        notification.createdAt()
                );
                notifications.set(index, updated);
                return updated;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "通知不存在");
    }

    public ReportDto createReport(ReportRequest request) {
        synchronized (stateLock) {
            String targetType = normalizeTargetType(request.targetType());
            String target = targetTitle(targetType, request.targetId());
            ReportDto report = new ReportDto(
                    idSequence.incrementAndGet(),
                    targetType,
                    request.targetId(),
                    target,
                    request.reason().trim(),
                    request.detail() == null ? "" : request.detail().trim(),
                    "PENDING",
                    inferRisk(request.reason(), request.detail()),
                    Instant.now()
            );
            reports.put(report.id(), report);
            notifications.add(new NotificationDto(
                    idSequence.incrementAndGet(),
                    "REPORT_CREATED",
                    "收到新举报",
                    target + " 进入治理队列。",
                    false,
                    Instant.now()
            ));
            return report;
        }
    }

    public List<ReportDto> listReports(String status) {
        synchronized (stateLock) {
            return reports.values()
                    .stream()
                    .filter(report -> status == null || status.isBlank() || report.status().equalsIgnoreCase(status))
                    .sorted(Comparator.comparing(ReportDto::createdAt).reversed())
                    .toList();
        }
    }

    public ModerationResultResponse handleReport(Long reportId, HandleReportRequest request) {
        synchronized (stateLock) {
            ReportDto report = reports.get(reportId);
            if (report == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "举报不存在");
            }
            String action = request.action().trim().toUpperCase(Locale.ROOT);
            applyModerationAction(report, action);
            String nextStatus = "REJECT".equals(action) ? "REJECTED" : "ACCEPTED";
            ReportDto updatedReport = new ReportDto(
                    report.id(),
                    report.targetType(),
                    report.targetId(),
                    report.target(),
                    report.reason(),
                    report.detail(),
                    nextStatus,
                    report.risk(),
                    report.createdAt()
            );
            reports.put(report.id(), updatedReport);

            ModerationRecordDto record = new ModerationRecordDto(
                    idSequence.incrementAndGet(),
                    report.id(),
                    currentUser.id(),
                    report.targetType(),
                    report.targetId(),
                    action,
                    request.note() == null ? "" : request.note().trim(),
                    TraceContext.currentTraceId(),
                    Instant.now()
            );
            moderationRecords.add(record);
            notifications.add(new NotificationDto(
                    idSequence.incrementAndGet(),
                    "REPORT_HANDLED",
                    "举报已处理",
                    report.target() + " 已执行 " + action + "。",
                    false,
                    Instant.now()
            ));
            return new ModerationResultResponse(updatedReport, record);
        }
    }

    public List<TopicSummaryDto> searchTopicsForAgent(String keyword, int limit) {
        return listTopics(null, keyword, "latest", 1, Math.max(limit, 1)).items();
    }

    public TopicDetailDto getTopicContextForAgent(Long topicId) {
        return getTopic(topicId);
    }

    public List<ReplyDto> getRepliesForAgent(Long topicId) {
        return listReplies(topicId)
                .stream()
                .limit(20)
                .toList();
    }

    private void seedCategories() {
        categories.addAll(List.of(
                new CategoryDto(1L, "开发讨论", "dev", "Java、Vue、Python、数据库和部署经验。", 128),
                new CategoryDto(2L, "AI 助手", "agent", "Agent 编排、提示词、工具调用和模型接入。", 42),
                new CategoryDto(3L, "项目展示", "showcase", "展示正在做的作品，收集反馈。", 36),
                new CategoryDto(4L, "反馈建议", "feedback", "产品体验、规则和社区治理建议。", 19)
        ));
    }

    private void seedTopicsAndReplies() {
        addSeedTopic(101L, 1L, beiqiao, "Spring Boot 和 Agent 服务边界怎么划分", "讨论 Java 主业务和 Python Agent 之间的权限、审计、工具调用边界。", seedTopicContent(), List.of("Java", "Agent", "架构"), 42, 7, 980, Instant.now().minusSeconds(7200));
        addSeedTopic(102L, 1L, lin, "Vue3 发帖编辑器需要哪些 AI 入口", "发帖时的润色、标签推荐、摘要预览和风险提示应该怎样放，才不打断写作。", "发帖编辑器要把 AI 入口放在写作流旁边，避免工具替用户做最终决定。", List.of("Vue3", "交互", "AI"), 31, 5, 622, Instant.now().minusSeconds(12800));
        addSeedTopic(103L, 2L, beiqiao, "站内问答先用 MySQL 检索还是向量库", "MVP 阶段可以先用关键词检索和摘要缓存，后续再加向量召回。", "第一阶段先用 MySQL FULLTEXT 和摘要缓存，等内容量上来后再补向量检索。", List.of("Python", "检索", "MySQL"), 28, 6, 714, Instant.now().minusSeconds(18400));
        addSeedTopic(104L, 3L, lin, "ThreadMind 第一版页面草图", "首页、帖子详情、发帖编辑器和管理台的初始页面设计记录。", "页面从论坛操作台出发，不做营销页，用户进入后直接看到讨论内容。", List.of("项目展示", "Vue3"), 24, 4, 406, Instant.now().minusSeconds(26000));

        addSeedReply(201L, 101L, lin, "建议先让 Agent 只读 Java 工具接口。等权限模型稳定后，再考虑是否加只读视图。", 18, Instant.now().minusSeconds(3600));
        addSeedReply(202L, 101L, beiqiao, "AI 的失败也要降级成普通论坛可用，不能让发帖流程依赖模型服务。", 11, Instant.now().minusSeconds(2200));
        addSeedReply(203L, 102L, beiqiao, "润色和标签推荐适合放在编辑器侧栏，摘要更适合帖子详情页。", 9, Instant.now().minusSeconds(1800));
    }

    private void seedNotificationsAndReports() {
        notifications.add(new NotificationDto(901L, "REPLY_CREATED", "收到新回复", "林同学 回复了你的帖子。", false, Instant.now().minusSeconds(1200)));
        notifications.add(new NotificationDto(902L, "AI_SUMMARY_READY", "摘要已生成", "帖子《Spring Boot 和 Agent 服务边界怎么划分》已有 AI 摘要。", true, Instant.now().minusSeconds(5600)));
        reports.put(301L, new ReportDto(301L, REPLY, 202L, "回复 #202", "疑似偏题", "希望版主确认是否需要移到架构讨论。", "PENDING", "LOW", Instant.now().minusSeconds(720)));
        reports.put(302L, new ReportDto(302L, TOPIC, 103L, "帖子 #103", "标题不清晰", "建议补充检索方案的边界。", "PENDING", "LOW", Instant.now().minusSeconds(2700)));
    }

    private void addSeedTopic(Long id, Long categoryId, AuthorDto author, String title, String excerpt, String content, List<String> tags, int likeCount, int favoriteCount, int viewCount, Instant createdAt) {
        CategoryDto category = requireCategory(categoryId);
        TopicSummaryDto summary = new TopicSummaryDto(id, title, excerpt, category, author, tags, 0, likeCount, favoriteCount, viewCount, "PUBLISHED", createdAt);
        topics.put(id, new TopicState(summary, content, "这条讨论集中在模块边界、权限和后续替换数据库实现的路径。"));
        repliesByTopic.put(id, new ArrayList<>());
    }

    private void addSeedReply(Long id, Long topicId, AuthorDto author, String content, int likeCount, Instant createdAt) {
        ReplyDto reply = new ReplyDto(id, topicId, author, content, likeCount, "PUBLISHED", createdAt);
        replies.put(id, reply);
        repliesByTopic.computeIfAbsent(topicId, ignored -> new ArrayList<>()).add(reply);
        TopicState state = topics.get(topicId);
        TopicSummaryDto summary = state.summary();
        topics.put(topicId, new TopicState(copyTopic(summary, summary.replyCount() + 1, summary.likeCount(), summary.favoriteCount(), summary.viewCount(), summary.status()), state.content(), state.aiSummary()));
    }

    private TopicState requireTopic(Long id) {
        TopicState state = topics.get(id);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        return state;
    }

    private ReplyDto requireReply(Long id) {
        ReplyDto reply = replies.get(id);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "回复不存在");
        }
        return reply;
    }

    private CategoryDto requireCategory(Long id) {
        return categories.stream()
                .filter(category -> category.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "板块不存在"));
    }

    private String targetTitle(String targetType, Long targetId) {
        if (TOPIC.equals(targetType)) {
            return "帖子 #" + requireTopic(targetId).summary().id();
        }
        if (REPLY.equals(targetType)) {
            return "回复 #" + requireReply(targetId).id();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的目标类型");
    }

    private void applyModerationAction(ReportDto report, String action) {
        if (TOPIC.equals(report.targetType()) && ("HIDE".equals(action) || "LOCK".equals(action))) {
            TopicState state = requireTopic(report.targetId());
            TopicSummaryDto summary = state.summary();
            String status = "HIDE".equals(action) ? "HIDDEN" : "LOCKED";
            topics.put(summary.id(), new TopicState(copyTopic(summary, summary.replyCount(), summary.likeCount(), summary.favoriteCount(), summary.viewCount(), status), state.content(), state.aiSummary()));
        }
        if (REPLY.equals(report.targetType()) && "HIDE".equals(action)) {
            ReplyDto reply = requireReply(report.targetId());
            replaceReply(reply, new ReplyDto(reply.id(), reply.topicId(), reply.author(), reply.content(), reply.likeCount(), "HIDDEN", reply.createdAt()));
        }
    }

    private void replaceReply(ReplyDto previous, ReplyDto next) {
        replies.put(next.id(), next);
        List<ReplyDto> topicReplies = repliesByTopic.getOrDefault(previous.topicId(), new ArrayList<>());
        for (int index = 0; index < topicReplies.size(); index++) {
            if (topicReplies.get(index).id().equals(previous.id())) {
                topicReplies.set(index, next);
                return;
            }
        }
    }

    private InteractionStatusResponse topicInteractionStatus(Long topicId) {
        TopicSummaryDto summary = requireTopic(topicId).summary();
        return new InteractionStatusResponse(TOPIC, topicId, likedTopics.contains(topicId), favoriteTopics.contains(topicId), summary.likeCount(), summary.favoriteCount());
    }

    private TopicDetailDto toDetail(TopicState state) {
        TopicSummaryDto summary = state.summary();
        return new TopicDetailDto(
                summary.id(),
                summary.title(),
                state.content(),
                summary.category(),
                summary.author(),
                summary.tags(),
                summary.replyCount(),
                summary.likeCount(),
                summary.favoriteCount(),
                summary.viewCount(),
                summary.status(),
                state.aiSummary(),
                summary.createdAt()
        );
    }

    private TopicSummaryDto copyTopic(TopicSummaryDto source, int replyCount, int likeCount, int favoriteCount, int viewCount, String status) {
        return new TopicSummaryDto(
                source.id(),
                source.title(),
                source.excerpt(),
                source.category(),
                source.author(),
                source.tags(),
                replyCount,
                likeCount,
                favoriteCount,
                viewCount,
                status,
                source.createdAt()
        );
    }

    private Comparator<TopicSummaryDto> topicComparator(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt((TopicSummaryDto topic) -> topic.likeCount() + topic.replyCount() + topic.favoriteCount()).reversed();
        }
        if ("unanswered".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(TopicSummaryDto::replyCount)
                    .thenComparing(TopicSummaryDto::createdAt, Comparator.reverseOrder());
        }
        return Comparator.comparing(TopicSummaryDto::createdAt).reversed();
    }

    private boolean matchesKeyword(TopicSummaryDto topic, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return topic.title().toLowerCase(Locale.ROOT).contains(normalized)
                || topic.excerpt().toLowerCase(Locale.ROOT).contains(normalized)
                || topic.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(normalized));
    }

    private List<String> normalizeTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of("未分类");
        }
        return tagNames.stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType.trim().toUpperCase(Locale.ROOT);
        if (!TOPIC.equals(normalized) && !REPLY.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的目标类型");
        }
        return normalized;
    }

    private String inferRisk(String reason, String detail) {
        String text = ((reason == null ? "" : reason) + " " + (detail == null ? "" : detail)).toLowerCase(Locale.ROOT);
        if (text.contains("攻击") || text.contains("辱骂") || text.contains("泄露")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String excerpt(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 72) {
            return compact;
        }
        return compact.substring(0, 72) + "...";
    }

    private String seedTopicContent() {
        return """
                我们准备把 Java 后端作为权限、事务和审计中心，Python Agent 只处理模型调用和工具编排。

                当前争议点在于 Agent 做站内问答时，应该直接查 MySQL，还是通过 Java 提供的内部工具接口。直接查库开发速度快，但会绕开权限和脱敏；走 Java 工具接口多一层调用，却能把规则留在主业务服务里。
                """;
    }

    private record TopicState(
            TopicSummaryDto summary,
            String content,
            String aiSummary
    ) {
    }
}
