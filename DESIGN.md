# Design

ThreadMind 的界面是一张论坛工作台，不是宣传页。第一屏要让用户直接看到帖子流、板块状态、AI 辅助和发布入口。

## 视觉基调

界面使用“折叠式工作台”的语言：清晰的平面、轻微纸面折角、稳定的列表密度和受控的功能色。论坛内容占主位，AI 以侧栏、状态提示和编辑器工具的形式出现。

## 色彩

- Ink：`#17151f`，用于主文字和左侧导航。
- Surface：`#fffefa`，用于帖子、表单和工作面。
- Workbench：`#f4f6f1`，用于页面底色。
- Vermilion：`#d84a32`，用于发布、登录、提交等主要动作。
- Teal：`#087f8c`，用于 AI、检索和可用状态。
- Indigo：`#31558f`，用于治理、引用和系统状态。
- Amber：`#d59b27`，用于标签和轻量提示。

颜色按功能分配，不做单色主题。页面主体保持浅色，因为论坛的主要场景是长时间阅读和重复操作。

## 字体

使用系统 sans 字体栈，兼容中文界面和本地开发环境：

```css
ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif
```

标题使用更高字重，不使用负字距。正文宽度控制在适合阅读的范围，列表信息优先保证扫描效率。

## 布局

- 桌面端使用左侧固定导航、顶部搜索操作条、主内容区和右侧状态栏。
- 移动端折叠为顶部导航和单列内容。
- 首页直接显示论坛操作台，不设置营销式 hero。
- 帖子列表使用独立卡片，卡片半径不超过 8px。
- 工具区不再套卡片，避免卡片包卡片。

## 组件规则

- 主要动作使用 vermilion 按钮，并配套 lucide 图标。
- 图标按钮用于通知等单一命令。
- 分段控件用于排序。
- 标签使用 amber chip。
- AI 区域使用 teal 状态和工具入口。
- 管理台使用表格式队列，避免装饰性卡片堆叠。

## 交互状态

所有可操作元素需要有 hover 和 focus-visible 状态。表单保留稳定高度，按钮和列表项不能因 hover 改变布局尺寸。

## 当前落地文件

- `frontend-vue/src/app/AppShell.vue`
- `frontend-vue/src/pages/HomePage.vue`
- `frontend-vue/src/pages/TopicDetailPage.vue`
- `frontend-vue/src/pages/TopicEditorPage.vue`
- `frontend-vue/src/pages/AssistantPage.vue`
- `frontend-vue/src/pages/AdminDashboardPage.vue`
- `frontend-vue/src/pages/LoginPage.vue`
- `frontend-vue/src/styles/tokens.css`
- `frontend-vue/src/styles/base.css`

