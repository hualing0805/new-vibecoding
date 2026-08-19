# ThreadMind

ThreadMind 是一个以网络论坛为主题的 AI 全栈项目：Java Spring Boot 做主后端，Vue3 做前端，Python FastAPI 做 Agent 服务，MySQL 作为本地数据库。

## 项目入口

- 项目说明书：[docs/forum-ai-project-spec.md](docs/forum-ai-project-spec.md)
- API 契约：[docs/api-contracts.md](docs/api-contracts.md)
- 数据库设计：[docs/database-design.md](docs/database-design.md)
- 服务边界决策：[docs/adr/0001-service-boundaries.md](docs/adr/0001-service-boundaries.md)
- 模块设计索引：[docs/modules/README.md](docs/modules/README.md)
- 开发标准：[docs/development-standard.md](docs/development-standard.md)
- 开发手册索引：[docs/development-manuals/README.md](docs/development-manuals/README.md)
- M12-M30 长线路线图：[docs/development-manuals/M12-M30-roadmap.md](docs/development-manuals/M12-M30-roadmap.md)
- 产品基线：[PRODUCT.md](PRODUCT.md)

## 目录

```text
backend-java/   Spring Boot 主业务服务
frontend-vue/   Vue3 论坛前端
agent-python/   FastAPI Agent 服务
infra/          本地 MySQL 配置
scripts/        本地开发脚本
docs/           项目说明和设计文档
```

## 本地启动

先复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

启动 MySQL：

```powershell
cd infra
docker compose up -d
```

如果使用的是你自己安装的本地 MySQL，而不是 `infra/docker-compose.yml`，先用 root 或管理员账号执行：

```powershell
mysql -uroot -p < scripts/mysql-bootstrap.sql
```

这个脚本会创建 `threadmind` 数据库和 `threadmind/threadmind` 应用账号。Spring Boot 默认使用这个低权限账号连接数据库。

启动 Java 后端：

```powershell
cd backend-java
mvn spring-boot:run
```

如果系统没有安装 Maven，可以使用仓库自带 Maven：

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd spring-boot:run
```

启动 Python Agent：

```powershell
cd agent-python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e .[dev]
uvicorn app.main:app --reload --port 8000
```

启动 Vue 前端：

```powershell
cd frontend-vue
npm install
npm run dev
```

前端默认使用 mock 数据，避免后端未启动时页面报错。需要接入 Java API 时，在根目录 `.env` 设置：

```powershell
VITE_API_BASE_URL=http://localhost:8080/api
```

开发脚本：

```powershell
.\scripts\dev.ps1
.\scripts\check.ps1
```

本地联调脚本：

```powershell
.\scripts\start-local-stack.ps1
.\scripts\verify-local-flow.ps1
.\scripts\stop-local-stack.ps1
```

`verify-local-flow.ps1` 会验证健康检查、登录、发帖、回复、点赞、收藏、通知、Agent chat、帖子摘要、举报和管理处理链路。

前端视觉检查：

```powershell
$env:PLAYWRIGHT_MODULE=".\frontend-vue\node_modules\playwright"
$env:PLAYWRIGHT_EXECUTABLE_PATH="C:\Program Files\Qoom Chrome\chrome.exe"
node .\scripts\visual-check.cjs
```

## 当前状态

当前仓库处于 M11 收口阶段。三端都已经有基础入口，Java 后端已经从内存 demo 服务切换到 MySQL-backed 的 controller / service / repository 结构，内容、互动、通知、治理、用户登录注册、Agent 内部工具、AI 审计和摘要缓存都会读写本地数据库。登录注册已经接入 HMAC JWT，写接口会从 Bearer token 识别当前用户；互动状态、通知中心、未读数和个人收藏入口已经接入真实 API。AI Agent Gateway 会记录会话、消息和工具调用审计，管理员可在管理台查看 AI session、消息和工具调用，普通用户可在助手页查看自己的 AI 历史并续聊。帖子摘要默认读取缓存，手动刷新才重新调用 Agent；Java 侧有单机内存限流和短暂熔断保护。顶部搜索已接入首页关键词查询，治理台支持状态筛选和详情侧栏。M11 已补强 AI 审计查询服务测试，覆盖用户权限边界、管理员详情和工具 JSON 摘要解析。前端仍保留公开读取的 mock fallback，方便后端未启动时继续设计页面。

本地 seed 演示账号：

```text
username: demo
password: ThreadMind@123
```

每轮开发必须遵守 `docs/development-standard.md`，阶段开发说明见 `docs/development-manuals/`。
阶段规划和完成记录见 `docs/development-manuals/`，M12-M30 长线路线图见 `docs/development-manuals/M12-M30-roadmap.md`。下一步建议进入 M12，优先评估 MySQL/Flyway 级数据库集成测试和多实例限流方案。

Java 后端可使用系统 Maven，或使用仓库自带的 `.\.tools\apache-maven\bin\mvn.cmd`。

## 已验证

- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过，22 个测试成功。
- Vue 前端：`npm run typecheck` 通过。
- Vue 前端：`npm run build` 通过。
- Python Agent：`C:\Users\16378\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest tests` 通过。
- 视觉检查：`node .\scripts\visual-check.cjs` 覆盖桌面和移动端核心路由，无横向溢出；另以 mock 登录态检查 `/assistant`、`/admin` 和 AI 审计标签，无横向溢出。
