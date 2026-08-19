# M7 治理操作台开发手册

## 阶段目标

把管理台从单一举报列表扩展为可筛选、可查看详情、可处理的治理工作台，同时和 AI 审计入口并列。

## 需要完成的事情

- 管理台增加治理 / AI 审计标签。
- 治理队列支持全部、待处理、已处理、已驳回筛选。
- 选中举报后在右侧展示目标、状态和补充说明。
- 保留驳回、隐藏、锁定三类人工处理动作。

## 已经如何完成

- `AdminDashboardPage.vue` 新增 `activeTab`、`reportStatus` 和详情侧栏。
- `fetchReports(status)` 支持将状态作为查询参数传给 `/api/admin/reports`。
- 处理举报后重新拉取列表，避免前端只改局部状态导致与后端不一致。
- 管理台布局继续使用工作台表格，不引入营销式或装饰性布局。

## 本地使用或验收步骤

1. 登录 demo 管理员账号。
2. 打开 `/admin`，在治理标签页切换状态筛选。
3. 选中一条举报，确认右侧详情同步变化。
4. 点击驳回、隐藏或锁定，确认列表刷新。

## 本地验证

- Vue 前端：`npm.cmd run typecheck` 通过。
- 联调脚本保留举报创建、管理员列表和处理链路。

## 关键文件

- `frontend-vue/src/pages/AdminDashboardPage.vue`
- `frontend-vue/src/api/forum.ts`
- `backend-java/src/main/java/com/threadmind/forum/moderation/controller/ReportController.java`

## 遗留风险

- 版主分区权限仍未细化，当前 `/api/admin/**` 对 `ADMIN` 和 `MODERATOR` 统一放行。

## 下一步建议

- M8 增加 AI 运营指标，让管理员能先看整体健康度，再进入 session 明细。
