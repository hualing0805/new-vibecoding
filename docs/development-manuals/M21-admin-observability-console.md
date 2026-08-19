# M21 管理观测台 README

## 阶段目标

把健康、错误和 AI 状态集中呈现，让管理员能从管理台定位常见故障入口。

## 交付范围

- 增加管理台健康页。
- 展示 Java、Python Agent 和数据库连接状态。
- 汇总近期错误摘要。
- 提供 traceId 查询入口。
- 展示 AI 调用状态、熔断和限流概览。

## 实现顺序

1. 梳理现有健康检查和错误日志字段。
2. 设计管理观测 API。
3. 实现后端聚合服务。
4. 实现前端健康和错误视图。
5. 补充 fallback 和权限测试。

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

联调时应验证 Agent 不可用、数据库异常提示和 traceId 查询入口。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/admin/`
- `backend-java/src/main/java/com/threadmind/forum/agent/`
- `frontend-vue/src/`

## 遗留风险

- 观测台可能暴露内部异常，需要对普通用户完全隔离。
- 错误摘要应脱敏，避免泄漏 token、密码或内部路径。

## 收口要求

M21 结束时，管理员能在一个页面看到核心服务状态和常见故障入口。
