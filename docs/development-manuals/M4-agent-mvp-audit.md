# M4 AI MVP 与审计落库开发手册

## 阶段目标

M4 的目标是把 ThreadMind 的 AI 能力从“可以调用 Agent”推进到“可审计、可解释、可降级的 AI MVP”。本阶段不引入 SSE、向量库或新的数据库表，优先复用已有 `ai_sessions`、`ai_messages`、`ai_tool_calls`。

## 需要完成的事情

- Java Agent Gateway 记录 AI 会话、用户输入、助手输出和工具调用摘要。
- Agent 服务不可用时继续返回可读 fallback，并记录失败审计。
- Python Agent 返回更稳定的工具调用轨迹，包括状态、耗时、摘要和错误摘要。
- AI 助手页展示引用、工具调用、耗时和审计 session。
- 帖子详情页展示摘要来源、工具调用和审计 session。
- 本地验收脚本加入 Agent chat 和 summarize-topic 调用。
- 检查前几阶段明显 bug 并修复。
- 规划 M5 开发路线。

## 已经如何完成

### Java 后端

- 新增 `AgentAuditRepository`：
  - `createSession`
  - `findOwnedSession`
  - `updateSessionStatus`
  - `createMessage`
  - `createToolCall`
- 新增 `AgentAuditService`：
  - 创建或复用当前用户 AI session。
  - 写入用户消息和助手消息。
  - 写入工具调用摘要、状态、耗时和错误摘要。
  - 审计写入失败只记录日志，不阻断主响应。
  - 对保存内容做长度限制，避免保存超长 prompt。
- 更新 `AgentService`：
  - `chat`、`polish`、`suggestTags`、`summarizeTopic`、`moderationCheck` 都接入审计。
  - Agent 成功时写 `COMPLETED`。
  - Agent 不可用时写 `FALLBACK`，并返回业务可读降级响应。
  - chat 和 summarize-topic 响应带 `sessionId`。
- 更新 `AgentDtos`：
  - `ToolCallDto` 新增 `elapsedMs`、`summary`、`error`。
  - `AgentChatResponse` 新增 `sessionId`。
  - `TopicSummaryResponse` 新增 `citations`、`toolCalls`、`sessionId`。
- 新增 `AgentServiceTest`：
  - 覆盖 chat 成功审计。
  - 覆盖 Agent 不可用时 fallback 审计。

### Python Agent

- `ToolCall` schema 新增：
  - `elapsedMs`
  - `summary`
  - `error`
- `TopicSummaryResponse` schema 新增：
  - `citations`
  - `toolCalls`
- `ForumAgent.chat`：
  - `search_topics` 成功时返回耗时和结果数量摘要。
  - 工具失败时返回 `FAILED` 和错误摘要。
- `ForumAgent.summarize_topic`：
  - `get_topic_context` 成功时返回引用和工具调用轨迹。
  - 工具失败时返回降级摘要和失败工具轨迹。
- `tests/test_agent.py` 增加工具轨迹响应测试。

### Vue 前端

- `types.ts` 新增 `AgentCitation`、`AgentToolCall`，并扩展 chat 和 summarize-topic 响应类型。
- `forum.ts` 更新 mock fallback：
  - AI chat 和 summary 返回引用、工具调用和 `sessionId`。
  - 修复 mock `sort=unanswered` 语义，从 `replyCount <= 10` 改为 `replyCount === 0`。
- `AssistantPage.vue`：
  - 展示审计 session。
  - 展示引用帖子。
  - 展示工具调用、状态、耗时、摘要和错误。
- `TopicDetailPage.vue`：
  - 摘要区展示 session、引用和工具调用。
- `base.css`：
  - 增加紧凑审计行样式。

### 脚本与文档

- `scripts/verify-local-flow.ps1` 增加：
  - `/api/agent/chat`
  - `/api/agent/summarize-topic`
- `docs/api-contracts.md` 同步 M4 响应结构。
- `docs/modules/agent.md` 同步审计边界、工具调用结构和后续方向。
- 新增 `docs/development-manuals/M5-ai-audit-console-plan.md` 规划 M5。

## 前几阶段 bug 检查与修复

- 修复前端 mock `sort=unanswered` 与后端语义不一致的问题。
- 确认 PowerShell 中看到的中文乱码不是源文件损坏，而是默认输出编码展示问题；文件按 UTF-8 读取正常。
- 确认 M3 联调脚本语法仍可解析，并扩展为 M4 验收脚本。

## 本地使用或验收步骤

1. 启动本地服务。

```powershell
.\scripts\start-local-stack.ps1
```

2. 执行联调验收。

```powershell
.\scripts\verify-local-flow.ps1
```

3. 打开前端。

```text
http://localhost:5173
```

4. 页面验收：

- 打开 `/assistant`，提问后确认右侧能看到 session、引用和工具调用。
- 打开任意帖子详情页，确认 AI 摘要区域能看到 session、引用和工具。
- 停掉 Python Agent 后再次请求 AI，确认 Java 仍返回 fallback，并且前端显示 `UNAVAILABLE`。
- 如果连接 MySQL，可检查 `ai_sessions`、`ai_messages`、`ai_tool_calls` 是否有记录。

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
C:\Users\16378\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest tests
```

```powershell
[scriptblock]::Create((Get-Content -Raw "scripts\verify-local-flow.ps1")).GetType().Name
```

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/service/AgentService.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/service/AgentAuditService.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/repository/AgentAuditRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/dto/AgentDtos.java`
- `backend-java/src/test/java/com/threadmind/forum/agent/service/AgentServiceTest.java`
- `agent-python/app/schemas/agent.py`
- `agent-python/app/agents/forum_agent.py`
- `agent-python/tests/test_agent.py`
- `frontend-vue/src/pages/AssistantPage.vue`
- `frontend-vue/src/pages/TopicDetailPage.vue`
- `frontend-vue/src/api/types.ts`
- `frontend-vue/src/api/forum.ts`
- `scripts/verify-local-flow.ps1`

## 遗留风险

- 还没有 AI 审计后台，当前只能通过响应 `sessionId` 和数据库排查。
- 审计保存的是摘要，不是完整 prompt；排查精度和隐私保护之间需要继续平衡。
- 还没有 AI 调用限流，M5 需要补基础限流和失败保护。
- 摘要没有缓存，帖子详情页每次进入都会请求 Agent。
- 没有 SSE 流式输出，回答仍是一次性返回。
- `ai_tool_calls` 仍缺少面向管理员的检索接口。

## 下一步建议

- 进入 M5：AI 审计台、会话历史、摘要缓存和限流。
- 给管理员增加 AI session 列表、消息详情和工具调用详情。
- 给普通用户增加自己的 AI 会话历史。
- 给帖子摘要增加缓存和手动刷新入口。
- 给 Agent 接口增加基础频率限制和更清晰的失败指标。
