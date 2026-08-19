# M17 搜索 2.0 README

## 阶段目标

把顶部搜索升级为真正的站内检索页，支持分页、筛选和更清晰的结果呈现。

## 交付范围

- 新增搜索结果页。
- 支持帖子、回复、标签、作者等维度。
- 评估 MySQL fulltext 或索引优化方案。
- 游客可使用公开内容搜索。
- 保持首页关键词联动不回归。

## 实现顺序

1. 确认搜索 API 请求和响应结构。
2. 实现后端分页和筛选。
3. 补充索引或 fulltext 迁移。
4. 实现前端搜索页和空状态。
5. 增加核心场景测试。

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

视觉检查应覆盖搜索页桌面和移动端。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/`
- `backend-java/src/main/resources/db/migration/`
- `frontend-vue/src/router/`
- `frontend-vue/src/views/`

## 遗留风险

- MySQL fulltext 的中文分词效果有限，可能需要后续搜索引擎或 ngram 策略。
- 搜索结果排序需要避免过早复杂化。

## 收口要求

M17 结束时，搜索页应支持可分页、可筛选、可分享 URL 的基础体验。
