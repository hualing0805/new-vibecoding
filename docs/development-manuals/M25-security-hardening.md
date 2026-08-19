# M25 安全加固 README

## 阶段目标

系统性处理认证、授权和敏感数据风险，为后续发布候选阶段打基础。

## 交付范围

- 增加刷新 token 或会话续期方案。
- 增加密码重置流程。
- 补充权限边界测试。
- 统一错误脱敏策略。
- 更新安全相关文档和本地配置说明。

## 实现顺序

1. 梳理认证和授权入口。
2. 优先补高风险越权测试。
3. 实现刷新 token 或会话续期。
4. 实现密码重置基础流程。
5. 检查错误响应和日志脱敏。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

如前端改动认证流程，补充：

```powershell
cd frontend-vue
npm run typecheck
npm run build
```

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/auth/`
- `backend-java/src/test/java/com/threadmind/forum/`
- `frontend-vue/src/`
- `docs/`

## 遗留风险

- 密码重置在本地原型中可能没有真实邮件服务，需要明确本地替代流程。
- 刷新 token 会影响前端登录态持久化和退出逻辑。

## 收口要求

M25 结束时，常见越权和敏感信息泄漏应有测试覆盖或明确风险记录。
