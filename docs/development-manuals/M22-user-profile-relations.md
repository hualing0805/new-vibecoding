# M22 用户主页与关系 README

## 阶段目标

补齐社区身份和用户内容归档，让用户主页、关注关系和收藏扩展形成完整社区体验。

## 交付范围

- 新增公开用户主页。
- 展示用户发帖和回复列表。
- 支持关注用户和关注板块。
- 扩展个人收藏入口。
- 保护隐私字段不泄漏给游客或普通用户。

## 实现顺序

1. 确认用户公开字段和隐私字段。
2. 增加关注关系迁移和 repository。
3. 实现用户主页 API。
4. 实现前端主页和关注入口。
5. 增加游客、本人、他人三类访问测试。

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

手动验证游客可看公开主页，但不可看到邮箱、token 或内部标识。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/user/`
- `backend-java/src/main/resources/db/migration/`
- `frontend-vue/src/`

## 遗留风险

- 关注关系会影响通知和推荐，M22 先做基础关系，不做复杂推荐。
- 用户主页列表分页需要与全站分页策略保持一致。

## 收口要求

M22 结束时，用户公开主页和关注关系应具备基础可用闭环。
