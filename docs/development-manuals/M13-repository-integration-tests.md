# M13 Repository 集成测试 README

## 阶段目标

让关键 SQL 不只靠 Mockito 保护，而是能在真实测试库中验证读写行为。M13 建立在 M12 的迁移测试能力之上。

## 交付范围

- 覆盖 AI 审计查询 repository。
- 覆盖帖子摘要缓存 repository。
- 覆盖论坛列表、互动状态和通知查询 repository。
- 建立测试数据装配方式，避免每个测试手写大量 SQL。
- 保持测试可重复运行，测试结束后不污染开发库。

## 实现顺序

1. 复用 M12 的测试数据库方案。
2. 选择最容易回归的 repository 作为第一批覆盖对象。
3. 抽取测试数据 seed helper。
4. 增加失败时可读的断言信息。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

如存在独立集成测试 profile，应同步运行该 profile。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/**/repository/`
- `backend-java/src/test/java/com/threadmind/forum/`
- `backend-java/src/main/resources/db/migration/`

## 遗留风险

- Repository 测试容易变慢，需要控制测试数据规模。
- 如果测试库复用本地 MySQL，需要避免误删开发数据。

## 收口要求

M13 结束时，应能说明哪些关键 SQL 已经由真实数据库测试保护，哪些仍只靠服务层测试间接覆盖。
