# M28 CI/CD 质量门禁 README

## 阶段目标

把本地检查迁移到 GitHub Actions，让 PR 或 push 自动验证核心质量。

## 交付范围

- Java test。
- Vue typecheck 和 build。
- Python pytest。
- 数据库迁移检查。
- 文档格式或链接检查。
- CI 失败时输出可定位的日志。

## 实现顺序

1. 梳理本地 `scripts/check.ps1` 和实际验证命令。
2. 增加 GitHub Actions workflow。
3. 配置 Java、Node 和 Python 版本。
4. 加入缓存但避免缓存掩盖依赖问题。
5. 在 push 后观察首轮 CI 结果并修正。

## 验收方式

```powershell
.\scripts\check.ps1
```

M28 实现后，还必须验证 GitHub Actions 在远端实际执行通过。

## 关键文件

- `.github/workflows/`
- `scripts/check.ps1`
- `backend-java/pom.xml`
- `frontend-vue/package.json`
- `agent-python/pyproject.toml`

## 遗留风险

- CI 网络和依赖下载速度可能不稳定，需要合理缓存。
- Testcontainers 在 CI 中需要 Docker 环境支持。

## 收口要求

M28 结束时，push 后应自动运行 Java、Vue、Python 和迁移相关质量门禁。
