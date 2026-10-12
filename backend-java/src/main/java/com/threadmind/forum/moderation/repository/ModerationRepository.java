package com.threadmind.forum.moderation.repository;

import com.threadmind.forum.moderation.dto.HandleReportRequest;
import com.threadmind.forum.moderation.dto.ModerationRecordDto;
import com.threadmind.forum.moderation.dto.ReportDto;
import com.threadmind.forum.moderation.dto.ReportRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ModerationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ModerationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReportDto createReport(Long reporterId, ReportRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO reports (reporter_id, target_type, target_id, reason, detail, status)
                VALUES (:reporterId, :targetType, :targetId, :reason, :detail, 'PENDING')
                """,
                new MapSqlParameterSource()
                        .addValue("reporterId", reporterId)
                        .addValue("targetType", normalize(request.targetType()))
                        .addValue("targetId", request.targetId())
                        .addValue("reason", request.reason().trim())
                        .addValue("detail", request.detail()),
                keyHolder,
                new String[]{"id"}
        );
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findReport(id).orElseThrow();
    }

    public List<ReportDto> listReports(String status) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = "";
        if (status != null && !status.isBlank()) {
            where = " WHERE r.status = :status\n";
            params.addValue("status", normalize(status));
        }
        return jdbc.query(reportSelectSql(where) + " ORDER BY r.created_at DESC, r.id DESC", params, this::mapReport);
    }

    public Optional<ReportDto> findReport(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    reportSelectSql(" WHERE r.id = :id\n"),
                    new MapSqlParameterSource("id", id),
                    this::mapReport
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean targetExists(String targetType, Long targetId) {
        String normalizedType = normalize(targetType);
        String table = switch (normalizedType) {
            case "TOPIC" -> "topics";
            case "REPLY" -> "replies";
            default -> null;
        };
        if (table == null) {
            return false;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", targetId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public void applyTargetAction(String targetType, Long targetId, String action) {
        String normalizedType = normalize(targetType);
        String normalizedAction = normalize(action);
        if ("TOPIC".equals(normalizedType)) {
            applyTopicAction(targetId, normalizedAction);
            return;
        }
        if ("REPLY".equals(normalizedType)) {
            applyReplyAction(targetId, normalizedAction);
        }
    }

    private void applyTopicAction(Long targetId, String action) {
        switch (action) {
            case "LOCK" -> jdbc.update(
                    "UPDATE topics SET status = 'LOCKED' WHERE id = :id",
                    new MapSqlParameterSource("id", targetId)
            );
            case "HIDE" -> jdbc.update(
                    "UPDATE topics SET status = 'HIDDEN' WHERE id = :id",
                    new MapSqlParameterSource("id", targetId)
            );
            case "DELETE" -> jdbc.update(
                    "UPDATE topics SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = :id",
                    new MapSqlParameterSource("id", targetId)
            );
            default -> {
            }
        }
    }

    private void applyReplyAction(Long targetId, String action) {
        if (!"HIDE".equals(action) && !"DELETE".equals(action)) {
            return;
        }
        jdbc.update(
                """
                UPDATE replies
                SET status = :status,
                    deleted_at = CASE WHEN :status = 'DELETED' THEN CURRENT_TIMESTAMP ELSE deleted_at END
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", targetId)
                        .addValue("status", action)
        );
    }

    public ModerationRecordDto createModerationRecord(
            Long reportId,
            Long operatorId,
            String targetType,
            Long targetId,
            HandleReportRequest request,
            String traceId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO moderation_records (report_id, operator_id, target_type, target_id, action, note, trace_id)
                VALUES (:reportId, :operatorId, :targetType, :targetId, :action, :note, :traceId)
                """,
                new MapSqlParameterSource()
                        .addValue("reportId", reportId)
                        .addValue("operatorId", operatorId)
                        .addValue("targetType", normalize(targetType))
                        .addValue("targetId", targetId)
                        .addValue("action", normalize(request.action()))
                        .addValue("note", request.note())
                        .addValue("traceId", traceId),
                keyHolder,
                new String[]{"id"}
        );
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findModerationRecord(id).orElseThrow();
    }

    public void updateReportStatus(Long reportId, String status) {
        jdbc.update(
                "UPDATE reports SET status = :status WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", reportId)
                        .addValue("status", normalize(status))
        );
    }

    private Optional<ModerationRecordDto> findModerationRecord(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                    SELECT id, report_id, operator_id, target_type, target_id, action, note, trace_id, created_at
                    FROM moderation_records
                    WHERE id = :id
                    LIMIT 1
                    """,
                    new MapSqlParameterSource("id", id),
                    this::mapModerationRecord
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String reportSelectSql(String where) {
        return """
                SELECT r.id, r.target_type, r.target_id,
                       CASE r.target_type
                         WHEN 'TOPIC' THEN COALESCE((SELECT t.title FROM topics t WHERE t.id = r.target_id), CONCAT('Topic #', r.target_id))
                         WHEN 'REPLY' THEN CONCAT('Reply #', r.target_id)
                         ELSE CONCAT(r.target_type, ' #', r.target_id)
                       END AS target_label,
                       r.reason, r.detail, r.status, r.created_at
                FROM reports r
                """ + where;
    }

    private ReportDto mapReport(ResultSet rs, int rowNum) throws SQLException {
        return new ReportDto(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("target_label"),
                rs.getString("reason"),
                rs.getString("detail"),
                rs.getString("status"),
                riskFromReason(rs.getString("reason"), rs.getString("detail")),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private ModerationRecordDto mapModerationRecord(ResultSet rs, int rowNum) throws SQLException {
        return new ModerationRecordDto(
                rs.getLong("id"),
                rs.getObject("report_id") == null ? null : rs.getLong("report_id"),
                rs.getLong("operator_id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("action"),
                rs.getString("note"),
                rs.getString("trace_id"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String riskFromReason(String reason, String detail) {
        String text = ((reason == null ? "" : reason) + " " + (detail == null ? "" : detail)).toLowerCase(Locale.ROOT);
        if (text.contains("spam") || text.contains("abuse") || text.contains("attack")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
