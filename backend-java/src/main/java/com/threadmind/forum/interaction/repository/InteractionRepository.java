package com.threadmind.forum.interaction.repository;

import com.threadmind.forum.interaction.dto.InteractionStatusResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InteractionRepository {

    private static final String TOPIC = "TOPIC";
    private static final String REPLY = "REPLY";
    private static final String LIKE = "LIKE";

    private final NamedParameterJdbcTemplate jdbc;

    public InteractionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean topicExists(Long topicId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM topics WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", topicId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public boolean replyExists(Long replyId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM replies WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", replyId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public InteractionStatusResponse likeTopic(Long userId, Long topicId) {
        int inserted = insertReaction(userId, TOPIC, topicId);
        if (inserted > 0) {
            jdbc.update("UPDATE topics SET like_count = like_count + 1 WHERE id = :id", new MapSqlParameterSource("id", topicId));
        }
        return topicStatus(userId, topicId);
    }

    public InteractionStatusResponse getTopicStatus(Long userId, Long topicId) {
        return topicStatus(userId, topicId);
    }

    public InteractionStatusResponse getReplyStatus(Long userId, Long replyId) {
        return replyStatus(userId, replyId);
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

    public Optional<Long> findReplyAuthorId(Long replyId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                    SELECT author_id
                    FROM replies
                    WHERE id = :id AND deleted_at IS NULL
                    LIMIT 1
                    """,
                    new MapSqlParameterSource("id", replyId),
                    Long.class
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public InteractionStatusResponse unlikeTopic(Long userId, Long topicId) {
        int deleted = deleteReaction(userId, TOPIC, topicId);
        if (deleted > 0) {
            jdbc.update(
                    "UPDATE topics SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id",
                    new MapSqlParameterSource("id", topicId)
            );
        }
        return topicStatus(userId, topicId);
    }

    public InteractionStatusResponse likeReply(Long userId, Long replyId) {
        int inserted = insertReaction(userId, REPLY, replyId);
        if (inserted > 0) {
            jdbc.update("UPDATE replies SET like_count = like_count + 1 WHERE id = :id", new MapSqlParameterSource("id", replyId));
        }
        return replyStatus(userId, replyId);
    }

    public InteractionStatusResponse unlikeReply(Long userId, Long replyId) {
        int deleted = deleteReaction(userId, REPLY, replyId);
        if (deleted > 0) {
            jdbc.update(
                    "UPDATE replies SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id",
                    new MapSqlParameterSource("id", replyId)
            );
        }
        return replyStatus(userId, replyId);
    }

    public InteractionStatusResponse favoriteTopic(Long userId, Long topicId) {
        int inserted = jdbc.update(
                """
                INSERT IGNORE INTO favorites (user_id, topic_id)
                VALUES (:userId, :topicId)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("topicId", topicId)
        );
        if (inserted > 0) {
            jdbc.update("UPDATE topics SET favorite_count = favorite_count + 1 WHERE id = :id", new MapSqlParameterSource("id", topicId));
        }
        return topicStatus(userId, topicId);
    }

    public InteractionStatusResponse unfavoriteTopic(Long userId, Long topicId) {
        int deleted = jdbc.update(
                "DELETE FROM favorites WHERE user_id = :userId AND topic_id = :topicId",
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("topicId", topicId)
        );
        if (deleted > 0) {
            jdbc.update(
                    "UPDATE topics SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = :id",
                    new MapSqlParameterSource("id", topicId)
            );
        }
        return topicStatus(userId, topicId);
    }

    private int insertReaction(Long userId, String targetType, Long targetId) {
        return jdbc.update(
                """
                INSERT IGNORE INTO reactions (user_id, target_type, target_id, reaction_type)
                VALUES (:userId, :targetType, :targetId, :reactionType)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("targetType", targetType)
                        .addValue("targetId", targetId)
                        .addValue("reactionType", LIKE)
        );
    }

    private int deleteReaction(Long userId, String targetType, Long targetId) {
        return jdbc.update(
                """
                DELETE FROM reactions
                WHERE user_id = :userId
                  AND target_type = :targetType
                  AND target_id = :targetId
                  AND reaction_type = :reactionType
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("targetType", targetType)
                        .addValue("targetId", targetId)
                        .addValue("reactionType", LIKE)
        );
    }

    private InteractionStatusResponse topicStatus(Long userId, Long topicId) {
        return jdbc.queryForObject(
                """
                SELECT t.like_count, t.favorite_count,
                       EXISTS (
                         SELECT 1 FROM reactions r
                         WHERE r.user_id = :userId
                           AND r.target_type = 'TOPIC'
                           AND r.target_id = t.id
                           AND r.reaction_type = 'LIKE'
                       ) AS liked,
                       EXISTS (
                         SELECT 1 FROM favorites f
                         WHERE f.user_id = :userId AND f.topic_id = t.id
                       ) AS favorited
                FROM topics t
                WHERE t.id = :topicId
                LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("topicId", topicId),
                (rs, rowNum) -> new InteractionStatusResponse(
                        TOPIC,
                        topicId,
                        rs.getBoolean("liked"),
                        rs.getBoolean("favorited"),
                        rs.getInt("like_count"),
                        rs.getInt("favorite_count")
                )
        );
    }

    private InteractionStatusResponse replyStatus(Long userId, Long replyId) {
        return jdbc.queryForObject(
                """
                SELECT r.like_count,
                       EXISTS (
                         SELECT 1 FROM reactions rx
                         WHERE rx.user_id = :userId
                           AND rx.target_type = 'REPLY'
                           AND rx.target_id = r.id
                           AND rx.reaction_type = 'LIKE'
                       ) AS liked
                FROM replies r
                WHERE r.id = :replyId
                LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("replyId", replyId),
                (rs, rowNum) -> new InteractionStatusResponse(
                        REPLY,
                        replyId,
                        rs.getBoolean("liked"),
                        false,
                        rs.getInt("like_count"),
                        0
                )
        );
    }
}
