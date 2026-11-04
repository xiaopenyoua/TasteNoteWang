---
doc: design/product/recipe-preview-directions
title: 菜谱预览页视觉方向（选定 01 编辑部）
status: accepted
date: 2026-11-04
owner: wang
related:
  - design/product/implemented-ui-spec
  - design/product/visual-language
  - design/prototype/recipe-preview-editorial.html
  - design/prototype/recipe-preview-immersive.html
  - design/prototype/recipe-preview-minimal.html
  - design/prototype/recipe-preview-warm.html
  - design/prototype/recipe-preview-gallery.html
---

# 菜谱预览页视觉方向（5 版待选）

> **状态：accepted（2026-11-04 选定 01 编辑部并落地）。** 其余四个方向保留作参考；落地记录见 `implemented-ui-spec.md` §4.3。
> 五版整页长图：[`../exports/recipe-preview-directions.png`](../exports/recipe-preview-directions.png)。

## 1. 问题

用户反馈「菜谱预览页太丑，一点也没有 Apple 的精致感」。对照当前只读预览（`design/product/implemented-ui-spec.md` §4.3）与线上截图，具体是四件事：

1. **没有视觉主角**：封面、视频、标题、食材、步骤的权重几乎一样，首屏看完不知道先看哪里。
2. **形状语言不统一**：封面直角、卡片 16、按钮胶囊、对话框 28，圆角和阴影各说各话。
3. **字阶只有大小没有语气**：标题、正文、辅助信息都在用同一套无衬线字重，缺少「大标题—说明—元信息」的节奏。
4. **步骤像表单**：序号、时长胶囊、正文、备注挤在一张卡里，信息密度高，读起来累。

上一轮试稿用了插画封面和示意图，反而放大了「不精致」：食物类产品的高级感主要来自**真实摄影 + 克制的排版**，不是装饰。

## 2. 本轮目标与非目标

**目标**：信息架构、字段和交互不变，只重做视觉；给出 5 个方向，每个都是 390pt 宽、可滚动的整页设计，能直接对照选型。

**非目标**：不加新功能，不改底部导航与路由，不做平板专属版式（沿用 `screen-specs.md` 的宽屏规则），不写 Compose 代码。

## 3. 共同基准

| 项 | 规则 |
|---|---|
| 画布 | 390 × 自适应高度（整页长图），状态栏 54，安全区 20，底部 home indicator |
| 字阶 | 大标题 30–34 / 行高 1.12–1.18；节标题 21–25；正文 14.5–16 / 1.62–1.72；辅助 11–13；标签 9.5–10 + 字距 |
| 触控 | 图标按钮 ≥ 38，主按钮 48–54，食材行 ≥ 44 |
| 形状 | 每个方向只用一套圆角：0 / 12 / 18 / 26；不混用「胶囊 + 大圆角卡 + 直角封面」 |
| 材质 | 只在压图控件上用毛玻璃（`blur 18–26`），正文不叠玻璃 |
| 图片 | 真实食物摄影；统一轻暖调，禁止插画、emoji、渐变占位 |
| 字重 | 中文只用 400 / 500 / 650 / 700 四档；大标题字距 −0.02 ~ −0.03em |
| 动效（未在静态稿体现） | 入场 220ms ease-out；按钮按下 0.98；勾选 160ms；不做弹跳 |

## 4. 五个方向

### 4.1 编辑部 · editorial（已选定并落地）

- **一句话**：把菜谱当一本杂志的一页。
- **版式**：4:5 封面（底部 140px 渐隐到纸白，和正文无缝接上）→ 宋体 42pt 大标题 → 点状引导线的食材清单 → 编号步骤 → 与正文同宽、圆角 12 的翻炒图 → 引文式备注。
- **颜色**：纸白 `#FBF8F3`、墨 `#191512`、焦橙 `#B3471E`。
- **入口**：本页不放「加入今天」按钮；唯一入口是菜谱列表卡片**长按** → 菜单「加入今天食谱」。
- **为什么精致**：宋体标题与无衬线正文形成对比；点状引导线是印刷排版的语言；没有一张卡片，全靠留白和细线分区。封面不做硬切，用和「暗房」同一套渐隐逻辑把照片溶进纸色；步骤图收回正文栏宽、与文字左右对齐，页面不再有第二种对齐线。
- **适合**：想让菜谱有「收藏感」、愿意牺牲一点信息密度。
- **风险**：宋体在低端 Android 上可能回退到黑体；食材清单较长时页面会很长；入口只靠长按，新用户第一次可能找不到，需要在列表空态/首次进入时给一次轻提示（另议）。
- **看稿**：[`../prototype/recipe-preview-editorial.html`](../prototype/recipe-preview-editorial.html) · [`../exports/recipe-preview-editorial.png`](../exports/recipe-preview-editorial.png)

### 4.2 暗房 · immersive

- **一句话**：关灯，只留食物和一块玻璃。
- **版式**：568pt 视频/封面主视觉 + 中央毛玻璃播放键 → 大标题 → 三格玻璃数据条 → 橙色主按钮 → 玻璃食材表 → 竖向时间线步骤（带时长胶囊）。
- **颜色**：近黑 `#08090A`、白 90% / 58% / 32% 三档、系统橙 `#FF9F0A`。
- **为什么精致**：暗色让食物颜色更饱和；玻璃层只有一层，信息和画面分离；时间线比卡片更像「流程」。
- **适合**：视频是主角、用户常在晚上看菜谱；也最适合展示抖音原视频。
- **风险**：常暗不跟随系统主题；封面质量差时暗底会放大噪点。
- **看稿**：[`../prototype/recipe-preview-immersive.html`](../prototype/recipe-preview-immersive.html) · [`../exports/recipe-preview-immersive.png`](../exports/recipe-preview-immersive.png)

### 4.3 留白 · minimal

- **一句话**：只留字和留白，像 iOS 的设置页。
- **版式**：16:9 封面 → 34pt 标题 → 三列数据（发丝线上下夹住）→ 黑色胶囊主按钮 → 食材列表（圆形勾选 + 内缩发丝线）→ 编号步骤 → 一张圆角步骤图 → 灰底备注。
- **颜色**：纯白、`#0A0A0A`、灰阶 `#8A8A8E / #AEAEB2 / #E5E5EA`、唯一强调橙 `#FF5A1F`。
- **为什么精致**：没有阴影、没有卡片；所有分隔都是 0.5px 发丝线；数字用 tabular-nums 对齐；主按钮只有一个。
- **适合**：每天做饭时快速扫读；实现成本最低，和现有 Compose 组件最接近。
- **风险**：视觉记忆点弱；封面图一般时整页会显得素。
- **看稿**：[`../prototype/recipe-preview-minimal.html`](../prototype/recipe-preview-minimal.html) · [`../exports/recipe-preview-minimal.png`](../exports/recipe-preview-minimal.png)

### 4.4 暖厨 · warm

- **一句话**：奶油纸、陶土色、圆角卡片，像一本手写菜谱。
- **版式**：圆角 28 封面 → 上浮的标题卡 → 三枚信息胶囊 → 陶土色主按钮 → 双列食材卡（右侧勾选）→ 步骤卡（编号 + 时长 + 可选方图）。
- **颜色**：奶油 `#F7EFE2`、纸白 `#FFFDF9`、陶土 `#C25B33`、橄榄 `#6F7454`。
- **为什么精致**：暖色纸张底 + 极轻阴影，卡片有层次但不脏；双列食材卡把「8 项食材」变成可扫的网格。
- **适合**：家庭用户、想要「有温度」而不是「很科技」。
- **风险**：卡片多，页面最长；深色模式下需要重新配一套暖色，不能简单反色。
- **看稿**：[`../prototype/recipe-preview-warm.html`](../prototype/recipe-preview-warm.html) · [`../exports/recipe-preview-warm.png`](../exports/recipe-preview-warm.png)

### 4.5 影像 · gallery

- **一句话**：用照片讲故事，像系统相册的「回忆」。
- **版式**：三格拼贴开场（大图 + 两张小图）→ 上浮白色内容页 → 黑色主按钮 → 食材胶囊流 → **横向滑动**步骤胶片 → 备注 → 底部毛玻璃操作条。
- **颜色**：系统灰 `#F2F2F7`、白、黑、橙 `#FF5A1F`。
- **为什么精致**：图片先于文字；拼贴和胶片是 iOS 原生的浏览语言；底部浮层只保留一个主操作。
- **适合**：内容多、封面好看、想让「食欲」先说话；也适合分享页。
- **风险**：横向步骤与现有「单列连续滚动」的阅读习惯冲突；静态截图只能看到 1.5 张步骤卡。
- **看稿**：[`../prototype/recipe-preview-gallery.html`](../prototype/recipe-preview-gallery.html) · [`../exports/recipe-preview-gallery.png`](../exports/recipe-preview-gallery.png)

## 5. 对比

| 方向 | 视觉冲击 | 信息效率 | 实现成本 | 深色模式 | 最适合 |
|---|---|---|---|---|---|
| 01 编辑部 | 高 | 中 | 中（需宋体回退） | 需另配纸黑 | 收藏型用户、分享长图 |
| 02 暗房 | 高 | 中高 | 中高（玻璃 + 时间线） | 常暗 | 视频优先、夜间使用 |
| 03 留白 | 低 | 最高 | **最低** | 可跟随系统 | 每天做饭、快速扫读 |
| 04 暖厨 | 中高 | 中 | 中（卡片体系） | 需另配暖暗色 | 家庭厨房、老用户 |
| 05 影像 | 最高 | 中低 | 高（横滑 + 拼贴） | 可跟随系统 | 内容运营、分享页 |

**最终选择：01 编辑部**（2026-11-04 落地到 `RecipeDetailScreen.kt`）。当时的对比结论保留如下：

- 03 留白的优势是最像 iOS、改造风险最低；02 暗房适合视频优先；04 暖厨是折中项；01/05 更偏分享页。
- 选 01 的代价是页面更长、宋体在低端机可能回退；收益是封面渐隐、点状食材和衬线标题形成的「杂志收藏感」最明确。

## 6. 选中后的落地

1. **Token 先行**：把所选方向的颜色、字阶、圆角、间距写进 `android/app/src/main/java/com/weiji/wang/ui/theme/`，不在页面里写魔法数。
   - 01 落地时已确认交互：详情页删除「今晚做这道 / 今天做这道菜」主按钮；加入今天菜单的唯一入口改为菜谱列表卡片长按 →「加入今天食谱」，首页与空态提示同步。
2. **按区块替换**：封面 → 标题/来源/数据 → 原视频（有则显示）→ 食材 → 步骤 → 原文 → 备注 → 页脚；每步保持可编译、可回滚。
3. **组件映射**：`RecipeCover`（新增 `fallback` 占位）、`VideoPlayerCard`、`EditorialSectionHeader`、食材点状行、无卡片步骤、`EditorialFooter`。
4. **状态验收**：390 / 411 宽、浅色/深色、超长标题、无封面、无视频、8 项以上食材、10 步以上、备注为空。
5. **性能**：封面与步骤图继续走本地文件 + Coil；毛玻璃只用于压图控件，避免整页 blur。
6. **文档同步**：落地后更新 `design/product/implemented-ui-spec.md` §4.3 与 `design/product/screen-specs.md` 的详情页小节。

## 7. 文件与素材

| 内容 | 位置 |
|---|---|
| 五版可交互原型 | `design/prototype/recipe-preview-{editorial,immersive,minimal,warm,gallery}.html` |
| 五版整页导出（780px 宽） | `design/exports/recipe-preview-{editorial,immersive,minimal,warm,gallery}.png` |
| 五版对比长图 | `design/exports/recipe-preview-directions.png` |

设计稿里的食物照片来自 TheMealDB、Wikimedia Commons 与 Unsplash，仅用于方向评审；产品内仍使用用户导入的封面和步骤图，不随设计稿分发。

## 8. 待确认

1. 步骤是否接受横向滑动（05），还是必须保持单列连续滚动？
2. 深色模式跟随系统，还是像 02 一样常暗？
