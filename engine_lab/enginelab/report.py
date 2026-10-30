"""Turns games.csv into summary.json, report.md, by_opening.csv and games.pgn."""

from __future__ import annotations

import csv
import json
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

import chess
import chess.pgn

from . import stats
from .config import Experiment
from .game import FAILURES
from .runner import LAB_DIR, read_rows


def _side_stats(rows: list[dict], prefix: str) -> dict:
    nodes = sum(float(r[f"{prefix}_nodes"]) for r in rows)
    engine_s = sum(float(r[f"{prefix}_engine_time_s"]) for r in rows)
    wall_s = sum(float(r[f"{prefix}_wall_time_s"]) for r in rows)
    moves = sum(int(r[f"{prefix}_moves"]) for r in rows)
    depth_sum = sum(int(r[f"{prefix}_depth_sum"]) for r in rows)
    depth_samples = sum(int(r[f"{prefix}_depth_samples"]) for r in rows)
    peak = max((float(r[f"{prefix}_peak_rss_mb"]) for r in rows), default=0.0)
    return {
        "moves": moves,
        "avg_nps": round(nodes / engine_s) if engine_s > 0 else None,
        "avg_depth": round(depth_sum / depth_samples, 2) if depth_samples else None,
        "avg_move_ms": round(1000 * wall_s / moves, 2) if moves else None,
        "peak_rss_mb": peak or None,
    }


def summarize_rows(rows: list[dict], experiment: Experiment, meta: dict | None = None) -> dict:
    """Pure function over game rows, so it can be tested without running any engine."""
    scores = [float(r["candidate_score"]) for r in rows]
    wins = sum(1 for s in scores if s == 1.0)
    draws = sum(1 for s in scores if s == 0.5)
    losses = sum(1 for s in scores if s == 0.0)

    by_pair: dict[int, list[float]] = defaultdict(list)
    for row in rows:
        by_pair[int(row["pair_id"])].append(float(row["candidate_score"]))
    complete = {pair: sum(values) for pair, values in by_pair.items() if len(values) == 2}
    incomplete = len(by_pair) - len(complete)
    buckets = stats.pair_buckets(list(complete.values()))

    tri = stats.trinomial(wins, draws, losses)
    penta = stats.pentanomial(buckets) if complete else None
    primary = penta if penta and len(complete) >= 10 else tri

    failures = Counter(r["termination"] for r in rows if r["termination"] in FAILURES)
    n_pairs = len(complete)
    # Every pair scoring exactly 1-1 is the signature of two identical deterministic engines replaying
    # the same game with colours swapped: the score is 50% by construction and says nothing about bias.
    mirrored = n_pairs >= 10 and all(total == 1.0 for total in complete.values())
    warnings = []
    if mirrored:
        warnings.append(
            "Every opening pair scored exactly 1-1. That happens when two identical deterministic engines "
            "replay the same game with colours swapped, so this run cannot detect bias. Add jitter or "
            "make the configurations differ."
        )
    conclusive = n_pairs >= experiment.min_pairs_for_conclusion
    if primary is None:
        verdict = "No games were completed."
    elif mirrored:
        verdict = "Not informative: every opening pair was split 1-1, so the interval carries no information."
    elif primary.excludes_zero:
        direction = "stronger" if primary.elo > 0 else "weaker"
        verdict = f"The candidate measured {direction} than the baseline; the 95% interval excludes zero."
    else:
        verdict = "No measurable difference: the 95% interval includes zero at this sample size."

    return {
        "id": experiment.id,
        "title": experiment.title,
        "description": experiment.description,
        "hypothesis": experiment.hypothesis,
        "status": "degenerate" if mirrored else ("sufficient sample" if conclusive else "preliminary"),
        "warnings": warnings,
        "min_pairs_for_conclusion": experiment.min_pairs_for_conclusion,
        "pairs": n_pairs,
        "incomplete_pairs": incomplete,
        "games": len(rows),
        "engine_version": rows[0]["engine_version"] if rows else "",
        "time_control": rows[0]["time_control"] if rows else "",
        "baseline": {"name": experiment.baseline.name, "limits": experiment.baseline.describe_limits(), "options": experiment.baseline.options},
        "candidate": {"name": experiment.candidate.name, "limits": experiment.candidate.describe_limits(), "options": experiment.candidate.options},
        "wins": wins, "draws": draws, "losses": losses,
        "score_pct": round(100 * sum(scores) / len(scores), 2) if scores else 0.0,
        "elo_diff": _bound(primary.elo) if primary else None,
        "elo_ci_low": _bound(primary.elo_low) if primary and not mirrored else None,
        "elo_ci_high": _bound(primary.elo_high) if primary and not mirrored else None,
        "ci_open_ended": bool(primary and not mirrored and (_bound(primary.elo_low) is None or _bound(primary.elo_high) is None)),
        "ci_method": ("n/a (degenerate)" if mirrored else primary.method) if primary else "",
        "confidence": 0.95,
        "los_pct": round(100 * primary.los, 1) if primary else None,
        "trinomial": _estimate(tri),
        "pentanomial": _estimate(penta),
        "pair_buckets": {"0": buckets[0], "0.5": buckets[1], "1": buckets[2], "1.5": buckets[3], "2": buckets[4]},
        "crashes": failures.get("crash", 0),
        "illegal_moves": failures.get("illegal_move", 0),
        "timeouts": failures.get("timeout", 0),
        "terminations": dict(Counter(r["termination"] for r in rows)),
        "avg_plies": round(sum(int(r["plies"]) for r in rows) / len(rows), 1) if rows else None,
        "baseline_stats": _side_stats(rows, "b"),
        "candidate_stats": _side_stats(rows, "c"),
        "verdict": verdict,
        "created_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "reproduce": f"cd engine_lab && ./run_experiment.sh {experiment.id}",
        "meta": meta or {},
    }


OPEN_ENDED_ELO = 1500   # score_to_elo clamps at about +/-1600, so anything near that is not a measurement


def _bound(value: float | None) -> float | None:
    """Elo bounds that hit the arithmetic clamp are reported as open-ended (None), not as numbers."""
    return None if value is None or abs(value) >= OPEN_ENDED_ELO else round(value, 1)


def _estimate(estimate: stats.EloEstimate | None) -> dict | None:
    if estimate is None:
        return None
    return {"method": estimate.method, "samples": estimate.samples, "score": round(estimate.score, 4),
            "elo": round(estimate.elo, 1), "low": round(estimate.elo_low, 1), "high": round(estimate.elo_high, 1),
            "los": round(estimate.los, 4)}


def opening_table(rows: list[dict]) -> list[dict]:
    pairs: dict[int, dict] = {}
    for row in rows:
        entry = pairs.setdefault(int(row["pair_id"]), {"opening_id": row["opening_id"], "score": 0.0, "games": 0})
        entry["score"] += float(row["candidate_score"])
        entry["games"] += 1
    return sorted(({"pair_id": p, **e} for p, e in pairs.items()), key=lambda e: (-e["score"], e["pair_id"]))


def write_pgn(rows: list[dict], experiment: Experiment, path: Path) -> None:
    with path.open("w") as handle:
        for row in rows:
            board = chess.Board(row["opening_fen"])
            game = chess.pgn.Game()
            game.headers.update({
                "Event": experiment.id, "Round": row["game_id"], "White": f"{row['white']}", "Black": f"{row['black']}",
                "Result": row["result"], "Termination": row["termination"], "FEN": row["opening_fen"], "SetUp": "1",
            })
            game.setup(board)
            node = game
            for uci in row["moves_uci"].split():
                node = node.add_variation(chess.Move.from_uci(uci))
            print(game, file=handle, end="\n\n")


def markdown(summary: dict, table: list[dict]) -> str:
    def fmt(value, spec="{:+.0f}"):
        return spec.format(value) if value is not None else "n/a"

    lines = [
        f"# {summary['title']}", "",
        f"Experiment `{summary['id']}`, status: **{summary['status']}** "
        f"({summary['pairs']} complete opening pairs, minimum for a conclusion: {summary['min_pairs_for_conclusion']}).", "",
        summary["description"], "",
        f"**Hypothesis:** {summary['hypothesis']}" if summary["hypothesis"] else "", "",
        "## Configurations", "",
        "| | Name | Search limit | UCI options |", "|---|---|---|---|",
        f"| Baseline | {summary['baseline']['name']} | {summary['baseline']['limits']} | `{json_compact(summary['baseline']['options'])}` |",
        f"| Candidate | {summary['candidate']['name']} | {summary['candidate']['limits']} | `{json_compact(summary['candidate']['options'])}` |", "",
        f"Engine: {summary['engine_version']}.", "",
        "## Result (candidate point of view)", "",
        f"- Games: {summary['games']} (wins {summary['wins']}, draws {summary['draws']}, losses {summary['losses']})",
        f"- Score: {summary['score_pct']:.2f}%",
        f"- Elo difference: {elo_text(summary)} "
        f"(95% CI {ci_text(summary['elo_ci_low'], 'low')} to {ci_text(summary['elo_ci_high'], 'high')}, {summary['ci_method']})",
        f"- Likelihood of superiority: {fmt(summary['los_pct'], '{:.1f}')}%",
        f"- Pair totals 0 / 0.5 / 1 / 1.5 / 2: {' / '.join(str(v) for v in summary['pair_buckets'].values())}",
        f"- **Verdict:** {summary['verdict']}",
        *[f"- **Warning:** {w}" for w in summary.get("warnings", [])], "",
        "## Reliability", "",
        f"- Crashes: {summary['crashes']}, illegal moves: {summary['illegal_moves']}, timeouts: {summary['timeouts']}, "
        f"incomplete pairs: {summary['incomplete_pairs']}",
        f"- Terminations: {json_compact(summary['terminations'])}",
        f"- Average game length: {summary['avg_plies']} plies", "",
        "## Engine speed", "",
        "| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |", "|---|---|---|---|---|",
    ]
    for label, key in (("Baseline", "baseline_stats"), ("Candidate", "candidate_stats")):
        s = summary[key]
        lines.append(f"| {label} | {fmt(s['avg_nps'], '{:,.0f}')} | {fmt(s['avg_depth'], '{:.1f}')} | "
                     f"{fmt(s['avg_move_ms'], '{:.1f}')} | {fmt(s['peak_rss_mb'], '{:.0f}')} |")
    lines += ["", "## Openings", "",
              f"Best pairs for the candidate: " + ", ".join(f"{e['opening_id']} ({e['score']:g}/2)" for e in table[:5]),
              f"Worst pairs for the candidate: " + ", ".join(f"{e['opening_id']} ({e['score']:g}/2)" for e in table[-5:]), "",
              "## Method and limits", "",
              "- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).",
              "- Engines run single-threaded with identical hash settings unless the options above differ.",
              "- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.",
              "- A confidence interval that includes zero means the experiment could not tell the engines apart.",
              f"- Reproduce: `{summary['reproduce']}`", ""]
    return "\n".join(lines)


def elo_text(summary: dict) -> str:
    if summary["elo_diff"] is not None:
        return f"{summary['elo_diff']:+.0f}"
    return "off the scale" if summary["ci_method"] and not summary["ci_method"].startswith("n/a") else "n/a"


def ci_text(value, side: str) -> str:
    if value is not None:
        return f"{value:+.0f}"
    return "open-ended (the score interval reaches the extreme)" if side == "high" else "open-ended"


def json_compact(value) -> str:
    return json.dumps(value, separators=(",", ":"))


def summarize(results_dir: Path, experiment: Experiment) -> dict:
    rows = read_rows(results_dir / "games.csv")
    meta_path = results_dir / "meta.json"
    meta = json.loads(meta_path.read_text()) if meta_path.exists() else {}
    summary = summarize_rows(rows, experiment, meta)
    table = opening_table(rows)
    (results_dir / "summary.json").write_text(json.dumps(summary, indent=2))
    (results_dir / "report.md").write_text(markdown(summary, table))
    with (results_dir / "by_opening.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["pair_id", "opening_id", "score", "games"])
        writer.writeheader()
        writer.writerows(table)
    write_pgn(rows, experiment, results_dir / "games.pgn")
    return summary
