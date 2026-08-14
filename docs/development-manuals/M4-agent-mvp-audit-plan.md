# M4 AI MVP 与审计落库开发规划

## 阶段定位

M4 的目标是把 ThreadMind 的 AI 能力从“可调用 Agent”推进到“可审计、可解释、可复盘的 AI MVP”。这一阶段仍坚持论坛主流程优先：AI 是辅助层，不直接修改业务状态。

M4 不追求复杂 Agent 框架，优先把现有 Java Agent Gateway、Python Agent 和 MySQL 审计表串成稳定闭环。

## 核心目标

- Java 记录每次 AI 会话、用户输入、Agent 输出和工具调用摘要。
- Python Agent 返回稳定的工具调用轨迹和引用。
- 前端展示 AI 回答的引用和工具状态。
- 发帖页润色、标签推荐、风险检查的失败状态可追踪。
- 帖子摘要和站内问答能基于 Java 内部工具读取授权上下文。
- AI 服务不可用时，论坛主流程继续可用。

## 功能范围

### 1. AI 审计落库

复用已有表：

- `ai_sessions`
- `ai_messages`
- `ai_tool_calls`

Java 后端建议新增：

- `AgentAuditRepository`
- `AgentAuditService`

记录内容：

- 会话场景：`CHAT`、`POLISH`、`SUGGEST_TAGS`、`SUMMARY`、`MODERATION_CHECK`
- 当前用户 ID。
- traceId。
- 用户输入摘要或脱敏内容。
- Agent 输出摘要。
- 工具调用名称、状态、输入摘要、输出摘要、耗时。

注意：

- 不把内部 token 写入数据库。
- 不无限制保存超长 prompt。
- 不记录未授权内容。

### 2. Agent Gateway 收口

当前 Java `AgentService` 已负责调用 Python Agent 和 fallback。M4 需要：

- 调用前创建或复用 `ai_session`。
- 调用后写入用户消息和助手消息。
- 如果 Python 返回 `toolCalls`，写入 `ai_tool_calls`。
- 如果 Agent 服务失败，也写入失败状态，便于排查。
- 返回给前端的结构保持兼容。

### 3. Python Agent 输出稳定化

Python Agent 需要继续保证：

- `chat` 返回 `answer`、`citations`、`toolCalls`。
- `summarize-topic` 返回摘要、要点、后续问题。
- `moderation-check` 返回风险等级、原因、建议动作。

M4 可补：

- 工具调用耗时。
- 工具失败原因摘要。
- 引用来源数量控制。

### 4. 前端 AI 体验

前端建议补：

- AI 助手页展示引用帖子。
- AI 助手页展示工具调用状态。
- 帖子详情页摘要失败时展示可重试入口。
- 发帖页 AI 操作显示明确失败状态。

M4 不做流式输出，避免同时引入 SSE 和审计复杂度。

## 推荐开发顺序

1. Java 新增 Agent 审计 repository/service。
2. Java `AgentService` 为 `chat`、`polish`、`suggest-tags`、`summarize-topic`、`moderation-check` 写审计。
3. Python Agent 补工具调用耗时和失败原因字段，保持兼容。
4. 前端 AI 助手页展示 citations 和 toolCalls。
5. 帖子详情页摘要增加重试状态。
6. 更新 API 契约、Agent 模块文档和 M4 开发手册。
7. 扩展 `verify-local-flow.ps1`，加入 Agent chat 和 summarize-topic 验收。

## 接口和数据注意事项

- 前端仍只访问 Java `/api/agent/**`。
- Python Agent 只接受 Java 内部调用。
- Python Agent 不直接写 MySQL。
- AI 审计写入失败不应导致论坛主流程失败，但应记录日志。
- `ai_tool_calls.input_json` 和 `output_json` 只保存摘要和必要结构，不保存密钥。

## 测试计划

Java：

- `AgentServiceTest`：
  - Agent 成功时写 session/message/tool audit。
  - Agent 失败 fallback 时写失败审计。
  - audit 写入异常不影响响应。
- `AgentAuditRepository` 可先用单元测试覆盖结构构造，后续再补数据库集成测试。

Python：

- `pytest` 覆盖工具调用成功、失败、空结果。

Vue：

- `npm.cmd run typecheck`
- `npm.cmd run build`
- 手工检查 AI 助手页引用和工具状态。

联调：

- 登录。
- 调用 `/api/agent/chat`。
- 调用 `/api/agent/summarize-topic`。
- 检查 `ai_sessions`、`ai_messages`、`ai_tool_calls` 有记录。

## M4 验收标准

- AI chat 调用后能在 MySQL 看到会话、消息和工具调用审计。
- Agent 服务不可用时，前端收到可读 fallback，数据库保留失败记录。
- AI 助手页能展示引用和工具状态。
- 帖子摘要仍通过 Java Gateway 调用 Python Agent。
- Python Agent 不直接访问 MySQL。
- Java、Vue、Python 检查通过。
- M4 开发手册写清楚完成范围、验证结果和遗留风险。

## 暂不放入 M4

- SSE 流式输出。
- 向量检索。
- 多模型选择 UI。
- RAG 文档库。
- 自动治理处罚。
- AI 自动发布或删除内容。

