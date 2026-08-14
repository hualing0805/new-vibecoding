# M5 AI 审计台与体验完善开发规划

## 阶段定位

M5 建议把 M4 已经写入数据库的 AI 审计数据变成可运营、可排查、可复用的产品能力。重点不是继续扩展模型能力，而是让 AI 调用可查看、可回放、可控成本，并减少重复请求。

## 核心目标

- 管理员可以查看 AI session、消息和工具调用。
- 普通用户可以查看自己的 AI 会话历史。
- 帖子摘要支持缓存，避免每次打开详情页都重新调用 Agent。
- Agent 接口有基础限流和失败保护。
- 前端 AI 体验更清晰：可重试、可刷新摘要、可查看历史。
- 保持 Python Agent 不直接访问 MySQL 的边界不变。

## 推荐开发范围

### 1. AI 审计后台

新增 Java 查询能力：

- `AgentAuditQueryRepository`
- `AgentAuditQueryService`
- `GET /api/admin/ai/sessions`
- `GET /api/admin/ai/sessions/{id}`

列表字段：

- sessionId
- userId / nickname
- scene
- status
- traceId
- createdAt
- updatedAt
- messageCount
- toolCallCount

详情字段：

- 用户消息摘要
- 助手消息摘要
- 工具调用列表
- 工具状态、耗时、输入摘要、输出摘要、错误摘要

### 2. 用户 AI 会话历史

新增接口：

- `GET /api/users/me/ai/sessions`
- `GET /api/users/me/ai/sessions/{id}`

限制：

- 用户只能查看自己的 session。
- 默认分页。
- 不展示内部错误堆栈和管理侧备注。

### 3. 帖子摘要缓存

建议新增或复用字段前先评估 schema：

- 可选方案 A：新增 `topic_ai_summaries` 表。
- 可选方案 B：在 `topics.ai_summary` 已存在时复用字段。

推荐优先方案 A，便于保存版本、生成时间、模型来源和审计 session：

- topicId
- summary
- pointsJson
- followUpQuestionsJson
- auditSessionId
- generatedBy
- generatedAt
- expiresAt

前端详情页：

- 默认读取缓存。
- 用户点击刷新时才重新调用 Agent。
- 刷新失败保留旧摘要。

### 4. 限流和失败保护

Java 层先实现轻量内存限流：

- 按用户 ID + scene 限制短时间调用次数。
- 超限返回 429。
- 限流事件写入日志，M6 再考虑 Redis 或更正式的限流组件。

失败保护：

- Agent 连续失败时短暂熔断。
- 熔断期间直接返回 fallback，并写 `FALLBACK` 审计。

### 5. 前端体验

- `/assistant` 增加会话历史入口。
- 帖子详情页摘要区增加刷新按钮。
- 管理台增加 AI 审计标签页。
- 工具调用错误以摘要展示，不直接暴露内部堆栈。
- 长工具输出折叠展示。

## 推荐开发顺序

1. 后端查询 DTO 和 repository，先读 `ai_sessions`、`ai_messages`、`ai_tool_calls`。
2. 管理端 AI 审计接口。
3. 用户自己的 AI 会话历史接口。
4. 前端管理台 AI 审计页。
5. 前端助手页历史入口。
6. 摘要缓存表和缓存读取逻辑。
7. 摘要刷新入口。
8. 基础限流和失败保护。
9. 更新 API 契约、模块文档、M5 开发手册和验收脚本。

## 测试计划

Java：

- 管理员可以分页查询 AI sessions。
- 普通用户不能访问 `/api/admin/ai/**`。
- 用户只能查看自己的 AI sessions。
- AI session 详情能返回 messages 和 toolCalls。
- 摘要缓存命中时不调用 Python Agent。
- 刷新摘要时写入新缓存和审计。
- 限流超限返回 429。

Vue：

- `npm.cmd run typecheck`
- `npm.cmd run build`
- 管理台 AI 审计页字段展示完整。
- 助手页历史入口不会遮挡移动端布局。
- 摘要刷新失败时旧摘要仍保留。

Python：

- 继续保持 `pytest tests`。
- 确认工具调用字段兼容旧响应。

联调：

- 执行 `verify-local-flow.ps1`。
- 触发一次 chat，一次 summarize-topic。
- 检查管理台能看到 AI session。
- 刷新帖子摘要并检查缓存记录。

## M5 验收标准

- 管理员能在前端查看 AI 调用记录和工具调用详情。
- 普通用户能查看自己的 AI 会话历史，不能查看他人记录。
- 帖子摘要有缓存，重复打开详情页不必每次调用 Agent。
- 手动刷新摘要可生成新审计 session。
- Agent 失败时仍保留旧摘要或 fallback。
- 高频调用会被基础限流拦截。
- Java、Vue、Python 检查通过。
- M5 开发手册、API 契约和模块文档同步完成。

## 暂不放入 M5

- SSE 流式输出。
- 向量检索和 RAG 文档库。
- 多模型路由和模型选择 UI。
- 成本账单和 token 级统计。
- Redis 分布式限流。
- AI 自动治理处罚。
