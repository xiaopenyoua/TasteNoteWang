---
doc: android/docs/release
title: 发布与在线更新
status: accepted
date: 2026-11-05
owner: wang
related:
  - android/docs/architecture
  - design/product/implemented-ui-spec
---

# 发布与在线更新

## 1. 版本轴

| 位置 | 值 | 规则 |
|---|---|---|
| `android/app/build.gradle.kts` `versionName` | `1.0.0` | 展示给用户、与 GitHub tag 对应（tag 加 `v`） |
| `android/app/build.gradle.kts` `versionCode` | `1` | 每次发布必须递增；Android 用它判断能否覆盖安装 |
| `BuildConfig.VERSION_NAME` | 同上 | 应用内「我的 → 关于 → 版本」与更新比较的唯一来源 |

## 2. 签名

发布签名只在**本机**，不进仓库：

```
local/keystore/weiji-release.jks          # RSA 4096，有效期 10000 天
local/keystore/release.properties         # storeFile / storePassword / keyAlias / keyPassword
```

`android/app/build.gradle.kts` 读取 `local/keystore/release.properties`。文件不存在时 release
构建会在签名阶段失败，避免把 debug 签名的 APK 发出去——签名不一致会导致后续在线更新无法覆盖安装。

> 务必备份 `weiji-release.jks` 和密码；丢失后已安装用户只能卸载重装。

## 3. 构建

```bash
cd android
~/.gradle/wrapper/dists/gradle-8.9-bin/*/gradle-8.9/bin/gradle :app:assembleRelease
```

产物：`android/app/build/outputs/apk/release/app-release.apk`。

校验：

```bash
$ANDROID_HOME/build-tools/<版本>/apksigner verify --print-certs \
  android/app/build/outputs/apk/release/app-release.apk
$ANDROID_HOME/build-tools/<版本>/aapt2 dump badging \
  android/app/build/outputs/apk/release/app-release.apk | head -1
```

## 4. 发布到 GitHub Release

1. 提交代码，打 tag 并推送：
   ```bash
   git tag v1.0.0
   git push origin main --tags
   ```
2. 在 `xiaopenyoua/TasteNoteWang` 创建 release（仓库为 public，应用内更新不需要 token）：
   - tag：`v1.0.0`
   - 标题：`味记 v1.0.0`
   - 附件：`app-release.apk`（可重命名为 `weiji-1.0.0.apk`）
   - 说明：写用户可见的变更；应用内会原样展示前 600 字。
3. 确认 `https://api.github.com/repos/xiaopenyoua/TasteNoteWang/releases/latest` 返回该版本；
   草稿和 prerelease 不会出现在 `latest`。

## 5. 应用内更新

代码：

- `core/update/UpdateChecker.kt`：`GET releases/latest`，取第一个 `.apk` 资产；有 `digest`
  时校验 SHA-256，没有时校验大小；下载到 `cacheDir/update/`。
- `core/update/ApkInstaller.kt`：`FileProvider` + `ACTION_VIEW` 交给系统安装器；
  未授予「安装未知应用」时跳到系统设置页。
- `feature/settings/SettingsScreen.kt`：关于分组里的「检查更新」。
- `AndroidManifest.xml`：`REQUEST_INSTALL_PACKAGES`。
- `res/xml/file_paths.xml`：`<cache-path name="cache" path="." />` 已覆盖下载目录。

版本比较规则：忽略前导 `v`，按 major/minor/patch 数字比较；数字相同时正式版高于同号预发布版。

## 6. 验收

- 单测：`UpdateCheckerTest`（解析 release、版本比较、下载校验）。
- 模拟器：安装 release APK → 我的 → 关于 → 检查更新，应显示「已是最新」；
  若发布更高的测试版本，应能下载并拉起系统安装器。
