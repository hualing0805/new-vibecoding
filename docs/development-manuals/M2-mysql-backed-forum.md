# M2 MySQL-backed 论坛核心开发手册

## 阶段目标

M2 前半段目标是把 Java 后端从内存 demo 服务切换到 MySQL-backed 的 controller / service / repository 结构，让账号、论坛内容、互动、通知、治理和 Agent 内部工具都能读写本地数据库。

## 需要完成的事情

- 删除内存态 `ForumDemoService`，替换为数据库 repository 和业务 service。
- 账号注册、登录、当前用户读取改为访问 `users`、`roles`、`user_roles`。
- 板块、帖子、回复、标签改为访问 `categories`、`topics`、`replies`、`tags`、`topic_tags`。
- 点赞、收藏改为访问 `reactions`、`favorites`，并维护帖子和回复计数。
- 通知中心改为访问 `notifications`。
- 举报和治理记录改为访问 `reports`、`moderation_records`。
- Agent 内部工具通过 Java 查询站内帖子和详情上下文。
- 增加本地 seed 数据，保证空库启动后可演示。

## 已经如何完成

- 论坛主服务落在 `backend-java/src/main/java/com/threadmind/forum/forum/service/ForumService.java`。
- 论坛 SQL 访问落在 `backend-java/src/main/java/com/threadmind/forum/forum/repository/ForumRepository.java`。
- 账号 SQL 访问落在 `backend-java/src/main/java/com/threadmind/forum/auth/repository/UserRepository.java`。
- 互动、通知、治理分别落在对应模块的 `repository/` 和 `service/` 下。
- Agent 内部工具入口落在 `backend-java/src/main/java/com/threadmind/forum/agent/controller/InternalAgentToolController.java`，通过 `X-Internal-Token` 校验内部调用。
- 本地 seed 数据写入 `backend-java/src/main/resources/db/migration/V2__seed_local_forum_data.sql`。
- 本地 MySQL 初始化脚本补充在 `scripts/mysql-bootstrap.sql` 和 `infra/mysql/init/01-create-threadmind-user.sql`。

## 本地验证

1. 启动 MySQL。

```powershell
cd infra
docker compose up -d
```

2. 启动 Java 后端。

```powershell
cd backend-java
mvn spring-boot:run
```

3. 访问核心接口。

```powershell
Invoke-RestMethod http://localhost:8080/api/categories
Invoke-RestMethod http://localhost:8080/api/topics
Invoke-RestMethod http://localhost:8080/api/topics/101
```

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/forum/service/ForumService.java`
- `backend-java/src/main/java/com/threadmind/forum/forum/repository/ForumRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/auth/repository/UserRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/interaction/service/InteractionService.java`
- `backend-java/src/main/java/com/threadmind/forum/notification/service/NotificationService.java`
- `backend-java/src/main/java/com/threadmind/forum/moderation/service/ModerationService.java`
- `backend-java/src/main/resources/db/migration/V2__seed_local_forum_data.sql`

## 遗留风险

- 这个阶段仍然使用固定当前用户，写接口还没有真正绑定 JWT 登录态。
- Spring Security 在该阶段仍偏向本地演示放行，管理接口和写接口需要后续收紧。
- Java 测试覆盖较薄，需要在真实登录态收口时补核心单元测试。

