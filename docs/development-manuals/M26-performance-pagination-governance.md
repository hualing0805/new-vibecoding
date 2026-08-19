# M26 性能与分页治理 README

## 阶段目标

面向中等数据量优化体验和 SQL，让核心列表、搜索和管理查询在更大 seed 数据下仍保持稳定。

## 交付范围

- 梳理慢查询和高频查询。
- 补充必要索引。
- 统一分页参数和响应格式。
- 增加 seed 大数据或负载脚本。
- 记录性能验收方式和基准结果。

## 实现顺序

1. 找出帖子列表、搜索、通知、治理和 AI 审计高频查询。
2. 建立可重复生成的测试数据。
3. 补索引并验证执行计划。
4. 统一分页边界和默认值。
5. 更新自动化或手动性能验收脚本。

## 验收方式

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

如有负载脚本，应提供一键运行命令并记录基准结果。

## 关键文件

- `backend-java/src/main/java/com/threadmind/forum/**/repository/`
- `backend-java/src/main/resources/db/migration/`
- `scripts/`

## 遗留风险

- 本地机器性能不能代表生产环境，基准只用于回归比较。
- 过早加索引可能增加写入成本，需要保持范围克制。

## 收口要求

M26 结束时，核心列表在 seed 大数据下应可稳定分页，且慢查询风险有记录。
