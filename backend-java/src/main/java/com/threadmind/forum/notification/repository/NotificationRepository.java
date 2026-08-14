package com.threadmind.forum.notification.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadmind.forum.notification.dto.NotificationDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public NotificationRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public java.util.List<NotificationDto> listByUser(Long userId) {
        String sql = """
                SELECT id, type,
                       JSON_UNQUOTE(JSON_EXTRACT(payload, '$.title')) AS title,
                       JSON_UNQUOTE(JSON_EXTRACT(payload, '$.body')) AS body,
                       read_at,
                       created_at
                FROM notifications
                WHERE user_id = :userId
                ORDER BY created_at DESC, id DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), this::mapNotification);
    }

    public NotificationDto createNotification(Long userId, String type, String title, String body) {
        String payload = notificationPayload(title, body);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO notifications (user_id, type, payload)
                VALUES (:userId, :type, :payload)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("type", type)
                        .addValue("payload", payload),
                keyHolder,
                new String[]{"id"}
        );
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findById(userId, id).orElseThrow();
    }

    public Optional<NotificationDto> findById(Long userId, Long notificationId) {
        String sql = """
                SELECT id, type,
                       JSON_UNQUOTE(JSON_EXTRACT(payload, '$.title')) AS title,
                       JSON_UNQUOTE(JSON_EXTRACT(payload, '$.body')) AS body,
                       read_at,
                       created_at
                FROM notifications
                WHERE id = :id AND user_id = :userId
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource()
                            .addValue("id", notificationId)
                            .addValue("userId", userId),
                    this::mapNotification
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<NotificationDto> markRead(Long userId, Long notificationId) {
        jdbc.update(
                """
                UPDATE notifications
                SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
                WHERE id = :id AND user_id = :userId
                """,
                new MapSqlParameterSource()
                        .addValue("id", notificationId)
                        .addValue("userId", userId)
        );
        return findById(userId, notificationId);
    }

    private NotificationDto mapNotification(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationDto(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getTimestamp("read_at") != null,
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String notificationPayload(String title, String body) {
        try {
            return objectMapper.writeValueAsString(Map.of("title", title, "body", body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to build notification payload", ex);
        }
    }
}
