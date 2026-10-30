"""The JVM UCI front end for the app's built-in engines. Skipped until `./gradlew :uci-cli:installDist` has run."""

import os
import subprocess

import chess
import chess.engine
import pytest

from conftest import LAB_DIR

CLI = LAB_DIR.parent / "uci-cli" / "build" / "install" / "uci-cli" / "bin" / "uci-cli"
needs_cli = pytest.mark.skipif(not CLI.exists(), reason="run ./gradlew :uci-cli:installDist first")


def java_env():
    env = dict(os.environ)
    if "JAVA_HOME" not in env:
        env["JAVA_HOME"] = subprocess.run(["/usr/libexec/java_home", "-v", "17"], capture_output=True, text=True).stdout.strip()
    return env


@pytest.fixture()
def engine():
    eng = chess.engine.SimpleEngine.popen_uci([str(CLI)], env=java_env())
    yield eng
    eng.quit()


@needs_cli
@pytest.mark.parametrize("variant", ["baseline", "advanced"])
def test_both_variants_return_legal_moves(engine, variant):
    engine.configure({"Variant": variant, "Depth": 3})
    for fen in (chess.STARTING_FEN, "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"):
        board = chess.Board(fen)
        move = engine.play(board, chess.engine.Limit(depth=3)).move
        assert move in board.legal_moves


@needs_cli
def test_advanced_variant_finds_mate_and_reports_a_mate_score(engine):
    board = chess.Board("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1")
    result = engine.play(board, chess.engine.Limit(depth=5), info=chess.engine.INFO_ALL)
    assert result.move == chess.Move.from_uci("a1a8")
    assert result.info["score"].white().is_mate()


@needs_cli
def test_move_history_is_applied(engine):
    board = chess.Board()
    for uci in ("e2e4", "e7e5", "g1f3"):
        board.push_uci(uci)
    move = engine.play(board, chess.engine.Limit(depth=3)).move
    assert move in board.legal_moves


@needs_cli
def test_time_limit_is_respected(engine):
    import time

    board = chess.Board("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
    engine.play(board, chess.engine.Limit(depth=2))  # warm up
    started = time.perf_counter()
    engine.play(board, chess.engine.Limit(time=0.1))
    assert time.perf_counter() - started < 0.6


@needs_cli
def test_unknown_options_are_rejected(engine):
    with pytest.raises(chess.engine.EngineError):
        engine.configure({"NoSuchOption": 1})


@needs_cli
def test_ablation_switches_change_behaviour_end_to_end(engine):
    """Quiescence off makes the engine grab a pawn that is defended; on, it declines."""
    board = chess.Board("4k3/8/4p3/3p4/8/8/8/3QK3 w - - 0 1")
    grab = chess.Move.from_uci("d1d5")
    engine.configure({"Variant": "advanced", "Quiescence": False})
    assert engine.play(board, chess.engine.Limit(depth=1)).move == grab
    engine.configure({"Quiescence": True})
    assert engine.play(board, chess.engine.Limit(depth=1)).move != grab


@needs_cli
def test_every_switch_is_accepted(engine):
    engine.configure({"Variant": "advanced", "Quiescence": False, "TranspositionTable": False, "Reductions": False,
                      "OrderingHeuristics": False, "CheckExtension": False, "Evaluation": "legacy"})
    board = chess.Board()
    assert engine.play(board, chess.engine.Limit(depth=3)).move in board.legal_moves
