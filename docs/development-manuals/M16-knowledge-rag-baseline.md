# M16 知识库与 RAG 基线 README

## 阶段目标

支持站内文档和规则检索，让 Agent 回答可以引用可审计的知识来源，同时继续保持 Java Gateway 边界。

## 交付范围

- 新增知识文档存储结构或复用现有文档表设计。
- 提供本地导入脚本和最小 seed 文档。
- Java 暴露检索接口，Python Agent 通过工具调用访问。
- 前端展示引用来源或工具轨迹摘要。
- 暂不引入复杂多模型路由。

## 实现顺序

1. 确认知识文档 schema 和 API 契约。
2. 增加迁移和导入脚本。
3. 实现 Java 检索接口和权限边界。
4. 接入 Python Agent 工具。
5. 在助手页展示引用信息。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

```powershell
cd agent-python
python -m pytest tests
```

如前端展示引用来源，补充：

```powershell
cd frontend-vue
npm run typecheck
npm run build
```

## 关键文件

- `backend-java/src/main/resources/db/migration/`
- `backend-java/src/main/java/com/threadmind/forum/agent/`
- `agent-python/app/services/`
- `frontend-vue/src/`

## 遗留风险

- 简单检索可能召回不稳定，需要先保证可审计再优化效果。
- 文档权限边界需要避免将管理内部材料暴露给普通用户。

## 收口要求

M16 结束时，每次 RAG 工具调用都应能回看引用来源和审计记录。
