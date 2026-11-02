---
doc: android/docs/README
title: 实现文档索引
status: accepted
date: 2026-11-02
owner: wang
related:
  - docs/research/2026-10-30-tech-research
  - design/product/prd
---

# android/docs/ — 实现文档

这里只回答「**我们这个工程怎么实现、怎么验证**」。产品规格在 [`design/product/`](../../design/product/)；
外部事实与取舍在 [`docs/`](../../docs/)。

## 文档

| 文档 | 回答什么问题 |
|---|---|
| [`architecture.md`](architecture.md) | 用什么技术、代码怎么分层、数据怎么流、失败怎么降级 |
| [`data-model.md`](data-model.md) | 有哪些表、字段、关系、ID 与不变量 |
| [`data-storage.md`](data-storage.md) | 设备上文件怎么放、怎么原子写、怎么导出/恢复（九条铁律） |
| [`import-pipeline.md`](import-pipeline.md) | 三平台怎么解析、怎么下载、失败怎么降级 |
| [`performance.md`](performance.md) | 性能预算、已做的优化、怎么测 |
| [`test-plan.md`](test-plan.md) | 测试矩阵、用例、缺陷记录、验收标准 |
| [`test-data.md`](test-data.md) | 真实测试链接与预期结果 |

## 规矩

- **语义名，不编号。** 编号只属于 ADR 与日期快照（`AGENTS.md` R4）。
- **活文档。** 这里没有冻结记录；路径必须今天就能用，断链要真修。
- **只写实现与验证。** 需求写 `design/product/`，调研与 ADR 写 `docs/`。
- **测试数据写进 `.md`。** 不把抓包 JSON、截图批、测试账号文件放进本目录。

## 相关依据

- 技术方案调研：[`docs/research/2026-10-30-tech-research.md`](../../docs/research/2026-10-30-tech-research.md)
- 存储布局决策：[`docs/decisions/0005-storage-layout.md`](../../docs/decisions/0005-storage-layout.md)
- 抖音降级决策：[`docs/decisions/0006-douyin-fallback.md`](../../docs/decisions/0006-douyin-fallback.md)
