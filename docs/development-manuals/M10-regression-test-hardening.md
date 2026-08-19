# M10 回归测试补强开发手册

## 阶段目标

对 M5-M9 和先前 Agent 开发的关键行为补自动化测试，减少后续改动破坏缓存、审计和保护逻辑的风险。

## 需要完成的事情

- 更新 AgentService 测试以适配限流、缓存和熔断依赖。
- 增加摘要缓存命中测试，确保缓存命中不调用 Python Agent。
- 增加限流测试，确保短窗口超限返回 429。
- 增加熔断测试，确保连续失败后场景进入保护状态，成功后可恢复。

## 已经如何完成

- `AgentServiceTest` 新增 `summarizeTopicUsesFreshCacheWithoutCallingAgent`。
- `AgentServiceTest` 继续覆盖 chat 成功审计和 fallback 审计，并验证保护服务成功/失败记录。
- 新增 `AgentProtectionServiceTest` 覆盖短窗口限流、窗口过期恢复、连续失败熔断和成功重置。

## 本地使用或验收步骤

执行：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

## 本地验证

- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过，18 个测试成功。
- Vue 前端：`npm.cmd run typecheck` 通过。
- Vue 前端：`npm.cmd run build` 通过。
- Python Agent：`C:\Users\16378\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest tests` 通过，5 个测试成功。
- UI 检测：Impeccable detector 无发现，Playwright 核心路由和登录态管理/助手视图无横向溢出。

## 关键文件

- `backend-java/src/test/java/com/threadmind/forum/agent/service/AgentServiceTest.java`
- `backend-java/src/test/java/com/threadmind/forum/agent/service/AgentProtectionServiceTest.java`

## 遗留风险

- AI 审计查询 repository 目前未接入真实数据库集成测试；后续可引入 Testcontainers 或本地 MySQL profile。

## 下一步建议

- 后续如果进入 M11，建议优先做 Testcontainers 级数据库迁移和 repository 集成测试。
