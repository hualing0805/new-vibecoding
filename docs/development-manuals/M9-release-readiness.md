# M9 本地验收与发布收口开发手册

## 阶段目标

把 M5-M8 新增能力纳入本地验收脚本、API 契约和模块文档，确保后续开发不会只依赖聊天记录理解系统状态。

## 需要完成的事情

- 更新 `verify-local-flow.ps1` 覆盖 AI 缓存、刷新、用户历史、管理员审计和指标。
- 更新 API 契约，记录新增接口和字段。
- 更新 Agent、前端、治理模块文档。
- README 当前状态从 M4 推进到 M10 收口状态。

## 已经如何完成

- 联调脚本新增 `/agent/summarize-topic/refresh`、`/users/me/ai/sessions`、`/admin/ai/sessions` 和 `/admin/ai/metrics` 检查。
- API 契约适用阶段更新到 M10，并补充 AI 审计、摘要缓存、会话历史和管理指标。
- 模块文档同步说明前端搜索、治理标签、AI 审计和单机限流边界。

## 本地使用或验收步骤

1. 启动本地 MySQL、Java、Python Agent 和 Vue。
2. 执行 `.\scripts\verify-local-flow.ps1`。
3. 确认脚本输出新增 AI 审计步骤均为 OK。

## 本地验证

- 本轮已验证 Java 单元测试、Vue 类型检查、Vue production build、Python Agent pytest、UI 机械检测和 Playwright 视觉检查。
- 完整本地联调脚本未执行，因为需要用户本机同时启动数据库、Java、Python Agent 服务。

## 关键文件

- `scripts/verify-local-flow.ps1`
- `docs/api-contracts.md`
- `docs/modules/agent.md`
- `docs/modules/frontend.md`
- `docs/modules/moderation.md`
- `README.md`

## 遗留风险

- 本地联调脚本仍依赖 demo 管理员账号和本机服务端口，CI 化需要额外环境准备。

## 下一步建议

- M10 对前序核心逻辑增加自动化测试，优先覆盖缓存、限流和熔断。
