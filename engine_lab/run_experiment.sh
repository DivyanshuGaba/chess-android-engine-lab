#!/usr/bin/env bash
# Usage: ./run_experiment.sh <experiment-id-prefix> [--pairs N] [--workers W] [--fresh]
set -euo pipefail
cd "$(dirname "$0")"
# The built-in engines run on the JVM; use JDK 17 if JAVA_HOME is not set.
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}"
[ -d .venv ] || { python3 -m venv .venv && ./.venv/bin/pip install -q -r requirements.txt; }
exec ./.venv/bin/python -m enginelab run "$@"
