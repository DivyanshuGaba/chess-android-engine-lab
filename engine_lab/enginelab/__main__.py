"""Command line: python -m enginelab run|summarize|openings|check experiments/<file>.json"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from datetime import datetime, timezone
from pathlib import Path

import chess.engine

import csv

from .config import Experiment
from .device import read_device_csv, strip_opening_moves
from .openings import load_openings
from .openings import book_openings, random_openings, save_openings
from .report import summarize
from .runner import LAB_DIR, UciHandle, resolve_command, resolve_engine, run_experiment


def find_experiment(arg: str) -> Path:
    path = Path(arg)
    if path.exists():
        return path
    matches = sorted((LAB_DIR / "experiments").glob(f"{arg}*.json"))
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one experiment matching '{arg}', found {len(matches)}")
    return matches[0]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="enginelab")
    sub = parser.add_subparsers(dest="command", required=True)

    run = sub.add_parser("run", help="play an experiment and write its report")
    run.add_argument("experiment")
    run.add_argument("--pairs", type=int, help="play only the first N opening pairs")
    run.add_argument("--workers", type=int)
    run.add_argument("--fresh", action="store_true", help="discard earlier results for this experiment")

    summ = sub.add_parser("summarize", help="rebuild summary.json and report.md from games.csv")
    summ.add_argument("experiment")

    check = sub.add_parser("check", help="start both engine configurations and verify every option is supported")
    check.add_argument("experiment")

    device = sub.add_parser("import-device", help="import games played on a phone by the on-device runner")
    device.add_argument("experiment")
    device.add_argument("--csv", required=True, help="games CSV pulled from the phone")
    device.add_argument("--meta", help="device meta JSON pulled from the phone")

    book = sub.add_parser("openings", help="list the built-in book openings")

    args = parser.parse_args(argv)

    if args.command == "openings":
        for opening in book_openings():
            print(opening.id, opening.fen)
        return 0

    path = find_experiment(args.experiment)
    experiment = Experiment.load(path)

    if args.command == "check":
        command = resolve_engine(experiment)
        for config in (experiment.baseline, experiment.candidate):
            handle = UciHandle(resolve_command(command, config), config, experiment.move_timeout_s)
            print(f"{config.name}: {handle.name} accepted {sorted(config.options)} with limit {config.describe_limits()}")
            handle.close()
        return 0

    if args.command == "import-device":
        results = LAB_DIR / "results" / experiment.id
        results.mkdir(parents=True, exist_ok=True)
        cfg = experiment.openings
        openings = load_openings(LAB_DIR / "openings" / f"random-{cfg.seed}-{cfg.count}-{cfg.plies}.json")
        fields, raw_rows = read_device_csv(Path(args.csv))
        rows = strip_opening_moves(raw_rows, openings)
        with (results / "games.csv").open("w", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fields)
            writer.writeheader()
            writer.writerows(rows)
        hardware = json.loads(Path(args.meta).read_text()) if args.meta else {}
        (results / "meta.json").write_text(json.dumps({
            "experiment_id": experiment.id,
            "source": "on-device runner (OnDeviceCalibrationRun)",
            "imported_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "hardware": hardware,
        }, indent=2))
        summary = summarize(results, experiment)
        print(f"{summary['id']}: {summary['games']} games, score {summary['score_pct']:.1f}%, Elo {summary['elo_diff']} "
              f"(95% CI {summary['elo_ci_low']} to {summary['elo_ci_high']}, {summary['ci_method']})")
        print(summary["verdict"])
        return 0

    if args.command == "run":
        results = run_experiment(path, pairs=args.pairs, workers=args.workers, fresh=args.fresh)
        summary = summarize(results, experiment)
        print(f"\n{summary['id']}: {summary['games']} games, score {summary['score_pct']:.1f}%, "
              f"Elo {summary['elo_diff']} (95% CI {summary['elo_ci_low']} to {summary['elo_ci_high']}, {summary['ci_method']})")
        print(summary["verdict"])
        print(f"Report: {results / 'report.md'}")
        return 0

    if args.command == "summarize":
        results = LAB_DIR / "results" / experiment.id
        summary = summarize(results, experiment)
        print(summary["verdict"])
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
