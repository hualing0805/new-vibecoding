CREATE DATABASE IF NOT EXISTS threadmind
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'threadmind'@'%'
    IDENTIFIED BY 'threadmind';

GRANT ALL PRIVILEGES ON threadmind.* TO 'threadmind'@'%';

FLUSH PRIVILEGES;
