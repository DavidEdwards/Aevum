#!/bin/sh
# Seeds the emulator and walks through the app capturing raw screenshots.
# Tap coordinates target a 1080x2400 @ 420dpi device.
# Usage: capture.sh <light|dark>
set -e
cd "$(dirname "$0")"
MODE=${1:-light}
OUT=raw/$MODE
mkdir -p "$OUT"

if [ "$MODE" = dark ]; then night=yes; else night=no; fi
adb shell cmd uimode night $night

cap() {
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$(adb shell date +%H%M)" >/dev/null
    sleep 1
    rm -f "$OUT/$1.png"
    android screen capture -o "$OUT/$1.png" >/dev/null
    echo "captured $OUT/$1.png"
}
tap() { adb shell input tap "$1" "$2"; sleep "${3:-2}"; }
NAV_Y=2245

./seed_device.sh 14:20
sleep 6

tap 790 433; cap 01_favourites
tap 292 433; cap 01b_search
tap 790 433; tap 469 1137 3; cap 02_timer
tap 932 757; cap 03_stop_dialog
tap 356 1268; tap 540 745 1
adb shell input text "Built%sthe%snew%swelcome%scarousel%sin%sCompose%sand%swired%sup%sthe%spermission%sscreens"
sleep 1; adb shell input keyevent KEYCODE_BACK; sleep 1
cap 04_editor
tap 176 314 3
tap 944 $NAV_Y 3; cap 05_pending
tap 586 1640 3; tap 469 2020 3; tap 600 630 1; cap 09_split
tap 675 $NAV_Y 10; cap 06_summary
tap 250 820 3; cap 07_day
tap 135 $NAV_Y; tap 963 314 3; cap 08_settings
