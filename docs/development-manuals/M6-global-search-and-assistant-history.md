# M6 全局搜索与助手历史开发手册

## 阶段目标

让用户从任意页面都能搜索帖子，并让 AI 助手从一次性问答变成可延续的会话体验。

## 需要完成的事情

- 顶部搜索框提交后跳转首页并携带关键词。
- 首页读取 `keyword` 查询参数并复用 `/api/topics?keyword=`。
- 搜索结果可以一键清除关键词。
- 助手页加载当前用户 AI session 历史。
- 点击历史 session 后恢复消息与工具调用，并沿用 `sessionId` 继续提问。

## 已经如何完成

- `AppShell.vue` 把顶部搜索框改为 `role="search"` 表单，提交时导航到 `/?keyword=...`。
- `HomePage.vue` 监听路由关键词，把关键词传给 `fetchTopics`，并显示可清除的搜索 chip。
- `forum.ts` 的 `askAgent` 支持可选 `sessionId`，助手页发送问题时会续用当前 session。
- `AssistantPage.vue` 读取 `/api/users/me/ai/sessions` 和详情接口，恢复历史消息和工具调用摘要。

## 本地使用或验收步骤

1. 在顶部搜索框输入关键词并回车。
2. 确认首页列表按关键词过滤，点击搜索 chip 可清除。
3. 在 `/assistant` 发起问答，刷新历史列表。
4. 点击历史 session，确认消息恢复，继续提问时仍携带该 session。

## 本地验证

- Vue 前端：`npm.cmd run typecheck` 通过。
- Java 后端：`..\.tools\apache-maven\bin\mvn.cmd test` 通过。

## 关键文件

- `frontend-vue/src/app/AppShell.vue`
- `frontend-vue/src/pages/HomePage.vue`
- `frontend-vue/src/pages/AssistantPage.vue`
- `frontend-vue/src/api/forum.ts`

## 遗留风险

- 顶部搜索当前只查帖子标题和正文，暂未查作者昵称、标签和回复内容。

## 下一步建议

- M7 完善管理台治理操作流，把 AI 审计和举报处理放在同一工作台内。
