package com.threadmind.forum.auth.repository;

import com.threadmind.forum.auth.dto.RegisterRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRecord> findActiveByLogin(String login) {
        String sql = """
                SELECT id, username, email, password_hash, nickname, avatar_url, status, created_at
                FROM users
                WHERE deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND (username = :login OR email = :login)
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource("login", login),
                    this::mapUser
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<UserRecord> findById(Long id) {
        String sql = """
                SELECT id, username, email, password_hash, nickname, avatar_url, status, created_at
                FROM users
                WHERE id = :id AND deleted_at IS NULL
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource("id", id),
                    this::mapUser
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Long createUser(RegisterRequest request, String passwordHash) {
        String sql = """
                INSERT INTO users (username, email, password_hash, nickname, status)
                VALUES (:username, :email, :passwordHash, :nickname, 'ACTIVE')
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("username", request.username().trim())
                        .addValue("email", request.email().trim())
                        .addValue("passwordHash", passwordHash)
                        .addValue("nickname", request.nickname().trim()),
                keyHolder,
                new String[]{"id"}
        );
        Long userId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        jdbc.update(
                """
                INSERT INTO user_roles (user_id, role_id)
                SELECT :userId, id FROM roles WHERE code = 'USER'
                ON DUPLICATE KEY UPDATE user_id = user_id
                """,
                new MapSqlParameterSource("userId", userId)
        );
        return userId;
    }

    public List<String> findRoleCodes(Long userId) {
        String sql = """
                SELECT r.code
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = :userId
                ORDER BY r.code
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("userId", userId), String.class);
    }

    private UserRecord mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserRecord(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    public record UserRecord(
            Long id,
            String username,
            String email,
            String passwordHash,
            String nickname,
            String avatarUrl,
            String status,
            Instant createdAt
    ) {
    }
}
