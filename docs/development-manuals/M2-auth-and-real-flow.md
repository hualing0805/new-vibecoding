# M2 真实登录态与发帖闭环开发手册

## 阶段目标

本阶段目标是收口 M2 的真实使用链路：用户通过 Java API 登录或注册，前端保存真实 token，后端写接口从 JWT 中识别当前用户，发帖、回复、点赞、收藏、通知和举报不再依赖固定演示用户。

## 需要完成的事情

- 后端发行和校验 HMAC JWT。
- Spring Security 使用 Bearer token 建立请求用户上下文。
- `CurrentUserProvider` 从认证上下文读取真实用户 ID。
- 写接口默认需要登录，管理接口需要管理员或版主角色。
- 前端登录页调用真实 `/api/auth/login` 和 `/api/auth/register`。
- 前端发帖页板块数据改为 API 优先。
- 前端受保护页面增加登录和角色守卫。
- 补充 JWT 和登录服务单元测试。
- 新增当前阶段开发手册，并补齐历史阶段手册。

## 已经如何完成

### 后端

- 新增 `AuthenticatedUser` 表示认证后的请求用户。
- 新增 `JwtService`，负责签发、解析、验签和过期判断。
- 新增 `JwtAuthenticationFilter`，从 `Authorization: Bearer <token>` 解析用户并写入 Spring Security 上下文。
- 更新 `SecurityConfig`：
  - `/api/auth/**`、健康检查、Agent 内部工具继续放行。
  - `GET /api/categories`、`GET /api/topics/**` 允许游客浏览。
  - `/api/admin/**` 需要 `ADMIN` 或 `MODERATOR`。
  - 其他接口需要登录。
- 更新 `AuthService`，登录和注册后返回真实 JWT。
- 更新 `CurrentUserProvider`，从 `SecurityContextHolder` 读取真实用户 ID。
- 新增 `V3__align_demo_passwords.sql`，把早期 seed 演示账号密码对齐为 `ThreadMind@123`。
- 在 `application.yml` 和 `application-demo.yml` 增加 `threadmind.security.jwt-secret`、`threadmind.security.token-ttl-seconds`。

### 前端

- 新增 `loginUser`、`registerUser`、`fetchCurrentUser` API 方法。
- 更新 Pinia session store，保存 token、username、nickname、roles。
- 登录页支持登录和注册两种模式，真实 API 失败时显示错误。
- 路由守卫保护发帖页、AI 助手页和管理页。
- 发帖页板块列表改为 `fetchCategories()` API 优先，保留 mock fallback。
- 顶部导航显示当前昵称，并提供退出登录按钮。

### 测试

- 新增 `JwtServiceTest`：
  - 验证 token 能解析回用户。
  - 验证篡改签名会被拒绝。
  - 验证过期 token 会被拒绝。
- 新增 `AuthServiceTest`：
  - 验证登录成功会返回 JWT 和用户摘要。

## 本地使用手册

1. 确认根目录 `.env` 或运行环境包含：

```powershell
JWT_SECRET=replace-with-local-secret
VITE_API_BASE_URL=http://localhost:8080/api
```

2. 启动 MySQL。

```powershell
cd infra
docker compose up -d
```

3. 启动 Java 后端。

```powershell
cd backend-java
mvn spring-boot:run
```

4. 启动前端。

```powershell
cd frontend-vue
npm run dev
```

5. 使用演示账号登录：

```text
username: demo
password: ThreadMind@123
```

6. 验证真实闭环：

- 打开首页，确认板块和帖子可浏览。
- 登录后进入发帖页，发布一条主题。
- 进入新主题详情页，提交回复。
- 点击点赞或收藏，确认请求不再依赖固定用户。
- 打开通知入口或调用 `/api/notifications`，确认返回当前登录用户通知。

## 本地验证

```powershell
cd backend-java
mvn test
```

如果系统没有安装 Maven，使用：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

```powershell
cd frontend-vue
npm run typecheck
```

```powershell
cd agent-python
python -m pytest
```

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/common/security/JwtService.java`
- `backend-java/src/main/java/com/threadmind/forum/common/security/JwtAuthenticationFilter.java`
- `backend-java/src/main/java/com/threadmind/forum/common/security/CurrentUserProvider.java`
- `backend-java/src/main/java/com/threadmind/forum/common/security/SecurityConfig.java`
- `backend-java/src/main/java/com/threadmind/forum/auth/service/AuthService.java`
- `frontend-vue/src/stores/session.ts`
- `frontend-vue/src/pages/LoginPage.vue`
- `frontend-vue/src/pages/TopicEditorPage.vue`
- `frontend-vue/src/router/index.ts`
- `docs/development-manuals/README.md`

## 遗留风险

- 当前 JWT 没有做刷新 token 和服务端撤销列表，适合本地 MVP，不适合直接生产。
- 前端 API 仍保留 mock fallback，后续需要为真实联调增加更明确的错误状态。
- Java 还缺数据库集成测试，后续建议用 Testcontainers 或本地测试库覆盖发帖、回复、点赞、举报事务。
- AI 会话表和工具调用表还没有在 Java Agent Gateway 中落审计记录。
