#!/usr/bin/env bash
# Runs the Android instrumented tests on a connected device WITHOUT uninstalling the app, so saved
# games and settings survive (Gradle's connectedAndroidTest removes the app afterwards).
# Usage: tools/run_instrumented.sh [adb-serial]
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
SERIAL="${1:-}"
adb() { if [ -n "$SERIAL" ]; then "$ADB" -s "$SERIAL" "$@"; else "$ADB" "$@"; fi; }

./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

mkdir -p tools/artifacts
adb shell am instrument -w com.divyanshu.chess.test/androidx.test.runner.AndroidJUnitRunner | tee tools/artifacts/instrumented.txt
adb uninstall com.divyanshu.chess.test >/dev/null || true
tail -5 tools/artifacts/instrumented.txt
