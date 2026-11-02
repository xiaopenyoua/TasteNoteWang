---
doc: docs/README
title: 判断依据索引
status: accepted
date: 2026-11-02
owner: wang
related:
  - AGENTS
  - docs/decisions/0008-dir-contract
---

# docs/ — 判断依据

这个仓库有三个内容目录，分工按「它回答什么问题」切：

| 目录 | 回答 | 换掉实现语言后还成立吗 |
|---|---|---|
| [`design/`](../design/) | 产品与界面**长什么样** | 成立 |
| [`android/`](../android/) | 我们**怎么实现**的 | 不成立（换语言就句句作废） |
| `docs/` | **为什么做**、参考的应用**是怎么做的**、当初**怎么选的** | 成立（说的是外部世界与取舍，不是我们的代码） |

`docs/` 是「事实在中间」的位置：既不描述我们的界面，也不描述我们的代码，但设计决策和工程决策都要回头指它。

**判据（三问，依次问，先命中先定）：**

1. 能被 `assembleDebug` 直接吃掉吗（`res/`、`src/`、构建配置）？ → `android/`
2. 是产品规格、页面规格、视觉语言、线框或视觉稿吗？ → `design/`
3. 都不是，但**下一个做决定的人需要它**吗？ → `docs/`

三个都不命中，就按 `AGENTS.md` R6 删除，不要另建第四个目录。

## 三个段

| 目录 | 只进 | 回答 |
|---|---|---|
| [`research/`](research/) | 带日期的调研快照与证据附录 | 外部世界现在是什么样、参考产品怎么做 |
| [`decisions/`](decisions/) | `NNNN-<slug>.md` 的 ADR | 当初为什么这么选、放弃了什么 |
| [`conventions/`](conventions/) | 文档写作规范、ADR 指南 | 这个仓库的文档怎么写 |

## 规矩

1. **语义名。** 不编号（编号只属于 ADR 与日期快照），不写版本号、状态词。
2. **只写「为什么」和「参考的是什么」。** 实现细节 → `android/docs/`；界面规格 → `design/product/`；同一个结论不要在两地各写一份。
3. **这里不是记录位 —— 断链必须真修。** 依据里的路径指错了，下一个人就找不到依据本身。
4. **不存二进制。** 不存 APK、不存抓包、不存从 APK 里抽出来的库文件；记字节数与 SHA-256 就够。
5. **一份只回答一个问题。** 新增文件前先答：删掉它损失的是哪条判断依据？答不出就不要建。

## 阅读顺序

1. [`research/2026-10-30-market-research.md`](research/2026-10-30-market-research.md) —— 为什么要做这个产品。
2. [`research/2026-10-30-tech-research.md`](research/2026-10-30-tech-research.md) —— 三平台能力与技术选型的实测依据。
3. [`decisions/`](decisions/) —— 关键取舍的来龙去脉。
4. [`conventions/doc-style.md`](conventions/doc-style.md) —— 要改文档时先看。
