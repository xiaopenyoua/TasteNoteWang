---
doc: docs/decisions/0008-dir-contract
title: 目录契约：三内容目录 + 可执行自检
status: accepted
date: 2026-11-02
owner: wang
supersedes:
  - docs/decisions/0007-repo-layout
superseded-by: []
---

# 0008. 目录契约：三内容目录 + 可执行自检

> 2026-11-02 修订：§6 的 `dist/` 暂存区已由 [0009](0009-apk-location.md) 取消；APK 留在 `android/app/build/outputs/apk/`。其余决定不变。

## 背景

ADR 0007 把仓库分成「设计区 / 开发区 / 交付区 / 本地私有区」四块，但落地后仍有三类问题：

1. **归属判据不止一处。** `design/README.md`、`docs/00-conventions/01-file-organization.md`、根 `README.md` 各说一套，冲突时不知道听谁的。
2. **实现文档留在了设计区。** 架构、数据模型、存储、导入管线、测试方案都在 `design/` 下，但它们是「我们这个工程怎么实现」，换语言就作废，与产品规格的生命周期不同。
3. **规则不可执行。** 「不要乱放」是一句愿望，没有人能在改完后 10 秒内判断仓库是否干净；也没有一条命令能阻止下一个人再把抓包堆到根目录。

参考项目 LiveHub 的做法：**一份 `AGENTS.md` 作为唯一归属判据 + 每条规则给出可跑命令 + 一个自检脚本**。这套做法的价值不在目录长什么样，而在于「归属争议有唯一裁判，违规能被机器发现」。

## 选项

| 选项 | 做法 | 代价 |
|---|---|---|
| A. 保持 ADR 0007 的四分区 | 只补文档，不动目录 | 归属仍多源；实现文档继续留在设计区 |
| B. 对齐三内容目录 + 单一契约 + 自检脚本 | `design/` `android/` `docs/` 三分；Gradle 根进 `android/`；实现文档进 `android/docs/`；`AGENTS.md` 唯一裁判；`verify-layout.sh` 可执行 | 一次性路径迁移；构建命令从 `gradle :app:...` 变为 `cd android && gradle :app:...` |
| C. 只加自检脚本，不改目录 | 改动最小 | 脚本会持续报旧布局的违规，等于给违规发合格证 |

## 决定

采用 **B**，并把边界写死：

1. **三个内容目录**：
   - `design/` — 设计侧：`product/`（PRD、信息架构、页面规格、视觉语言）、`wireframes/`、`exports/`、`source/`（gitignore）。
   - `android/` — 开发侧：Gradle 根、`app/`、`docs/`（实现文档）、`tools/`（自检脚本）。
   - `docs/` — 判断依据：`research/`（带日期调研快照）、`decisions/`（ADR）、`conventions/`（写作规范）。
2. **两个显式让步**（不是第四、第五个内容目录）：
   - `dist/` — 用户要求试用 APK 留在项目目录内；只放 APK + 安装说明，APK 被 gitignore。
   - `local/` — 平台抓包与调试 JSON；整体 gitignore，文档不得依赖。
3. **`AGENTS.md` 是唯一归属判据。** 其余 README 只讲自己目录内部怎么摆；冲突以 `AGENTS.md` 为准。
4. **删除 `docs/conventions/01-file-organization.md`**，其目录规则全部并入 `AGENTS.md`；写作规范与 ADR 指南保留在 `docs/conventions/`。
5. **编号规则收紧**：`NN-` 只允许出现在 `docs/decisions/`（ADR 永久编号）与 `docs/research/`（日期快照）；其余文档一律语义名。
6. **不建 `archive/`**。被取代的 ADR 原地保留、`status: superseded`；历史在 git。
7. **新增 `android/tools/verify-layout.sh`**：七项机器可判定的检查，退出码 0 才算干净。

## 后果

- 正面：归属争议有唯一裁判；实现文档与产品规格的生命周期分开；规则可执行，改完 10 秒内能知道是否干净；根目录只剩 5 个语义明确的目录。
- 负面：一次性迁移成本（文档路径、frontmatter ID、Gradle 根）；聊天记录与旧文档里的 `docs/...`、`app/...`、`design/30-tech/...` 路径失效；Android Studio 需要打开 `android/` 而不是仓库根。
- 触发重审：出现第二份可独立构建的产物（服务端、脚本包），需要为它单开内容目录；或 `docs/` 文档超过约 100 份、三分法不再够用。
