# M12 MySQL/Flyway 集成测试方案开发手册

## 阶段目标

证明 V1-V4 数据库迁移可以在空库上完整运行，并让失败原因能被开发者快速定位。M12 只处理迁移和基础连接验证，不扩展业务功能。

## 需要完成的事情

- 探测当前开发机是否能使用 Docker/Testcontainers。
- 在 Docker 不可用时，提供本地 MySQL 测试库方案。
- 增加显式 Flyway 迁移集成测试入口。
- 确保默认 `mvn test` 不清空任何数据库。
- 文档化测试库初始化、执行命令、清理保护和本轮验证结果。

## 已经如何完成

- 已探测本机环境：`docker` 命令不存在，`MySQL` Windows 服务存在但处于 stopped，当前会话无法启动该服务。
- 未引入 Testcontainers 依赖，避免默认测试依赖 Docker daemon 或镜像下载。
- 新增 Maven profile `mysql-migration-test`，仅显式执行 `-Pmysql-migration-test` 时通过 Surefire include 运行 `*IT.java` 集成测试。
- 新增 `FlywayMySqlMigrationIT`，默认连接 `threadmind_migration_test`，执行 `Flyway.clean()` 后重新迁移 V1-V4。
- 集成测试在清库前强制检查 JDBC URL 中的数据库名必须包含 `test`，避免误清开发库。
- 集成测试会校验 `flyway_schema_history` 中 V1-V4 均成功、关键表存在，并确认 V3 已把 demo 密码更新为 `{noop}ThreadMind@123`。
- 新增 `scripts/mysql-test-bootstrap.sql`，用于创建 `threadmind_migration_test` 测试库和 `threadmind_test/threadmind_test` 测试账号。
- 新增 `scripts/verify-mysql-migrations.ps1`，统一设置默认测试库连接变量并运行迁移测试 profile。
- `scripts/verify-mysql-migrations.ps1` 支持 `-Url`、`-Username` 和 `-Password` 参数，方便临时端口或 CI 环境覆盖连接。
- `.env.example` 新增 `THREADMIND_MYSQL_IT_*` 变量，便于本地覆盖测试库连接。

## 本地使用或验收步骤

先启动本地 MySQL。首次使用测试库时，用 root 或管理员账号执行：

```powershell
mysql -uroot -p < scripts/mysql-test-bootstrap.sql
```

执行迁移集成测试：

```powershell
.\scripts\verify-mysql-migrations.ps1
```

也可以直接在 Java 后端目录运行：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd -Pmysql-migration-test test
```

如需覆盖测试库连接，设置：

```powershell
$env:THREADMIND_MYSQL_IT_URL="jdbc:mysql://localhost:3306/threadmind_migration_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:THREADMIND_MYSQL_IT_USERNAME="threadmind_test"
$env:THREADMIND_MYSQL_IT_PASSWORD="threadmind_test"
```

如果使用临时 MySQL 端口，可以直接传参：

```powershell
.\scripts\verify-mysql-migrations.ps1 `
  -Url "jdbc:mysql://127.0.0.1:3307/threadmind_migration_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" `
  -Username "threadmind_test" `
  -Password "threadmind_test"
```

## 本地验证

- Java 后端默认测试：`..\.tools\apache-maven\bin\mvn.cmd test` 通过，22 个测试成功。
- Docker 探测：`docker version` 失败，本机无 `docker` 命令。
- MySQL 探测：`mysql -uthreadmind -pthreadmind -e "SELECT VERSION();"` 失败，`localhost:3306` 未运行 MySQL。
- MySQL 服务探测：存在 `MySQL` Windows 服务，但 `Start-Service -Name MySQL` 失败，当前会话无法打开该服务。
- MySQL/Flyway 迁移集成测试：在 `logs/mysql-m12-data` 初始化临时 MySQL 9.5 实例，使用 `127.0.0.1:3307/threadmind_migration_test` 执行 `.\scripts\verify-mysql-migrations.ps1` 通过，1 个 IT 测试成功，V1-V4 均迁移成功。
- 临时 MySQL 执行后已关闭；`logs/` 目录被 `.gitignore` 忽略，不进入提交。
- 未执行 Vue 和 Python 测试，因为本轮没有修改前端或 Python Agent。

## 关键文件

- `backend-java/pom.xml`
- `backend-java/src/test/java/com/threadmind/forum/migration/FlywayMySqlMigrationIT.java`
- `backend-java/src/main/resources/db/migration/`
- `scripts/mysql-test-bootstrap.sql`
- `scripts/verify-mysql-migrations.ps1`
- `.env.example`

## 遗留风险

- 当前机器无法启动系统 MySQL 服务；本轮通过工作区临时 MySQL 进程完成验证，常规本地使用仍需要开发者自行启动 MySQL 或使用 Docker。
- 本地 MySQL profile 依赖开发机数据库版本；迁移脚本使用 `utf8mb4_0900_ai_ci` 和 JSON 字段，建议使用 MySQL 8.0 或更高版本。本轮在 MySQL 9.5 上通过，但 Flyway 日志提示当前 Flyway 版本最新声明支持到 MySQL 8.1。
- MySQL 9.5 对 V2 中 `ON DUPLICATE KEY UPDATE ... VALUES(col)` 给出未来弃用警告，M13 或后续迁移整理时应评估改为 alias 写法。
- Testcontainers 仍未接入；后续如果 CI/CD 具备 Docker 环境，可在 M28 或独立阶段再升级到容器化迁移验证。

## 下一步建议

- M13 进入 Repository 集成测试时，复用 `threadmind_migration_test` 的连接变量和数据库名保护策略。
- 后续处理迁移脚本兼容性时，优先消化 MySQL 9.5 的 `VALUES()` 弃用警告。
