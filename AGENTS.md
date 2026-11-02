# AGENTS.md · 目录契约与执行规则

> **先读这份，再动手。** 本文件是仓库里**唯一的归属判据**：任何其他 README 与本文件冲突时，以本文件为准。
> 每条规则都尽量可判定，并给出能当场跑的命令，以及「违规长什么样 / 正确长什么样」。
> 改完自检：`bash android/tools/verify-layout.sh`（退出码 0 才算干净）。

---

## 0. 三十秒版

**三个内容目录，一个方向。**

```
design/     设计侧 —— 产品怎么呈现：产品规格、页面规格、视觉语言、线框与视觉稿
android/    开发侧 —— 怎么实现：Gradle 工程、源码、资源、实现文档、测试与自检脚本
docs/       判断依据 —— 为什么做、参考了什么、当初怎么选的：调研、ADR、写作规范
```

另外**一个不进版本库**的本地目录（`.gitignore` 已排除）：

```
local/      本地私有 —— 平台抓包、调试 JSON、临时导出
```

构建产物不单设交付目录：APK 留在 `android/app/build/outputs/apk/`，随 `gradle clean` 一起清空。

**单向理解顺序（不是自动管线，是决策顺序）：**

```
docs/ 事实与取舍  ──▶  design/ 产品与界面规格  ──▶  android/ 代码  ──▶  APK（android/app/build/）
        （为什么）            （做成什么样）              （怎么实现）        （构建产物）
```

反向不成立：代码不反过来改规格；设计不读 Android 资源；文档不复制代码。

**判定三问（依次问，先命中先定）：**

1. 它能被 `assembleDebug` 直接吃掉吗（`res/`、`src/`、构建配置）？ → `android/`
2. 它是产品规格、页面规格、视觉语言、线框或视觉稿吗？ → `design/`
3. 都不是，但**下一个做决定的人需要它**吗（外部事实、取舍记录、写作规范）？ → `docs/`

三个都不命中 → 按 R6 删除，或放 `local/`（仅自己用、永不分享）。

**四条最常被违反的禁令：**

1. **`design/` 里不写代码。** 不出现 `.kt`、`.kts`、`build.gradle*`、`res` 形状目录。
2. **`android/` 里不放设计稿。** 不出现 `.html`、`.svg`；实现文档只能进 `android/docs/`。
3. **同一份字节只存一处。** Android 资源只存在于 `android/app/src/main/res/`，不在 `design/` 再存副本。
4. **文件名不写 `v2` `final` `new` `old` `tmp`。** 这些词描述阶段，不描述事物。

---

## 1. 目录契约

格式：**`目录` — 谁的产物；只进什么；永远不进什么；谁有写权限**

| 目录 | 谁的产物 | 只进 | 永远不进 | 写权限 |
|---|---|---|---|---|
| `design/` | 设计侧全部产物 | 产品规格 `.md`、页面规格、视觉语言、线框与视觉稿图片 | `.kt` / `.kts` / `build.gradle*`、`res/` 形状目录、APK、构建产物 | 人 |
| `design/product/` | 产品与界面规格 | PRD、信息架构、页面规格、视觉语言（语义名 `.md`） | 实现细节、代码片段文件、调研正文 | 人 |
| `design/wireframes/` | 低保真线框 | 布局草图（`.png` / `.jpg` / `.pdf`） | 高保真稿、源文件、Android 资源 | 人 |
| `design/exports/` | 视觉稿导出与切图 | `.png` / `.jpg` / `.pdf` / `.svg` 导出物 | `res/` 字节副本、未引用批次图 | 人 |
| `design/prototype/` | 可交互设计稿 | 自包含 `.html` 原型、说明 `.md` | 代码工程、构建配置、图片批次、未实现功能 | 人 |
| `design/source/` | 设计源文件（本地） | Figma / Sketch / PSD 等源文件 | 任何需要入库的内容（整个目录 gitignore） | 人（本机） |
| `android/` | 开发侧全部产物 | Gradle 工程、`app/`、实现文档、开发脚本 | 设计稿、市场调研、APK、`build/`（生成物） | 开发侧 |
| `android/app/` | 可执行模块 | `src/`、`res/`、`schemas/`、`build.gradle.kts` | 任何文档、设计稿、抓包 | 开发侧 |
| `android/docs/` | 实现文档 | 架构、数据模型、存储、导入管线、性能、测试方案与样本（语义名 `.md`） | 产品需求、市场调研、设计规格 | 人 |
| `android/tools/` | 开发侧脚本 | 读写 `android/` 或只读 `design/`、`docs/` 的校验/构建脚本 | `*/build/` 下的任何东西、产物 | 开发侧 |
| `docs/` | 既不属设计也不属开发的判断依据 | 外部事实、取舍记录、写作规范 | 实现细节、设计规格、代码、抓包 | 人 |
| `docs/research/` | 外部世界的事实 | 带日期的调研快照与证据附录（只增不改） | 会持续更新的结论（那进 `design/` 或 `android/docs/`） | 人 |
| `docs/decisions/` | 决策记录 | `NNNN-<kebab-slug>.md` 的 ADR | 教程、操作手册、实现细节 | 人 |
| `docs/conventions/` | 写作规则 | 文档写作规范、ADR 指南 | 目录归属规则（归本文件） | 人 |
| `local/` | 本机私有（gitignore） | 平台抓包、调试 JSON、临时导出 | 需要长期保留或共享的东西 | 本机 |
| `README.md` | 仓库总索引 | 「东西在哪」+ 规矩摘要 | 规格正文 | 人 |
| `AGENTS.md` | **本文件：归属判据** | 规则与禁令 | 任何具体规格内容 | 人 |

**新增顶层目录前必须答「删掉它损失什么」四问：**
`design/` → 失去产品与界面的定稿；`android/` → 失去可执行工程；`docs/` → 失去「为什么做」的唯一叙述；`AGENTS.md` → 失去归属判据，下一个人会重新把东西堆成垃圾场。答不出损失，就不要建。

---

## 2. 规则清单

### R1 · 顶层只有三个内容目录

```
ls -d */        # 只允许 android/ design/ docs/ local/
```

- ❌ 违规：`assets/`（混装袋）、`tools/`（跨侧脚本没有归属）、`dyres/`（抓包堆在源码层）
- ✅ 正确：素材进 `design/`，脚本进 `android/tools/`，抓包进 `local/`，APK 留在 `android/app/build/outputs/apk/`

### R2 · 单一归属：一个字节只能属于一侧

判定法：**它能被 `assembleDebug` 直接吃掉吗？**

```
find design -type f \( -name '*.kt' -o -name '*.kts' -o -name 'build.gradle*' \)   # 必须为空
find design -type d \( -name res -o -name 'mipmap-*' -o -name 'drawable-*' -o -name values \)  # 必须为空
find design -type f -name '*.html' -not -path 'design/prototype/*'                # 必须为空
find android -type f \( -name '*.html' -o -name '*.svg' \)                        # 必须为空
```

- ❌ 违规：`design/.../mipmap-mdpi/ic_launcher.png`（给同一份字节造了第二个真相源）
- ✅ 正确：资源只存在 `android/app/src/main/res/`；设计侧要看，看 `design/exports/`

### R3 · 文档与产物分层

- `design/source/` 只放源文件，且整体 gitignore；需要共享的导出物进 `design/exports/`。
- `design/exports/`、`design/wireframes/` 只放图片/PDF，不放 `.md`。
- `design/prototype/` 只放自包含 `.html` 与说明 `.md`；HTML 内可以内联 CSS/JS，但**不得外链网络资源，也不得放独立脚本文件**。
- `android/docs/` 只放实现文档，不放代码、不放测试数据文件（测试数据写进 `.md`）。
- 生成物（`build/`、`.gradle/`、`.kotlin/`）只在 `android/` 下出现，且必须 gitignore。

### R4 · 编号只属于 ADR 和日期快照

```
find design docs android/docs -name '[0-9][0-9]-*' -not -path 'docs/decisions/*' -not -path 'docs/research/*'   # 必须为空
```

- ❌ 违规：`docs/02-设计规范.md`（编号不是地址，只被当排序用）
- ✅ 正确：`design/product/screen-specs.md`、`android/docs/test-plan.md`
- **例外 1**：`docs/decisions/NNNN-` 是 ADR 的永久编号空间，作废不复用，允许空号。
- **例外 2**：`docs/research/YYYY-MM-DD-` 是快照日期，不是编号。

### R5 · 文件名不出现版本/状态词

```
find design docs android/docs -name '*v[0-9]*' -o -name '*final*' -o -name '*new*' -o -name '*old*' -o -name '*tmp*'   # 必须为空
```

- ❌ 违规：`splash-v2.html`、`08-品牌裁决-final.md`
- ✅ 正确：`splash.html`；改版就改内容，版本在 git 历史与 `versionName` 里
- **范围说明**：Android 的 `mipmap-anydpi-v26`、`values-v21` 是平台 API 限定符，不受此规则约束。

### R6 · 不归属即删除，不建 `archive/`

判定法：**`grep` 零命中，且有生成命令 → 删。** 零命中但没有生成命令的，是「证据」，先放 `docs/research/` 或 `local/`。

```
find . -path ./.git -prune -o -type d \( -name archive -o -name old -o -name backup -o -name tmp -o -name out -o -name misc \) -print   # 必须为空
```

- ❌ 违规：`archive/2026-10/`、`90-archive/`
- ✅ 正确：删掉；结论写进 `docs/decisions/` 或 `android/docs/`；**历史在 git，不在目录里**

**ADR 被取代时不移动**：`status` 改为 `superseded` 并填 `superseded-by`，编号原地保留 —— 编号是永久地址，移动会让所有旧引用指向空。

### R7 · 活文档的路径必须今天就能用

- 本仓库**没有冻结记录**：所有 `.md` 都是活文档。
- 文档里的相对链接必须能打开；跨层引用写全路径 + 节号（见 R10）。
- 旧路径不是「历史坐标」，是一条必然失败的命令 —— 复制粘贴就报错。

```
bash android/tools/verify-layout.sh   # 第 6 项会检查所有 .md 的相对链接
```

- ❌ 违规：`见 02 第四节`、`见 docs/02`
- ✅ 正确：`见 android/docs/architecture.md §4.1`

### R8 · 禁止的目录名

`assets` `tmp` `out` `misc` `archive` `old` `backup` `dyres` `build`（作为源码目录）。

```
find design docs -type d \( -name assets -o -name tmp -o -name out -o -name misc -o -name archive -o -name old -o -name backup -o -name dyres -o -name build \)   # 必须为空
```

- ❌ 违规：`design/assets/`（同时装线框、切图、字体）
- ✅ 正确：线框 → `design/wireframes/`；切图 → `design/exports/`；源文件 → `design/source/`

**唯一豁免**：`local/` —— 它是本仓库对「抓包只在本机」这条现实约束的显式让步，整体 gitignore，不参与构建与文档导航。构建产物留在 `android/app/build/`，那是 Gradle 的固定输出位，不属于源码目录。

### R9 · 脚本归属：谁拥有目标目录，谁执行写入

判定法：**看脚本的写路径。写进谁的目录，脚本就属于谁。**

- `design/` 下**不得有任何独立脚本文件**（`.sh` / `.py` / `.mjs` / `.js`）；`design/prototype/*.html` 内联的 JS 属于设计稿，不算脚本。
- 写 `android/` 的脚本放 `android/tools/`。
- 脚本不得写 `design/` 或 `docs/`。

```
find design -type f \( -name '*.sh' -o -name '*.py' -o -name '*.mjs' -o -name '*.js' \)   # 必须为空
```

### R10 · 跨目录引用写全路径 + `§节`

- ❌ 违规：`见 architecture 那篇`、`见 ../30-tech/...`
- ✅ 正确：`见 android/docs/architecture.md §4`、`见 docs/decisions/0005-storage-layout.md`

---

## 3. 允许与禁止的箭头（穷举）

| 方向 | 形式 | 允许？ |
|---|---|---|
| `docs/` → `design/`、`android/docs/` | 文本路径引用 | ✅ 依据指路 |
| `design/product/` → `docs/decisions/`、`docs/research/` | 文本路径引用 | ✅ 规格回指依据 |
| `android/docs/` → `docs/`、`design/product/` | 文本路径引用 | ✅ 实现回指规格 |
| `design/*.md` → `design/exports/`、`design/wireframes/` | 相对路径 | ✅ 同侧引用 |
| `android/app/build/outputs/apk/` | 构建产物原址，不复制 | ✅ |
| `design/**` 写 `android/**` | 任何 | ❌ |
| `android/**` 写 `design/**` | 任何 | ❌ |
| `res/` 的字节在 `design/` 下再存一份 | 任何 | ❌ |
| 仓库内的产物位（`build/`、`out/`） | 任何 | ❌ 默认输出到 `$TMPDIR` |

---

## 4. 版本轴（互不推断）

| 轴 | 数的是 | 唯一写法 | 唯一权威位置 |
|---|---|---|---|
| 应用版本 | 可发布的 APK | `versionName = "x.y.z"` | `android/app/build.gradle.kts` |
| 文档修订 | 某份文档改到第几稿 | frontmatter 的 `date` | 文档自身 |
| 文档 ID | 文档的永久地址 | `doc: <repo 路径去扩展名>` | 文档 frontmatter |
| 决策编号 | ADR 的永久地址 | `NNNN-` | 文件名 |

- ❌ 违规：「v2 规格」「final 版设计」
- ✅ 正确：「应用版本 0.1.0」「2026-11-02 修订」「ADR 0008」

---

## 5. 改完自检

```
bash android/tools/verify-layout.sh
```

七项，输出 `★ 布局自检通过` 即退出码 0，任一 FAIL 即退出码 1（不在仓库根跑是退出码 2）：

1. 顶层只有三个内容目录 + `local/`（R1）
2. 单一归属：`design/` 无代码与 `res` 形状目录、`.html` 只在 `design/prototype/`；`android/` 无 `.html` / `.svg`（R2）
3. 文档与产物分层：`design/exports`、`wireframes` 只有图片/PDF；`design/prototype` 只有 `.html`/`.md`；生成物只在 `android/`（R3）
4. 命名：无 `NN-`（ADR 与日期快照除外）、无版本/状态词（R4 / R5）
5. 禁用目录：无 `archive` `old` `backup` `tmp` `out` `misc` `assets` `dyres`（R6 / R8）
6. 活文档断链：所有 `.md` 的相对链接必须能解析（R7）
7. 构建产物与本地私有：APK 只在 `android/app/build/`，`local/` 在 `.gitignore` 里（R8 豁免边界）

---

## 6. 本项目的显式让步

这条不是「第四个内容目录」，是现实约束，所以规则写死边界：

| 目录 | 为什么存在 | 边界 | 谁可以删 |
|---|---|---|---|
| `local/` | 平台抓包与调试 JSON 是过程证据，不能入库 | 整体 gitignore；文档不得依赖它；进 `local/` 就默认「可随时丢失」 | 本机，随时可删 |

**`local/` 不得被 `README.md` 之外的文档当作事实来源引用。** 需要长期保留的证据，写进 `docs/research/` 的文字里（记字节数、SHA-256、抓取日期），不要把原件搬进仓库。
