#!/usr/bin/env bash
# Plays the built-in Hard engine against Stockfish rungs on a connected phone and pulls the games back.
# Resumable: if the connection drops, run it again and it continues where it stopped.
# Usage: tools/run_device_calibration.sh [pairs] [adb-serial]
set -uo pipefail
cd "$(dirname "$0")/.."

PAIRS="${1:-30}"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
SERIAL="${2:-$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
adb() { "$ADB" -s "$SERIAL" "$@"; }

for RUNG in 1800 2200 2600; do
  case "$RUNG" in 1800) EXP=exp041-device-vs-stockfish-1800;; 2200) EXP=exp042-device-vs-stockfish-2200;; *) EXP=exp043-device-vs-stockfish-2600;; esac
  for ATTEMPT in 1 2 3 4 5 6; do
    ROWS=$(adb exec-out run-as com.divyanshu.chess cat "files/calibration/$EXP.csv" 2>/dev/null | wc -l | tr -d ' ')
    if [ "${ROWS:-0}" -ge $((PAIRS * 2 + 1)) ]; then break; fi
    echo "[$(date +%H:%M:%S)] $EXP attempt $ATTEMPT (rows so far: ${ROWS:-0})"
    adb shell am instrument -w -e class com.divyanshu.chess.OnDeviceCalibrationRun -e calibrate true \
      -e elo "$RUNG" -e pairs "$PAIRS" -e experiment "$EXP" com.divyanshu.chess.test/androidx.test.runner.AndroidJUnitRunner || sleep 20
  done
  mkdir -p tools/artifacts/device
  adb exec-out run-as com.divyanshu.chess cat "files/calibration/$EXP.csv" > "tools/artifacts/device/$EXP.csv"
  adb exec-out run-as com.divyanshu.chess cat "files/calibration/$EXP.meta.json" > "tools/artifacts/device/$EXP.meta.json"
  echo "[$(date +%H:%M:%S)] $EXP pulled: $(($(wc -l < "tools/artifacts/device/$EXP.csv") - 1)) games"
done
echo CALIBRATION_DONE
