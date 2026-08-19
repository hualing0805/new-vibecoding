# M20 治理 V2 README

## 阶段目标

细化版主权限和治理处理流程，让管理台从单一人工处理走向可分派、可追踪、可申诉。

## 交付范围

- 增加板块版主模型。
- 支持处理分派和处理状态流转。
- 增加申诉入口。
- 支持处理记录筛选和详情查看。
- 保证版主只能处理授权板块，管理员可全站处理。

## 实现顺序

1. 梳理当前举报和管理处理模型。
2. 增加版主授权结构和迁移。
3. 扩展治理 API 权限判断。
4. 更新前端治理台。
5. 补充越权和状态流转测试。

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

需要用管理员和版主两类账号验证权限差异。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/moderation/`
- `backend-java/src/main/resources/db/migration/`
- `frontend-vue/src/`

## 遗留风险

- 权限规则复杂后容易出现越权，需要测试优先。
- 申诉流程不要过度产品化，先保证可追踪。

## 收口要求

M20 结束时，治理处理应能回答“谁在什么权限下处理了什么内容”。
