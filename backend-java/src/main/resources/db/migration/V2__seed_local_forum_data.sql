INSERT INTO roles (id, code, name) VALUES
    (1, 'USER', 'Registered User'),
    (2, 'MODERATOR', 'Moderator'),
    (3, 'ADMIN', 'Administrator')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (id, username, email, password_hash, nickname, avatar_url, bio, points, status) VALUES
    (1, 'demo', 'demo@threadmind.local', '{noop}threadmind123', 'Demo User', '', 'Local development account.', 128, 'ACTIVE'),
    (2, 'beiqiao', 'beiqiao@threadmind.local', '{noop}threadmind123', 'Beiqiao', '', 'Forum seed author.', 64, 'ACTIVE'),
    (3, 'lin', 'lin@threadmind.local', '{noop}threadmind123', 'Lin', '', 'Agent module explorer.', 56, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    avatar_url = VALUES(avatar_url),
    bio = VALUES(bio),
    status = VALUES(status);

INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE code IN ('USER', 'ADMIN')
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 2, id FROM roles WHERE code = 'USER'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT 3, id FROM roles WHERE code = 'USER'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO topics (
    id, category_id, author_id, title, content, status,
    view_count, reply_count, like_count, favorite_count, last_replied_at, created_at
)
SELECT
    101,
    c.id,
    2,
    'How should we split the Java backend modules?',
    'The backend should keep HTTP controllers thin, put transaction decisions in services, and keep SQL in repositories. This makes the forum easier to extend when auth, moderation, and agent tools become real production modules.',
    'PUBLISHED',
    42,
    2,
    8,
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP - INTERVAL 2 DAY
FROM categories c
WHERE c.slug = 'dev'
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content);

INSERT INTO topics (
    id, category_id, author_id, title, content, status,
    view_count, reply_count, like_count, favorite_count, last_replied_at, created_at
)
SELECT
    102,
    c.id,
    3,
    'Agent tools should read forum context from Java',
    'The Python agent should not guess forum state. It should call internal Java endpoints with a scoped token, fetch topic context and replies, then generate summaries or moderation hints from current data.',
    'PUBLISHED',
    31,
    1,
    6,
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP - INTERVAL 1 DAY
FROM categories c
WHERE c.slug = 'agent'
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content);

INSERT INTO topics (
    id, category_id, author_id, title, content, status,
    view_count, reply_count, like_count, favorite_count, last_replied_at, created_at
)
SELECT
    103,
    c.id,
    1,
    'First project baseline is ready',
    'The current baseline includes project specification docs, Vue pages, Spring Boot APIs, Python agent endpoints, and MySQL migrations. The next phase should replace temporary auth and add real permissions.',
    'PUBLISHED',
    24,
    0,
    5,
    1,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL 8 HOUR
FROM categories c
WHERE c.slug = 'showcase'
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content);

INSERT INTO replies (id, topic_id, author_id, content, status, like_count, created_at) VALUES
    (201, 101, 1, 'Agree. A controller-service-repository split is enough for the first real backend, and we can add domain events later.', 'PUBLISHED', 3, CURRENT_TIMESTAMP - INTERVAL 36 HOUR),
    (202, 101, 3, 'Keeping Agent access behind internal endpoints also makes audit and rate limits easier.', 'PUBLISHED', 2, CURRENT_TIMESTAMP - INTERVAL 30 HOUR),
    (203, 102, 1, 'The agent contract should include citations and tool calls so the frontend can show why an answer is trustworthy.', 'PUBLISHED', 4, CURRENT_TIMESTAMP - INTERVAL 18 HOUR)
ON DUPLICATE KEY UPDATE content = VALUES(content), status = VALUES(status);

INSERT INTO topic_tags (topic_id, tag_id)
SELECT 101, id FROM tags WHERE name IN ('Java', 'MySQL')
ON DUPLICATE KEY UPDATE topic_id = topic_id;

INSERT INTO topic_tags (topic_id, tag_id)
SELECT 102, id FROM tags WHERE name IN ('Python', 'Agent')
ON DUPLICATE KEY UPDATE topic_id = topic_id;

INSERT INTO topic_tags (topic_id, tag_id)
SELECT 103, id FROM tags WHERE name IN ('Vue3', '项目展示')
ON DUPLICATE KEY UPDATE topic_id = topic_id;

INSERT INTO notifications (id, user_id, type, payload, created_at) VALUES
    (301, 1, 'SYSTEM', JSON_OBJECT('title', 'Backend switched to MySQL', 'body', 'Forum APIs now read and write local MySQL data.'), CURRENT_TIMESTAMP - INTERVAL 1 HOUR),
    (302, 1, 'REPLY_CREATED', JSON_OBJECT('title', 'New reply', 'body', 'A seeded topic has replies available for local testing.'), CURRENT_TIMESTAMP - INTERVAL 30 MINUTE)
ON DUPLICATE KEY UPDATE payload = VALUES(payload);

INSERT INTO reports (id, reporter_id, target_type, target_id, reason, detail, status, created_at) VALUES
    (401, 1, 'TOPIC', 102, 'Needs review', 'Seed report for validating the moderation workflow.', 'PENDING', CURRENT_TIMESTAMP - INTERVAL 20 MINUTE)
ON DUPLICATE KEY UPDATE reason = VALUES(reason), detail = VALUES(detail), status = VALUES(status);
