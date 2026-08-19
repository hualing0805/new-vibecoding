# M15 AI SSE 流式响应 README

## 阶段目标

让站内助手支持流式回答体验，同时保证现有非流式 Agent chat 接口不回归。

## 交付范围

- Java 新增或完善 `/api/agent/chat/stream`。
- Python Agent 提供稳定的流式输出协议。
- Vue 助手页支持消息流式渲染、结束态和错误态。
- 断流时可 fallback 到明确错误提示或非流式重试。
- 审计仍由 Java Gateway 统一记录。

## 实现顺序

1. 定义 SSE 事件类型和字段。
2. 先完成 Java 到 Python 的流式代理。
3. 再完成前端渲染和取消/重试行为。
4. 补充非流式回归测试。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

```powershell
cd frontend-vue
npm run typecheck
npm run build
```

需要手动或脚本验证 `/assistant` 流式输出不会造成布局溢出。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/`
- `agent-python/app/`
- `frontend-vue/src/`
- `docs/api-contracts.md`

## 遗留风险

- SSE 在代理、浏览器和本地网络异常下可能出现半开连接。
- 流式审计需要避免记录不完整或重复消息。

## 收口要求

M15 结束时，非流式接口必须继续可用；前端应能处理开始、增量、完成、错误和取消状态。
