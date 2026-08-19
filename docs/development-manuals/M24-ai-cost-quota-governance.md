# M24 AI 成本与配额治理 README

## 阶段目标

控制 AI 调用成本和滥用风险，让用户、场景和管理员都能看到明确的配额边界。

## 交付范围

- 增加用户和场景级 AI 配额。
- 记录调用次数、token 或估算成本字段。
- 管理员可查看和调整配额配置。
- 超额时返回明确错误。
- 审计记录能解释一次调用被允许或拒绝的原因。

## 实现顺序

1. 梳理现有 Agent 审计字段。
2. 设计配额模型和默认配置。
3. 在 Java Gateway 接入配额判断。
4. 更新前端错误提示和管理台配置。
5. 补充超额、恢复和管理员调整测试。

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

需要手动验证配额耗尽后助手页给出明确提示。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/`
- `backend-java/src/main/resources/db/migration/`
- `frontend-vue/src/`

## 遗留风险

- token 成本估算可能受模型提供商差异影响。
- 配额规则需要避免影响管理员调试和本地演示。

## 收口要求

M24 结束时，AI 调用应有可审计的配额决策和用户可理解的超额反馈。
