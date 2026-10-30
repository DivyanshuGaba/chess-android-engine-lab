"""Opening positions. Every opening is played twice with colours swapped."""

from __future__ import annotations

import hashlib
import json
import random
from dataclasses import asdict, dataclass
from pathlib import Path

import chess


@dataclass(frozen=True)
class Opening:
    id: str
    fen: str
    moves: tuple[str, ...]      # UCI moves from the starting position
    source: str

    def to_json(self) -> dict:
        data = asdict(self)
        data["moves"] = list(self.moves)
        return data

    @staticmethod
    def from_json(data: dict) -> "Opening":
        return Opening(data["id"], data["fen"], tuple(data["moves"]), data.get("source", "file"))


# Hand-written, well-known lines (SAN). They are validated for legality at load time.
BOOK_LINES: dict[str, str] = {
    "ruy-lopez": "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6",
    "italian": "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6",
    "scotch": "e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nf6",
    "petroff": "e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4",
    "sicilian-najdorf": "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6",
    "sicilian-dragon": "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6",
    "sicilian-sveshnikov": "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5",
    "french-winawer": "e4 e6 d4 d5 Nc3 Bb4 e5 c5",
    "french-classical": "e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7",
    "caro-kann": "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5",
    "scandinavian": "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 Nf6",
    "pirc": "e4 d6 d4 Nf6 Nc3 g6 Nf3 Bg7",
    "alekhine": "e4 Nf6 e5 Nd5 d4 d6 Nf3 Bg4",
    "queens-gambit-declined": "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7",
    "queens-gambit-accepted": "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6",
    "slav": "d4 d5 c4 c6 Nf3 Nf6 Nc3 dxc4",
    "kings-indian": "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6",
    "nimzo-indian": "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O",
    "queens-indian": "d4 Nf6 c4 e6 Nf3 b6 g3 Bb7",
    "grunfeld": "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5",
    "english": "c4 e5 Nc3 Nf6 Nf3 Nc6 g3 d5",
    "reti": "Nf3 d5 c4 e6 g3 Nf6 Bg2 Be7",
    "london": "d4 d5 Bf4 Nf6 e3 e6 Nf3 c5",
    "dutch": "d4 f5 g3 Nf6 Bg2 e6 Nf3 Be7",
}


def book_openings() -> list[Opening]:
    openings = []
    for name, san_line in BOOK_LINES.items():
        board = chess.Board()
        moves = []
        for san in san_line.split():
            move = board.parse_san(san)  # raises if the line is illegal
            moves.append(move.uci())
            board.push(move)
        openings.append(Opening(name, board.fen(), tuple(moves), "book"))
    return openings


def random_openings(count: int, seed: int, plies: int = 8, *, scorer=None, max_abs_cp: int | None = 100) -> list[Opening]:
    """Reproducible random openings.

    A seeded generator plays ``plies`` random legal moves. When a ``scorer`` (fen -> centipawns
    from White's side) is supplied, openings that start clearly unbalanced are discarded, so that
    colour assignment and random luck matter less. The accepted list is written to disk by the
    caller, so later runs do not depend on the scorer.
    """
    rng = random.Random(seed)
    seen: set[str] = set()
    result: list[Opening] = []
    attempts = 0
    while len(result) < count:
        attempts += 1
        if attempts > count * 200:
            raise RuntimeError("Could not generate enough balanced openings; relax max_abs_cp")
        board = chess.Board()
        moves: list[str] = []
        for _ in range(plies):
            legal = list(board.legal_moves)
            if not legal:
                break
            move = rng.choice(legal)
            moves.append(move.uci())
            board.push(move)
        key = board.epd()
        if len(moves) < plies or board.is_game_over(claim_draw=True) or key in seen:
            continue
        if scorer is not None and max_abs_cp is not None and abs(scorer(board.fen())) > max_abs_cp:
            continue
        seen.add(key)
        result.append(Opening(f"rnd-{seed}-{len(result):04d}", board.fen(), tuple(moves), f"random seed={seed} plies={plies}"))
    return result


def save_openings(openings: list[Opening], path: Path) -> str:
    payload = {"openings": [o.to_json() for o in openings]}
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, indent=1)
    path.write_text(text)
    return hashlib.sha256(text.encode()).hexdigest()


def load_openings(path: Path) -> list[Opening]:
    data = json.loads(path.read_text())
    openings = [Opening.from_json(item) for item in data["openings"]]
    for opening in openings:
        board = chess.Board()
        for uci in opening.moves:
            move = chess.Move.from_uci(uci)
            if move not in board.legal_moves:
                raise ValueError(f"Opening {opening.id} contains an illegal move {uci}")
            board.push(move)
        if board.fen() != opening.fen:
            raise ValueError(f"Opening {opening.id} FEN does not match its moves")
    return openings


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()
