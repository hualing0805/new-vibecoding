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
