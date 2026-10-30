"""Plays one game between two engines and records what happened, including failures."""

from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass, field
from typing import Protocol

import chess
import chess.engine

from .config import Adjudication
from .openings import Opening

MATE_CP = 10_000


class EngineLike(Protocol):
    def play(self, board: chess.Board, limit: chess.engine.Limit, *, game: object = None,
             info: chess.engine.Info = chess.engine.INFO_NONE) -> chess.engine.PlayResult: ...

    def rss_mb(self) -> float | None: ...


@dataclass
class SideStats:
    moves: int = 0
    depth_sum: int = 0
    depth_samples: int = 0
    nodes: int = 0
    engine_time_s: float = 0.0
    wall_time_s: float = 0.0
    peak_rss_mb: float = 0.0

    def add(self, info: chess.engine.InfoDict, wall_s: float) -> None:
        self.moves += 1
        self.wall_time_s += wall_s
        if "depth" in info:
            self.depth_sum += int(info["depth"])
            self.depth_samples += 1
        if "nodes" in info:
            self.nodes += int(info["nodes"])
        if "time" in info:
            self.engine_time_s += float(info["time"])


@dataclass
class GameOutcome:
    result: str                     # "1-0", "0-1" or "1/2-1/2"
    termination: str
    moves: list[str]
    stats: dict[str, SideStats]
    offender: str | None = None     # the engine that crashed, played illegally or timed out
    error: str | None = None
    white_scores: list[int] = field(default_factory=list)


FAILURES = {"crash", "illegal_move", "timeout"}


def _loser_result(offender_is_white: bool) -> str:
    return "0-1" if offender_is_white else "1-0"


def _adjudicate(scores: list[int], rule: Adjudication) -> str | None:
    if rule.plies <= 0 or len(scores) < rule.plies:
        return None
    recent = scores[-rule.plies:]
    if all(score >= rule.cp for score in recent):
        return "1-0"
    if all(score <= -rule.cp for score in recent):
        return "0-1"
    return None


def play_game(
    engines: dict[str, EngineLike],
    limits: dict[str, chess.engine.Limit],
    white: str,
    black: str,
    opening: Opening,
    adjudication: Adjudication,
    game_token: object,
) -> GameOutcome:
    board = chess.Board(opening.fen)
    moves: list[str] = []
    scores: list[int] = []
    stats = {name: SideStats() for name in (white, black)}

    while True:
        outcome = board.outcome(claim_draw=True)
        if outcome is not None:
            return GameOutcome(outcome.result(), outcome.termination.name.lower(), moves, stats, white_scores=scores)
        if len(moves) >= adjudication.max_plies:
            return GameOutcome("1/2-1/2", "max_plies", moves, stats, white_scores=scores)
        verdict = _adjudicate(scores, adjudication)
        if verdict is not None:
            return GameOutcome(verdict, "adjudicated_win", moves, stats, white_scores=scores)

        mover = white if board.turn == chess.WHITE else black
        started = time.perf_counter()
        try:
            played = engines[mover].play(board, limits[mover], game=game_token, info=chess.engine.INFO_ALL)
        except chess.engine.EngineTerminatedError as error:
            return _failed("crash", mover, mover == white, moves, stats, scores, str(error))
        except (chess.engine.EngineError, chess.IllegalMoveError, chess.InvalidMoveError) as error:
            return _failed("illegal_move", mover, mover == white, moves, stats, scores, str(error))
        except (TimeoutError, asyncio.TimeoutError) as error:
            return _failed("timeout", mover, mover == white, moves, stats, scores, repr(error))
        wall = time.perf_counter() - started

        move = played.move
        if move is None or not board.is_legal(move):
            return _failed("illegal_move", mover, mover == white, moves, stats, scores, f"returned {move}")

        stats[mover].add(played.info, wall)
        if move_count_is_sampled(len(moves)):
            rss = engines[mover].rss_mb()
            if rss:
                stats[mover].peak_rss_mb = max(stats[mover].peak_rss_mb, rss)
        score = played.info.get("score")
        if score is not None:
            scores.append(score.white().score(mate_score=MATE_CP))
        else:
            scores.clear()  # a missing evaluation breaks the adjudication streak
        board.push(move)
        moves.append(move.uci())


def move_count_is_sampled(ply: int) -> bool:
    return ply % 10 == 0


def _failed(kind: str, offender: str, offender_is_white: bool, moves, stats, scores, error: str) -> GameOutcome:
    return GameOutcome(_loser_result(offender_is_white), kind, moves, stats, offender, error, scores)
