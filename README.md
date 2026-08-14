# ThreadMind

ThreadMind 是一个以网络论坛为主题的 AI 全栈项目：Java Spring Boot 做主后端，Vue3 做前端，Python FastAPI 做 Agent 服务，MySQL 作为本地数据库。

## 项目入口

- 项目说明书：[docs/forum-ai-project-spec.md](docs/forum-ai-project-spec.md)
- API 契约：[docs/api-contracts.md](docs/api-contracts.md)
- 数据库设计：[docs/database-design.md](docs/database-design.md)
- 服务边界决策：[docs/adr/0001-service-boundaries.md](docs/adr/0001-service-boundaries.md)
- 模块设计索引：[docs/modules/README.md](docs/modules/README.md)
- 开发手册索引：[docs/development-manuals/README.md](docs/development-manuals/README.md)
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

前端视觉检查：

```powershell
$env:PLAYWRIGHT_MODULE=".\frontend-vue\node_modules\playwright"
$env:PLAYWRIGHT_EXECUTABLE_PATH="C:\Program Files\Qoom Chrome\chrome.exe"
node .\scripts\visual-check.cjs
```

## 当前状态

当前仓库处于 M2 收口阶段。三端都已经有基础入口，Java 后端已经从内存 demo 服务切换到 MySQL-backed 的 controller / service / repository 结构，内容、互动、通知、治理、用户登录注册和 Agent 内部工具都会读写本地数据库。登录注册已经接入 HMAC JWT，写接口会从 Bearer token 识别当前用户；前端仍保留 mock fallback，方便后端未启动时继续设计页面。

本地 seed 演示账号：

```text
username: demo
password: ThreadMind@123
```

每轮阶段开发说明见 `docs/development-manuals/`。

Java 后端可使用系统 Maven，或使用仓库自带的 `.\.tools\apache-maven\bin\mvn.cmd`。

## 已验证

- Python Agent：`python -m pytest` 通过。
- Vue 前端：`npm run typecheck` 通过。
- Vue 前端：`npm run build` 通过。
- 视觉检查：`node .\scripts\visual-check.cjs` 覆盖桌面和移动端核心路由，无横向溢出。
- Java 后端：`mvn test` 通过。
