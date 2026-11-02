---
doc: docs/conventions/adr-guide
title: 决策记录（ADR）规范
status: accepted
date: 2026-10-30
owner: wang
related:
  - AGENTS
---

# 决策记录（ADR）规范

## 1. 什么时候必须写 ADR

满足任意一条：

- 选型：技术栈、数据库、同步方案、第三方服务
- 数据结构或文件存储布局的**不可逆**变更
- 产品范围：砍掉/新增核心功能
- 多方案权衡后做出的取舍，且未来可能有人问"当初为什么"
- 修改本仓库的规范

不需要 ADR：改文案、修 bug、调样式、加日志。

## 2. 编号与状态

- 文件：`docs/decisions/NNNN-<kebab-slug>.md`，从 `0001` 顺序递增，**永不复用编号**。
- 状态：`proposed` → `accepted` → `superseded` / `rejected`
- `accepted` 之后**正文不可编辑**。要改，写新 ADR 并在两者 frontmatter 互相链接。
- `superseded` 的 ADR **原地保留**，不移动、不归档；编号是永久地址。

## 3. 模板

```markdown
---
doc: docs/decisions/0001-example
title: 使用 Kotlin + Jetpack Compose
status: accepted            # proposed | accepted | rejected | superseded
date: 2026-10-30
owner: wang
supersedes: []
superseded-by: []
---

# 0001. 使用 Kotlin + Jetpack Compose

## 背景
（为什么现在要决定这件事，约束是什么）

## 选项
| 选项 | 优点 | 缺点 |
|---|---|---|
| A | | |
| B | | |

## 决定
我们选择 **A**，因为……

## 后果
- 正面：
- 负面（必须承认的代价）：
- 后续需要观察：

## 触发重审的条件
（什么情况发生时应重新讨论这个决定）
```

## 4. 现有决策索引

| 编号 | 决策 | 状态 |
|---|---|---|
| 0001 | 收录策略：链接建卡 + 用户导入官方保存的媒体，不下载、不去水印 | superseded by 0003 |
| 0002 | 分发方式：仅侧载自用 | accepted |
| 0003 | 收录策略修订：端侧解析三平台链接、下载视频/图片、优先无水印源 | accepted |
| 0004 | 技术栈与工程结构：Kotlin + Compose + Room，单模块，手写 DI | accepted |
| 0005 | 存储布局：DB 为事实源 + 一条菜谱一个文件夹 + 派生快照 | accepted |
| 0006 | 抖音收录：WebView 页面内签名 + JSON-LD 匿名兜底，不破解签名 | accepted |
| 0007 | 仓库顶层按设计区 / 开发区 / 交付区 / 本地私有区四分 | superseded by 0008 |
| 0008 | 目录契约：三内容目录 + 单一归属判据 + 可执行自检 | accepted（§6 由 0009 修订） |
| 0009 | 取消 dist 交付暂存区，APK 留在 android/app/build | accepted |
| 0010 | 增加 design/prototype 交互原型目录，以已实现代码为唯一基准 | accepted |
