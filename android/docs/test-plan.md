---
doc: android/docs/test-plan
title: 测试方案与执行记录
status: accepted
date: 2026-10-30
owner: wang
related:
  - android/docs/test-data
  - android/docs/performance
  - design/product/prd
---

# 测试方案与执行记录

> 原则：核心逻辑用 JVM 测试兜底，关键流程在真机跑通，真实链接做端到端回归；性能单独设门槛。
> 结论：**60 个单元测试 + 12 个真机测试全部通过**；5 条真实链接中 5 条可用（抖音视频部分成功）。

## 1. 测试分层

| 层 | 范围 | 工具 | 数量 |
|---|---|---|---|
| 单元 | 链接解析、文本拆解、份量、ULID、JSON/HTML 解析、快照 | JUnit4 | 57 |
| 单元（集成） | 三平台提取器 + MockWebServer | JUnit4 + MockWebServer | 3 |
| Robolectric | FileStore、Room 仓储 | Robolectric 4.12 | 10（含在上） |
| 真机 UI | 导航、详情、烹饪、记录、收录、采购、备份、媒体导入、搜索、收藏、编辑、删除 | Compose Test + AndroidX Test | 12 |
| 手动 | 真实链接、分享面板、登录、深色、平板、播放 | 模拟器 | 见 02 |
| 性能 | 启动、帧、内存 | adb / Macrobenchmark（计划） | 见 05 |

## 2. 用例矩阵

### 2.1 单元 / 集成（自动）

| ID | 模块 | 用例 | 结果 |
|---|---|---|---|
| U-01 | LinkParser | 抖音视频/图文、小红书、B站分享文案；URL-only；ID 提取；未知链接 | ✅ |
| U-02 | TextParser | 酱汁一行多料；抖音食材块；编号步骤；份量；适量；约4瓣；区间；标签清洗 | ✅ |
| U-03 | IngredientScaler | 2→4/2→1/同份量；厨房取整；区间；模糊词 | ✅ |
| U-04 | Ulid | 26 位、合法、时间有序、时间戳反解 | ✅ |
| U-05 | DouyinJsonLdParser | VideoObject（标题/食材/封面/时长）；article（正文/原图/标签）；水印图过滤 | ✅ |
| U-06 | DouyinJsonParser | `play_addr` 优先于 `download_addr`；`aweme_list` 按 ID 匹配；图集 | ✅ |
| U-07 | XhsWebViewParser | 标题/作者/标签；`ci.xiaohongshu.com/{fileId}`；视频流；HTTP→HTTPS | ✅ |
| U-08 | RecipeSnapshot | JSON 往返；Markdown 含来源与食材；无 source；坏 JSON | ✅ |
| U-09 | FileStore | 原子写、sha256、文本快照、删除目录、空间统计 | ✅ |
| U-10 | RecipeRepository | 保存/读取、搜索食材/作者、烹饪记录、收藏、采购去重、删除 | ✅ |
| U-11 | ExtractorMock | B站短链+view+playurl；小红书笔记页；抖音 JSON-LD 降级 | ✅ |

### 2.2 真机 UI（自动）

| ID | 流程 | 断言 | 结果 |
|---|---|---|---|
| I-01 | 首页 → 菜谱 → 采购 → 我的 | 页面标题与导航可达 | ✅ |
| I-02 | 菜谱 → 详情 → 开始做 → 下一步 → 做完了 → 保存 | 第 1/2 步、记录弹层、回到详情 | ✅ |
| I-03 | FAB 收录 | 解析/手动创建按钮存在 | ✅ |
| I-04 | 采购空状态 | “采购清单是空的” | ✅ |
| I-05 | 备份导出 → 清库 → 恢复 | 菜谱/食材/步骤/媒体恢复，计数正确 | ✅ |
| I-06 | 重复恢复 | 旧版本被跳过（skipped=1） | ✅ |
| I-07 | 分享单图导入 | 生成菜谱、封面正确、快照落盘 | ✅ |
| I-08 | 分享多图导入 | 封面 + 照片，媒体文件完整 | ✅ |
| I-09 | 搜索“土豆” | 命中含该食材的菜谱 | ✅ |
| I-10 | 收藏 + 加入采购 | 取消收藏图标出现；采购清单含牛肉/土豆 | ✅ |
| I-11 | 编辑保存 | 返回详情，开始做按钮存在 | ✅ |
| I-12 | 删除菜谱 | 确认后从列表消失 | ✅ |

### 2.3 真实链接（手动端到端）

详见 [`02-test-data.md`](test-data.md)。摘要：

| 平台 | 类型 | 结果 |
|---|---|---|
| 抖音 | 视频 | 标题/作者/封面/6 项食材成功；视频匿名被风控，提示登录 |
| 抖音 | 图文 | 4 张原图 + 正文成功 |
| 小红书 | 视频 | 19.6MB 视频 + 封面 + 标题/作者成功 |
| 小红书 | 图文 | 5 张原图 + 酱汁食材成功 |
| B站 | 视频 | 36MB 720p + 封面 + 标题/作者/时长成功 |

### 2.4 手动补充

| ID | 场景 | 结果 |
|---|---|---|
| M-01 | 系统分享面板选择“味记・王” | ✅ 进入收录页并自动解析 |
| M-02 | 深色模式 | ✅ 首页/详情/烹饪对比度正常 |
| M-03 | 平板 1066dp | ✅ NavigationRail + 2/3 列网格 |
| M-04 | 烹饪模式音量键翻步 | ✅ `CookKeys` 桥接 |
| M-05 | 屏幕常亮 | ✅ `keepScreenOn` |
| M-06 | 视频播放 | ✅ ExoPlayer 播放本地视频，显示时长/控制条 |
| M-07 | 登录页 WebView | ✅ 加载平台登录页，读取 Cookie 保存 |
| M-08 | 备份 UI（SAF） | 逻辑由 I-05 覆盖；系统文件选择器待真机点验 |

## 3. 测试环境

> 手机/平板试用包：`android/app/build/outputs/apk/debug/app-debug.apk`（debug 签名）。

| 项 | 值 |
|---|---|
| 设备 | Pixel 7 AVD（API 36，arm64，1080×2400，420dpi） |
| 平板模拟 | `wm size 1600x2560; wm density 240`（1066dp 宽） |
| JDK | 21.0.11 |
| Gradle | 8.10.2 |
| AGP / Kotlin | 8.5.2 / 2.0.21 |
| 网络 | 宿主机直连；真实链接在 2026-10-30 采集 |

## 4. 执行命令

```bash
cd android

# 单元 + 集成
gradle :app:testDebugUnitTest

# 真机 UI（指定独立模拟器）
ANDROID_SERIAL=emulator-5556 gradle :app:connectedDebugAndroidTest

# 安装
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
```

## 5. 结果与缺陷记录

### 5.1 结果

- 单元/集成：**60/60 通过**。
- 真机 UI：**12/12 通过**（Pixel 7 AVD，1080×2400）。
- 真实链接：5/5 可用；其中抖音视频为部分成功（符合预期降级）。
- 构建：`assembleDebug` 通过；APK 15MB（debug）。

### 5.2 本轮发现并修复的问题

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 1 | B站视频下载 403 | CDN 对移动 UA 拒绝 | API/媒体统一桌面 UA |
| 2 | 抖音下载到 199KB 假视频 | 把页面静态 `.mp4` 当媒体 | 只接受 `douyinvod/byteicdn/aweme/v1/play` |
| 3 | 抖音把作者其他作品缩略图当步骤 | 扫描所有 `<img>` | 只收 `~tplv-dy-aweme-images` 原图 |
| 4 | 抖音 detail 被替换成空响应，页面无数据 | 拦截后返回自己的空 body | 不替换页面响应，仅后台重放 |
| 5 | 抖音分享页拉起 App 被判加载失败 | `snssdk://` 主文档错误 | 拦截非 http 跳转；只对 http(s) 失败降级 |
| 6 | 小红书话题标签被当成步骤 | 无结构时兜底抓段落 | 只认标题/编号；无结构留空 |
| 7 | `适量水搅拌均匀` 食材名过长 | 正则未截断 | lookahead 截断到 1-4 字 |
| 8 | `2-3勺` 只解析出 2 | 数量前置正则不支持区间 | 支持区间取平均 |
| 9 | 删除菜谱后目录仍在 | `recipeDir()` 读取时 mkdirs | 读写分离：`ensureRecipeDir` |
| 10 | 备份包含已删除菜谱 | 导出扫描目录 | 改为以 DB 为准导出 |
| 11 | 手动/编辑创建的菜谱不进备份 | 只有导入流程写快照 | 仓储保存时统一写 `recipe.json` |
| 12 | 深色模式首次封面黑块 | Coil 解码前无占位 | 占位碗图标 + 图片覆盖 |
| 13 | 分享的单图被识别成 PHOTO、封面为空 | `file://` 的 `getType()` 返回 null | 新增 `MimeTypes` 按扩展名推断 |
| 14 | 横屏/矮屏空状态按钮文字被裁掉 | 空状态用 `fillParentMaxHeight(0.62f)`，横向高度不足 | 固定 520dp + 内容最大宽度 520dp 居中 |
| 15 | 外接键盘/焦点导航时自定义按钮没有焦点框 | 只有 ripple，无 focus 视觉 | 新增 `focusRing`，按钮/卡片/FAB 统一描边 |

## 6. 准入 / 准出

**准入**：构建通过、单元测试通过、模拟器可用、测试数据可访问。

**准出**：
- P0 用例 100% 通过；
- 无 S1/S2 缺陷；
- 5 条真实链接中至少 4 条端到端可用；
- 冷启动/滚动无肉眼卡顿；
- 备份恢复可完整还原一条含媒体的菜谱。

## 7. 未覆盖与后续

- 抖音登录后的视频成功率（需真实账号）。
- SAF 系统文件选择器的端到端点验（逻辑已测）。
- 后台计时/通知、进程被杀后的大文件下载恢复。
- OCR/ASR、WebDAV、多设备同步（P1/P2）。
- Macrobenchmark 真机基线（模拟器数据不作为验收）。
