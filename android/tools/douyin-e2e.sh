#!/usr/bin/env bash
# 抖音分享链接端到端验收：真机/模拟器上跑收录，检查落盘媒体与数据库记录。
#
# 用法：
#   bash android/tools/douyin-e2e.sh                 # 默认 emulator-5558
#   SERIAL=emulator-5554 bash android/tools/douyin-e2e.sh
#   bash android/tools/douyin-e2e.sh --install       # 先装当前 debug APK
#
# 依赖：adb、sqlite3、已启动的模拟器/设备；首次解析抖音分享页可能要 30–60 秒。
# 退出码 0 表示两条链接都拿到预期媒体。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
SERIAL="${SERIAL:-emulator-5558}"
PKG="com.weiji.wang.debug"
ACT="com.weiji.wang.capture.ShareReceiverActivity"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/weiji-douyin-e2e.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT

VIDEO_TEXT='8.41 复制打开抖音，看看【英雄&联盟的作品】恐怖上演  https://v.douyin.com/7ESUV_w26M0/ Ulc:/ 01/31 Z@m.DU :9pm '
NOTE_TEXT='5.61 复制打开抖音，看看【胖娃要开方盒子的图文作品】长城汽车哈弗猛龙HEV/PHEV。工信部第413批... https://v.douyin.com/wrCuEvuKwVk/ YzT:/ :3pm 05/07 E@H.vF '

say() { printf '\n== %s\n' "$*"; }
adb_() { "$ADB" -s "$SERIAL" "$@"; }

if [[ "${1:-}" == "--install" ]]; then
  say "安装 $APK"
  adb_ install -r "$APK" >/dev/null
fi

adb_ shell pm clear "$PKG" >/dev/null
say "设备 $(adb_ shell getprop ro.product.model | tr -d '\r') / Android $(adb_ shell getprop ro.build.version.release | tr -d '\r')"

pull_db() {
  for f in weiji.db weiji.db-wal weiji.db-shm; do
    adb_ exec-out run-as "$PKG" cat "databases/$f" >"$WORK/$f" 2>/dev/null || true
  done
}

# 等待文件出现，最多 120 秒；参数是设备上的 glob 与最少文件数。
wait_files() {
  local glob="$1" least="$2" i
  for i in $(seq 1 40); do
    local n
    n=$(adb_ shell "ls $glob 2>/dev/null | wc -l" | tr -d '\r ')
    if [[ "$n" -ge "$least" ]]; then return 0; fi
    sleep 3
  done
  return 1
}

# 分享/粘贴后不再自动解析：脚本必须像用户一样点一次「解析」。
tap_parse() {
  adb_ shell uiautomator dump /sdcard/weiji-e2e-ui.xml >/dev/null 2>&1
  adb_ exec-out cat /sdcard/weiji-e2e-ui.xml >"$WORK/ui.xml"
  local xy
  xy=$(python3 - "$WORK/ui.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    if node.get("text") == "解析":
        x1, y1, x2, y2 = map(int, node.get("bounds").replace("][", ",").strip("[]").split(","))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  [[ -n "$xy" ]] || { echo "FAIL 找不到「解析」按钮"; return 1; }
  adb_ shell input tap $xy
}

run_case() {
  local name="$1" text="$2" kind="$3" least="$4" glob="$5"
  say "${name}：清空数据并发送分享文案"
  adb_ shell pm clear "$PKG" >/dev/null
  # 文案里含 & @ : 等字符，交给设备端单引号包住，避免被 shell 拆开。
  adb_ shell "am start -n $PKG/$ACT -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT '$text'" >/dev/null
  sleep 2
  tap_parse || return 1
  if ! wait_files "/sdcard/Android/data/$PKG/files/weiji/recipes/*/$glob" "$least"; then
    echo "FAIL ${name}：120 秒内没有出现 $least 个 ${kind} 文件"
    adb_ exec-out screencap -p >"$WORK/$name-fail.png"
    return 1
  fi
  # 文件落盘和数据库提交不是同一时刻：边等边查，直到 media 行数达标或 60 秒超时。
  local rows=0 total=0 i
  for i in $(seq 1 20); do
    pull_db
    rows=$(sqlite3 "$WORK/weiji.db" "select count(*) from media where kind='$kind';" 2>/dev/null || echo 0)
    total=$(sqlite3 "$WORK/weiji.db" "select count(*) from media;" 2>/dev/null || echo 0)
    if [[ "$rows" -ge "$least" ]]; then break; fi
    sleep 3
  done
  echo "DB：${kind}=${rows}，media 总数=$total"
  if [[ "$rows" -lt "$least" ]]; then
    echo "FAIL ${name}：数据库里只有 $rows 条 ${kind}"
    return 1
  fi
  if [[ "${kind}" == "VIDEO" ]]; then
    local bytes
    bytes=$(adb_ shell "wc -c /sdcard/Android/data/$PKG/files/weiji/recipes/*/video.mp4" | tr -d '\r' | awk '{print $1}' | head -1)
    echo "video.mp4 = ${bytes:-?} bytes"
    if [[ -z "${bytes:-}" || "$bytes" -lt 1000000 ]]; then
      echo "FAIL ${name}：video.mp4 小于 1MB"
      return 1
    fi
  fi
  echo "PASS $name"
}

say "抖音视频链接"
run_case video "$VIDEO_TEXT" VIDEO 1 'video.mp4'

say "抖音图文链接"
run_case note "$NOTE_TEXT" STEP 5 'step-*.webp'

say "全部通过"
