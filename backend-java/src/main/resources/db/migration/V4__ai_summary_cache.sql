CREATE TABLE topic_ai_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    summary TEXT NOT NULL,
    points_json JSON NOT NULL,
    follow_up_questions_json JSON NOT NULL,
    audit_session_id BIGINT NULL,
    generated_by BIGINT NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    CONSTRAINT fk_topic_ai_summaries_topic FOREIGN KEY (topic_id) REFERENCES topics (id),
    CONSTRAINT fk_topic_ai_summaries_session FOREIGN KEY (audit_session_id) REFERENCES ai_sessions (id),
    CONSTRAINT fk_topic_ai_summaries_user FOREIGN KEY (generated_by) REFERENCES users (id),
    KEY idx_topic_ai_summaries_topic_expires (topic_id, expires_at, generated_at),
    KEY idx_topic_ai_summaries_generated_by (generated_by, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
