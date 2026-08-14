# M3 互动与通知开发手册

## 阶段目标

M3 的目标是把 ThreadMind 的论坛基础流程从“可以发帖回复”推进到“有持续互动反馈”。本阶段聚焦点赞、收藏、通知中心、未读状态、收藏列表和热门排序，不引入 Redis、WebSocket 或新的数据库表。

## 需要完成的事情

- 后端新增当前用户对主题和回复的互动状态查询。
- 后端新增当前用户收藏主题列表。
- 后端新增未读通知数量接口。
- 后端修正 `sort=hot` 和 `sort=unanswered` 的排序/筛选语义。
- 后端明确通知规则：自己回复自己不通知，首次点赞才通知，点赞自己的内容不通知。
- 前端帖子详情页接真实点赞/收藏状态，支持取消点赞和取消收藏。
- 前端新增通知中心页面，支持标记已读。
- 前端新增我的收藏页面。
- 顶部通知入口显示未读数量。
- 更新 API 契约、模块文档和本地验收脚本。
- 规划 M4 开发路线。

## 已经如何完成

### 后端

- `InteractionRepository` 新增：
  - `getTopicStatus`
  - `getReplyStatus`
  - `findTopicAuthorId`
  - `findReplyAuthorId`
- `InteractionService` 新增：
  - 查询主题互动状态。
  - 查询回复互动状态。
  - 首次点赞时通知内容作者。
  - 内容作者和操作者相同则不通知。
- `InteractionController` 新增：
  - `GET /api/topics/{id}/interaction-status`
  - `GET /api/replies/{id}/interaction-status`
- `NotificationRepository`、`NotificationService`、`NotificationController` 新增未读数：
  - `GET /api/notifications/unread-count`
- `ForumRepository` 新增收藏主题列表：
  - `listFavoriteTopics`
  - `sort=hot` 使用点赞、回复、收藏、浏览综合分。
  - `sort=unanswered` 只返回未回复主题。
- `ForumService` 新增收藏列表服务，并避免用户回复自己主题时通知自己。
- 新增 `UserContentController`：
  - `GET /api/users/me/favorites`

### 前端

- `frontend-vue/src/api/types.ts` 新增：
  - `InteractionStatus`
  - `NotificationUnreadCount`
- `frontend-vue/src/api/forum.ts` 新增：
  - 收藏列表 API。
  - 互动状态 API。
  - 取消点赞、取消收藏。
  - 通知未读数和标记已读。
- `TopicDetailPage.vue`：
  - 加载真实互动状态。
  - 点赞按钮支持点赞/取消点赞。
  - 收藏按钮支持收藏/取消收藏。
  - 按后端返回结果刷新计数。
- `NotificationsPage.vue`：
  - 展示通知列表。
  - 展示未读状态。
  - 支持单条标记已读。
- `FavoritesPage.vue`：
  - 展示当前用户收藏主题。
  - 复用 `TopicList`。
- `AppShell.vue` 和 `session.ts`：
  - 顶部通知入口显示未读数量。
  - 标记已读后刷新共享未读数。
- `router/index.ts`：
  - 新增 `/notifications`。
  - 新增 `/favorites`。

### 脚本与文档

- `scripts/verify-local-flow.ps1` 扩展：
  - 查询互动状态。
  - 取消点赞。
  - 查询收藏列表。
  - 取消收藏。
  - 查询未读数。
  - 标记通知已读。
- `docs/api-contracts.md` 同步 M3 新接口。
- `docs/modules/interaction-notification.md` 同步 M3 规则和后续扩展点。
- 新增 `docs/development-manuals/M4-agent-mvp-audit-plan.md` 规划 M4。

## 本地使用或验收步骤

1. 启动本地服务。

```powershell
.\scripts\start-local-stack.ps1
```

2. 执行核心验收。

```powershell
.\scripts\verify-local-flow.ps1
```

3. 打开前端。

```text
http://localhost:5173
```

4. 使用 seed 管理员登录。

```text
username: demo
password: ThreadMind@123
```

5. 页面验收：

- 打开帖子详情页，确认点赞和收藏按钮能显示已点赞/已收藏状态。
- 再次点击点赞或收藏，确认可以取消并刷新计数。
- 打开 `/notifications`，确认通知列表和未读状态可见。
- 标记通知已读，确认顶部 badge 数量刷新。
- 打开 `/favorites`，确认收藏主题列表可见。
- 首页切换“热门”和“待回复”，确认列表返回正常。

## 本地验证

本阶段执行：

```powershell
git diff --check
```

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

```powershell
cd frontend-vue
npm.cmd run typecheck
npm.cmd run build
```

```powershell
cd agent-python
python -m pytest
```

如果当前机器无系统 Python，可使用 Codex bundled Python 执行 Agent 测试。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/interaction/controller/InteractionController.java`
- `backend-java/src/main/java/com/threadmind/forum/interaction/service/InteractionService.java`
- `backend-java/src/main/java/com/threadmind/forum/interaction/repository/InteractionRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/notification/controller/NotificationController.java`
- `backend-java/src/main/java/com/threadmind/forum/notification/service/NotificationService.java`
- `backend-java/src/main/java/com/threadmind/forum/forum/repository/ForumRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/auth/controller/UserContentController.java`
- `frontend-vue/src/pages/TopicDetailPage.vue`
- `frontend-vue/src/pages/NotificationsPage.vue`
- `frontend-vue/src/pages/FavoritesPage.vue`
- `frontend-vue/src/app/AppShell.vue`
- `frontend-vue/src/stores/session.ts`
- `scripts/verify-local-flow.ps1`

## 遗留风险

- 仍未加入 WebSocket，通知不会实时推送，只在路由切换和页面操作后刷新。
- 收藏列表只有第一页，前端还没有分页控件。
- 回复点赞状态没有在回复列表逐条展示，后续可补充。
- 互动通知内容目前是通用文案，尚未包含跳转目标和操作者昵称。
- Repository SQL 仍主要靠服务层单测保护，后续需要真实数据库集成测试。

## 下一步建议

- 进入 M4：AI MVP 与审计落库。
- 把 `/api/agent/**` 的请求、响应、工具调用写入 `ai_sessions`、`ai_messages`、`ai_tool_calls`。
- 前端展示 AI 回答引用和工具轨迹。
- 给 Agent 失败和超时增加更清晰的审计记录。

