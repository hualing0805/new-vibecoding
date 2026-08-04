# 数据库设计

版本：v0.1  
数据库：MySQL 8.x

## 1. 命名约定

- 表名使用复数或业务集合名，统一小写下划线。
- 主键统一为 `BIGINT`。
- 时间字段统一为 `created_at`、`updated_at`、`deleted_at`。
- 状态字段使用大写英文枚举值，例如 `PUBLISHED`、`HIDDEN`。
- 可扩展结构使用 `JSON` 字段，但查询频繁的字段必须独立成列。

## 2. 核心表分组

账号权限：

- `users`
- `roles`
- `user_roles`

论坛内容：

- `categories`
- `topics`
- `replies`
- `tags`
- `topic_tags`

互动通知：

- `reactions`
- `favorites`
- `follows`
- `notifications`

治理审计：

- `reports`
- `moderation_records`

AI 审计：

- `ai_sessions`
- `ai_messages`
- `ai_tool_calls`

## 3. 状态枚举

`users.status`：

- `ACTIVE`
- `LOCKED`
- `DISABLED`

`topics.status` / `replies.status`：

- `DRAFT`
- `PUBLISHED`
- `HIDDEN`
- `LOCKED`
- `DELETED`

`reports.status`：

- `PENDING`
- `ACCEPTED`
- `REJECTED`

`ai_sessions.status`：

- `OPEN`
- `CLOSED`
- `FAILED`

## 4. 索引策略

第一阶段索引不要做得太复杂，保证高频列表和详情页即可。

- `users.username` 唯一索引。
- `users.email` 唯一索引。
- `categories.slug` 唯一索引。
- `topics.category_id + topics.created_at` 普通索引。
- `topics.author_id + topics.created_at` 普通索引。
- `topics.status + topics.created_at` 普通索引。
- `replies.topic_id + replies.created_at` 普通索引。
- `reactions.user_id + target_type + target_id + reaction_type` 唯一索引。
- `favorites.user_id + topic_id` 唯一索引。
- `notifications.user_id + read_at + created_at` 普通索引。
- `reports.status + created_at` 普通索引。
- `ai_messages.session_id + created_at` 普通索引。

## 5. 迁移脚本

初始迁移脚本放在：

```text
backend-java/src/main/resources/db/migration/V1__init_schema.sql
```

Flyway 从空库启动时应完成建表和少量种子数据：

- 角色：`USER`、`MODERATOR`、`ADMIN`
- 板块：开发讨论、灌水闲谈、AI 助手、反馈建议
- 标签：Java、Vue3、Python、MySQL、Agent、项目展示

## 6. 数据边界

Java 后端是唯一直接写业务表的服务。Python Agent 不直接连接 MySQL，不直接修改帖子、回复、用户状态和治理结果。

AI 会话表用于审计和调试。生产化时需要增加保留周期、脱敏策略和用户删除请求处理。

