---
doc: android/docs/performance
title: 性能预算与优化记录
status: draft
date: 2026-10-30
owner: wang
related:
  - android/docs/architecture
  - android/docs/test-plan
---

# 性能预算与优化记录

> 性能是这个项目的硬约束：本地优先、列表可滚、导入不卡 UI、做饭时打开要快。

## 1. 预算

| 指标 | 目标 | 说明 |
|---|---|---|
| 冷启动到首页可交互 | ≤ 1.2s | 中端机（骁龙 7 系 / 天玑 8000） |
| 温启动 | ≤ 350ms | 从最近任务返回 |
| 列表滚动 | 稳定 60fps，掉帧 <1% | 1000 条菜谱 |
| 搜索响应 | ≤ 300ms | 输入后首帧结果 |
| 详情首屏 | ≤ 200ms（不含图片解码） | 先 DB 文本，后图片 |
| 图片解码 | ≤ 80ms/张 | 480×270 封面目标 |
| 导入 UI 卡顿 | 主线程阻塞 <16ms | 下载/解析全在 IO |
| APK（release） | ≤ 12MB | 当前 release 2.8MB（unsigned），debug 15MB |
| 常驻内存 | ≤ 180MB | 首页 + 一张详情 |
| 视频播放起播 | ≤ 500ms | 本地文件 |

## 2. 已实施的优化

### 启动

- 无 Hilt/Koin：`AppContainer` 手写，零反射、零注解处理运行时。
- 无广告/统计/崩溃 SDK；`Application.onCreate` 只建 DB、FileStore、Repository。
- 首页只订阅两个 Flow（建议/最近做过），不预加载全部菜谱。
- Room 查询走索引；`RecipeCard` 是窄投影，不加载 ingredients/steps。
- 图片懒加载：Compose `AsyncImage` + `matchParentSize`，先显示占位碗图标，避免黑块与布局跳动。

### 列表

- `LazyColumn`/`LazyVerticalGrid` 稳定 `key`，避免重组错位。
- 菜谱网格按断点固定 2/3 列，不做每帧 Adaptive 计算。
- 卡片高度固定（封面 1:1 / 16:9），图片 `ContentScale.Crop`，无二次测量。
- 搜索输入直接映射 `Flow`，数据量 <2000 时 SQL `LIKE` 足够；未过早引入 FTS。

### 导入

- WebView 只在抖音需要时创建，1dp 隐藏；15s 超时销毁。
- 下载单并发、64KB buffer、Range 续传；不转码。
- 图片采样解码（`inSampleSize`）+ WebP q85，长边 1600；封面目标 480×270。
- 解析/下载在 `Dispatchers.IO`，UI 只收进度。
- 导入前检查空间，避免写满磁盘导致系统卡顿。

### 渲染与包体

- 单 Activity + Compose，无 Fragment/Navigation XML。
- 不使用 Material Icons Extended（仅核心图标 + 1 个自绘碗）。
- 不内置字体；中文用系统 Noto Sans CJK。
- Release 开启 R8 + `shrinkResources`（见 `android/app/build.gradle.kts`）。

## 3. 实测记录

| 场景 | 设备 | 结果 | 备注 |
|---|---|---|---|
| 冷启动 `am start -W` | Pixel 7 模拟器（API 36，软件 GPU） | 3.1s / 3.8s | 模拟器远慢于真机，仅作回归基线 |
| 温启动 | 同上 | ~0.3-0.6s | 系统缓存 |
| 首页图片首次显示 | 同上 | 2-5s | Coil 冷启动解码；已有占位，不黑屏 |
| B站 5.8 分钟 720p 下载 | 同上 | 36MB，约 10-20s | 网络决定 |
| 小红书 2.5 分钟视频 | 同上 | 19.7MB，约 10s | 同上 |
| 60 单元测试 | host | 7-10s | 含 Robolectric |
| 12 真机测试 | 模拟器 | 19-40s | 含 UI 流程、备份、媒体导入、搜索/编辑/删除 |

> 模拟器使用 `swiftshader_indirect` 软件渲染，启动与图片解码数据不代表真机；真机验收需用 `Macrobenchmark` 复测。

## 4. 后续优化（按收益排序）

1. **Baseline Profile**：为首页/菜谱/详情生成 AOT 配置，目标冷启动 -20-30%。
2. **Coil 预热/采样**：封面按显示尺寸请求（`size(480, 270)`），减少大图解码。
3. **导入 WorkManager 化**：大视频下载放前台 Service/WorkManager，避免 ViewModel 被杀。
4. **分页**：菜谱超过 2000 条时改用 Paging 3；当前一次性加载可接受。
5. **图片内容寻址/缩略图**：为列表单独生成 320px 缩略图，详情用原图。
6. **FTS4**：搜索数据量上来后替换 LIKE，支持中文 trigram。
7. **视频封面预抽**：导入时同步生成 480×270 WebP，避免列表解码大图。

## 5. 性能测试方法

```bash
# 冷启动
adb shell am force-stop com.weiji.wang.debug
adb shell am start -W -n com.weiji.wang.debug/com.weiji.wang.MainActivity

# 帧统计
adb shell dumpsys gfxinfo com.weiji.wang.debug reset
# 滚动列表后
adb shell dumpsys gfxinfo com.weiji.wang.debug

# 内存
adb shell dumpsys meminfo com.weiji.wang.debug
```

真机验收建议：

- Macrobenchmark `StartupTimingMetric` + `FrameTimingMetric`。
- Perfetto 抓 10s 导入过程，确认主线程无 >16ms 阻塞。
- Layout Inspector 检查重组次数；重点看详情页与烹饪模式。

## 6. 性能红线

- 绝不在主线程做网络、文件 IO、Bitmap 解码。
- 绝不在 `LazyColumn` item 内创建 ExoPlayer/WebView。
- 绝不为每次重组创建新的 OkHttpClient/DB。
- 绝不把大图原图直接塞进列表。
- 任何新增依赖先看包体与启动成本。
