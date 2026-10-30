"""End to end checks against the real Stockfish binary. Skipped when it has not been fetched."""

import csv
import json

import chess
import chess.engine
import psutil

from enginelab import report, runner
from enginelab.config import EngineConfig, Experiment
from conftest import LAB_DIR, STOCKFISH, needs_stockfish


@needs_stockfish
def test_engine_starts_and_identifies_itself():
    with chess.engine.SimpleEngine.popen_uci([str(STOCKFISH)]) as engine:
        assert engine.id["name"].startswith("Stockfish")
        for option in ("Hash", "Threads", "UCI_Elo", "UCI_LimitStrength", "Skill Level"):
            assert option in engine.options


@needs_stockfish
def test_engine_returns_legal_moves_in_varied_positions():
    fens = [
        chess.STARTING_FEN,
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
    ]
    with chess.engine.SimpleEngine.popen_uci([str(STOCKFISH)]) as engine:
        for fen in fens:
            board = chess.Board(fen)
            result = engine.play(board, chess.engine.Limit(nodes=2000))
            assert result.move in board.legal_moves


@needs_stockfish
def test_unsupported_options_are_rejected_not_ignored():
    config = EngineConfig.from_json({"name": "bad", "options": {"Not An Option": 1}, "limits": {"nodes": 100}})
    try:
        runner.UciHandle(STOCKFISH, config, timeout=10)
    except chess.engine.EngineError:
        return
    raise AssertionError("an unknown option should raise EngineError")


@needs_stockfish
def test_search_can_be_stopped_and_the_engine_reused():
    with chess.engine.SimpleEngine.popen_uci([str(STOCKFISH)]) as engine:
        board = chess.Board()
        with engine.analysis(board, chess.engine.Limit(depth=60)) as analysis:
            for info in analysis:
                if info.get("depth", 0) >= 6:
                    break
            analysis.stop()
        assert engine.play(board, chess.engine.Limit(nodes=1000)).move in board.legal_moves


@needs_stockfish
def test_quitting_the_engine_leaves_no_process_behind():
    engine = chess.engine.SimpleEngine.popen_uci([str(STOCKFISH)])
    pid = engine.transport.get_pid()
    engine.quit()
    assert not psutil.pid_exists(pid) or psutil.Process(pid).status() == psutil.STATUS_ZOMBIE


@needs_stockfish
def test_a_tiny_experiment_runs_end_to_end_and_resumes(tmp_path, monkeypatch):
    spec = json.loads((LAB_DIR / "experiments" / "exp001-doubled-nodes.json").read_text())
    spec["id"] = "exp-pytest-smoke"
    spec["openings"].update({"source": "book", "count": 3})
    spec["baseline"]["limits"] = {"nodes": 300}
    spec["candidate"]["limits"] = {"nodes": 600}
    spec["workers"] = 2
    path = tmp_path / "exp-pytest-smoke.json"
    path.write_text(json.dumps(spec))
    results = LAB_DIR / "results" / "exp-pytest-smoke"
    try:
        runner.run_experiment(path, workers=2, fresh=True, log=lambda *_: None)
        rows = list(csv.DictReader((results / "games.csv").open()))
        assert len(rows) == 6
        assert {r["pair_id"] for r in rows} == {"0", "1", "2"}
        assert {r["white"] for r in rows} == {"candidate", "baseline"}
        assert all(r["termination"] not in {"crash", "illegal_move", "timeout"} for r in rows)

        runner.run_experiment(path, workers=2, log=lambda *_: None)   # resume: nothing left to play
        assert len(list(csv.DictReader((results / "games.csv").open()))) == 6

        summary = report.summarize(results, Experiment.load(path))
        assert summary["games"] == 6 and summary["pairs"] == 3 and summary["status"] == "preliminary"
        assert (results / "report.md").exists() and (results / "games.pgn").exists()
    finally:
        import shutil
        shutil.rmtree(results, ignore_errors=True)


@needs_stockfish
def test_a_rejected_option_does_not_leave_the_interpreter_hanging():
    """Regression: a failed configure() once leaked the engine process, so Python could never exit."""
    import subprocess
    import sys

    code = (
        "import chess.engine\n"
        "from enginelab.config import EngineConfig\n"
        "from enginelab.runner import UciHandle\n"
        "from pathlib import Path\n"
        f"cfg = EngineConfig.from_json({{'name': 'x', 'options': {{'Nope': 1}}, 'limits': {{'nodes': 10}}}})\n"
        f"try:\n    UciHandle(Path({str(STOCKFISH)!r}), cfg, 10)\nexcept chess.engine.EngineError:\n    print('rejected')\n"
    )
    done = subprocess.run([sys.executable, "-c", code], cwd=LAB_DIR, capture_output=True, text=True, timeout=30)
    assert "rejected" in done.stdout
