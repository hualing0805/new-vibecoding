# M23 分类与标签运营 README

## 阶段目标

让管理员能够维护内容结构，支持分类、标签合并、热门标签和推荐标签审核。

## 交付范围

- 分类 CRUD。
- 标签合并和别名处理。
- 热门标签统计。
- 推荐标签审核。
- 内容结构变更保留权限和审计记录。

## 实现顺序

1. 梳理当前板块、分类和标签模型。
2. 设计分类和标签运营 API。
3. 增加必要迁移和审计字段。
4. 实现管理台操作界面。
5. 补充权限、合并和回滚测试。

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

需要手动验证标签合并后帖子列表和搜索结果仍可正常展示。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/`
- `backend-java/src/main/resources/db/migration/`
- `frontend-vue/src/`

## 遗留风险

- 标签合并属于数据修正操作，需要避免误操作不可恢复。
- 分类变更可能影响旧帖子 URL 和筛选条件。

## 收口要求

M23 结束时，管理员可维护分类和标签，且每次结构变更可审计。
