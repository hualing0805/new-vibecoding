# M12-M30 长线开发路线图

## 路线图定位

本路线图用于把 ThreadMind 从当前 M11 的本地可运行全栈项目，推进到一个具备稳定测试、可观测、可部署、可持续演进能力的 AI 论坛产品原型。M12-M30 不再把每个阶段的详细 README 放在一个文件里；本文件只保留总览、批次和索引，具体准入、范围、验收和风险以对应阶段手册为准。

## 总体原则

- 每个阶段只解决一个主问题，避免把功能、重构和视觉改版混在一起。
- 涉及数据库结构时，先迁移脚本，再业务代码，再测试。
- AI 能力继续保持 Java Gateway 边界：Python Agent 不直接写 MySQL。
- 新基础设施先本地可运行，再考虑生产部署；Redis、CI、Docker、观测等都要有降级方案。
- 每阶段都要留下可执行验收命令，不能只写设计描述。

## 阶段索引

| 阶段 | 主题 | 手册 |
| --- | --- | --- |
| M12 | MySQL/Flyway 集成测试方案 | `M12-mysql-flyway-integration-tests.md` |
| M13 | Repository 集成测试 | `M13-repository-integration-tests.md` |
| M14 | Redis 限流与熔断抽象 | `M14-redis-rate-limit-circuit-breaker.md` |
| M15 | AI SSE 流式响应 | `M15-ai-sse-streaming.md` |
| M16 | 知识库与 RAG 基线 | `M16-knowledge-rag-baseline.md` |
| M17 | 搜索 2.0 | `M17-search-v2.md` |
| M18 | 实时通知 | `M18-realtime-notifications.md` |
| M19 | 草稿与编辑器版本 | `M19-drafts-editor-versioning.md` |
| M20 | 治理 V2 | `M20-moderation-v2.md` |
| M21 | 管理观测台 | `M21-admin-observability-console.md` |
| M22 | 用户主页与关系 | `M22-user-profile-relations.md` |
| M23 | 分类与标签运营 | `M23-category-tag-operations.md` |
| M24 | AI 成本与配额治理 | `M24-ai-cost-quota-governance.md` |
| M25 | 安全加固 | `M25-security-hardening.md` |
| M26 | 性能与分页治理 | `M26-performance-pagination-governance.md` |
| M27 | 部署打包 | `M27-deployment-packaging.md` |
| M28 | CI/CD 质量门禁 | `M28-ci-cd-quality-gates.md` |
| M29 | 产品体验与可访问性收口 | `M29-product-accessibility-polish.md` |
| M30 | Release Candidate | `M30-release-candidate.md` |

## 推荐批次

### M12-M14：工程地基

优先把数据库、repository、限流熔断从“能跑”推进到“可验证、可替换”。这批次完成后，再做流式输出和知识库会更稳。

### M15-M19：AI 与内容体验

围绕用户日常使用：流式问答、RAG、搜索、通知和草稿。目标是让 AI 像工作流的一部分，而不是孤立按钮。

### M20-M24：治理与运营能力

补齐管理员和版主视角：权限、申诉、观测、分类标签运营、AI 成本和配额。目标是让产品能被持续运营。

### M25-M30：发布硬化

安全、性能、部署、CI、可访问性和 RC 收口。目标是把项目从本地练习作品推进到可稳定演示的候选版本。

## 阶段准入和退出

每个阶段开始前：

- 读取 `docs/development-standard.md`。
- 确认 `git status --short --branch`。
- 阅读本路线图和上一阶段手册。
- 明确是否需要数据库迁移、环境变量、脚本或 API 契约更新。

每个阶段结束前：

- 新增或更新该阶段手册。
- 根据改动范围执行 Java、Vue、Python 或联调验证。
- 更新 `README.md` 的当前状态和已验证列表。
- 提交并推送到 GitHub。

## 暂缓事项

以下事项不建议在 M12-M30 主线中提前插入，除非用户明确调整优先级：

- 多租户和组织空间。
- 复杂推荐算法。
- 付费订阅和真实支付。
- 移动 App 原生客户端。
- 多语言国际化全文翻译。
- 生产级日志平台和指标平台接入。

## 下一阶段建议

M12 优先做 MySQL/Flyway 集成测试方案。先探测本机是否具备 Docker/Testcontainers 条件；如果不稳定，采用本地 MySQL test profile，保证迁移验证能在当前开发机稳定执行。
