#!/usr/bin/env python3
"""Regenerates the measured sections of README.md from the result files.

It rewrites only the text between the EXPERIMENTS and TESTS markers, so hand-written notes elsewhere
in the README are never touched, and every number comes from engine_lab/results or the test reports.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "lab"


def replace_block(text: str, name: str, body: str) -> str:
    pattern = re.compile(rf"(<!-- {name}:START -->).*?(<!-- {name}:END -->)", re.S)
    if not pattern.search(text):
        raise SystemExit(f"README.md is missing the {name} markers")
    return pattern.sub(lambda m: f"{m.group(1)}\n{body.strip()}\n{m.group(2)}", text)


def fmt_elo(value) -> str:
    return "n/a" if value is None else f"{value:+.0f}"


def experiments_block() -> str:
    path = ASSETS / "results.json"
    if not path.exists():
        return "_No experiment has been run yet. Run `engine_lab/run_experiment.sh exp001`, then `tools/export_dashboard_data.py`._"
    data = json.loads(path.read_text())
    lines = [
        f"_Generated from `engine_lab/results/*/summary.json` ({data['generated_at']}). Wins, draws and losses are from the candidate's point of view._",
        "",
        "| Experiment | Baseline vs candidate | Games (pairs) | W / D / L | Score | Elo difference (95% CI) | Status |",
        "|---|---|---|---|---|---|---|",
    ]
    for e in data["experiments"]:
        if e["status"] == "degenerate":
            ci = f"{fmt_elo(e['elo_diff'])} (interval not informative)"
        else:
            low = fmt_elo(e["elo_ci_low"]) if e["elo_ci_low"] is not None else "open-ended"
            high = fmt_elo(e["elo_ci_high"]) if e["elo_ci_high"] is not None else "open-ended"
            diff = fmt_elo(e["elo_diff"]) if e["elo_diff"] is not None else "off the scale"
            ci = f"{diff} ({low} to {high}, {e['ci_method']})"
        lines.append(
            f"| `{e['id']}` | {e['baseline']['name']} vs {e['candidate']['name']} | {e['games']} ({e['pairs']}) | "
            f"{e['wins']} / {e['draws']} / {e['losses']} | {e['score_pct']:.2f}% | {ci} | {e['status']} |"
        )
    lines.append("")
    for e in data["experiments"]:
        lines.append(f"* **{e['title']}**: {e['verdict']}")
        for warning in e.get("warnings", []):
            lines.append(f"  * Warning: {warning}")
        b, c = e["baseline_stats"], e["candidate_stats"]
        if b.get("avg_nps") and c.get("avg_nps"):
            lines.append(f"  * Speed: baseline {b['avg_nps']:,} nodes/s at depth {b['avg_depth']}, candidate {c['avg_nps']:,} nodes/s at depth {c['avg_depth']}; "
                         f"peak engine memory {b.get('peak_rss_mb') or 0:.0f} MB. Crashes {e['crashes']}, illegal moves {e['illegal_moves']}, timeouts {e['timeouts']}.")
    return "\n".join(lines)


def tests_block() -> str:
    path = ASSETS / "tests.json"
    if not path.exists():
        return "_No test summary has been exported yet._"
    data = json.loads(path.read_text())
    lines = [f"_Last recorded run: {data['generated_at']}._", "", "| Suite | Tests | Failed | Skipped |", "|---|---|---|---|"]
    for s in data["suites"]:
        lines.append(f"| {s['name']} | {s['total']} | {s['failures']} | {s['skipped']} |")
    return "\n".join(lines)


def main() -> int:
    text = README.read_text()
    text = replace_block(text, "EXPERIMENTS", experiments_block())
    text = replace_block(text, "TESTS", tests_block())
    README.write_text(text)
    print("README.md updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
