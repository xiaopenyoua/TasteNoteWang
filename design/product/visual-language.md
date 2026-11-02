---
doc: design/product/visual-language
title: 视觉语言与设计系统（已实现值）
status: accepted
date: 2026-11-02
owner: wang
related:
  - design/product/implemented-ui-spec
  - docs/decisions/0010-design-prototype
---

# 视觉语言与设计系统（已实现值）

> 本文记录**代码里真实生效**的视觉值，供设计稿与实现对照。
> 早期设想但未实现的条目集中在第 9 节，不要画进设计稿。
> 可视稿：[`../prototype/ui.html`](../prototype/ui.html)；逐屏对照：[`implemented-ui-spec.md`](implemented-ui-spec.md)。

## 1. 色彩（来自 `Color.kt` / `Theme.kt`）

### 1.1 浅色

| Token | 值 | 实际用途 |
|---|---|---|
| `background` | `#F2F2F7` | 页面底色 |
| `surface` | `#FFFFFF` | 卡片、导航栏（96% 不透明度）、设置分组 |
| `surfaceVariant` | `#E5E5EA` | 搜索框、MetaChip、分段控件轨道、封面占位、空态圆形底、禁用按钮 |
| `onSurface` | `#1C1C1E` | 主文字、主图标、焦点描边 |
| `onSurfaceVariant` | `#6C6C70` | 副标题、说明、占位图标 |
| `outline` | `#C7C7CC` | 次级按钮 1dp 描边 |
| `outlineVariant` | `#E5E5EA` | 0.5dp 分隔线 |
| `primary` | `#FF9500` | 主按钮、FAB、可点文字、光标 |
| `tertiary` | `#34C759` | **被当作三级/提示文字使用**：搜索占位、时间提示、适量/少许、已购、删除图标 |
| `error` | `#FF3B30` | 删除、危险文字、收藏心形 |
| `secondaryContainer` | `#F2F2F7` | 底部导航/侧栏选中指示器 |

### 1.2 深色

| Token | 值 |
|---|---|
| `background` | `#000000` |
| `surface` | `#1C1C1E` |
| `surfaceVariant` | `#2C2C2E` |
| `onSurface` | `#FFFFFF` |
| `onSurfaceVariant` | `#EBEBF5`（不透明，不是 60%） |
| `outline` | `#38383A` |
| `outlineVariant` | `#2C2C2E` |
| `primary` | `#FF9F0A` |
| `tertiary` | `#30D158` |
| `error` | `#FF453A` |
| `secondaryContainer` | `#2C2C2E` |

### 1.3 对比与用法

- 主文字/背景 ≥ 15:1。
- 橙色只做背景、图标、进度；**橙色按钮文字固定纯黑**（`PrimaryButton` 写死 `Color.Black`）。
- `tertiary` 是绿色，不是灰色；深色下 `#30D158`。
- 平台色：抖音 `#161823`、小红书 `#FF2442`、B站 `#00A1D6`、其他 `#8E8E93`（徽章底色为 10% 透明度）。

## 2. 字体（来自 `Type.kt`）

系统字体（Android 上 Roboto + Noto Sans CJK），不内置字体。

| 角色 | 字号 / 行高 | 字重 | 用途 |
|---|---|---|---|
| `displayMedium` | 34 / 40 | Bold，字距 -0.4 | 页面大标题（今天、菜谱、采购、我的、收录、新建菜谱） |
| `headlineLarge` | 28 / 34 | Bold，字距 -0.2 | 详情标题 |
| `headlineMedium` | 22 / 28 | SemiBold | 空状态标题 |
| `titleLarge` | 20 / 26 | SemiBold | 区块标题、烹饪页份量加减 |
| `titleMedium` | 17 / 22 | SemiBold | 首页建议卡标题、详情「份量」、烹饪按钮 |
| `bodyLarge` | 17 / 26 | Regular | 正文、食材行、网格卡标题、设置行 |
| `bodyMedium` | 15 / 22 | Regular | 副标题、搜索框、说明、区块动作 |
| `bodySmall` | 13 / 18 | Regular | 来源条、风险说明 |
| `labelLarge` | 15 / 20 | SemiBold | 按钮、分段控件、烹饪页「第 N 步」 |
| `labelMedium` | 13 / 18 | Medium | MetaChip、筛选胶囊、烹饪页顶部 |
| `labelSmall` | 11 / 14 | Medium | 底部导航标签、平台徽章、卡内小字 |
| `CookStepStyle` | 26 / 38 | Medium | 烹饪模式步骤正文 |

`displayLarge`（40/44）已定义但全仓库未使用。

## 3. 形状与间距

- 圆角档位：8 / 10 / 14 / 20 / 28；组件实际使用：
  - 20：首页建议卡、WeijiCard
  - 16：菜谱网格卡、设置分组、状态卡、步骤卡、空态卡片
  - 14：主/次按钮、快捷开始卡、最近做过卡、输入框
  - 12：购物行、计时块、步骤图、烹饪时长/计时胶囊
  - 10：来源条、最近封面
  - 9 / 7：分段控件外/内
  - 圆形：搜索框、筛选胶囊、MetaChip、平台徽章、FAB、勾选圈、返回按钮
- 间距：4pt 网格（4/6/8/10/12/14/16/20/24）。
- 页面左右边距：手机 16dp；平板上内容同样 16dp（导航 Rail 宽 80dp）。
- 触控：FAB 56dp；烹饪上下步 64dp；返回 40dp；步进器 36dp；收藏 30dp；勾选 24dp。

## 4. 共享组件（来自 `Common.kt`）

| 组件 | 关键尺寸 |
|---|---|
| `PageHeader` | 上 12；标题 34/40；副标题 15/22、上 2；下 12；左右 16 |
| `SectionHeader` | 左右 16、上下 8；标题 20/26；动作 15/22 橙色，8dp 圆角点击区 + 8/4 内边距 |
| `Hairline` | 0.5dp，`outlineVariant` |
| `RecipeCover` | 默认圆角 14；无图时居中 28dp 碗图标，底色 `surfaceVariant` |
| `PlatformBadge` | 圆形，底色平台色 10%；5dp 圆点 + 4dp 间距 + 11sp 文字；紧凑版左右 6dp |
| `SourceBar` | 圆角 10，上下 6；平台徽章 + 作者 13sp + 「原链接 ↗」橙色 |
| `MetaChip` | 圆形，`surfaceVariant` 底，左右 10 / 上下 5；13sp；可选 13dp 图标 |
| `SegmentedControl` | 外圆角 9、内 7、padding 2；每段上下 6；15sp；选中白底、未选透明 |
| `EmptyState` | 72dp 圆底 + 34dp 碗；标题 22/28；说明 15/22；主按钮；次按钮橙色文字 |
| `PrimaryButton` | 圆角 14，左右撑满，上下 15（约 50dp 高）；橙色底、黑字 15sp SemiBold |
| `SecondaryButton` | 圆角 14，左右 18 / 上下 13（约 46dp 高）；1dp `outline` 描边 |
| `CaptureFab` | 56dp 圆形，橙色底，黑色加号；无阴影 |
| `SearchField` | 高 38，圆形，`surfaceVariant` 底；17dp 放大镜；15sp；占位用**绿色** `tertiary` |
| `FilterPill` | 圆形，左右 14 / 上下 7；选中 `onSurface` 底 + `surface` 字，未选 `surface` 底 + `onSurfaceVariant` 字 |

## 5. 导航外壳（来自 `WeijiApp.kt`）

- 手机（<600dp）：底部 `NavigationBar`，4 个 Tab「今天 / 菜谱 / 采购 / 我的」；图标 22dp，标签 11sp；选中指示器为 `secondaryContainer` 胶囊；`capture`、`login`、`edit`、`recipe` 也显示底部栏；烹饪路由隐藏。
- 平板（≥600dp）：左侧 `NavigationRail`，宽 80dp；图标 22dp；Rail 内 FAB（上间距 12dp）。
- FAB：仅 `home` / `recipes` 在手机显示；平板在 Rail 内始终显示（烹饪除外）。
- 菜谱网格：窗口宽度 ≥840dp 为 3 列，否则 2 列。
- 状态栏/手势条：edge-to-edge；内容按 Scaffold insets 下移；导航栏背景 `surface` 96%。

## 6. 动效（全部为系统/框架默认，无自定义）

| 场景 | 实际行为 |
|---|---|
| 页面切换 | Navigation Compose 2.7.7 默认 `fadeIn` / `fadeOut`，`tween(700ms, FastOutSlowInEasing)`；**无位移、无缩放** |
| 烹饪翻步 | `HorizontalPager` 手势横滑；按钮触发 `animateScrollToPage`（框架默认弹簧） |
| 点击 | Material 3 ripple：约 225ms 扩散 + 150ms 淡出 |
| 开关 Switch | 100ms `TweenSpec` |
| 底部导航/侧栏选中指示器 | alpha 100ms |
| 分段控件 | 选中态即时切换 |
| 进度条 | `LinearProgressIndicator` determinate，进度变化由组件自身过渡 |
| 计时 chip | 每秒更新数字；运行中橙色 20% 底 + 橙字，暂停时白色 8% 底 + 白字 60%；颜色即时切换 |
| 对话框 | Compose `AlertDialog`，窗口默认呈现 |

**没有**：按压缩放、8dp 位移、220ms spring、大标题收缩、数字滚动、骨架屏、Tab 切换动画。

## 7. 烹饪模式（独立视觉）

- 强制纯黑 `#000000`，文字纯白，强调色 `#FF9F0A`；不跟随浅/深色主题。
- 顶部：40dp 关闭按钮；菜名 13sp 白 55%；进度 `N/M` 15sp 白 55%；「计时」「食材」胶囊 13sp、白色 10% 底、左右 12 / 上下 7。
- 步骤：序号 15sp 橙色；正文 26/38 Medium；时长胶囊圆角 12、白色 8% 底、橙色 15sp、左右 14 / 上下 10；步骤图高 240、圆角 16。
- 底部：上一步/下一步高 64、圆角 18；权重 1 : 1.4；间距 12；禁用白色 5% 底 + 白 30% 字；主按钮橙色底 + 黑字。
- 计时 chips：圆角 12；最多显示 3 个；运行中橙色 20% 底，暂停白色 8% 底。
- 屏幕常亮；音量键翻步；返回键弹「退出烹饪？」。

## 8. 平板

- 同屏更多信息，不是放大手机。
- 首页建议卡仍为 264dp 宽；三张刚好铺满 820dp 内容区。
- 菜谱 3 列（宽度 ≥840dp）；详情仍是单栏上下结构（**没有左右分栏**）。
- 烹饪模式仍是手机同款单栏 pager（**没有左右 40/60 分屏**）。

## 9. 规划中（未实现，不要画进设计稿）

以下来自早期 `screen-specs.md` / 旧版本文档，代码中不存在：

- 首页右上搜索、设置入口；长按快捷菜单；下拉刷新；离线细条。
- 菜谱排序菜单；「快手 / 有视频」筛选；列表/网格切换；5 秒撤销 Snackbar。
- 详情食材勾选、份量数字滚动、橙色描边份量胶囊；时间戳跳转视频；分享导出；顶部「待补全」提示条。
- 烹饪模式视频/图片左右分屏；后台计时通知；步骤朗读；拍照写烹饪记录；字体缩放 >1.3 自动降字号。
- 平板详情左右分栏、折叠屏姿态适配、三栏导航。
- 自定义页面转场（淡入 + 8dp 上移）、按压缩放 0.97、220ms spring、大标题滚动收缩。
- 通知、深链接 `weiji://open?url=` 的完整解析（Manifest 有 scheme，UI 未处理 url 参数）。

## 10. 检查清单

- [ ] 颜色、字号、圆角与本文一致
- [ ] 橙色按钮文字是黑色
- [ ] 提示文字是绿色（`tertiary`），不是灰色
- [ ] 页面切换只有 700ms 淡入淡出，没有位移/缩放
- [ ] 没有画第 9 节的未实现功能
- [ ] 手机 360–390dp 宽、平板 ≥600dp 两种布局都检查
- [ ] 烹饪模式纯黑、白字、橙色强调
