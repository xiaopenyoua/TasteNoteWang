---
doc: android/docs/data-storage
title: 数据与文件存储规范
status: accepted
date: 2026-10-30
owner: wang
related:
  - android/docs/data-model
  - docs/decisions/0005-storage-layout
---

# 数据与文件存储规范

> 这份文档是设备端存储的"宪法"。任何改动先改这里，再写 ADR，再动代码。
> 目标：任何时候用户都能把数据带走；崩溃最多丢一个临时文件，不丢菜谱。

## 1. 九条铁律

1. **DB 是运行时唯一事实源。** `recipe.json` 是派生快照，丢了可由 DB 重建；反向不成立。
2. **一条菜谱一个文件夹。** `recipes/{recipeId}/` 自包含：快照、封面、步骤图、视频。删除菜谱 = 删整个目录。
3. **DB 只存相对路径。** 绝不存绝对路径；根目录变化（外置存储挂载/迁移）不影响数据。
4. **写入必须原子。** 先写 `*.part`，`fsync`，再 `rename`；崩溃最多留下 `.part`。
5. **媒体文件名固定、ASCII、可预测。** `cover.webp`、`step-01.webp`、`video.mp4`；不用中文、不用用户标题命名。
6. **用户可读的元数据永远有副本。** 每条菜谱都有 `recipe.json` + `recipe.md`（导出时生成），防止应用停更导致数据变砖。
7. **登录态与用户数据物理隔离。** Cookie 加密存 SharedPreferences，**永不进入导出包、日志、崩溃上报**。
8. **导出以 DB 为准。** 目录里的孤儿文件不打包；恢复按 ID 与 `updatedAt` 去重，不覆盖更新的版本。
9. **不静默删除用户内容。** 自动清理只允许 `.part` 与明确标记的缓存；媒体删除必须由用户操作触发。

## 2. 目录树

根目录：`getExternalFilesDir(null)/weiji`，不可用时回退 `filesDir/weiji`。

```
weiji/
├── recipes/
│   └── {recipeId}/
│       ├── recipe.json          # 快照（派生）
│       ├── cover.webp           # 封面，最长边 1600，q85
│       ├── step-01.webp         # 步骤图
│       ├── step-02.webp
│       ├── photo-01.webp        # 用户拍照
│       ├── video.mp4            # 原视频（音视频已封装）
│       └── audio.m4a            # 预留：分离音频
├── exports/                     # 应用内临时导出（最终由 SAF 写到用户选择位置）
└── cache/                       # 导入临时文件、恢复 staging（可清）
```

- `recipe.json` 与媒体文件同级；导出 zip 时媒体放在 `recipes/{id}/media/`，恢复时摊平回同级。
- 应用缓存目录 `cacheDir/import` 只放下载临时文件，随时可清。
- 未来如引入内容寻址去重，必须保留"一条菜谱一个文件夹"的**导出视图**，不能破坏用户可读性。

## 3. 文件命名

| 类型 | 规则 | 示例 |
|---|---|---|
| 封面 | 固定 | `cover.webp` |
| 步骤图 | `step-NN.webp`，两位序号 | `step-01.webp` |
| 用户照片 | `photo-NN.webp` | `photo-01.webp` |
| 视频 | `video.mp4`（多视频 `video-02.mp4`） | `video.mp4` |
| 快照 | 固定 | `recipe.json` |
| 临时 | 原名 + `.part` | `video.mp4.part` |

- 文件名只用 ASCII 小写 + 数字 + `-`/`.`；长度 < 64。
- 用户可见的导出文件名可含中文，但需过滤 `/\:*?"<>|` 并截断到 60 字节；稳定 ID 放前面。
- 平台作品 ID 仅存 DB 与快照，不用于文件名。

## 4. 写入流程

```
解析/下载
  → 写 recipes/{id}/xxx.part
  → fsync
  → rename 为目标名
  → 计算 sha256 / 尺寸
  → 写 MediaEntity
  → 写 recipe.json（原子）
```

- 图片：先落 `.raw`，按最长边 1600 采样解码，EXIF 旋转，WebP q85，再原子替换。
- 视频：流式写 `.part`，支持 Range 续传；不转码、不合并（B站 720p 已是单文件）。
- 快照：`RecipeRepository.saveRecipe` 在 DB 事务成功后写入；失败只记日志，不影响主流程。

## 5. 容量与清理

- 单条体积 ≈ 码率 × 时长；5 分钟 720p 约 20-40 MB（实测小红书 2.5 分钟 19.7 MB，B站 5.8 分钟 36 MB）。
- 下载前检查剩余空间：需求 + 100 MB 缓冲；不足则跳过并标记 `PARTIAL`。
- 默认仅 Wi-Fi 下载（设置可关）；可在详情逐条删除媒体。
- 自动清理：只清 `.part`（>24h）与 `cache/`；**绝不**自动删 `recipes/` 下的成品文件。
- 孤儿目录（DB 无对应菜谱）不自动删；备份不导出，后续可在设置页提供"清理孤儿文件"（P1）。

## 6. 导出包格式

```
weiji-backup-YYYYMMDD-HHmm.zip
├── manifest.json
└── recipes/
    └── {recipeId}/
        ├── recipe.json
        ├── recipe.md
        └── media/
            ├── cover.webp
            ├── step-01.webp
            └── video.mp4
```

`manifest.json`：

```json
{
  "app": "weiji",
  "version": 1,
  "exportedAt": 1793357812286,
  "recipeCount": 12,
  "includesMedia": true,
  "recipes": [
    { "id": "01...", "title": "孜然土豆牛肉", "updatedAt": 1793357812286, "fileCount": 3 }
  ]
}
```

- 导出可选 `includeMedia=false`（只导文字与结构）。
- 不含 Cookie、不含草稿、不含缓存。
- 恢复：解压到 staging → 校验路径（防 zip-slip）→ 按 ID 查 DB → `updatedAt` 较新则跳过 → 复制媒体 → 重建实体 → 保存。
- 恢复失败不回滚整个包；逐条统计 imported/skipped/failed。

## 7. 备份策略

| 手段 | 内容 | 频率 |
|---|---|---|
| 系统 Auto Backup | DB + `recipes/` 下的快照/图片（排除视频、Cookie、cache） | 系统决定 |
| 手动 zip | 全量，含视频 | 用户触发，建议每月/换机前 |
| Markdown | 人类可读，随 zip | 每次导出 |

> 视频体积大，Auto Backup 的 25MB 上限放不下；**手动 zip 是唯一可靠的全量备份**。

## 8. 迁移与兼容

- DB 迁移：Room Migration，逐版本升级；禁止破坏性迁移。
- 文件布局：`recipe.json.version` 标记；恢复时兼容旧版本字段。
- 换机：安装新 APK → 设置 → 从备份恢复；恢复后媒体路径保持相对，无需重关联。
- 卸载：系统会删除应用专属外部目录；**卸载前必须导出**。这是本方案唯一不可逆的操作，设置页需明确提示。

## 9. 检查清单（改存储前逐条确认）

- [ ] 新文件是否有固定、ASCII、可预测的名字？
- [ ] 是否先写 `.part` 再 rename？
- [ ] DB 里是否只存相对路径？
- [ ] 删除菜谱是否会清理整个目录？
- [ ] 导出是否以 DB 为准、是否排除 Cookie？
- [ ] 恢复是否会覆盖更新版本？
- [ ] 是否会自动删除用户内容？（必须为否）
