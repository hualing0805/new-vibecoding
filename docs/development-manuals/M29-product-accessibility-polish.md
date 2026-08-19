# M29 产品体验与可访问性收口 README

## 阶段目标

做一次面向真实使用的 UI/UX 打磨，统一文案、焦点、空状态和移动端降级体验。

## 交付范围

- 核心路由键盘导航。
- 表单、弹窗和侧栏焦点管理。
- 空状态、加载态和错误态统一。
- 移动端治理和管理视图降级。
- 文案和按钮命名统一。

## 实现顺序

1. 列出核心用户路径和管理路径。
2. 用键盘完成关键操作并记录缺口。
3. 修正焦点、空状态和错误提示。
4. 执行桌面和移动端视觉检查。
5. 更新验收截图或脚本说明。

## 验收方式

```powershell
cd frontend-vue
npm run typecheck
npm run build
```

```powershell
$env:PLAYWRIGHT_MODULE=".\frontend-vue\node_modules\playwright"
$env:PLAYWRIGHT_EXECUTABLE_PATH="C:\Program Files\Qoom Chrome\chrome.exe"
node .\scripts\visual-check.cjs
```

## 关键文件

- `frontend-vue/src/`
- `scripts/visual-check.cjs`
- `docs/`

## 遗留风险

- 可访问性改动可能影响现有视觉布局，需要用截图回归确认。
- 管理台信息密度和移动端可用性需要平衡。

## 收口要求

M29 结束时，核心路由应无横向溢出，关键操作可键盘完成。
