# AI Agent 模块

## 职责

Agent 模块负责把 AI 能力接入论坛，但不让 AI 绕过业务规则。它分为 Java Agent Gateway 和 Python Agent 两部分。

第一阶段功能：

- 站内问答。
- 发帖润色。
- 标签推荐。
- 帖子摘要。
- 内容风险提示。
- 内部工具：搜索帖子、读取帖子上下文。
- 审计落库：记录 AI session、用户消息、助手消息和工具调用摘要。
- 审计查询：管理员可查全站 AI session，用户可查自己的 AI 历史。
- 摘要缓存：帖子摘要写入 `topic_ai_summaries`，默认命中缓存，刷新时重新生成。
- 保护能力：Java 网关按用户 + 场景限流，按场景连续失败短暂熔断。

## 设计思路

Java 对前端暴露 `/api/agent/**`，Python 只接受 Java 内部调用。这样前端不用知道 Agent 服务地址，Agent 也不能脱离 Java 权限体系访问业务数据。

Python Agent 不直接连接 MySQL。它通过 Java 的内部工具接口拿到授权后的帖子和回复。这个约束比开发速度更重要，因为论坛内容天然涉及隐私、隐藏状态和治理备注。

M4 起，Java Agent Gateway 也是 AI 审计边界。Python 只返回 `answer`、`citations`、`toolCalls` 等结构化结果；Java 负责把可保存的摘要写入 `ai_sessions`、`ai_messages`、`ai_tool_calls`。审计失败不会阻断前端响应，但要写日志，便于后续排查。

M5 起，AI 调用还要经过 Java 侧保护层。限流失败返回 429，不调用 Python；连续失败触发短暂熔断，熔断期间返回 fallback 并写 `FALLBACK` 审计。这个实现是单机内存版，适合本地开发，生产多实例需要迁移到 Redis、网关或集中式限流组件。

## 功能设计

### 站内问答

输入用户问题和可选板块，Agent 通过工具搜索帖子，再组织回答。

设计理念：回答必须基于可见内容。没有检索结果时，要说明信息不足，而不是编造站内共识。

M4 返回内容包含：

- `answer`：助手回答。
- `citations`：引用帖子。
- `toolCalls`：工具名称、状态、耗时、摘要和失败原因。
- `sessionId`：Java 审计会话 ID。

### 发帖润色

输入标题、正文和语气偏好，返回建议标题、建议正文和说明。

设计理念：润色不新增事实，不替用户发布。前端应让用户手动采纳。

### 标签推荐

输入标题、正文和板块，返回候选标签。

设计理念：标签是内容组织的一部分，AI 只推荐，不自动写死。用户可以删改。

### 帖子摘要

输入帖子 ID，返回摘要、要点和可追问问题。

设计理念：摘要帮助读者快速进入讨论，但不应该替代原帖。详情页仍以正文和回复为主。

M4 摘要响应同样返回引用、工具调用和 `sessionId`。前端详情页应展示这些轨迹，让用户知道摘要来自哪个上下文。

M5 起摘要优先读取缓存。缓存命中时不会调用 Python Agent，工具轨迹显示 `summary-cache`；手动刷新才调用 `/api/agent/summarize-topic/refresh`。刷新失败时保留旧缓存，避免详情页体验因为 Agent 波动而退化。

### 审计后台与用户历史

管理员接口：

- `GET /api/admin/ai/metrics`
- `GET /api/admin/ai/sessions`
- `GET /api/admin/ai/sessions/{id}`

用户接口：

- `GET /api/users/me/ai/sessions`
- `GET /api/users/me/ai/sessions/{id}`

设计理念：管理员看全站调用和工具错误，用于排查；普通用户只看自己的历史，用于续聊和理解 AI 输出来源。

### 风险提示

输入内容，返回风险等级、原因和建议动作。

设计理念：风险提示是复核工具，不是处罚工具。真正隐藏、锁定、警告必须走治理模块。

### 内部工具

`search_topics` 搜索可见帖子，`get_topic_context` 获取帖子和回复上下文。

设计理念：工具返回结构化数据，便于审计和测试。工具接口使用 `X-Internal-Token`，避免被浏览器直接调用。

工具调用约定：

- `status=OK` 表示工具成功返回。
- `status=FAILED` 表示工具执行失败但 Agent 可以降级回答。
- `status=UNAVAILABLE` 表示 Python Agent 服务整体不可用，由 Java fallback 生成。
- `elapsedMs` 记录工具调用耗时。
- `summary` 和 `error` 必须是摘要，不保存内部 token、完整堆栈或超长 prompt。

## 后续实现要点

- 模型返回需要 schema 校验。
- 支持无模型 Key 时的 mock 响应，保证本地开发不断。
- 正式环境要继续加强 prompt、工具输入和输出脱敏。
- 后续建议把限流迁移到 Redis 或网关层，并增加数据库级 repository 集成测试。
