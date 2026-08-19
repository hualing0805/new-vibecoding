# M19 草稿与编辑器版本 README

## 阶段目标

提升发帖和编辑工作流可靠性，避免刷新页面或网络失败导致内容丢失。

## 交付范围

- 新增草稿自动保存。
- 新增草稿列表和恢复入口。
- 记录帖子编辑历史。
- 保留 AI 润色或摘要建议记录。
- 前端提供清晰的保存中、已保存和失败状态。

## 实现顺序

1. 设计草稿和编辑历史表。
2. 实现后端 CRUD 和权限校验。
3. 接入前端编辑器自动保存。
4. 增加草稿列表和恢复流程。
5. 补充刷新页面、失败重试和权限测试。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

```powershell
cd frontend-vue
npm run typecheck
npm run build
```

手动验证刷新页面不丢草稿，且其他用户不可读取私有草稿。

## 关键文件

- `backend-java/src/main/resources/db/migration/`
- `backend-java/src/main/java/com/threadmind/forum/post/`
- `frontend-vue/src/`

## 遗留风险

- 高频自动保存可能增加数据库写入压力。
- 编辑历史与治理审计的边界需要明确。

## 收口要求

M19 结束时，草稿恢复和编辑历史都应有明确入口和权限保护。
