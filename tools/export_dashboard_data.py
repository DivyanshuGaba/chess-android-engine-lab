#!/usr/bin/env python3
"""Copies real benchmark and test results into the app's assets for the Engine Lab screen.

Nothing is invented: a suite or experiment appears only if its result files exist. Run it after
the experiments and tests, then rebuild the app.
"""

import json
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "lab"
RESULTS = ROOT / "engine_lab" / "results"


def now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")


def junit_suite(name: str, files: list[Path]) -> dict | None:
    total = failures = skipped = 0
    for path in files:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            total += int(suite.get("tests", 0))
            failures += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
            skipped += int(suite.get("skipped", 0))
    return {"name": name, "total": total, "failures": failures, "skipped": skipped} if files else None


def instrumented_suite(path: Path) -> dict | None:
    if not path.exists():
        return None
    text = path.read_text()
    import re
    ok = re.search(r"OK \((\d+) tests?\)", text)
    if ok:
        return {"name": "Android instrumented tests (Pixel 8)", "total": int(ok.group(1)), "failures": 0, "skipped": 0}
    bad = re.search(r"Tests run: ?(\d+), ?Failures: ?(\d+)", text)
    if bad:
        return {"name": "Android instrumented tests (Pixel 8)", "total": int(bad.group(1)), "failures": int(bad.group(2)), "skipped": 0}
    return None


def main() -> int:
    ASSETS.mkdir(parents=True, exist_ok=True)

    experiments = []
    for summary_path in sorted(RESULTS.glob("*/summary.json")):
        summary = json.loads(summary_path.read_text())
        if summary["id"].startswith("exp-pytest"):
            continue
        summary.pop("meta", None)
        experiments.append(summary)
    results_file = ASSETS / "results.json"
    if experiments:
        results_file.write_text(json.dumps({"generated_at": now(), "experiments": experiments}, indent=1))
    elif results_file.exists():
        results_file.unlink()

    suites = [
        junit_suite("Android JVM unit tests", sorted((ROOT / "app" / "build" / "test-results" / "testDebugUnitTest").glob("*.xml"))),
        junit_suite("Python engine_lab tests", [p for p in [RESULTS / "pytest.xml"] if p.exists()]),
        instrumented_suite(ROOT / "tools" / "artifacts" / "instrumented.txt"),
    ]
    suites = [s for s in suites if s]
    tests_file = ASSETS / "tests.json"
    if suites:
        tests_file.write_text(json.dumps({"generated_at": now(), "suites": suites}, indent=1))
    elif tests_file.exists():
        tests_file.unlink()

    print(f"Exported {len(experiments)} experiments and {len(suites)} test suites to {ASSETS}")
    for suite in suites:
        print(f"  {suite['name']}: {suite['total']} tests, {suite['failures']} failed, {suite['skipped']} skipped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
