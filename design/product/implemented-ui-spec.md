---
doc: design/product/implemented-ui-spec
title: 已实现 UI 规格（设计稿 ↔ 代码对照）
status: accepted
date: 2026-11-02
owner: wang
related:
  - design/product/screen-specs
  - design/product/visual-language
  - design/prototype/ui.html
  - android/docs/architecture
  - docs/decisions/0010-design-prototype
---

# 已实现 UI 规格（设计稿 ↔ 代码对照）

> 本文描述**代码已经实现**的 UI。设计意图仍看 [`screen-specs.md`](screen-specs.md)；两者冲突时，以本文和代码为准。
> 可视稿：[`../prototype/ui.html`](../prototype/ui.html)。静态截图：`design/exports/phone-*.png`（411×914，浅色/深色）。
> 基准代码：`android/app/src/main/java/com/weiji/wang/`；行号以 2026-11-02 为准。

## 1. 全局 token

### 1.1 颜色

| Token | 浅色 | 深色 | 用途 |
|---|---|---|---|
| background | `#F2F2F7` | `#000000` | 页面底色 |
| surface | `#FFFFFF` | `#1C1C1E` | 卡片、导航栏（96%）、分组 |
| surfaceVariant | `#E5E5EA` | `#2C2C2E` | 搜索、chip、轨道、封面占位 |
| onSurface | `#1C1C1E` | `#FFFFFF` | 主文字/图标 |
| onSurfaceVariant | `#6C6C70` | `#EBEBF5` | 次要文字 |
| outline | `#C7C7CC` | `#38383A` | 次级按钮描边 |
| outlineVariant | `#E5E5EA` | `#2C2C2E` | 0.5dp 分隔线 |
| primary | `#FF9500` | `#FF9F0A` | 主按钮、FAB、链接 |
| tertiary | `#34C759` | `#30D158` | **提示文字/已购/适量（绿色）** |
| error | `#FF3B30` | `#FF453A` | 删除、危险、收藏 |
| secondaryContainer | `#F2F2F7` | `#2C2C2E` | 导航选中指示器 |

平台色：抖音 `#161823`、小红书 `#FF2442`、B站 `#00A1D6`、其他 `#8E8E93`；徽章底色为 10% 透明度。

### 1.2 字阶

| 角色 | 字号/行高 | 字重 | 用途 |
|---|---|---|---|
| displayMedium | 34/40 | Bold，-0.4 | 页面大标题 |
| headlineLarge | 28/34 | Bold，-0.2 | 详情标题 |
| headlineMedium | 22/28 | SemiBold | 空状态标题 |
| titleLarge | 20/26 | SemiBold | 区块标题 |
| titleMedium | 17/22 | SemiBold | 建议卡标题、份量 |
| bodyLarge | 17/26 | Regular | 正文、食材行、网格卡标题 |
| bodyMedium | 15/22 | Regular | 副标题、搜索、说明 |
| bodySmall | 13/18 | Regular | 来源条 |
| labelLarge | 15/20 | SemiBold | 按钮、分段、步骤标签 |
| labelMedium | 13/18 | Medium | chip、筛选、烹饪顶部 |
| labelSmall | 11/14 | Medium | 导航标签、徽章、卡内小字 |
| CookStepStyle | 26/38 | Medium | 烹饪步骤 |

### 1.3 圆角与触控

- 20：首页建议卡、WeijiCard。
- 16：网格卡、设置分组、状态卡、步骤卡。
- 14：主/次按钮、快捷卡、最近卡、输入框。
- 12：购物行、计时块、步骤图。
- 10：来源条、最近封面。
- 9/7：分段控件外/内。
- 圆形：搜索、筛选、chip、徽章、FAB、勾选、返回。
- 触控：FAB 56、烹饪按钮 64、返回 40、步进 36、收藏 30、勾选 24。
- 注意：FilterPill 32、SearchField 38、分段 32、收藏 30 等**小于 48dp**，这是已实现事实，设计稿不要擅自放大。

## 2. 外壳与导航

- `MainActivity.enableEdgeToEdge()`；`Scaffold(containerColor = background)`。
- 画布：手机按 Pixel 7 的 **411×914dp**（density 420）设计；平板 900×1280dp。窗口宽 <600dp 用底部栏，≥600dp 用 Rail。
- 手机（窗口宽 <600dp）：底部 `NavigationBar`，高 80dp；4 个 Tab「今天/菜谱/采购/我的」；图标 22dp、标签 11sp；选中指示器为 `secondaryContainer` 胶囊 64×32；标签始终显示。
- 平板（≥600dp）：左侧 `NavigationRail`，宽 80dp；图标 22dp；FAB 位于 4 个 Tab 下方，上间距 12dp。
- 手机 FAB 只在 `home` / `recipes` 显示；平板 Rail 内 FAB 在非烹饪路由始终显示。
- `capture`、`login`、`edit`、`recipe` 也显示手机底部栏（无对应 Tab，全部未选中）；`cook` 隐藏底部栏/Rail/FAB。
- 菜谱网格：窗口宽 ≥840dp 为 3 列，否则 2 列。

## 3. 动效

| 场景 | 已实现行为 | 来源 |
|---|---|---|
| 页面切换 | `fadeIn` / `fadeOut`，`tween(700ms, FastOutSlowInEasing)`；无位移/缩放 | Navigation 2.7.7 默认 |
| 烹饪翻步 | `HorizontalPager` 手势横滑；按钮 `animateScrollToPage` | CookModeScreen.kt:187,193,273 |
| 点击 | Material 3 ripple：225ms 扩散 + 150ms 淡出 | material-ripple 1.6.8 |
| Switch | 100ms Tween | material3 1.2.1 |
| 导航选中指示器 | alpha 100ms | material3 1.2.1 |
| 分段控件 | 选中态即时切换 | Common.kt:273 |
| 进度条 | determinate，组件自身过渡 | CaptureScreen.kt:345 |
| 计时 chip | 每秒更新；运行橙 20% 底 + 橙字，暂停白 8% 底 + 白 60% 字；即时切换 | CookModeScreen.kt:462 |
| 对话框 | Compose AlertDialog 窗口默认呈现 | 各 Screen |

**不存在**：自定义页面转场、8dp 上移、按压缩放、220ms spring、大标题收缩、数字滚动、骨架屏、Tab 切换动画、呼吸/悬浮/发光。

## 4. 逐屏规格

### 4.1 今天（home）

- 页头：`PageHeader`，上 12 / 标题 34/40 Bold / 副标题 15/22 上 2 / 下 12；左右 16。无右侧搜索/设置按钮。
- 空库：`EmptyState` 放在 520dp 高容器内，垂直居中；72dp 圆底 + 34dp 碗图标；标题「今晚吃什么？」22/28；说明 15/22；主按钮「收录第一条」；次按钮「手动创建」橙色文字。
- 有数据：
  - `SectionHeader`「今晚吃什么」+ 动作「全部」（15/22 橙色）。
  - 建议卡 264dp 宽、封面 148dp、圆角 20、内容 padding 12、标题 17/22 两行、平台徽章 + 份量 13、提示 11 绿色。
  - `SectionHeader`「最近做过」；最近卡 228dp 宽、padding 8、封面 56 圆角 10、标题 17/26 两行、时间 11 灰色。
  - `SectionHeader`「快速开始」；三个等宽卡，圆角 14、上下 16、文字 15/20 SemiBold。
- 无长按菜单、下拉刷新、加载骨架。
- 代码：`HomeScreen.kt`；组件 `Common.kt`。

### 4.2 菜谱（recipes）

- 页头「菜谱」+ 副标题「N 道」。
- 搜索框：高 38、圆形、`surfaceVariant`；17dp 放大镜；占位 15/22 **绿色**；有内容时显示 24dp 清除。
- 筛选：全部 / 收藏 / 没做过 / 30 分钟内；圆角 999、padding 14/7、13/18；选中 `onSurface` 底 + `surface` 字，未选 `surface` 底 + `onSurfaceVariant` 字。
- 网格：手机 2 列、平板 ≥840dp 3 列；左右 16、列距 12、行距 14。
- 卡片：圆角 16；1:1 封面（无图时 28dp 碗）；右上 30dp 圆形收藏钮（`surface` 82%，16dp 心形，收藏为 `error` 红）；内容 padding 10；标题 17/26 两行（最小两行）；平台徽章或「手动」+ 份量 11。
- 空态：420dp 高，标题「还没有菜谱」/「没有找到」，主按钮「去收录」/「清空搜索」。
- 无排序菜单、无长按删除、无防抖加载条。
- 代码：`RecipesScreen.kt`。

### 4.3 详情（recipe/{id}）

- 封面：16:9 全宽；无图时 `surfaceVariant` + 28dp 碗；返回按钮 40dp 圆形 `surface` 86%，左上 8dp。
- 标题 28/34 Bold；副标题 15/22；来源条：平台徽章 + `@作者` 13 + 「原链接 ↗」橙色右对齐。
- MetaChip：圆角 999、`surfaceVariant`、padding 10/5、13/18；示例「N 人份 / 约 N 分钟 / 已收藏」；有视频时加「含视频」。
- 分段控件：食材 / 步骤 / 原文 / 记录；外 9、内 7、padding 2、每段高 32、15/20；选中白底（深色 `surface`）；无阴影、无切换动画。
- 食材页：份量行（17/22 SemiBold + 36dp 步进器，范围 1–12）；提示 11 绿色「按 N 人份记录，已换算为 N 人份」；食材行左右 20、上下 12，名称 17/26，数量右对齐，适量/少许绿色；0.5dp 分隔线（左 20）；分组标题 15/20 灰色；底部「全部加入采购清单」主按钮。
- 步骤页：有视频媒体时先显示「原视频」+ 220dp 播放器（圆角 14，黑底，系统控制器）；步骤卡圆角 16、padding 14、间距 6：26dp 橙色序号（黑字 13 Bold）+「第 N 步」15/20 灰 + 时长 chip + 时间戳 chip；正文 17/26；可选 16:10 图（圆角 12）；底部「开始做」。
- 原文页：正文 17/26 可选中；底部提示 13/18 绿色「自动解析可能有遗漏。以原文和原视频为准。」
- 记录页：卡片圆角 16、padding 14；日期 15/20、份量 13、5 颗 13dp 星（实心橙/空心 `outlineVariant`）；备注 15/22。
- 底部操作条：高 70、`surface` 97%、padding 10/16；收藏（红心）/编辑/删除 48dp 图标；右侧 160dp「开始做」主按钮。
- 删除对话框：标题「删除这道菜谱？」，正文「会同时删除已下载的图片和视频，无法恢复。」，取消 / 删除（红）。
- 代码：`RecipeDetailScreen.kt`、`VideoPlayerCard.kt`。

### 4.4 烹饪（cook/{id}）

- 纯黑 `#000000`，白字，强调 `#FF9F0A`；不跟随浅/深色主题；隐藏底部栏/Rail/FAB；屏幕常亮。
- 注意：`Scaffold` 已应用一次系统栏 inset，`CookModeScreen` 又调用 `statusBarsPadding()` + `navigationBarsPadding()`，实际顶部/底部各有**两次**状态栏/导航栏 inset；设计稿保留这个已实现间距。
- 顶部：40dp 关闭；菜名 13/18 白 55% 单行；进度 `N/M` 15/20 白 55%；「计时」「食材」胶囊 13/18、白 10% 底、padding 12/7。
- 步骤：HorizontalPager 横滑；「第 N 步」15/20 橙色；正文 26/38 Medium 白色；时长胶囊圆角 12、白 8% 底、橙字 15/20、padding 14/10；可选 240dp 图圆角 16。
- 计时 chips：最多显示 3 个；圆角 12；运行橙 20% 底 + 橙字，暂停白 8% 底 + 白 60% 字；右侧 36dp 加号。
- 底部按钮：高 64、圆角 18、权重 1 : 1.4、间距 12、左右 16；禁用白 5% 底 + 白 30% 字；主按钮橙底黑字。
- 对话框：
  - 退出：标题「退出烹饪？」，正文「当前步骤会保留，下次可以接着做。」，继续做 / 退出。
  - 食材：标题「食材（N 人份）」，列表，知道了。
  - 计时：标题「计时多久？」，1/3/5/10 分钟，取消。
  - 完成记录：标题「记录这次」，5 颗 30dp 星、份量步进、备注输入，跳过 / 保存。
- 到点震动一次（600ms），无通知；后台不保留计时。
- 代码：`CookModeScreen.kt`。

### 4.5 收录（capture）

- 页头「收录」+ 关闭 36dp 圆形；副标题「粘贴一条抖音、小红书或 B 站链接」。
- 输入框：多行 4–8 行（最小 150dp），圆角 16，1dp `outline`，padding 16，15/22；占位绿色「分享文案或链接」。
- 按钮行：主「解析」（空输入/解析中禁用，禁用为 `surfaceVariant` 底 + 灰字），次「手动创建」描边。
- 识别到链接时，输入框与按钮之间显示平台徽章 + 单行标题提示（13/18 灰），来自 `LinkParser.parse`。
- 状态卡：`surface`、圆角 16、padding 16；标题 17/22、正文 15/22 灰；成功带 20dp 绿色对勾；失败标题红色。
- 六态：
  - Idle：不渲染状态卡。
  - Preparing：「正在读取页面… / 本地解析，不上传你的链接。」
  - NeedsWebView：「正在用内置浏览器读取抖音…」+ 两个次级按钮「去登录抖音」「只保留链接」；WebView 1dp、alpha 0.01，不可见。
  - Downloading：「正在保存」+ 4dp `LinearProgressIndicator`（橙色）。
  - Success：「收录完成」+ 对勾 + 「打开菜谱」/「再收一条」。
  - Failed：「没有完整收录」+ 原因 + 「重试」/「去登录」或「手动创建」。
- 草稿：行圆角 14、`surface`、padding 12；平台徽章 + 单行标题 15/22 + 状态 11（失败红/其他绿）+ 24dp 刷新图标。
- 自动：进入页面读剪贴板含 http 则填入；有链接时 250ms 后自动解析。
- 代码：`CaptureScreen.kt`。

### 4.6 采购（shopping）

- 页头「采购」+ 36dp 白色圆形加号；副标题「还需买 N 项 · 已买 N 项」。
- 分组：按 `category` 字段；组标题 20/26。
- 行：圆角 12、`surface`、padding 12/10、间距 2；24dp 勾选圆（未选 `surfaceVariant`，已选绿色 + 15dp 白勾）；名称 17/26（已购绿色 + 删除线）；数量 11/14 灰；右侧 22dp 绿色删除图标。
- 已购段标题「已购」。
- 底部两个「清除已购」「清空清单」次级按钮，各占一半。
- 空态：480dp 高，「采购清单是空的」+「去菜谱看看」。
- 无左滑删除、无撤销、无二次确认。
- 代码：`ShoppingScreen.kt`。

### 4.7 我的（settings）

- 页头「我的」+ 副标题「所有内容只保存在本机」。
- 分组：`surface`、圆角 16、左右 16；行 padding 16/14；行间 0.5dp 分隔线（左 16）。
- 数据：存储空间 / 导出备份 / 从备份恢复。
- 收录：仅 Wi-Fi 下载媒体（Switch）/ 自动下载视频与图片（Switch）/ 抖音登录 / 小红书登录 / B 站登录 / 清除全部登录信息（红字）。
- 体验：外观 + 三段控件「跟随系统 / 浅色 / 深色」。
- 关于：版本号 + 三段说明（灰 / 红 / 绿）。
- **已知事实**：三段控件写入 DataStore，但 `MainActivity` 调 `WeijiTheme()` 未传 `darkTheme`，实际只跟随系统主题；设计稿保留控件，但不要声称它能切换。
- Switch：52×32，滑块 24，选中橙色，100ms 过渡。
- 代码：`SettingsScreen.kt`、`MainActivity.kt`、`Theme.kt`。

### 4.8 编辑（edit/{id}）

- 页头「新建菜谱」/「编辑」+ 右上「保存」15/20 橙色 TextButton；副标题「自动解析的结果可以随时改」。
- 字段顺序：菜名 / 几人份 + 时长（分钟）并排 / 来源链接（可选）/ 食材（多行 6–14 行）/ 步骤（多行 8–20 行）/ 备注（多行）。
- 输入框：圆角 14、1dp `outline`、padding 16；浮动标签 12/14 灰；食材/步骤上方有 17/22 标题 + 11/14 绿色提示。
- 底部大「保存」主按钮（与页头保存并存）。
- 校验：仅菜名非空，行内错误提示。
- 代码：`EditRecipeScreen.kt`。

### 4.9 登录（login/{platform}）

- 页头「登录抖音」等 + 关闭；副标题「登录信息只保存在本机，用于解析你主动提交的链接」。
- WebView 占满中段（真实可见，加载平台登录页；设计稿用占位框表示第三方页面）。
- 底部说明 13/18 灰；「我已登录 / 已保存，返回」主按钮 + 「清除」次级按钮；提示 11/14 绿色。
- 代码：`LoginScreen.kt`。

## 5. 未实现清单（不要画进设计稿）

- 首页搜索/设置入口、长按快捷菜单、下拉刷新、离线细条、加载骨架。
- 菜谱排序菜单、快手/有视频筛选、单列/网格切换、长按删除、搜索防抖、5 秒撤销。
- 详情份量数字滚动、橙色描边份量胶囊、食材勾选、时间戳跳转视频、分享导出、顶部待补全提示条。
- 烹饪食材上拉抽屉、后台计时通知、步骤朗读、拍照写记录、平板 40/60 分屏、字体缩放降字号。
- 平板详情左右分栏、折叠屏姿态、三栏导航、Medium/Expanded 页边距 24/32。
- 自定义转场（淡入 + 8dp 上移）、按压缩放 0.97、220ms spring、大标题滚动收缩、数字滚动。
- 导航 Rail 选中项橙色胶囊；实际是 `secondaryContainer` 灰胶囊。
- 触控目标全部 ≥48dp；实际有 24–40dp 控件。

## 6. 变更规则

1. 改 UI 先改代码，再改本文，再改 `design/prototype/ui.html`。
2. 本文与代码冲突时，以代码为准；发现冲突先修本文。
3. 新增页面/状态必须同时补原型；原型不得引入未实现功能。
4. 设计稿只做“已实现”的可视记录；新想法写进 `screen-specs.md` 的规划区，不要混进原型。
