package com.threadmind.forum.agent.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class AgentAuditRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AgentAuditRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long createSession(Long userId, String scene, String status, String traceId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO ai_sessions (user_id, scene, status, trace_id)
                VALUES (:userId, :scene, :status, :traceId)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("scene", scene)
                        .addValue("status", status)
                        .addValue("traceId", traceId),
                keyHolder,
                new String[]{"id"}
        );
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Optional<Long> findOwnedSession(Long userId, Long sessionId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                    SELECT id
                    FROM ai_sessions
                    WHERE id = :sessionId AND user_id = :userId
                    LIMIT 1
                    """,
                    new MapSqlParameterSource()
                            .addValue("sessionId", sessionId)
                            .addValue("userId", userId),
                    Long.class
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void updateSessionStatus(Long sessionId, String status) {
        jdbc.update(
                """
                UPDATE ai_sessions
                SET status = :status
                WHERE id = :sessionId
                """,
                new MapSqlParameterSource()
                        .addValue("sessionId", sessionId)
                        .addValue("status", status)
        );
    }

    public Long createMessage(Long sessionId, String role, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO ai_messages (session_id, role, content)
                VALUES (:sessionId, :role, :content)
                """,
                new MapSqlParameterSource()
                        .addValue("sessionId", sessionId)
                        .addValue("role", role)
                        .addValue("content", content),
                keyHolder,
                new String[]{"id"}
        );
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createToolCall(
            Long messageId,
            String toolName,
            String status,
            String inputJson,
            String outputJson,
            Integer elapsedMs
    ) {
        jdbc.update(
                """
                INSERT INTO ai_tool_calls (message_id, tool_name, input_json, output_json, status, elapsed_ms)
                VALUES (:messageId, :toolName, :inputJson, :outputJson, :status, :elapsedMs)
                """,
                new MapSqlParameterSource()
                        .addValue("messageId", messageId)
                        .addValue("toolName", toolName)
                        .addValue("status", status)
                        .addValue("inputJson", inputJson)
                        .addValue("outputJson", outputJson)
                        .addValue("elapsedMs", elapsedMs)
        );
    }
}
