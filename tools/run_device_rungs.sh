#!/usr/bin/env bash
# Same as run_device_calibration.sh but for an explicit list of rungs.
# Usage: tools/run_device_rungs.sh "<pairs>" "<rung> [<rung> ...]" [adb-serial]
set -uo pipefail
cd "$(dirname "$0")/.."

PAIRS="${1:-30}"
RUNGS="${2:?list the Stockfish UCI_Elo rungs, e.g. \"1350 1500\"}"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
SERIAL="${3:-$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
adb() { "$ADB" -s "$SERIAL" "$@"; }

name_for() { ls engine_lab/experiments | sed -n "s/^\\(exp04[0-9]-device-vs-stockfish-$1\\)\\.json$/\\1/p"; }

for RUNG in $RUNGS; do
  EXP="$(name_for "$RUNG")"
  [ -n "$EXP" ] || { echo "no experiment file for rung $RUNG"; continue; }
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
echo RUNGS_DONE
