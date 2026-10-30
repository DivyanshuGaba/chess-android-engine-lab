import json

from enginelab import report
from enginelab.config import Experiment

from conftest import LAB_DIR


def row(pair, candidate_score, termination="checkmate", color="cw"):
    return {
        "experiment_id": "exp-test", "game_id": f"{pair:05d}-{color}", "pair_id": str(pair), "opening_id": f"o{pair}",
        "opening_fen": "", "white": "candidate", "black": "baseline", "result": "1-0",
        "candidate_score": str(candidate_score), "termination": termination, "plies": "60", "moves_uci": "", "error": "",
        "b_moves": "30", "b_depth_sum": "300", "b_depth_samples": "30", "b_nodes": "300000", "b_engine_time_s": "0.3",
        "b_wall_time_s": "0.6", "b_peak_rss_mb": "120", "c_moves": "30", "c_depth_sum": "330", "c_depth_samples": "30",
        "c_nodes": "600000", "c_engine_time_s": "0.4", "c_wall_time_s": "0.7", "c_peak_rss_mb": "130",
        "engine_version": "TestFish", "time_control": "nodes", "duration_s": "1", "finished_at": "",
    }


def experiment(min_pairs=100):
    base = Experiment.load(LAB_DIR / "experiments" / "exp001-doubled-nodes.json")
    return Experiment(**{**base.__dict__, "min_pairs_for_conclusion": min_pairs})


def make_rows(pair_scores):
    rows = []
    for pair, (a, b) in enumerate(pair_scores):
        rows += [row(pair, a), row(pair, b, color="bw")]
    return rows


def test_counts_scores_and_pairs():
    summary = report.summarize_rows(make_rows([(1.0, 0.5), (0.5, 0.5), (0.0, 1.0)]), experiment())
    assert (summary["wins"], summary["draws"], summary["losses"]) == (2, 3, 1)
    assert summary["games"] == 6 and summary["pairs"] == 3
    assert summary["score_pct"] == 58.33
    assert summary["pair_buckets"] == {"0": 0, "0.5": 0, "1": 2, "1.5": 1, "2": 0}


def test_small_samples_are_labelled_preliminary():
    summary = report.summarize_rows(make_rows([(1.0, 1.0)] * 5), experiment(min_pairs=100))
    assert summary["status"] == "preliminary"


def test_large_samples_are_not_labelled_preliminary():
    summary = report.summarize_rows(make_rows([(1.0, 0.5), (0.5, 0.0)] * 60), experiment(min_pairs=100))
    assert summary["status"] == "sufficient sample"
    assert summary["ci_method"] == "pentanomial"


def test_a_balanced_result_never_claims_a_difference():
    # Pair totals 1.5, 0.5, 1, 1, 1, 0.5, 1.5 average exactly one point per pair, with real variation.
    balanced = [(1.0, 0.5), (0.0, 0.5), (1.0, 0.0), (0.5, 0.5), (0.0, 1.0), (0.5, 0.0), (1.0, 0.5)] * 15
    summary = report.summarize_rows(make_rows(balanced), experiment())
    assert summary["score_pct"] == 50.0
    assert summary["elo_ci_low"] < 0 < summary["elo_ci_high"]
    assert summary["verdict"].startswith("No measurable difference")


def test_a_clear_gap_is_reported_as_significant():
    summary = report.summarize_rows(make_rows([(1.0, 1.0)] * 40 + [(1.0, 0.5)] * 10), experiment())
    assert summary["verdict"].startswith("The candidate measured stronger")


def test_failures_are_counted():
    rows = make_rows([(1.0, 0.0)] * 3)
    rows[0]["termination"] = "crash"
    rows[1]["termination"] = "illegal_move"
    rows[2]["termination"] = "timeout"
    summary = report.summarize_rows(rows, experiment())
    assert (summary["crashes"], summary["illegal_moves"], summary["timeouts"]) == (1, 1, 1)


def test_incomplete_pairs_are_excluded_from_pair_statistics():
    rows = make_rows([(1.0, 1.0)] * 3) + [row(99, 1.0)]
    summary = report.summarize_rows(rows, experiment())
    assert summary["pairs"] == 3 and summary["incomplete_pairs"] == 1


def test_speed_statistics_are_weighted_by_time():
    summary = report.summarize_rows(make_rows([(1.0, 0.5)] * 2), experiment())
    assert summary["baseline_stats"]["avg_nps"] == 1_000_000
    assert summary["candidate_stats"]["avg_nps"] == 1_500_000
    assert summary["baseline_stats"]["avg_depth"] == 10.0
    assert summary["candidate_stats"]["avg_depth"] == 11.0


def test_empty_results_are_handled():
    summary = report.summarize_rows([], experiment())
    assert summary["games"] == 0 and summary["elo_diff"] is None
    assert summary["verdict"] == "No games were completed."


def test_summary_is_json_serialisable_and_markdown_renders():
    rows = make_rows([(1.0, 0.5)] * 4)
    summary = report.summarize_rows(rows, experiment())
    json.dumps(summary)
    text = report.markdown(summary, report.opening_table(rows))
    assert "Verdict" in text and "preliminary" in text


def test_mirrored_pairs_are_flagged_as_uninformative():
    summary = report.summarize_rows(make_rows([(1.0, 0.0)] * 30), experiment())
    assert summary["status"] == "degenerate"
    assert summary["elo_ci_low"] is None and summary["elo_ci_high"] is None
    assert summary["verdict"].startswith("Not informative")
    assert summary["warnings"]


def test_normal_results_have_no_warnings():
    summary = report.summarize_rows(make_rows([(1.0, 0.5), (0.5, 0.0), (1.0, 1.0)] * 10), experiment())
    assert summary["warnings"] == []
    assert summary["status"] != "degenerate"


def test_a_near_sweep_has_an_open_ended_interval_not_a_fake_number():
    rows = make_rows([(1.0, 1.0)] * 38 + [(1.0, 0.5)] * 2)
    summary = report.summarize_rows(rows, experiment())
    assert summary["elo_ci_low"] is not None and summary["elo_ci_low"] > 300
    assert summary["elo_ci_high"] is None
    assert summary["ci_open_ended"] is True
    assert "open-ended" in report.markdown(summary, report.opening_table(rows))


def test_ordinary_results_are_not_open_ended():
    summary = report.summarize_rows(make_rows([(1.0, 0.5), (0.5, 0.0), (1.0, 1.0)] * 10), experiment())
    assert summary["ci_open_ended"] is False


def test_a_perfect_sweep_has_no_finite_point_estimate():
    rows = make_rows([(1.0, 1.0)] * 40)
    summary = report.summarize_rows(rows, experiment())
    assert summary["score_pct"] == 100.0
    assert summary["elo_diff"] is None
    assert summary["elo_ci_low"] > 200
    assert "off the scale" in report.markdown(summary, report.opening_table(rows))


def test_importing_device_games_builds_the_usual_report(tmp_path):
    """The phone writes the lab's CSV format; import-device must turn it into the standard summary."""
    import csv
    import json
    import shutil

    from enginelab.__main__ import main
    from enginelab.runner import CSV_FIELDS

    experiment_id = "exp-pytest-device-import"
    spec = json.loads((LAB_DIR / "experiments" / "exp041-device-vs-stockfish-1800.json").read_text())
    spec["id"] = experiment_id
    spec_path = tmp_path / f"{experiment_id}.json"
    spec_path.write_text(json.dumps(spec))

    import chess

    rows = make_rows([(1.0, 0.5), (0.5, 0.0), (1.0, 1.0)])
    for row in rows:
        row["opening_fen"] = chess.STARTING_FEN
    csv_path = tmp_path / "device.csv"
    with csv_path.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    meta_path = tmp_path / "device.meta.json"
    meta_path.write_text(json.dumps({"device": "Google Pixel 8"}))

    results = LAB_DIR / "results" / experiment_id
    try:
        assert main(["import-device", str(spec_path), "--csv", str(csv_path), "--meta", str(meta_path)]) == 0
        summary = json.loads((results / "summary.json").read_text())
        assert summary["games"] == 6 and summary["pairs"] == 3
        assert summary["meta"]["hardware"]["device"] == "Google Pixel 8"
        assert summary["meta"]["source"].startswith("on-device")
    finally:
        shutil.rmtree(results, ignore_errors=True)
