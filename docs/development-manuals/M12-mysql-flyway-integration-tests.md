# M12 MySQL/Flyway 集成测试方案 README

## 阶段目标

证明 V1-V4 数据库迁移可以在空库上完整运行，并让失败原因能被开发者快速定位。M12 只处理迁移和基础连接验证，不扩展业务功能。

## 交付范围

- 评估当前机器是否具备 Docker/Testcontainers MySQL 条件。
- 如果 Docker 条件稳定，新增 Testcontainers 级 Flyway migrate 测试。
- 如果 Docker 条件不稳定，建立本地 MySQL test profile 和可重复执行的验证脚本。
- 覆盖最小连接链路：启动测试库、执行迁移、确认关键表存在。
- 文档化运行前置条件、常见失败原因和清理方式。

## 实现顺序

1. 阅读 `backend-java/pom.xml`、`backend-java/src/main/resources/db/migration/` 和现有测试配置。
2. 探测 Docker/Testcontainers 可用性，并决定默认方案和 fallback。
3. 增加迁移集成测试或本地 test profile。
4. 将验证命令写入本手册和根 README 的已验证列表。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

如果采用独立 profile，应额外提供明确命令，例如：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test -Dspring.profiles.active=test-mysql
```

## 关键文件

- `backend-java/pom.xml`
- `backend-java/src/main/resources/db/migration/`
- `backend-java/src/test/`
- `scripts/`

## 遗留风险

- Testcontainers 依赖 Docker daemon 和镜像下载能力，在受限环境中可能不稳定。
- 本地 MySQL profile 需要开发者机器存在可用数据库，隔离性弱于容器方案。

## 收口要求

M12 结束时必须明确最终采用的集成测试路径，并把不可用路径的原因写入本手册。
