# M30 Release Candidate README

## 阶段目标

形成可演示、可复盘的候选版本，把 M0-M30 的阶段手册、验证脚本和已知风险收口成完整闭环。

## 交付范围

- 全链路验收脚本。
- 版本说明。
- 已知风险清单。
- 演示脚本。
- 回滚方案。
- README 和开发手册索引最终校准。

## 实现顺序

1. 复核 M0-M29 阶段手册和遗留风险。
2. 运行 Java、Vue、Python 和联调验证。
3. 准备演示数据和演示流程。
4. 生成版本说明和已知风险。
5. 标记候选版本并推送。

## 验收方式

```powershell
.\scripts\check.ps1
```

```powershell
.\scripts\start-local-stack.ps1
.\scripts\verify-local-flow.ps1
.\scripts\stop-local-stack.ps1
```

M30 还应执行前端视觉检查和必要的手动演示走查。

## 关键文件

- `README.md`
- `PRODUCT.md`
- `docs/development-manuals/`
- `scripts/`
- `.github/workflows/`

## 遗留风险

- RC 不是生产发布，仍需明确哪些生产级能力暂缓。
- 演示脚本应避免依赖开发者机器上的隐式状态。

## 收口要求

M30 结束时，项目应具备一键验收、版本说明、已知风险和可复盘的阶段文档闭环。
