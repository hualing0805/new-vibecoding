# M0-M1 项目基线开发手册

## 阶段目标

M0-M1 的目标是把 ThreadMind 从产品设想落成可继续开发的全栈工程基线：明确产品边界、服务边界、本地拓扑、数据库设计和三端目录结构。

## 需要完成的事情

- 写清楚论坛产品定位、MVP 功能和 AI 边界。
- 建立 Java Spring Boot、Vue3 + Vite、Python FastAPI 三个服务目录。
- 建立 MySQL 本地开发配置和 Flyway 初始迁移。
- 提供首页、帖子详情、发帖、助手、管理台、登录页等前端页面骨架。
- 提供 Java 健康检查、账号接口、论坛接口、Agent Gateway 入口。
- 提供 Python Agent 健康检查、基础 Agent 路由、mock 模型响应和 Java 工具客户端。
- 提供本地启动脚本和检查脚本。

## 已经如何完成

- 产品与架构说明写入 `PRODUCT.md`、`DESIGN.md`、`docs/forum-ai-project-spec.md`。
- API 草案写入 `docs/api-contracts.md`。
- 数据库表设计写入 `docs/database-design.md`，初始迁移为 `backend-java/src/main/resources/db/migration/V1__init_schema.sql`。
- 服务边界 ADR 写入 `docs/adr/0001-service-boundaries.md`，明确 Java 是权限、事务和审计中心，Python Agent 不直接写 MySQL。
- Java 后端放在 `backend-java/`，使用 Spring Boot 3、Spring Security、JDBC、Flyway。
- Vue 前端放在 `frontend-vue/`，使用 Vue Router、Pinia、Axios 和 lucide 图标。
- Python Agent 放在 `agent-python/`，使用 FastAPI、httpx、Pydantic。
- 本地 MySQL 配置放在 `infra/`，常用脚本放在 `scripts/`。

## 本地验证

```powershell
cd backend-java
mvn test
```

```powershell
cd agent-python
python -m pytest
```

```powershell
cd frontend-vue
npm run typecheck
```

## 关键文件

- `README.md`
- `PRODUCT.md`
- `DESIGN.md`
- `docs/forum-ai-project-spec.md`
- `docs/api-contracts.md`
- `docs/database-design.md`
- `backend-java/pom.xml`
- `frontend-vue/package.json`
- `agent-python/pyproject.toml`
- `infra/docker-compose.yml`

## 遗留风险

- 早期 Java 服务以演示流程为主，真实用户身份、权限和数据库写入还没有完全收口。
- 前端保留 mock fallback，方便页面开发，但后续必须逐步转成真实 API 优先。
- Agent 有 mock 模型响应，真实模型接入和审计记录需要在后续阶段完善。

