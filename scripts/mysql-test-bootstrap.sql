CREATE DATABASE IF NOT EXISTS threadmind_migration_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'threadmind_test'@'%'
    IDENTIFIED BY 'threadmind_test';

GRANT ALL PRIVILEGES ON threadmind_migration_test.* TO 'threadmind_test'@'%';

FLUSH PRIVILEGES;
