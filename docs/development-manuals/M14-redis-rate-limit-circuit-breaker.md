# M14 Redis 限流与熔断抽象 README

## 阶段目标

把当前单机内存限流和熔断保护升级为可替换实现，允许在有 Redis 时多实例共享状态，同时保留无 Redis 的本地开发体验。

## 交付范围

- 新增限流/熔断接口，隔离业务服务和具体存储实现。
- 保留当前内存实现作为默认 fallback。
- 增加 Redis 可选实现和 `infra` 配置。
- 增加配置项，允许明确选择 memory 或 redis。
- 补充单元测试和必要的本地联调说明。

## 实现顺序

1. 梳理现有 Agent 保护服务调用点。
2. 抽取稳定接口和内存实现。
3. 增加 Redis 实现并放到可选配置后面。
4. 更新 `infra/docker-compose.yml` 和 `.env.example`。
5. 覆盖无 Redis 与有 Redis 两种路径。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

本地 Redis profile 启用后，应补充对应联调命令。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/agent/service/`
- `backend-java/src/test/java/com/threadmind/forum/agent/service/`
- `infra/docker-compose.yml`
- `.env.example`

## 遗留风险

- Redis 不可用时必须明确降级，不应导致核心论坛功能启动失败。
- 分布式限流窗口语义需要与当前内存实现保持可解释。

## 收口要求

M14 结束时，默认本地启动仍不强依赖 Redis；启用 Redis 后，多实例保护状态应共享。
