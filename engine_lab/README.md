# engine_lab

A small benchmark harness that compares two UCI engine configurations (a **baseline** and a
**candidate**) by playing paired games. It has no dependency on the Android app and runs on a
laptop.

## Quick start

```bash
cd engine_lab
../tools/fetch_stockfish.sh            # downloads and verifies the official Stockfish 19 binary
./run_experiment.sh exp001             # plays an experiment (matches by id prefix), writes results/
.venv/bin/python -m pytest             # harness tests
.venv/bin/python -m enginelab check exp001   # verifies the engine accepts every option
```

`run_experiment.sh` creates `.venv` on first use. Results are resumable: re-running an experiment
only plays the opening pairs that are missing. Use `--fresh` to start over, `--pairs N` for a short
trial and `--workers W` to change parallelism.

## What one experiment does

1. **Openings.** Balanced random openings are generated from a seed (8 random plies, kept only if
   the engine scores them within +/-100 cp at depth 8) and saved to `openings/`, so a run never
   depends on the filter engine again. A hand-written book of 24 named openings is also available.
2. **Pairs.** Every opening is played twice, once with the candidate as White and once as Black.
   The pair is the statistical unit.
3. **Controls.** Both engines get a fixed search budget (nodes, depth or time), one thread and the
   same hash unless the configurations differ on purpose. Node budgets make a run independent of
   machine load; time budgets do not (see Limitations).
4. **Safety.** Every move is checked against python-chess's rules. Crashes, illegal moves and
   timeouts lose the game, are counted, and the engine is restarted.
5. **Adjudication.** Draws by repetition, the fifty-move rule, stalemate and insufficient material
   are claimed as in the app. Clear wins are adjudicated when both engines evaluate the position
   beyond 1000 cp for six plies in a row. Games longer than 400 plies are drawn.
6. **Output** in `results/<experiment-id>/`: `games.csv` (one row per game), `summary.json`,
   `report.md`, `by_opening.csv`, `games.pgn`, `meta.json` (engine hash, hardware, seed, command).

## Statistics

* Score is the candidate's points divided by games.
* Elo difference is `-400 * log10(1/score - 1)`.
* The 95% interval uses the **pentanomial** method on opening-pair totals (0, 0.5, 1, 1.5, 2), which
  accounts for the two games of a pair sharing an opening. With fewer than 10 pairs the trinomial
  (per game) method is used instead, and the report says which one.
* "Likelihood of superiority" is the normal-approximation probability that the candidate is stronger.
* A result is called **preliminary** until the experiment's `min_pairs_for_conclusion` is reached,
  and a difference is only reported when the interval excludes zero.
* If a sample shows no variance at all, a half-count is added to each outcome so the interval is
  never reported as zero width.

## Writing an experiment

```json
{
  "id": "exp004-my-idea",
  "baseline":  {"name": "Default", "options": {"Threads": 1, "Hash": 16}, "limits": {"nodes": 10000}},
  "candidate": {"name": "My change", "options": {"Threads": 1, "Hash": 16, "Skill Level": 18}, "limits": {"nodes": 10000}},
  "openings": {"source": "random", "seed": 20261029, "count": 100, "plies": 8, "max_abs_cp": 100},
  "min_pairs_for_conclusion": 100
}
```

Options are validated by the engine at start-up: an unsupported option raises an error instead of
being silently ignored. A candidate can also point at a different binary with `"command"`, which is
how a patched Stockfish build is compared with the original.

## Benchmarking the app's own engines

`../uci-cli` is a small JVM program that speaks UCI and wraps the app's two built-in engines. After
`./gradlew :uci-cli:installDist`, point an engine config at it:

```json
{"name": "Built-in advanced, 25 ms", "command": "../uci-cli/build/install/uci-cli/bin/uci-cli",
 "options": {"Variant": "advanced", "Hash": 32}, "limits": {"time": 0.025}}
```

`Variant` is `baseline` (the original fixed-depth engine, set its depth with `Depth` and `go depth`) or
`advanced`. Compare engines at the *measured* average time per move (the report lists it), not at a
guessed budget.

## Files

| Path | Purpose |
|---|---|
| `enginelab/stats.py` | score, Elo, confidence intervals, likelihood of superiority |
| `enginelab/openings.py` | book and seeded random openings, validation |
| `enginelab/game.py` | plays one game, detects crashes, illegal moves, timeouts |
| `enginelab/runner.py` | parallel paired matches, resumable CSV |
| `enginelab/report.py` | summary, markdown report, PGN |
| `experiments/` | experiment definitions |
| `tests/` | pytest suite |
