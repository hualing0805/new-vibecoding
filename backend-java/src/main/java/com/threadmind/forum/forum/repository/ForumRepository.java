package com.threadmind.forum.forum.repository;

import com.threadmind.forum.common.api.PageResponse;
import com.threadmind.forum.forum.dto.AuthorDto;
import com.threadmind.forum.forum.dto.CategoryDto;
import com.threadmind.forum.forum.dto.CreateReplyRequest;
import com.threadmind.forum.forum.dto.CreateTopicRequest;
import com.threadmind.forum.forum.dto.ReplyDto;
import com.threadmind.forum.forum.dto.TopicDetailDto;
import com.threadmind.forum.forum.dto.TopicSummaryDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ForumRepository {

    private static final String VISIBLE_TOPIC_STATUS = "('PUBLISHED', 'LOCKED')";

    private final NamedParameterJdbcTemplate jdbc;

    public ForumRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CategoryDto> listCategories() {
        String sql = """
                SELECT c.id, c.name, c.slug, c.description,
                       COUNT(t.id) AS topic_count
                FROM categories c
                LEFT JOIN topics t
                  ON t.category_id = c.id
                 AND t.deleted_at IS NULL
                 AND t.status IN ('PUBLISHED', 'LOCKED')
                WHERE c.deleted_at IS NULL AND c.status = 'ACTIVE'
                GROUP BY c.id, c.name, c.slug, c.description, c.sort_order
                ORDER BY c.sort_order, c.id
                """;
        return jdbc.query(sql, Map.of(), this::mapCategory);
    }

    public boolean categoryExists(Long categoryId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM categories
                WHERE id = :id AND deleted_at IS NULL AND status = 'ACTIVE'
                """,
                new MapSqlParameterSource("id", categoryId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public PageResponse<TopicSummaryDto> listTopics(
            Long categoryId,
            String keyword,
            String sort,
            int page,
            int pageSize
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = topicWhereClause(categoryId, keyword, params);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;
        params.addValue("limit", safePageSize);
        params.addValue("offset", offset);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM topics t " + where,
                params,
                Long.class
        );
        List<TopicRow> rows = jdbc.query(
                topicSelectSql(where, sort) + " LIMIT :limit OFFSET :offset",
                params,
                this::mapTopicRow
        );
        return new PageResponse<>(
                attachTopicTags(rows, false).stream().map(TopicProjection::summary).toList(),
                safePage,
                safePageSize,
                total == null ? 0 : total
        );
    }

    public List<TopicSummaryDto> searchTopicsForAgent(String keyword, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = topicWhereClause(null, keyword, params);
        params.addValue("limit", Math.min(Math.max(limit, 1), 20));
        List<TopicRow> rows = jdbc.query(
                topicSelectSql(where, "active") + " LIMIT :limit",
                params,
                this::mapTopicRow
        );
        return attachTopicTags(rows, false).stream().map(TopicProjection::summary).toList();
    }

    public Optional<TopicDetailDto> findTopicDetail(Long topicId, boolean increaseViewCount) {
        if (increaseViewCount) {
            jdbc.update(
                    """
                    UPDATE topics
                    SET view_count = view_count + 1
                    WHERE id = :id AND deleted_at IS NULL
                    """,
                    new MapSqlParameterSource("id", topicId)
            );
        }
        List<TopicRow> rows = jdbc.query(
                topicSelectSql("""
                        WHERE t.id = :id
                          AND t.deleted_at IS NULL
                        """, "latest"),
                new MapSqlParameterSource("id", topicId),
                this::mapTopicRow
        );
        return attachTopicTags(rows, true).stream().findFirst().map(TopicProjection::detail);
    }

    public Long createTopic(CreateTopicRequest request, Long authorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO topics (category_id, author_id, title, content, status)
                VALUES (:categoryId, :authorId, :title, :content, 'PUBLISHED')
                """,
                new MapSqlParameterSource()
                        .addValue("categoryId", request.categoryId())
                        .addValue("authorId", authorId)
                        .addValue("title", request.title().trim())
                        .addValue("content", request.content().trim()),
                keyHolder,
                new String[]{"id"}
        );
        Long topicId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        replaceTopicTags(topicId, normalizeTags(request.tagNames()));
        return topicId;
    }

    public Long createReply(Long topicId, CreateReplyRequest request, Long authorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO replies (topic_id, author_id, content, status)
                VALUES (:topicId, :authorId, :content, 'PUBLISHED')
                """,
                new MapSqlParameterSource()
                        .addValue("topicId", topicId)
                        .addValue("authorId", authorId)
                        .addValue("content", request.content().trim()),
                keyHolder,
                new String[]{"id"}
        );
        jdbc.update(
                """
                UPDATE topics
                SET reply_count = reply_count + 1,
                    last_replied_at = CURRENT_TIMESTAMP
                WHERE id = :topicId
                """,
                new MapSqlParameterSource("topicId", topicId)
        );
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<ReplyDto> listReplies(Long topicId) {
        String sql = """
                SELECT r.id, r.topic_id, r.content, r.like_count, r.status, r.created_at,
                       u.id AS author_id, u.nickname AS author_nickname, u.avatar_url AS author_avatar_url
                FROM replies r
                JOIN users u ON u.id = r.author_id
                WHERE r.topic_id = :topicId
                  AND r.deleted_at IS NULL
                  AND r.status = 'PUBLISHED'
                ORDER BY r.created_at, r.id
                """;
        return jdbc.query(sql, new MapSqlParameterSource("topicId", topicId), this::mapReply);
    }

    public boolean topicExists(Long topicId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM topics
                WHERE id = :id AND deleted_at IS NULL
                """,
                new MapSqlParameterSource("id", topicId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public Optional<Long> findTopicAuthorId(Long topicId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                    SELECT author_id
                    FROM topics
                    WHERE id = :id AND deleted_at IS NULL
                    LIMIT 1
                    """,
                    new MapSqlParameterSource("id", topicId),
                    Long.class
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<TargetRef> findTarget(String targetType, Long targetId) {
        String normalizedType = targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
        try {
            if ("TOPIC".equals(normalizedType)) {
                return Optional.ofNullable(jdbc.queryForObject(
                        """
                        SELECT id, title AS label
                        FROM topics
                        WHERE id = :id AND deleted_at IS NULL
                        LIMIT 1
                        """,
                        new MapSqlParameterSource("id", targetId),
                        (rs, rowNum) -> new TargetRef("TOPIC", rs.getLong("id"), rs.getString("label"))
                ));
            }
            if ("REPLY".equals(normalizedType)) {
                return Optional.ofNullable(jdbc.queryForObject(
                        """
                        SELECT id, CONCAT('Reply #', id) AS label
                        FROM replies
                        WHERE id = :id AND deleted_at IS NULL
                        LIMIT 1
                        """,
                        new MapSqlParameterSource("id", targetId),
                        (rs, rowNum) -> new TargetRef("REPLY", rs.getLong("id"), rs.getString("label"))
                ));
            }
            return Optional.empty();
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void applyModerationAction(String targetType, Long targetId, String action) {
        String normalizedType = targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if ("TOPIC".equals(normalizedType)) {
            if ("LOCK".equals(normalizedAction)) {
                jdbc.update("UPDATE topics SET status = 'LOCKED' WHERE id = :id", new MapSqlParameterSource("id", targetId));
            } else if ("HIDE".equals(normalizedAction)) {
                jdbc.update("UPDATE topics SET status = 'HIDDEN' WHERE id = :id", new MapSqlParameterSource("id", targetId));
            } else if ("DELETE".equals(normalizedAction)) {
                jdbc.update("UPDATE topics SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = :id", new MapSqlParameterSource("id", targetId));
            }
        }
        if ("REPLY".equals(normalizedType) && ("HIDE".equals(normalizedAction) || "DELETE".equals(normalizedAction))) {
            jdbc.update(
                    """
                    UPDATE replies
                    SET status = :status,
                        deleted_at = CASE WHEN :status = 'DELETED' THEN CURRENT_TIMESTAMP ELSE deleted_at END
                    WHERE id = :id
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", targetId)
                            .addValue("status", normalizedAction.equals("DELETE") ? "DELETED" : "HIDDEN")
            );
        }
    }

    private String topicWhereClause(Long categoryId, String keyword, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder("""
                WHERE t.deleted_at IS NULL
                  AND t.status IN ('PUBLISHED', 'LOCKED')
                """);
        if (categoryId != null) {
            where.append(" AND t.category_id = :categoryId\n");
            params.addValue("categoryId", categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (t.title LIKE :keyword OR t.content LIKE :keyword)\n");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        return where.toString();
    }

    private String topicSelectSql(String where, String sort) {
        return """
                SELECT t.id, t.title, t.content, t.reply_count, t.like_count, t.favorite_count,
                       t.view_count, t.status, t.created_at,
                       c.id AS category_id, c.name AS category_name, c.slug AS category_slug,
                       c.description AS category_description,
                       (
                         SELECT COUNT(*)
                         FROM topics tc
                         WHERE tc.category_id = c.id
                           AND tc.deleted_at IS NULL
                           AND tc.status IN """ + VISIBLE_TOPIC_STATUS + """
                       ) AS category_topic_count,
                       u.id AS author_id, u.nickname AS author_nickname, u.avatar_url AS author_avatar_url
                FROM topics t
                JOIN categories c ON c.id = t.category_id
                JOIN users u ON u.id = t.author_id
                """ + where + orderBy(sort);
    }

    private String orderBy(String sort) {
        return switch (sort == null ? "latest" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "hottest" -> " ORDER BY (t.like_count * 3 + t.reply_count * 5 + t.view_count) DESC, t.created_at DESC\n";
            case "active" -> " ORDER BY COALESCE(t.last_replied_at, t.created_at) DESC, t.created_at DESC\n";
            default -> " ORDER BY t.created_at DESC\n";
        };
    }

    private List<TopicProjection> attachTopicTags(List<TopicRow> rows, boolean detail) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(TopicRow::id).toList();
        Map<Long, List<String>> tags = topicTags(ids);
        return rows.stream()
                .map(row -> new TopicProjection(row, tags.getOrDefault(row.id(), List.of()), detail))
                .toList();
    }

    private Map<Long, List<String>> topicTags(List<Long> topicIds) {
        if (topicIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT tt.topic_id, tg.name
                FROM topic_tags tt
                JOIN tags tg ON tg.id = tt.tag_id
                WHERE tt.topic_id IN (:topicIds)
                ORDER BY tg.name
                """;
        Map<Long, List<String>> result = new LinkedHashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("topicIds", topicIds), rs -> {
            result.computeIfAbsent(rs.getLong("topic_id"), ignored -> new ArrayList<>())
                    .add(rs.getString("name"));
        });
        return result;
    }

    private void replaceTopicTags(Long topicId, List<String> tagNames) {
        jdbc.update("DELETE FROM topic_tags WHERE topic_id = :topicId", new MapSqlParameterSource("topicId", topicId));
        for (String tagName : tagNames) {
            Long tagId = ensureTag(tagName);
            jdbc.update(
                    """
                    INSERT INTO topic_tags (topic_id, tag_id)
                    VALUES (:topicId, :tagId)
                    ON DUPLICATE KEY UPDATE topic_id = topic_id
                    """,
                    new MapSqlParameterSource()
                            .addValue("topicId", topicId)
                            .addValue("tagId", tagId)
            );
        }
    }

    private Long ensureTag(String name) {
        jdbc.update(
                """
                INSERT INTO tags (name, slug)
                VALUES (:name, :slug)
                ON DUPLICATE KEY UPDATE name = name
                """,
                new MapSqlParameterSource()
                        .addValue("name", name)
                        .addValue("slug", slugify(name))
        );
        return jdbc.queryForObject(
                "SELECT id FROM tags WHERE name = :name LIMIT 1",
                new MapSqlParameterSource("name", name),
                Long.class
        );
    }

    private List<String> normalizeTags(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }
        return tagNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!normalized.isBlank()) {
            return normalized;
        }
        return "tag-" + Integer.toUnsignedString(value.hashCode(), 36);
    }

    private TopicRow mapTopicRow(ResultSet rs, int rowNum) throws SQLException {
        CategoryDto category = new CategoryDto(
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("category_slug"),
                rs.getString("category_description"),
                rs.getInt("category_topic_count")
        );
        AuthorDto author = new AuthorDto(
                rs.getLong("author_id"),
                rs.getString("author_nickname"),
                rs.getString("author_avatar_url") == null ? "" : rs.getString("author_avatar_url")
        );
        return new TopicRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                category,
                author,
                rs.getInt("reply_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("view_count"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private CategoryDto mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getInt("topic_count")
        );
    }

    private ReplyDto mapReply(ResultSet rs, int rowNum) throws SQLException {
        AuthorDto author = new AuthorDto(
                rs.getLong("author_id"),
                rs.getString("author_nickname"),
                rs.getString("author_avatar_url") == null ? "" : rs.getString("author_avatar_url")
        );
        return new ReplyDto(
                rs.getLong("id"),
                rs.getLong("topic_id"),
                author,
                rs.getString("content"),
                rs.getInt("like_count"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String excerpt(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 96) {
            return compact;
        }
        return compact.substring(0, 96) + "...";
    }

    public record TargetRef(String targetType, Long targetId, String label) {
    }

    private record TopicRow(
            Long id,
            String title,
            String content,
            CategoryDto category,
            AuthorDto author,
            int replyCount,
            int likeCount,
            int favoriteCount,
            int viewCount,
            String status,
            Instant createdAt
    ) {
    }

    private final class TopicProjection {
        private final TopicRow row;
        private final List<String> tags;
        private final boolean detail;

        private TopicProjection(TopicRow row, List<String> tags, boolean detail) {
            this.row = row;
            this.tags = tags;
            this.detail = detail;
        }

        private TopicSummaryDto summary() {
            return new TopicSummaryDto(
                    row.id(),
                    row.title(),
                    excerpt(row.content()),
                    row.category(),
                    row.author(),
                    tags,
                    row.replyCount(),
                    row.likeCount(),
                    row.favoriteCount(),
                    row.viewCount(),
                    row.status(),
                    row.createdAt()
            );
        }

        private TopicDetailDto detail() {
            String aiSummary = detail
                    ? "AI summary can be regenerated from the current topic and replies."
                    : null;
            return new TopicDetailDto(
                    row.id(),
                    row.title(),
                    row.content(),
                    row.category(),
                    row.author(),
                    tags,
                    row.replyCount(),
                    row.likeCount(),
                    row.favoriteCount(),
                    row.viewCount(),
                    row.status(),
                    aiSummary,
                    row.createdAt()
            );
        }
    }
}
