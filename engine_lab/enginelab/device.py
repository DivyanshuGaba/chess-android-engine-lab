"""Helpers for games played on a phone by the on-device runner."""

from __future__ import annotations

import csv
from pathlib import Path

from .openings import Opening


def strip_opening_moves(rows: list[dict], openings: list[Opening]) -> list[dict]:
    """The phone replays the whole game from the start position, so its move lists begin with the opening.

    The lab's format holds only the moves played after the opening (the PGN starts from the opening position),
    so the opening prefix is removed here and the ply count is reduced to match. Rows already in the lab's
    format are left unchanged, which makes the function safe to apply twice.
    """
    by_id = {opening.id: opening for opening in openings}
    fixed = []
    for row in rows:
        opening = by_id.get(row["opening_id"])
        moves = row["moves_uci"].split()
        if opening is not None and tuple(moves[: len(opening.moves)]) == opening.moves:
            row = {**row, "moves_uci": " ".join(moves[len(opening.moves):]), "plies": str(len(moves) - len(opening.moves))}
        fixed.append(row)
    return fixed


def read_device_csv(path: Path) -> tuple[list[str], list[dict]]:
    """Reads the phone's CSV, repairing rows where a comma inside a text field split it into extra columns.

    The first runs of the on-device runner wrote "Stockfish 19 and built-in advanced, on device" unquoted, which
    shifts the last four columns of every row by one. Only `engine_version` can contain such a comma, so the
    surplus fields are joined back into it. Well-formed files pass through unchanged.
    """
    with path.open(newline="") as handle:
        raw = list(csv.reader(handle))
    header, body = raw[0], raw[1:]
    anchor = header.index("engine_version")
    rows = []
    for fields in body:
        surplus = len(fields) - len(header)
        if surplus > 0:
            fields = fields[:anchor] + [",".join(fields[anchor:anchor + surplus + 1])] + fields[anchor + surplus + 1:]
        rows.append(dict(zip(header, fields)))
    return header, rows
