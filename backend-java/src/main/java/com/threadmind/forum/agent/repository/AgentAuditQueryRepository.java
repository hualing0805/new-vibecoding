package com.threadmind.forum.agent.repository;

import com.threadmind.forum.agent.dto.AgentAuditDtos.AiAuditMetricsDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionListItemDto;
import com.threadmind.forum.agent.dto.AgentAuditDtos.AiSessionMessageDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentAuditQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AgentAuditQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long countAdminSessions(String scene, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filters(scene, status, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM ai_sessions s " + where, params, Long.class);
        return total == null ? 0 : total;
    }

    public List<AiSessionListItemDto> listAdminSessions(String scene, String status, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filters(scene, status, params);
        addPaging(params, page, pageSize);
        return jdbc.query(sessionSelectSql(where) + " LIMIT :limit OFFSET :offset", params, this::mapSession);
    }

    public Optional<AiSessionListItemDto> findAdminSession(Long sessionId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sessionSelectSql("WHERE s.id = :sessionId\n"),
                    new MapSqlParameterSource("sessionId", sessionId),
                    this::mapSession
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public long countUserSessions(Long userId) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_sessions s WHERE s.user_id = :userId",
                new MapSqlParameterSource("userId", userId),
                Long.class
        );
        return total == null ? 0 : total;
    }

    public List<AiSessionListItemDto> listUserSessions(Long userId, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        addPaging(params, page, pageSize);
        return jdbc.query(
                sessionSelectSql("WHERE s.user_id = :userId\n") + " LIMIT :limit OFFSET :offset",
                params,
                this::mapSession
        );
    }

    public Optional<AiSessionListItemDto> findUserSession(Long userId, Long sessionId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sessionSelectSql("WHERE s.id = :sessionId AND s.user_id = :userId\n"),
                    new MapSqlParameterSource()
                            .addValue("sessionId", sessionId)
                            .addValue("userId", userId),
                    this::mapSession
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<AiSessionMessageDto> listMessages(Long sessionId) {
        return jdbc.query(
                """
                SELECT id, role, content, created_at
                FROM ai_messages
                WHERE session_id = :sessionId
                ORDER BY created_at, id
                """,
                new MapSqlParameterSource("sessionId", sessionId),
                this::mapMessage
        );
    }

    public List<ToolCallRecord> listToolCalls(Long sessionId) {
        return jdbc.query(
                """
                SELECT c.id, c.message_id, c.tool_name, c.status, c.elapsed_ms,
                       c.input_json, c.output_json, c.created_at
                FROM ai_tool_calls c
                JOIN ai_messages m ON m.id = c.message_id
                WHERE m.session_id = :sessionId
                ORDER BY c.created_at, c.id
                """,
                new MapSqlParameterSource("sessionId", sessionId),
                this::mapToolCall
        );
    }

    public AiAuditMetricsDto metricsToday() {
        return jdbc.queryForObject(
                """
                SELECT
                  (SELECT COUNT(*) FROM ai_sessions WHERE created_at >= CURRENT_DATE()) AS sessions_today,
                  (SELECT COUNT(*) FROM ai_sessions WHERE created_at >= CURRENT_DATE() AND status = 'COMPLETED') AS completed_today,
                  (SELECT COUNT(*) FROM ai_sessions WHERE created_at >= CURRENT_DATE() AND status = 'FALLBACK') AS fallback_today,
                  (SELECT COUNT(*)
                   FROM ai_tool_calls c
                   JOIN ai_messages m ON m.id = c.message_id
                   WHERE c.created_at >= CURRENT_DATE()) AS tool_calls_today,
                  (SELECT COUNT(*)
                   FROM ai_tool_calls c
                   JOIN ai_messages m ON m.id = c.message_id
                   WHERE c.created_at >= CURRENT_DATE()
                     AND c.status IN ('FAILED', 'UNAVAILABLE', 'CIRCUIT_OPEN')) AS failed_tool_calls_today,
                  (SELECT COUNT(*) FROM topic_ai_summaries WHERE expires_at >= CURRENT_TIMESTAMP) AS cached_summaries,
                  (SELECT COUNT(*) FROM topic_ai_summaries WHERE expires_at < CURRENT_TIMESTAMP) AS stale_summaries
                """,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new AiAuditMetricsDto(
                        rs.getLong("sessions_today"),
                        rs.getLong("completed_today"),
                        rs.getLong("fallback_today"),
                        rs.getLong("tool_calls_today"),
                        rs.getLong("failed_tool_calls_today"),
                        rs.getLong("cached_summaries"),
                        rs.getLong("stale_summaries")
                )
        );
    }

    private String sessionSelectSql(String where) {
        return """
                SELECT s.id, s.user_id, u.nickname, s.scene, s.status, s.trace_id,
                       s.created_at, s.updated_at,
                       (SELECT COUNT(*) FROM ai_messages m WHERE m.session_id = s.id) AS message_count,
                       (
                         SELECT COUNT(*)
                         FROM ai_tool_calls c
                         JOIN ai_messages m ON m.id = c.message_id
                         WHERE m.session_id = s.id
                       ) AS tool_call_count
                FROM ai_sessions s
                JOIN users u ON u.id = s.user_id
                """ + where + " ORDER BY s.updated_at DESC, s.id DESC\n";
    }

    private String filters(String scene, String status, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1\n");
        if (scene != null && !scene.isBlank()) {
            where.append("  AND s.scene = :scene\n");
            params.addValue("scene", scene.trim().toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            where.append("  AND s.status = :status\n");
            params.addValue("status", status.trim().toUpperCase());
        }
        return where.toString();
    }

    private void addPaging(MapSqlParameterSource params, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        params.addValue("limit", safePageSize);
        params.addValue("offset", (safePage - 1) * safePageSize);
    }

    private AiSessionListItemDto mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new AiSessionListItemDto(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("nickname"),
                rs.getString("scene"),
                rs.getString("status"),
                rs.getString("trace_id"),
                toInstant(rs, "created_at"),
                toInstant(rs, "updated_at"),
                rs.getLong("message_count"),
                rs.getLong("tool_call_count")
        );
    }

    private AiSessionMessageDto mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new AiSessionMessageDto(
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                toInstant(rs, "created_at")
        );
    }

    private ToolCallRecord mapToolCall(ResultSet rs, int rowNum) throws SQLException {
        int elapsedMs = rs.getInt("elapsed_ms");
        return new ToolCallRecord(
                rs.getLong("id"),
                rs.getLong("message_id"),
                rs.getString("tool_name"),
                rs.getString("status"),
                rs.wasNull() ? null : elapsedMs,
                rs.getString("input_json"),
                rs.getString("output_json"),
                toInstant(rs, "created_at")
        );
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    public record ToolCallRecord(
            Long id,
            Long messageId,
            String toolName,
            String status,
            Integer elapsedMs,
            String inputJson,
            String outputJson,
            Instant createdAt
    ) {
    }
}
