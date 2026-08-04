# ThreadMind

ThreadMind 是一个以网络论坛为主题的 AI 全栈项目：Java Spring Boot 做主后端，Vue3 做前端，Python FastAPI 做 Agent 服务，MySQL 作为本地数据库。

## 项目入口

- 项目说明书：[docs/forum-ai-project-spec.md](docs/forum-ai-project-spec.md)
- API 契约：[docs/api-contracts.md](docs/api-contracts.md)
- 数据库设计：[docs/database-design.md](docs/database-design.md)
- 服务边界决策：[docs/adr/0001-service-boundaries.md](docs/adr/0001-service-boundaries.md)
- 模块设计索引：[docs/modules/README.md](docs/modules/README.md)
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

启动 Java 后端：

```powershell
cd backend-java
mvn spring-boot:run
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

当前仓库处于 M1 到 M2 之间的模块化阶段。三端都已经有基础入口，Java 后端补齐了内容、互动、通知、治理和 Agent 内部工具接口，前端已改为 API 模式可切换，数据库迁移脚本已准备好。后续重点是把 Java 的内存应用服务替换成真实 MySQL Repository。

Java 后端需要本机安装 Maven 后运行 `mvn test` 或 `mvn spring-boot:run`。

## 已验证

- Python Agent：`python -m pytest` 通过。
- Vue 前端：`npm run typecheck` 通过。
- Vue 前端：`npm run build` 通过。
- 视觉检查：`node .\scripts\visual-check.cjs` 覆盖桌面和移动端核心路由，无横向溢出。

当前机器没有 Maven，Java 测试文件已补齐，但还未实际执行。
