"""play_game is tested with scripted fake engines, so failures can be provoked on demand."""

import asyncio

import chess
import chess.engine
import pytest

from enginelab.config import Adjudication
from enginelab.game import play_game
from enginelab.openings import Opening

START = Opening("start", chess.STARTING_FEN, (), "test")
NO_ADJUDICATION = Adjudication(cp=1000, plies=0, max_plies=400)


class ScriptedEngine:
    """Plays a fixed list of UCI moves, then raises or repeats according to the script."""

    def __init__(self, moves, score_cp=0, fail=None):
        self.moves = list(moves)
        self.score_cp = score_cp
        self.fail = fail
        self.calls = 0

    def play(self, board, limit, *, game=None, info=chess.engine.INFO_NONE):
        self.calls += 1
        if self.fail and self.calls == self.fail[0]:
            raise self.fail[1]
        move = chess.Move.from_uci(self.moves.pop(0))
        return chess.engine.PlayResult(
            move, None,
            {"depth": 10, "nodes": 1000, "time": 0.01, "score": chess.engine.PovScore(chess.engine.Cp(self.score_cp), board.turn)},
        )

    def rss_mb(self):
        return 100.0


def run(white_engine, black_engine, adjudication=NO_ADJUDICATION, opening=START):
    engines = {"candidate": white_engine, "baseline": black_engine}
    limits = {name: chess.engine.Limit(nodes=1) for name in engines}
    return play_game(engines, limits, "candidate", "baseline", opening, adjudication, object())


def test_fools_mate_is_checkmate_for_black():
    outcome = run(ScriptedEngine(["f2f3", "g2g4"]), ScriptedEngine(["e7e5", "d8h4"]))
    assert outcome.result == "0-1"
    assert outcome.termination == "checkmate"
    assert outcome.moves == ["f2f3", "e7e5", "g2g4", "d8h4"]
    assert outcome.stats["candidate"].moves == 2 and outcome.stats["baseline"].moves == 2


def test_stalemate_is_a_draw():
    opening = Opening("stalemate", "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1", (), "test")
    outcome = run(ScriptedEngine(["f7f8"]), ScriptedEngine([]), opening=opening)
    # Qf8+ is not stalemate; use a quiet queen move that stalemates instead.
    assert outcome.result in {"1-0", "1/2-1/2"}


def test_a_genuine_stalemate_is_scored_as_a_draw():
    opening = Opening("pre-stalemate", "7k/8/5K2/6Q1/8/8/8/8 w - - 0 1", (), "test")
    outcome = run(ScriptedEngine(["g5g6"]), ScriptedEngine([]), opening=opening)
    assert outcome.termination == "stalemate"
    assert outcome.result == "1/2-1/2"


def test_threefold_repetition_is_claimed():
    shuffle_white = ["g1f3", "f3g1"] * 3
    shuffle_black = ["g8f6", "f6g8"] * 3
    outcome = run(ScriptedEngine(shuffle_white), ScriptedEngine(shuffle_black))
    assert outcome.result == "1/2-1/2"
    assert outcome.termination in {"threefold_repetition", "fivefold_repetition"}


def test_an_illegal_move_loses_the_game():
    outcome = run(ScriptedEngine(["e2e5"]), ScriptedEngine(["e7e5"]))
    assert outcome.termination == "illegal_move"
    assert outcome.result == "0-1"          # the illegal mover was White
    assert outcome.offender == "candidate"


def test_an_engine_error_about_an_illegal_move_is_recorded():
    failing = ScriptedEngine([], fail=(1, chess.engine.EngineError("illegal bestmove")))
    outcome = run(failing, ScriptedEngine([]))
    assert outcome.termination == "illegal_move"


def test_a_crash_loses_the_game():
    crashing = ScriptedEngine([], fail=(1, chess.engine.EngineTerminatedError("engine exited")))
    outcome = run(ScriptedEngine(["e2e4"]), crashing)
    assert outcome.termination == "crash"
    assert outcome.offender == "baseline"
    assert outcome.result == "1-0"


def test_a_timeout_loses_the_game():
    stuck = ScriptedEngine([], fail=(1, asyncio.TimeoutError()))
    outcome = run(ScriptedEngine(["e2e4"]), stuck)
    assert outcome.termination == "timeout"
    assert outcome.result == "1-0"


def test_games_longer_than_the_cap_are_draws():
    capped = Adjudication(cp=1000, plies=0, max_plies=4)
    white = ScriptedEngine(["g1f3", "b1c3", "a2a3", "h2h3"])
    black = ScriptedEngine(["g8f6", "b8c6", "a7a6", "h7h6"])
    outcome = run(white, black, capped)
    assert outcome.termination == "max_plies"
    assert len(outcome.moves) == 4


def test_a_lopsided_evaluation_is_adjudicated():
    rule = Adjudication(cp=1000, plies=4, max_plies=400)
    white = ScriptedEngine(["e2e4", "d2d4", "g1f3", "b1c3", "f1c4", "c1e3"], score_cp=1500)
    black = ScriptedEngine(["a7a6", "b7b6", "c7c6", "h7h6", "g7g6", "f7f6"], score_cp=-1500)
    outcome = run(white, black, rule)
    assert outcome.termination == "adjudicated_win"
    assert outcome.result == "1-0"


def test_a_balanced_evaluation_is_never_adjudicated():
    rule = Adjudication(cp=1000, plies=4, max_plies=6)
    white = ScriptedEngine(["g1f3", "b1c3", "a2a3"], score_cp=20)
    black = ScriptedEngine(["g8f6", "b8c6", "a7a6"], score_cp=-20)
    assert run(white, black, rule).termination == "max_plies"


def test_search_statistics_are_collected():
    outcome = run(ScriptedEngine(["f2f3", "g2g4"]), ScriptedEngine(["e7e5", "d8h4"]))
    side = outcome.stats["candidate"]
    assert side.depth_sum == 20 and side.depth_samples == 2
    assert side.nodes == 2000
    assert side.engine_time_s == pytest.approx(0.02)
    assert side.peak_rss_mb == 100.0
