"""Experiment definitions. An experiment is a JSON file naming a baseline and a candidate."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

import chess.engine


@dataclass(frozen=True)
class EngineConfig:
    name: str
    options: dict[str, str | int | bool]
    limits: dict[str, float | int]
    command: str | None = None     # defaults to the experiment's engine binary
    jitter: float = 0.0            # random +/- fraction applied to nodes and time on every move (seeded)

    def chess_limit(self) -> chess.engine.Limit:
        allowed = {"nodes", "depth", "time"}
        unknown = set(self.limits) - allowed
        if unknown:
            raise ValueError(f"{self.name}: unknown limit keys {sorted(unknown)}; use nodes, depth or time")
        if not self.limits:
            raise ValueError(f"{self.name}: a search limit (nodes, depth or time) is required")
        return chess.engine.Limit(**self.limits)

    def describe_limits(self) -> str:
        text = ", ".join(f"{key} {value}" for key, value in self.limits.items())
        return f"{text} (jitter +/-{self.jitter:.0%})" if self.jitter else text

    @staticmethod
    def from_json(data: dict) -> "EngineConfig":
        config = EngineConfig(
            name=data["name"],
            options=dict(data.get("options", {})),
            limits=dict(data["limits"]),
            command=data.get("command"),
            jitter=float(data.get("jitter", 0.0)),
        )
        if not 0.0 <= config.jitter < 1.0:
            raise ValueError(f"{config.name}: jitter must be in [0, 1)")
        config.chess_limit()  # validates
        return config


@dataclass(frozen=True)
class Adjudication:
    cp: int = 1000        # both sides' evaluations must exceed this...
    plies: int = 6        # ...for this many consecutive plies. 0 disables adjudication.
    max_plies: int = 400  # longer games are scored as draws


@dataclass(frozen=True)
class OpeningsConfig:
    source: str = "random"   # "random" or "book"
    seed: int = 1
    count: int = 100
    plies: int = 8
    max_abs_cp: int = 100
    filter_depth: int = 8


@dataclass(frozen=True)
class Experiment:
    id: str
    title: str
    description: str
    hypothesis: str
    engine: str                       # path to the UCI engine binary, relative to engine_lab/
    baseline: EngineConfig
    candidate: EngineConfig
    openings: OpeningsConfig = field(default_factory=OpeningsConfig)
    adjudication: Adjudication = field(default_factory=Adjudication)
    workers: int = 4
    min_pairs_for_conclusion: int = 100
    move_timeout_s: float = 30.0

    @staticmethod
    def load(path: Path) -> "Experiment":
        data = json.loads(path.read_text())
        return Experiment(
            id=data["id"],
            title=data.get("title", data["id"]),
            description=data.get("description", ""),
            hypothesis=data.get("hypothesis", ""),
            engine=data.get("engine", "bin/stockfish-macos-universal"),
            baseline=EngineConfig.from_json(data["baseline"]),
            candidate=EngineConfig.from_json(data["candidate"]),
            openings=OpeningsConfig(**data.get("openings", {})),
            adjudication=Adjudication(**data.get("adjudication", {})),
            workers=data.get("workers", 4),
            min_pairs_for_conclusion=data.get("min_pairs_for_conclusion", 100),
            move_timeout_s=data.get("move_timeout_s", 30.0),
        )


def apply_jitter(limit: chess.engine.Limit, jitter: float, rng) -> chess.engine.Limit:
    """Scales the node and time budgets by a random factor in [1 - jitter, 1 + jitter].

    Fixed-node, single-threaded Stockfish is fully deterministic, so two identical engines would
    replay the same game with the colours swapped. Jitter makes them diverge while keeping the
    expected strength the same; the generator is seeded per game, so runs stay reproducible.
    """
    if not jitter:
        return limit
    factor = 1.0 + rng.uniform(-jitter, jitter)
    return chess.engine.Limit(
        nodes=max(1, round(limit.nodes * factor)) if limit.nodes else None,
        time=limit.time * factor if limit.time else None,
        depth=limit.depth,
    )
