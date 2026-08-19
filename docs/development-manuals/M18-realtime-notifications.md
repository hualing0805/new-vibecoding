# M18 实时通知 README

## 阶段目标

让通知从刷新查询走向实时提醒，提升回复、点赞、收藏、治理处理等事件的即时感。

## 交付范围

- Java 提供 SSE 或 WebSocket 通知订阅。
- 前端建立通知订阅、重连和离线提示。
- 通知已读状态与未读数保持一致。
- 保留轮询或手动刷新 fallback。
- 覆盖断开重连和重复事件处理。

## 实现顺序

1. 选择 SSE 或 WebSocket，优先考虑当前栈实现成本。
2. 定义通知事件格式。
3. 后端接入通知创建和推送。
4. 前端接入订阅、重连和状态同步。
5. 增加回归测试和手动验收脚本。

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

联调时应验证新通知出现、断线重连、已读同步和未读数一致。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/notification/`
- `frontend-vue/src/`
- `docs/api-contracts.md`

## 遗留风险

- 长连接可能受本地代理、浏览器刷新和网络状态影响。
- 重连后需要避免重复推送导致未读数乱跳。

## 收口要求

M18 结束时，实时通知失败时仍能通过原查询接口看到完整通知。
