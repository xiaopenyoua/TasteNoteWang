#!/usr/bin/env bash
#
# 味记・王 · 目录契约校验器
#
#   用法：bash android/tools/verify-layout.sh        （在仓库任意位置跑都行）
#   退出码：0 = 七项全绿；1 = 有 FAIL；2 = 跑不起来（不在仓库里 / 缺工具）
#
# 它校验 AGENTS.md 里七条能被机器判定的规则，不是代码风格。
# 每项打印一句人话结论 + 违规清单；清单为空即 ✓。
#
# ── 判据边界（不是 bug，别照着它改文件）──────────────────────────────────
#
#   [A] 顶层允许 4 个目录：android/ design/ docs/ 是内容目录，
#       local/ 是 AGENTS.md §6 写死的显式让步（在 .gitignore 里）。
#       构建产物不单独设交付目录，APK 留在 android/app/build/outputs/apk/。
#
#   [B] android/app/src/main/assets/ 是 Android 平台约定的资源目录，
#       不受 R8「禁止 assets」约束；R8 只扫 design/ 与 docs/。
#
#   [C] android/app/build/ 是 Gradle 生成物，不参与归属检查，但必须被 gitignore。
#
#   [D] 链接检查只看「真超链接」（`[x](路径)`），不检查正文里的裸文件名；
#       所有 .md 都是活文档，断链一律 FAIL（AGENTS.md R7）。
#
#   [E] NN- 检查：ADR 用 4 位编号（0001-），日期快照用 2026-10-30-，
#       两者都不匹配 [0-9][0-9]-，所以无需额外豁免。
#
set -u

cd "$(dirname "${BASH_SOURCE[0]}")/../.." 2>/dev/null || { echo "找不到仓库根"; exit 2; }
ROOT=$(pwd)
[ -f AGENTS.md ] && [ -d design ] && [ -d android ] && [ -d docs ] \
  || { echo "这里不是 TasteNoteWang 仓库根：$ROOT"; exit 2; }

FAIL=0
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$*"; FAIL=$((FAIL+1)); }
item() { printf '      %s\n' "$*"; }
head_() { printf '\n\033[1m[%s]\033[0m %s\n' "$1" "$2"; }

printf '\033[1mTasteNoteWang 目录契约校验\033[0m  %s\n' "$ROOT"

# ─────────────────────────────────────────────────────────────────────────────
head_ 1 "顶层只有三个内容目录 + local（R1 / §6）"
tops=$(ls -d */ 2>/dev/null | sed 's:/$::' | sort | tr '\n' ' ')
if [ "$tops" = "android design docs local " ]; then
  ok "顶层目录 = android / design / docs / local"
else
  bad "顶层目录不符合契约：$tops"
  for d in $tops; do
    case "$d" in android|design|docs|dist|local) ;; *) item "多出来的：$d/";; esac
  done
fi

# ─────────────────────────────────────────────────────────────────────────────
head_ 2 "单一归属：design 无代码，android 无设计稿（R2）"
n=$(find design -type f \( -name '*.kt' -o -name '*.kts' -o -name 'build.gradle*' \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 下没有 Kotlin / Gradle 文件" || { bad "design/ 下有 $n 个代码/构建文件"; find design -type f \( -name '*.kt' -o -name '*.kts' -o -name 'build.gradle*' \) | sed 's/^/      /'; }
n=$(find design -type d \( -name res -o -name 'mipmap-*' -o -name 'drawable-*' -o -name values \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 下没有 res 形状目录" || { bad "design/ 下有 $n 个 res 形状目录"; find design -type d \( -name res -o -name 'mipmap-*' -o -name 'drawable-*' -o -name values \) | sed 's/^/      /'; }
n=$(find design -type f -name '*.html' -not -path 'design/prototype/*' 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 的 .html 只在 design/prototype/" || { bad "design/prototype/ 之外有 $n 个 .html"; find design -type f -name '*.html' -not -path 'design/prototype/*' | sed 's/^/      /'; }
n=$(find android \( -name build -o -name .gradle -o -name .kotlin \) -prune -o -type f \( -name '*.html' -o -name '*.svg' \) -print 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "android/ 下没有 .html / .svg 设计稿" || { bad "android/ 下有 $n 个 .html / .svg"; find android \( -name build -o -name .gradle -o -name .kotlin \) -prune -o -type f \( -name '*.html' -o -name '*.svg' \) -print | sed 's/^/      /'; }
n=$(find android -name '*.md' -not -path 'android/docs/*' -not -path '*/build/*' 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "android/ 的 .md 只在 android/docs/" || { bad "android/ 下 docs/ 之外有 $n 个 .md"; find android -name '*.md' -not -path 'android/docs/*' -not -path '*/build/*' | sed 's/^/      /'; }

# ─────────────────────────────────────────────────────────────────────────────
head_ 3 "文档与产物分层（R3）"
n=$(find design/exports design/wireframes -type f ! \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' -o -name '*.webp' -o -name '*.pdf' -o -name '*.svg' \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/exports 与 wireframes 只有图片/PDF" || { bad "design/exports 或 wireframes 混进非图片"; find design/exports design/wireframes -type f ! \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' -o -name '*.webp' -o -name '*.pdf' -o -name '*.svg' \) | sed 's/^/      /'; }
n=$(find design/product -type f ! -name '*.md' 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/product 只有 .md" || { bad "design/product 混进非 .md 文件"; find design/product -type f ! -name '*.md' | sed 's/^/      /'; }
n=$(find design/prototype -type f ! -name '*.html' ! -name '*.md' 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/prototype 只有 .html / .md" || { bad "design/prototype 混进其他文件"; find design/prototype -type f ! -name '*.html' ! -name '*.md' | sed 's/^/      /'; }
n=$(find design docs -type d \( -name build -o -name .gradle -o -name .kotlin \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 与 docs/ 下没有生成物目录" || { bad "design/ 或 docs/ 下出现生成物目录"; find design docs -type d \( -name build -o -name .gradle -o -name .kotlin \) | sed 's/^/      /'; }

# ─────────────────────────────────────────────────────────────────────────────
head_ 4 "命名：无 NN-，无版本/状态词（R4 / R5）"
n=$(find design docs android/docs -name '[0-9][0-9]-*' -not -path 'docs/decisions/*' -not -path 'docs/research/*' 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "没有 NN- 前缀（ADR 与日期快照除外）" || { bad "发现 $n 个 NN- 前缀"; find design docs android/docs -name '[0-9][0-9]-*' -not -path 'docs/decisions/*' -not -path 'docs/research/*' | sed 's/^/      /'; }
n=$(find design docs android/docs \( -name '*v[0-9]*' -o -name '*final*' -o -name '*new*' -o -name '*old*' -o -name '*tmp*' \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "文件名没有版本/状态词" || { bad "发现 $n 个版本/状态词文件名"; find design docs android/docs \( -name '*v[0-9]*' -o -name '*final*' -o -name '*new*' -o -name '*old*' -o -name '*tmp*' \) | sed 's/^/      /'; }

# ─────────────────────────────────────────────────────────────────────────────
head_ 5 "禁用目录（R6 / R8）"
n=$(find design docs -type d \( -name assets -o -name tmp -o -name out -o -name misc -o -name archive -o -name old -o -name backup -o -name dyres -o -name build \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 与 docs/ 下没有禁用目录" || { bad "发现 $n 个禁用目录"; find design docs -type d \( -name assets -o -name tmp -o -name out -o -name misc -o -name archive -o -name old -o -name backup -o -name dyres -o -name build \) | sed 's/^/      /'; }
n=$(find . -path ./.git -prune -o -path '*/build' -prune -o -type d \( -name archive -o -name old -o -name backup -o -name tmp -o -name out -o -name misc -o -name dyres \) -print 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "全仓库没有 archive/old/backup/tmp/out/misc/dyres" || { bad "发现 $n 个禁用目录"; find . -path ./.git -prune -o -path '*/build' -prune -o -type d \( -name archive -o -name old -o -name backup -o -name tmp -o -name out -o -name misc -o -name dyres \) -print | sed 's/^/      /'; }
n=$(find design -type f \( -name '*.sh' -o -name '*.py' -o -name '*.mjs' -o -name '*.js' \) 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "design/ 下没有脚本（R9）" || { bad "design/ 下有 $n 个脚本"; find design -type f \( -name '*.sh' -o -name '*.py' -o -name '*.mjs' -o -name '*.js' \) | sed 's/^/      /'; }

# ─────────────────────────────────────────────────────────────────────────────
head_ 6 "活文档断链（R7）"
if ! command -v python3 >/dev/null 2>&1; then
  bad "缺少 python3，无法检查链接"
else
  broken=$(python3 -I - <<'PY'
import re
from pathlib import Path
pat = re.compile(r'\[[^\]]*\]\(([^)]+)\)')
bad = []
for md in Path('.').rglob('*.md'):
    parts = md.parts
    if any(p in parts for p in ('build', 'node_modules', '.git', 'local')):
        continue
    text = re.sub(r'`[^`]*`', '', md.read_text(encoding='utf-8', errors='ignore'))
    for raw in pat.findall(text):
        link = raw.split('#')[0].strip()
        if not link or link.startswith(('http://', 'https://', 'mailto:', 'weiji:')):
            continue
        if '<' in link or '{' in link:
            continue
        if not (md.parent / link).resolve().exists():
            bad.append(f"{md}: {raw}")
print("\n".join(bad))
PY
)
  if [ -z "$broken" ]; then
    ok "所有 .md 的相对链接都能解析"
  else
    bad "发现断链："
    echo "$broken" | sed 's/^/      /'
  fi
fi

# ─────────────────────────────────────────────────────────────────────────────
head_ 7 "构建产物与本地私有边界（R8 豁免 / §6）"
n=$(find . -path ./.git -prune -o -path './android/app/build' -prune -o -name '*.apk' -print 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 0 ] && ok "APK 只出现在 android/app/build/" || { bad "android/app/build/ 之外有 $n 个 APK"; find . -path ./.git -prune -o -path './android/app/build' -prune -o -name '*.apk' -print | sed 's/^/      /'; }
missing=""
grep -q '^android/app/build/' .gitignore || missing="$missing android/app/build/"
grep -q '^local/' .gitignore || missing="$missing local/"
grep -q '^local\.properties' .gitignore || missing="$missing local.properties"
if [ -z "$missing" ]; then ok ".gitignore 覆盖 android/app/build / local / local.properties"; else bad ".gitignore 缺少：$missing"; fi

# ─────────────────────────────────────────────────────────────────────────────
printf '\n'
if [ "$FAIL" = 0 ]; then
  printf '\033[32m★ 布局自检通过\033[0m\n'
  exit 0
else
  printf '\033[31m✗ 布局自检失败：%s 项\033[0m\n' "$FAIL"
  exit 1
fi
