# API 契约草案

版本：v0.1  
适用阶段：M1-M10

## 1. 基础约定

前端只访问 Java 后端的 `/api/**`。Python Agent 只接受 Java 后端的内部调用，不面向浏览器开放。

基础地址：

- Vue 开发环境：`http://localhost:5173`
- Java API：`http://localhost:8080/api`
- Python Agent：`http://localhost:8000`

登录态：

- `/api/auth/login` 和 `/api/auth/register` 返回 `token`。
- 需要登录的接口使用请求头 `Authorization: Bearer <token>`。
- 游客可访问公开板块和帖子读取接口。
- 发帖、回复、点赞、收藏、通知、举报和 AI 助手接口需要登录。
- `/api/admin/**` 需要 `ADMIN` 或 `MODERATOR` 角色。

通用响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "req-20260804-000001"
}
```

分页响应：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

## 2. 错误码

| code | HTTP | 说明 |
| --- | --- | --- |
| OK | 200 | 成功 |
| BAD_REQUEST | 400 | 参数不合法 |
| UNAUTHORIZED | 401 | 未登录或登录失效 |
| FORBIDDEN | 403 | 无权限 |
| NOT_FOUND | 404 | 资源不存在 |
| CONFLICT | 409 | 状态冲突、重复操作 |
| RATE_LIMITED | 429 | 请求过快 |
| AGENT_UNAVAILABLE | 503 | AI 服务不可用 |
| INTERNAL_ERROR | 500 | 服务异常 |

## 3. 账号接口

### POST `/api/auth/register`

请求：

```json
{
  "username": "lin",
  "email": "lin@example.com",
  "password": "ThreadMind@123",
  "nickname": "林同学"
}
```

响应 `data`：

```json
{
  "token": "demo.jwt.token",
  "user": {
    "id": 1,
    "username": "lin",
    "nickname": "林同学",
    "avatarUrl": "",
    "roles": ["USER"]
  }
}
```

### POST `/api/auth/login`

请求：

```json
{
  "username": "lin",
  "password": "ThreadMind@123"
}
```

响应同注册接口。

### GET `/api/users/me`

返回当前登录用户。M1 骨架中可以先返回演示用户，正式实现时改为 JWT 解析。

## 4. 论坛接口

### GET `/api/categories`

响应 `data`：

```json
[
  {
    "id": 1,
    "name": "开发讨论",
    "slug": "dev",
    "description": "Java、Vue、Python、数据库和部署经验。",
    "topicCount": 128
  }
]
```

### GET `/api/topics`

查询参数：

| 参数 | 说明 |
| --- | --- |
| `categoryId` | 可选，板块 ID |
| `keyword` | 可选，标题或正文关键词 |
| `sort` | `latest`、`hot`、`unanswered` |
| `page` | 页码，默认 1 |
| `pageSize` | 每页条数，默认 20 |

响应 `data` 为分页结构，`items` 内元素：

```json
{
  "id": 101,
  "title": "Spring Boot 和 Agent 服务边界怎么划分",
  "excerpt": "讨论 Java 主业务和 Python Agent 之间的职责...",
  "category": {
    "id": 1,
    "name": "开发讨论",
    "slug": "dev"
  },
  "author": {
    "id": 9,
    "nickname": "北桥"
  },
  "tags": ["Spring Boot", "Agent"],
  "replyCount": 18,
  "likeCount": 42,
  "favoriteCount": 7,
  "viewCount": 980,
  "status": "PUBLISHED",
  "createdAt": "2026-08-04T12:00:00"
}
```

### POST `/api/topics`

请求：

```json
{
  "categoryId": 1,
  "title": "我的第一个帖子",
  "content": "Markdown 正文",
  "tagNames": ["Java", "Vue3"]
}
```

响应 `data`：

```json
{
  "id": 101
}
```

### GET `/api/topics/{id}`

返回帖子详情、作者、标签、互动状态和 AI 摘要缓存。

### POST `/api/topics/{id}/replies`

请求：

```json
{
  "content": "回复正文"
}
```

### GET `/api/topics/{id}/replies`

返回一级回复分页列表。

## 5. 互动接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/topics/{id}/like` | 点赞帖子 |
| DELETE | `/api/topics/{id}/like` | 取消点赞 |
| GET | `/api/topics/{id}/interaction-status` | 当前用户对主题的点赞和收藏状态 |
| POST | `/api/replies/{id}/like` | 点赞回复 |
| DELETE | `/api/replies/{id}/like` | 取消点赞回复 |
| GET | `/api/replies/{id}/interaction-status` | 当前用户对回复的点赞状态 |
| POST | `/api/topics/{id}/favorite` | 收藏帖子 |
| DELETE | `/api/topics/{id}/favorite` | 取消收藏 |
| GET | `/api/users/me/favorites` | 当前用户收藏主题列表 |
| GET | `/api/notifications` | 通知列表 |
| GET | `/api/notifications/unread-count` | 当前用户未读通知数量 |
| POST | `/api/notifications/{id}/read` | 标记已读 |

互动状态响应 `data`：

```json
{
  "targetType": "TOPIC",
  "targetId": 101,
  "liked": true,
  "favorited": true,
  "likeCount": 8,
  "favoriteCount": 3
}
```

未读通知响应 `data`：

```json
{
  "unreadCount": 2
}
```

## 6. AI 接口

Java 对前端暴露 `/api/agent/**`，Java 内部调用 Python `/agent/**`。
M4 起 Java Agent Gateway 会把 AI 会话、用户消息、助手消息和工具调用摘要写入 `ai_sessions`、`ai_messages`、`ai_tool_calls`，前端响应会返回 `sessionId` 方便排查。M5 起帖子摘要默认读取 `topic_ai_summaries` 缓存，刷新接口才重新调用 Agent；AI 接口具备单机内存限流和短暂熔断保护。

### POST `/api/agent/chat`

请求：

```json
{
  "message": "最近大家怎么讨论 Agent 和 Java 的边界？",
  "categoryId": 1,
  "sessionId": "optional-session-id"
}
```

响应 `data`：

```json
{
  "answer": "最近的讨论主要集中在权限、审计和工具调用边界上。",
  "citations": [
    {
      "topicId": 101,
      "title": "Spring Boot 和 Agent 服务边界怎么划分"
    }
  ],
  "toolCalls": [
    {
      "toolName": "search_topics",
      "status": "OK",
      "elapsedMs": 32,
      "summary": "found 3 topics",
      "error": null
    }
  ],
  "sessionId": 501
}
```

### POST `/api/agent/polish`

请求：

```json
{
  "title": "agent 怎么和 java 配合",
  "content": "我想问一下服务边界怎么设计...",
  "tone": "clear"
}
```

响应 `data`：

```json
{
  "title": "Agent 与 Java 后端的服务边界如何设计",
  "content": "润色后的 Markdown 正文",
  "notes": ["标题更具体", "保留原有问题，不替用户下结论"]
}
```

### POST `/api/agent/suggest-tags`

返回 3-5 个候选标签。

### POST `/api/agent/summarize-topic`

请求：

```json
{
  "topicId": 101
}
```

返回摘要、主要观点、可继续追问的问题、引用和工具调用轨迹。

默认行为：

- 如果存在未过期缓存，返回缓存摘要，`toolCalls[0].toolName=summary-cache`。
- 如果没有缓存，Java 调用 Python Agent，成功后写入新缓存。
- 如果 Agent 失败且存在旧缓存，返回旧缓存并附加失败工具摘要。

响应 `data`：

```json
{
  "summary": "这条主题主要讨论 Java 和 Python Agent 的职责边界。",
  "points": ["Java 保留业务状态", "Python Agent 只调用受控工具"],
  "followUpQuestions": ["是否需要把摘要写入缓存？"],
  "citations": [
    {
      "topicId": 101,
      "title": "Spring Boot 和 Agent 服务边界怎么划分"
    }
  ],
  "toolCalls": [
    {
      "toolName": "get_topic_context",
      "status": "OK",
      "elapsedMs": 18,
      "summary": "loaded topic with 2 replies",
      "error": null
    }
  ],
  "sessionId": 502
}
```

### POST `/api/agent/summarize-topic/refresh`

请求同 `/api/agent/summarize-topic`。该接口跳过新鲜缓存，尝试重新调用 Agent 并写入新缓存；如果刷新失败，优先返回旧缓存，不覆盖旧摘要。

### POST `/api/agent/moderation-check`

请求：

```json
{
  "targetType": "TOPIC",
  "content": "待检查内容"
}
```

响应 `data`：

```json
{
  "riskLevel": "LOW",
  "reasons": [],
  "suggestedAction": "ALLOW"
}
```

## 7. 内部 Agent 协议

Java 调 Python 时添加：

- `X-Internal-Token`
- `X-Trace-Id`

Python 返回结构必须能被 Java 反序列化。模型原始自然语言不能直接透传给前端，至少要经过 schema 校验。

工具调用结构：

```json
{
  "toolName": "search_topics",
  "status": "OK",
  "elapsedMs": 24,
  "summary": "found 3 topics",
  "error": null
}
```

失败时 `status` 使用 `FAILED` 或 `UNAVAILABLE`，`error` 只返回摘要，不返回密钥、内部 token 或完整堆栈。

### GET `/api/internal/agent/topics/search`

内部工具接口，只允许 Python Agent 调用。

请求头：

- `X-Internal-Token`
- `X-Trace-Id`

查询参数：

| 参数 | 说明 |
| --- | --- |
| `keyword` | 搜索关键词 |
| `limit` | 返回条数，默认 5 |

响应 `data`：

```json
{
  "topics": []
}
```

设计意图：Agent 搜索站内内容时必须走 Java 权限边界，不直接访问 MySQL。

### GET `/api/internal/agent/topics/{id}/context`

内部工具接口，返回帖子详情和最多 20 条可见回复。

响应 `data`：

```json
{
  "topic": {},
  "replies": []
}
```

设计意图：摘要和问答需要上下文，但上下文应由 Java 选择和脱敏。

## 8. AI 审计与历史接口

### GET `/api/admin/ai/metrics`

需要 `ADMIN` 或 `MODERATOR`。响应 `data`：

```json
{
  "sessionsToday": 8,
  "completedToday": 6,
  "fallbackToday": 1,
  "toolCallsToday": 14,
  "failedToolCallsToday": 1,
  "cachedSummaries": 3,
  "staleSummaries": 0
}
```

### GET `/api/admin/ai/sessions`

需要 `ADMIN` 或 `MODERATOR`。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `scene` | 可选，如 `CHAT`、`SUMMARY` |
| `status` | 可选，如 `COMPLETED`、`FALLBACK` |
| `page` | 页码，默认 1 |
| `pageSize` | 每页条数，默认 20，最大 100 |

响应 `data` 为分页结构，`items` 内元素：

```json
{
  "sessionId": 501,
  "userId": 1,
  "nickname": "演示用户",
  "scene": "CHAT",
  "status": "COMPLETED",
  "traceId": "req-20260819-001",
  "createdAt": "2026-08-19T10:00:00Z",
  "updatedAt": "2026-08-19T10:00:05Z",
  "messageCount": 2,
  "toolCallCount": 1
}
```

### GET `/api/admin/ai/sessions/{id}`

返回 session 基本信息、消息列表和工具调用详情。工具输入输出只返回摘要字段，不返回内部 token 或完整堆栈。

### GET `/api/users/me/ai/sessions`

返回当前用户自己的 AI session 分页列表。普通用户不能查看他人的 session。

### GET `/api/users/me/ai/sessions/{id}`

返回当前用户自己的 AI session 详情。不存在或不属于当前用户时返回 404。

## 9. 模块设计说明

每个模块的设计理念和功能取舍见：

- `docs/modules/auth-user.md`
- `docs/modules/forum-content.md`
- `docs/modules/interaction-notification.md`
- `docs/modules/moderation.md`
- `docs/modules/agent.md`
- `docs/modules/frontend.md`
