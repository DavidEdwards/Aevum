#!/bin/sh
# Seeds the debug app on a connected emulator with mocked marketing data.
# Usage: seed_device.sh [HH:MM]   (local emulator time to set, default 14:20)
set -e
cd "$(dirname "$0")"
PKG=dae.aevum.debug
TIME=${1:-14:20}

adb shell settings put global auto_time 0
adb shell cmd alarm set-time "$(date -d "$(adb shell date +%F) $TIME $(adb shell date +%Z)" +%s000)"

# Offline so refreshes against the fake Jira host fail fast instead of overwriting data.
adb shell cmd connectivity airplane-mode enable || true
adb shell svc wifi disable
adb shell svc data disable
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true

adb shell am force-stop $PKG
DB=$(mktemp -u).db
python3 seed_db.py "$DB" "$(adb shell date +%s)"
adb push "$DB" /data/local/tmp/aevum.db >/dev/null
rm -f "$DB"
adb shell "run-as $PKG sh -c 'mkdir -p databases && rm -f databases/database* && cp /data/local/tmp/aevum.db databases/database'"

adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4 -e airplane hide
demo notifications -e visible false

adb shell am start -W -n $PKG/dae.aevum.MainActivity >/dev/null
