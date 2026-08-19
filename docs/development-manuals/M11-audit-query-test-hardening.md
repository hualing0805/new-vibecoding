# M11 AI 审计查询测试补强开发手册

## 阶段目标

补强 M5-M8 新增 AI 审计查询能力的自动化测试，优先覆盖用户权限边界、管理员详情查询、工具调用 JSON 摘要解析和分页参数收敛。M11 的重点是让审计查询层可回归，而不是引入新的运行时依赖。

## 需要完成的事情

- 当前用户只能通过 `findUserSession(userId, sessionId)` 查询自己的 AI session。
- session 不存在或不属于当前用户时返回 404，且不继续读取消息和工具调用。
- 管理员 session 详情能正确组装消息和工具调用摘要。
- 工具调用输入/输出 JSON 损坏时返回安全摘要，不抛出内部异常。
- 审计查询筛选参数使用稳定的 `Locale.ROOT` 大写规范化。

## 已经如何完成

- 新增 `AgentAuditQueryServiceTest`，用 Mockito 覆盖用户列表、用户详情 404、管理员详情和损坏 JSON 四类行为。
- `AgentAuditQueryRepository` 的 `scene`、`status` 规范化改为 `toUpperCase(Locale.ROOT)`，避免受系统区域设置影响。
- 本轮没有引入 Testcontainers 或 Docker 依赖。原因是当前项目的常规测试应能在没有 Docker daemon、没有 MySQL 镜像下载权限的机器上稳定运行；真正的数据库迁移集成测试建议作为 M12 单独阶段处理。

## 本地使用或验收步骤

执行：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

## 本地验证

- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过，22 个测试成功。
- `git diff --check` 待提交前执行。
- 未执行 Vue 和 Python 测试，因为本轮没有修改前端或 Python Agent。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/repository/AgentAuditQueryRepository.java`
- `backend-java/src/test/java/com/threadmind/forum/agent/service/AgentAuditQueryServiceTest.java`

## 遗留风险

- 还没有真实 MySQL/Flyway 级别的迁移集成测试，无法在单元测试里证明 V1-V4 在空库上完整迁移成功。
- `AgentAuditQueryRepository` 的 SQL 仍主要通过服务层行为测试间接保护，后续应补数据库级 repository 测试。

## 下一步建议

- M12 建议专门做数据库集成测试方案：优先评估 Testcontainers MySQL；如果本机 Docker 不稳定，再退到独立本地 MySQL profile 和手动验收脚本。
