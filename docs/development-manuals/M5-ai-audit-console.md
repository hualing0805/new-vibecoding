# M5 AI 审计台与体验完善开发手册

## 阶段目标

把 M4 已落库的 AI 审计数据变成可查看、可复用、可保护的产品能力。M5 完成后，管理员能查 AI session，普通用户能查自己的会话历史，帖子摘要支持缓存与刷新，Agent 高频调用和连续失败有基础保护。

## 需要完成的事情

- 管理员查询 AI session 列表、详情和工具调用。
- 当前用户查询自己的 AI session 列表和详情。
- 帖子摘要默认读取缓存，手动刷新才重新调用 Agent。
- Agent 调用增加用户维度限流和场景维度熔断。
- 前端管理台增加 AI 审计入口，助手页和帖子详情展示历史、刷新和缓存状态。

## 已经如何完成

- 新增 `topic_ai_summaries` 迁移表，保存摘要、要点、追问、生成者、过期时间和审计 session。
- 新增 `AgentAuditQueryRepository`、`AgentAuditQueryService` 和 `AgentAuditController`，提供管理员与用户侧只读接口。
- `AgentService` 在摘要生成前读取 24 小时内缓存；刷新失败时优先返回旧缓存，不覆盖已有摘要。
- 新增 `AgentProtectionService`，按用户 + 场景做 1 分钟 8 次限流，按场景连续 3 次失败后短暂熔断。
- 前端 `AdminDashboardPage.vue` 增加 AI 审计标签页；`AssistantPage.vue` 增加历史 session；`TopicDetailPage.vue` 增加摘要刷新按钮。

## 本地使用或验收步骤

1. 启动 MySQL、Java、Python Agent 和 Vue。
2. 使用 demo 管理员账号登录。
3. 打开 `/assistant` 发起一次问答，确认右侧出现 session。
4. 打开帖子详情，确认摘要首次生成后再次打开会命中缓存，点击刷新会调用刷新接口。
5. 打开 `/admin` 的 AI 审计标签，确认能查看 session、消息和工具调用详情。

## 本地验证

- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过，18 个测试成功。
- Vue 前端：`npm.cmd run typecheck` 通过。
- Vue 前端：`npm.cmd run build` 通过。
- Python Agent：`C:\Users\16378\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest tests` 通过，5 个测试成功。
- UI 检测：`node C:\Users\16378\.codex\skills\impeccable\scripts\detect.mjs --json ...` 无发现；`node .\scripts\visual-check.cjs` 覆盖桌面和移动端核心路由无横向溢出，并以 mock 登录态额外检查 `/assistant`、`/admin`、AI 审计标签无横向溢出。
- 联调脚本已更新，新增 AI 历史、摘要缓存、刷新和管理员审计检查；本次未执行完整联调脚本，因为需要本地三服务同时运行。

## 关键文件

- `backend-java/src/main/resources/db/migration/V4__ai_summary_cache.sql`
- `backend-java/src/main/java/com/threadmind/forum/agent/controller/AgentAuditController.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/service/AgentService.java`
- `backend-java/src/main/java/com/threadmind/forum/agent/service/AgentProtectionService.java`
- `frontend-vue/src/pages/AdminDashboardPage.vue`
- `frontend-vue/src/pages/AssistantPage.vue`
- `frontend-vue/src/pages/TopicDetailPage.vue`

## 遗留风险

- 限流和熔断当前是单机内存实现，多实例部署需要 Redis 或网关层承接。
- 摘要缓存固定 24 小时，后续可按主题活跃度或回复更新时间失效。

## 下一步建议

- M6 继续把顶部搜索接入真实帖子查询，并让助手历史可直接续聊。
