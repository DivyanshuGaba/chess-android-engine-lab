"""Runs an experiment: paired games, several worker processes, resumable CSV output."""

from __future__ import annotations

import csv
import hashlib
import json
import platform
import random
import subprocess
import sys
import time
import uuid
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path

import chess
import chess.engine
import psutil

from . import __version__
from .config import EngineConfig, Experiment, apply_jitter
from .game import FAILURES, GameOutcome, play_game
from .openings import Opening, book_openings, file_sha256, load_openings, random_openings, save_openings

LAB_DIR = Path(__file__).resolve().parent.parent
ROOT = LAB_DIR.parent
NAMES = ("baseline", "candidate")

CSV_FIELDS = [
    "experiment_id", "game_id", "pair_id", "opening_id", "opening_fen", "white", "black",
    "result", "candidate_score", "termination", "plies", "moves_uci", "error",
] + [f"{prefix}_{field}" for prefix in ("b", "c") for field in
     ("moves", "depth_sum", "depth_samples", "nodes", "engine_time_s", "wall_time_s", "peak_rss_mb")] + [
    "engine_version", "time_control", "duration_s", "finished_at",
]


class UciHandle:
    """A running engine process with the options of one configuration applied."""

    def __init__(self, command: Path, config: EngineConfig, timeout: float):
        self.engine = chess.engine.SimpleEngine.popen_uci([str(command)], timeout=timeout)
        try:
            self.engine.configure(config.options)   # raises EngineError if the engine lacks an option
        except BaseException:
            # A half-built handle would leak the engine process, and python-chess's background thread
            # would then keep the interpreter from ever exiting.
            self.engine.close()
            raise
        self.config = config
        self._rng = random.Random(0)
        try:
            self._process = psutil.Process(self.engine.transport.get_pid())
        except Exception:
            self._process = None

    @property
    def name(self) -> str:
        return self.engine.id.get("name", "unknown")

    def reseed(self, seed: str) -> None:
        """Makes this engine's jitter reproducible for one game."""
        self._rng = random.Random(seed)

    def play(self, board, limit, *, game=None, info=chess.engine.INFO_NONE):
        return self.engine.play(board, apply_jitter(limit, self.config.jitter, self._rng), game=game, info=info)

    def rss_mb(self) -> float | None:
        try:
            return self._process.memory_info().rss / 1e6 if self._process else None
        except psutil.Error:
            return None

    def close(self) -> None:
        try:
            self.engine.quit()
        except Exception:
            self.engine.close()


def resolve_engine(experiment: Experiment) -> Path:
    path = Path(experiment.engine)
    return path if path.is_absolute() else LAB_DIR / path


def _outcome_row(experiment: Experiment, outcome: GameOutcome, opening: Opening, pair_id: int,
                 candidate_white: bool, engine_version: str, duration: float) -> dict:
    white, black = ("candidate", "baseline") if candidate_white else ("baseline", "candidate")
    result = outcome.result
    candidate_score = {"1-0": 1.0, "0-1": 0.0, "1/2-1/2": 0.5}[result]
    if not candidate_white:
        candidate_score = 1.0 - candidate_score
    row = {
        "experiment_id": experiment.id,
        "game_id": f"{pair_id:05d}-{'cw' if candidate_white else 'bw'}",
        "pair_id": pair_id,
        "opening_id": opening.id,
        "opening_fen": opening.fen,
        "white": white,
        "black": black,
        "result": result,
        "candidate_score": candidate_score,
        "termination": outcome.termination,
        "plies": len(outcome.moves),
        "moves_uci": " ".join(outcome.moves),
        "error": outcome.error or "",
        "engine_version": engine_version,
        "time_control": f"baseline {experiment.baseline.describe_limits()} | candidate {experiment.candidate.describe_limits()}",
        "duration_s": round(duration, 3),
        "finished_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    for prefix, name in (("b", "baseline"), ("c", "candidate")):
        side = outcome.stats[name]
        row.update({
            f"{prefix}_moves": side.moves, f"{prefix}_depth_sum": side.depth_sum,
            f"{prefix}_depth_samples": side.depth_samples, f"{prefix}_nodes": side.nodes,
            f"{prefix}_engine_time_s": round(side.engine_time_s, 4),
            f"{prefix}_wall_time_s": round(side.wall_time_s, 4),
            f"{prefix}_peak_rss_mb": round(side.peak_rss_mb, 1),
        })
    return row


def run_pair(experiment_path: str, opening_data: dict, pair_id: int) -> list[dict]:
    """Plays one opening twice, swapping colours. Runs inside a worker process."""
    experiment = Experiment.load(Path(experiment_path))
    opening = Opening.from_json(opening_data)
    command = resolve_engine(experiment)
    configs = {"baseline": experiment.baseline, "candidate": experiment.candidate}
    limits = {name: config.chess_limit() for name, config in configs.items()}
    rows: list[dict] = []
    handles: dict[str, UciHandle] = {}

    def start() -> None:
        for name, config in configs.items():
            handles[name] = UciHandle(resolve_command(command, config), config, experiment.move_timeout_s)

    def stop() -> None:
        for handle in handles.values():
            handle.close()
        handles.clear()

    start()
    try:
        version = handles["baseline"].name
        for candidate_white in (True, False):
            white, black = ("candidate", "baseline") if candidate_white else ("baseline", "candidate")
            for name, handle in handles.items():
                handle.reseed(f"{experiment.id}:{pair_id}:{candidate_white}:{name}")
            started = time.perf_counter()
            outcome = play_game(dict(handles), limits, white, black, opening, experiment.adjudication, uuid.uuid4())
            rows.append(_outcome_row(experiment, outcome, opening, pair_id, candidate_white, version,
                                     time.perf_counter() - started))
            if outcome.termination in FAILURES:
                stop()
                start()   # a crashed or stuck engine is replaced before the next game
    finally:
        stop()
    return rows


def resolve_command(default: Path, config: EngineConfig) -> Path:
    if not config.command:
        return default
    path = Path(config.command)
    return path if path.is_absolute() else LAB_DIR / path


def engine_score(command: Path, fen: str, depth: int) -> int:
    """White-relative centipawn score of a position, used to pick balanced random openings."""
    engine = chess.engine.SimpleEngine.popen_uci([str(command)])
    try:
        board = chess.Board(fen)
        info = engine.analyse(board, chess.engine.Limit(depth=depth))
        return info["score"].white().score(mate_score=10_000)
    finally:
        engine.quit()


def prepare_openings(experiment: Experiment, count_override: int | None = None) -> tuple[list[Opening], str]:
    cfg = experiment.openings
    count = count_override or cfg.count
    if cfg.source == "book":
        openings = book_openings()[:count]
        return openings, "book"
    path = LAB_DIR / "openings" / f"random-{cfg.seed}-{cfg.count}-{cfg.plies}.json"
    if not path.exists():
        command = resolve_engine(experiment)
        scorer = lambda fen: engine_score(command, fen, cfg.filter_depth)  # noqa: E731
        generated = random_openings(cfg.count, cfg.seed, cfg.plies, scorer=scorer, max_abs_cp=cfg.max_abs_cp)
        save_openings(generated, path)
    openings = load_openings(path)[:count]
    return openings, file_sha256(path)


def hardware_info() -> dict:
    brand = ""
    try:
        brand = subprocess.run(["sysctl", "-n", "machdep.cpu.brand_string"], capture_output=True, text=True).stdout.strip()
    except Exception:
        pass
    return {
        "platform": platform.platform(), "machine": platform.machine(), "cpu": brand or platform.processor(),
        "logical_cpus": psutil.cpu_count(), "memory_gb": round(psutil.virtual_memory().total / 1e9, 1),
        "python": sys.version.split()[0], "python_chess": chess.__version__,
    }


def read_rows(csv_path: Path) -> list[dict]:
    if not csv_path.exists():
        return []
    with csv_path.open(newline="") as handle:
        return list(csv.DictReader(handle))


def run_experiment(experiment_path: Path, *, pairs: int | None = None, workers: int | None = None,
                   fresh: bool = False, log=print) -> Path:
    experiment = Experiment.load(experiment_path)
    results_dir = LAB_DIR / "results" / experiment.id
    csv_path = results_dir / "games.csv"
    if fresh and results_dir.exists():
        for child in results_dir.iterdir():
            child.unlink()
    results_dir.mkdir(parents=True, exist_ok=True)

    command = resolve_engine(experiment)
    if not command.exists():
        raise FileNotFoundError(f"Engine binary not found: {command}. Run tools/fetch_stockfish.sh")

    openings, openings_ref = prepare_openings(experiment, pairs)
    done_pairs = {int(r["pair_id"]) for r in read_rows(csv_path)}
    todo = [(index, opening) for index, opening in enumerate(openings) if index not in done_pairs]
    log(f"{experiment.id}: {len(openings)} opening pairs, {len(done_pairs)} already done, {len(todo)} to play")

    meta = {
        "experiment_id": experiment.id,
        "started_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "lab_version": __version__,
        "engine_binary": str(command.relative_to(ROOT)) if command.is_relative_to(ROOT) else str(command),
        "engine_sha256": hashlib.sha256(command.read_bytes()).hexdigest(),
        "experiment_file": str(experiment_path),
        "experiment_sha256": file_sha256(experiment_path),
        "openings_ref": openings_ref,
        "hardware": hardware_info(),
        "workers": workers or experiment.workers,
        "command": " ".join(sys.argv),
    }
    (results_dir / "meta.json").write_text(json.dumps(meta, indent=2))

    new_file = not csv_path.exists()
    started = time.perf_counter()
    with csv_path.open("a", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS)
        if new_file:
            writer.writeheader()
        with ProcessPoolExecutor(max_workers=workers or experiment.workers) as pool:
            futures = {pool.submit(run_pair, str(experiment_path), opening.to_json(), index): index for index, opening in todo}
            finished = 0
            for future in as_completed(futures):
                for row in future.result():
                    writer.writerow(row)
                handle.flush()
                finished += 1
                if finished % 10 == 0 or finished == len(todo):
                    log(f"  {finished}/{len(todo)} pairs finished after {time.perf_counter() - started:.0f}s")

    meta["finished_at"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    (results_dir / "meta.json").write_text(json.dumps(meta, indent=2))
    return results_dir
