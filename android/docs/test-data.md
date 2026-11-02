---
doc: android/docs/test-data
title: 测试数据与真实链接库
status: accepted
date: 2026-10-30
owner: wang
related:
  - android/docs/test-plan
  - android/docs/import-pipeline
---

# 测试数据与真实链接库

> 采集日期 2026-10-30。平台内容可能被作者删除、链接会过期；每次回归前先确认链接可打开。
> 这些链接仅用于个人开发回归，不进入仓库媒体、不批量抓取。

## 1. 固定样本

| # | 平台 | 类型 | 分享文案（原文） | 链接 | 预期 |
|---|---|---|---|---|---|
| 1 | 抖音 | 视频 | 王强:3.07 复制打开抖音，看看【西关焖烧男的作品】比烧烤还好吃！直接香迷糊的孜然土豆牛肉 食材：牛里… | `https://v.douyin.com/auY2beN-dPo/` | 标题“比烧烤还好吃！直接香迷糊的孜然土豆牛肉”；作者“西关焖烧男”；JSON-LD 食材 6 项；封面；视频需登录 → `PARTIAL` |
| 2 | 抖音 | 图文 | 王强:2.05 复制打开抖音，看看【大地雷💣的图文作品】想吃谁做的，就@他吧！手到擒来👌。今日菜单… | `https://v.douyin.com/tJRlSFqmvnU/` | 标题“想吃谁做的，就@他吧！”；作者“大地雷💣”；4 张原图；正文；`READY` |
| 3 | 小红书 | 视频 | 王强:天冷吃一口🥘港式牛腩煲 \| 暖乎乎巨好吃😋 天气转凉，… | `https://xhslink.cn/o/6QDLJhk8FvQ` | 标题含“港式牛腩煲”；作者 Keling；视频 19.6MB（2:29）；封面；`READY` |
| 4 | 小红书 | 图文 | 王强:有幸在亲戚家吃过一回，被惊艳到了！！ 没想到牛肉可… | `https://xhslink.cn/o/5oHvxjNshoq` | 标题“有幸在亲戚家吃过一回，被惊艳到了！！”；作者“杰瑞的美食日记”；5 张原图；酱汁食材（生抽/蚝油/淀粉/水）；`READY` |
| 5 | B站 | 视频 | 王强:【记住这些万能炒菜公式，做菜怎么都不会难吃！-哔哩哔哩】 | `https://b23.tv/8xMyJNn` | 标题“记住这些万能炒菜公式，做菜怎么都不会难吃！”；UP 主“万能工具人阿伟”；720p 约 36MB；封面；时长约 5 分钟；`READY` |

## 2. 投放方式

```bash
# 方式 A：直接带 share_text 冷启动（最快）
adb shell am force-stop com.weiji.wang.debug
adb shell am start -n com.weiji.wang.debug/com.weiji.wang.MainActivity \
  --es share_text 'https://b23.tv/8xMyJNn'

# 方式 B：模拟系统分享（会弹出分享面板，选择“味记・王”）
adb shell am start -a android.intent.action.SEND -t text/plain \
  --es android.intent.extra.TEXT 'https://v.douyin.com/auY2beN-dPo/' \
  -n com.weiji.wang.debug/com.weiji.wang.capture.ShareReceiverActivity
```

> 含空格/中文的完整分享文案用 `am start` 容易被 shell 拆参；回归链接时建议只传 URL，文案解析由单元测试覆盖。

## 3. 预期结果明细

### #1 抖音视频（部分成功）

- 状态：`部分完成`，提示“视频需要登录抖音后重试”。
- 已保存：标题、作者、封面（约 48KB WebP）、食材：
  - 牛里脊或牛排肉 400 克
  - 土豆 300 克
  - 大蒜 4 瓣（“约4瓣”）
  - 小葱 15 克
  - 白芝麻 5 克
  - 孜然粉 5 克
- 步骤：0（视频教程，文字未结构化）。
- 不应出现：199KB 的 `uuu_265.mp4`、作者其他作品缩略图、头像。

### #2 抖音图文

- 状态：`已完成`。
- 媒体：4 张（cover + 3 step），无水印。
- 正文：完整 articleBody；步骤 3 段（来自正文）；食材 0（菜谱在图里）。
- 不应出现：`cropcenter:300:400` 缩略图、avatar。

### #3 小红书视频

- 状态：`已完成`。
- 媒体：`video.mp4` 19,653,461 字节，时长 148s；`cover.webp`。
- 食材/步骤：0（正文为推广文案，教程在视频里）。
- 详情「步骤」页显示「原视频」播放器，可播放。

### #4 小红书图文

- 状态：`已完成`。
- 媒体：5 张（cover + 4 step），原图无水印。
- 食材：生抽 1 勺、蚝油 1 勺、淀粉 1 勺、水（适量）。
- 步骤：0；详情「步骤」页以图片步骤展示（“按图示操作”）。

### #5 B站视频

- 状态：`已完成`。
- 媒体：`video.mp4` 约 36MB（720p，durl 单文件）；`cover.webp`。
- 标题/UP 主/时长（约 5 分钟）/原链接正确。
- 食材/步骤：0（简介无结构化食材）。

## 4. 断言清单（手工回归）

- [ ] 每条记录都显示平台、作者、原链接。
- [ ] 媒体文件在 `Android/data/com.weiji.wang.debug/files/weiji/recipes/{id}/`。
- [ ] `recipe.json` 的 `media[].watermarkFree` 与 `originUrl` 合理。
- [ ] 失败/部分成功不会产生空菜谱或错误封面。
- [ ] 同一链接重复导入不会产生第二条菜谱。
- [ ] 删除菜谱后目录消失；备份不再包含它。

## 5. 新增样本的规矩

1. 只加**单条、公开、用户主动分享**的链接。
2. 记录采集日期、平台、类型、预期字段；不保存媒体到仓库。
3. 平台改版后，先更新本文件的预期，再改适配器。
4. 不加入批量列表、主页链接、付费/私密内容。

## 6. 失效处理

- 短链 302 到首页/404：样本失效，换新样本。
- 小红书 300031/300015：需要登录，换样本或登录后重试。
- 抖音 detail 200/0：预期内，验证 JSON-LD 兜底仍能生成 `PARTIAL`。
- B站 403：检查 UA 是否被改回移动端。
