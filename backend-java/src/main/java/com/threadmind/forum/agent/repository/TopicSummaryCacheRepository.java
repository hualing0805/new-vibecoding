package com.threadmind.forum.agent.repository;

import com.threadmind.forum.agent.dto.AgentDtos.TopicSummaryResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

@Repository
public class TopicSummaryCacheRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TopicSummaryCacheRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CachedTopicSummary> findFreshByTopicId(Long topicId) {
        return findByTopicId(topicId, true);
    }

    public Optional<CachedTopicSummary> findLatestByTopicId(Long topicId) {
        return findByTopicId(topicId, false);
    }

    public void create(Long topicId, TopicSummaryResponse response, String pointsJson, String followUpQuestionsJson, Long generatedBy) {
        jdbc.update(
                """
                INSERT INTO topic_ai_summaries (
                    topic_id, summary, points_json, follow_up_questions_json,
                    audit_session_id, generated_by, expires_at
                )
                VALUES (
                    :topicId, :summary, :pointsJson, :followUpQuestionsJson,
                    :auditSessionId, :generatedBy, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
                )
                """,
                new MapSqlParameterSource()
                        .addValue("topicId", topicId)
                        .addValue("summary", response.summary())
                        .addValue("pointsJson", pointsJson)
                        .addValue("followUpQuestionsJson", followUpQuestionsJson)
                        .addValue("auditSessionId", response.sessionId())
                        .addValue("generatedBy", generatedBy)
        );
    }

    private Optional<CachedTopicSummary> findByTopicId(Long topicId, boolean freshOnly) {
        String freshnessClause = freshOnly ? " AND expires_at >= CURRENT_TIMESTAMP\n" : "";
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                    SELECT id, topic_id, summary, points_json, follow_up_questions_json,
                           audit_session_id, generated_by, generated_at, expires_at
                    FROM topic_ai_summaries
                    WHERE topic_id = :topicId
                    """ + freshnessClause + """
                    ORDER BY generated_at DESC, id DESC
                    LIMIT 1
                    """,
                    new MapSqlParameterSource("topicId", topicId),
                    this::mapSummary
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private CachedTopicSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
        Long auditSessionId = rs.getObject("audit_session_id") == null ? null : rs.getLong("audit_session_id");
        return new CachedTopicSummary(
                rs.getLong("id"),
                rs.getLong("topic_id"),
                rs.getString("summary"),
                rs.getString("points_json"),
                rs.getString("follow_up_questions_json"),
                auditSessionId,
                rs.getLong("generated_by"),
                rs.getTimestamp("generated_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant()
        );
    }

    public record CachedTopicSummary(
            Long id,
            Long topicId,
            String summary,
            String pointsJson,
            String followUpQuestionsJson,
            Long auditSessionId,
            Long generatedBy,
            Instant generatedAt,
            Instant expiresAt
    ) {
    }
}
