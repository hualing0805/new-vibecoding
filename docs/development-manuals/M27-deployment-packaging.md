# M27 部署打包 README

## 阶段目标

让 Java、Vue、Python Agent 和依赖能一键部署到本地或测试环境，减少新机器启动成本。

## 交付范围

- 增加 Java、Vue 和 Python Agent 的 Dockerfile。
- 增加 compose profile，区分开发和测试部署。
- 增加环境检查脚本。
- 补充生产构建说明。
- 保留本地非容器启动方式。

## 实现顺序

1. 梳理三端构建产物和运行端口。
2. 增加最小 Dockerfile。
3. 扩展 `infra/docker-compose.yml` 或新增部署 compose。
4. 增加环境变量模板和检查脚本。
5. 在干净环境中验证启动流程。

## 验收方式

```powershell
cd frontend-vue
npm run build
```

```powershell
cd backend-java
..\.tools\apache-maven\bin\mvn.cmd test
```

部署 compose 的启动命令应在 M27 实现后补入本手册。

## 关键文件

- `backend-java/Dockerfile`
- `frontend-vue/Dockerfile`
- `agent-python/Dockerfile`
- `infra/`
- `.env.example`

## 遗留风险

- 容器化可能暴露 Windows 路径、端口占用和镜像下载问题。
- 生产配置必须避免把本地默认密码当作安全配置。

## 收口要求

M27 结束时，新机器应能按文档启动完整栈，并清楚知道本地和部署配置差异。
