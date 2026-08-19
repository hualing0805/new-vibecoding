# 开发手册索引

本目录记录 ThreadMind 每个阶段要完成什么、已经如何完成、如何本地验证，以及下一阶段要注意什么。后续每次开发都必须先遵守 `docs/development-standard.md`，并在收口前新增或更新对应阶段手册。

## 维护规则

- 每个阶段一份手册，文件名使用 `阶段-主题.md`。
- 手册至少包含：阶段目标、完成范围、实现方式、验证方式、关键文件、遗留风险。
- 如果开发过程中改变了启动方式、环境变量、接口契约或数据库迁移，必须同步更新本目录和 README。
- 手册记录工程事实，不写营销描述；未验证的内容必须明确标注。
- 每轮开发结束后，必须提交并推送到 GitHub，提交信息和最终说明默认使用中文。

## 当前阶段索引

| 阶段 | 手册 | 说明 |
| --- | --- | --- |
| M0-M1 | `M0-M1-project-baseline.md` | 项目说明、三端骨架、本地拓扑和基础页面 |
| M2 前半 | `M2-mysql-backed-forum.md` | Java 从内存 demo 切到 MySQL-backed repository/service |
| M2 收口 | `M2-auth-and-real-flow.md` | 真实 JWT 登录态、前端登录注册、发帖链路守卫 |
| M2.5 | `M2.5-real-integration-and-tests.md` | 本地联调脚本、核心流程验收、前端错误状态和服务测试 |
| M3 规划 | `M3-interaction-notification-plan.md` | 互动状态、通知中心、收藏入口和热门排序规划 |
| M3 | `M3-interaction-notification.md` | 互动状态、通知中心、收藏列表、未读数和通知规则 |
| M4 规划 | `M4-agent-mvp-audit-plan.md` | AI MVP、Agent Gateway 审计落库和前端 AI 可解释体验规划 |
| M4 | `M4-agent-mvp-audit.md` | AI 审计落库、工具轨迹、前端可解释展示和 fallback 审计 |
| M5 规划 | `M5-ai-audit-console-plan.md` | AI 审计台、会话历史、摘要缓存和限流规划 |
| M5 | `M5-ai-audit-console.md` | AI 审计查询、会话历史、摘要缓存、刷新、限流和熔断 |
| M6 | `M6-global-search-and-assistant-history.md` | 顶部全局搜索、首页关键词联动和助手历史续聊 |
| M7 | `M7-moderation-operator-console.md` | 管理台治理标签、举报筛选、详情侧栏和人工处理闭环 |
| M8 | `M8-ai-ops-metrics.md` | AI 运营指标、审计指标卡和管理台可观测入口 |
| M9 | `M9-release-readiness.md` | 本地验收脚本、文档契约和阶段交付收口 |
| M10 | `M10-regression-test-hardening.md` | 前序 Agent 缓存、限流和熔断测试补强 |
| M11 | `M11-audit-query-test-hardening.md` | AI 审计查询服务测试、安全边界和查询参数规范化 |
| M12-M30 总览 | `M12-M30-roadmap.md` | M12 到 M30 的批次划分和阶段手册索引 |
| M12 | `M12-mysql-flyway-integration-tests.md` | MySQL/Flyway 迁移集成测试方案 |
| M13 | `M13-repository-integration-tests.md` | Repository 真实数据库集成测试 |
| M14 | `M14-redis-rate-limit-circuit-breaker.md` | Redis 限流与熔断抽象 |
| M15 | `M15-ai-sse-streaming.md` | AI SSE 流式响应 |
| M16 | `M16-knowledge-rag-baseline.md` | 知识库与 RAG 基线 |
| M17 | `M17-search-v2.md` | 站内搜索 2.0 |
| M18 | `M18-realtime-notifications.md` | 实时通知 |
| M19 | `M19-drafts-editor-versioning.md` | 草稿与编辑器版本 |
| M20 | `M20-moderation-v2.md` | 治理 V2 |
| M21 | `M21-admin-observability-console.md` | 管理观测台 |
| M22 | `M22-user-profile-relations.md` | 用户主页与关系 |
| M23 | `M23-category-tag-operations.md` | 分类与标签运营 |
| M24 | `M24-ai-cost-quota-governance.md` | AI 成本与配额治理 |
| M25 | `M25-security-hardening.md` | 安全加固 |
| M26 | `M26-performance-pagination-governance.md` | 性能与分页治理 |
| M27 | `M27-deployment-packaging.md` | 部署打包 |
| M28 | `M28-ci-cd-quality-gates.md` | CI/CD 质量门禁 |
| M29 | `M29-product-accessibility-polish.md` | 产品体验与可访问性收口 |
| M30 | `M30-release-candidate.md` | Release Candidate 收口 |
