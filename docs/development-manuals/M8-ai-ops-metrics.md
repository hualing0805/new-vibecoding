# M8 AI 运营指标开发手册

## 阶段目标

补齐 AI 管理视角的概览指标，帮助管理员快速判断当天调用、fallback、工具失败和摘要缓存状态。

## 需要完成的事情

- 新增 AI 指标查询接口。
- 管理台 AI 审计标签展示今日会话、工具调用和摘要缓存。
- 展示 fallback 与失败工具调用数量。
- 指标仍只读，不改变业务状态。

## 已经如何完成

- `AgentAuditQueryRepository.metricsToday()` 聚合当天 session、完成数、fallback 数、工具调用、失败工具调用、有效缓存和过期缓存。
- `GET /api/admin/ai/metrics` 返回 `AiAuditMetricsDto`。
- 前端 `fetchAdminAiMetrics()` 接入真实接口，并在管理台 AI 审计标签展示指标卡。

## 本地使用或验收步骤

1. 触发一次 AI chat 和一次 topic summary。
2. 登录管理员打开 `/admin`。
3. 切换到 AI 审计标签，确认指标卡和 session 列表展示。

## 本地验证

- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过。
- Vue 前端：`npm.cmd run typecheck` 通过。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/dto/AgentAuditDtos.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/repository/AgentAuditQueryRepository.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/controller/AgentAuditController.java`
- `frontend-vue/src/pages/AdminDashboardPage.vue`

## 遗留风险

- 指标按数据库服务器日期计算，跨时区生产部署需要明确业务时区。

## 下一步建议

- M9 把新接口纳入本地联调脚本和阶段文档，形成可重复验收路径。
