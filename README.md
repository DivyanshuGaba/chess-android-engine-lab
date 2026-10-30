# Chess for Android

A native Android chess game in Kotlin and Jetpack Compose. You can play a friend on one phone or play
the computer, which is either **Stockfish 19** (the official engine, talking UCI) or a built-in
minimax engine written for this project. The repository also contains `engine_lab`, an offline
benchmark harness that measures engine configurations with paired games and confidence intervals,
and an in-app **Engine lab** screen that shows live analysis next to the recorded results.

## Features

* Full rules: castling, en passant, promotion, check, checkmate, stalemate, fifty-move rule,
  threefold repetition, insufficient material
* Stockfish 19 opponent with three difficulty levels (Elo cap and search budget), or the built-in engine
  (its Hard level uses an improved search that beat the original engine in matched-time benchmarks)
* The engine's depth, evaluation, speed and principal variation shown after each computer move
* Chess clocks (10 minutes each), vibration feedback you can switch off, undo, resign, resume, game history
* Animated pieces, a fixed board for two players (the other player's panel faces them), a Learn section
  with engine-verified examples of every way a game can end
* Redesigned interface: bottom navigation (Play, Watch, Learn, History, Lab), light and dark themes
  (follows the system or set in Settings), a continue card with a board thumbnail, game history with
  thumbnails and result badges, an evaluation bar on the computer's analysis, and a one-tap rematch
* Engine lab screen: live position analysis, recorded benchmark results, recorded test counts, licence
* "Watch engines play": a spectator mode where any two engines play each other, with pause and speed control.
  It is labelled a demo, saves nothing and keeps no tallies, because a few games cannot measure strength

## Architecture

Unidirectional data flow with MVVM. The chess rules live in a pure-Kotlin domain layer with no Android
dependencies, so they are tested on the JVM.

```
ui/            Compose screens, ViewModels exposing StateFlow<UiState>, navigation
  game/ home/ history/ learn/ lab/ spectator/
domain/
  model/       Board, Square, Move, Game, GameRecord (immutable value types)
  engine/      ChessRules (legal moves), Fen, San
  ai/          ChessAi, MinimaxAi, ResilientAi (fallback), PvFormatter
  repository/  GameRepository, SettingsRepository
engine/        UCI client and Stockfish integration (see below)
uci-cli/       JVM UCI front end for the built-in engines (compiles the app's domain sources), used by the benchmarks
data/          Room database (3 migrations), DataStore settings, vibration
lab/           reads the benchmark/test JSON that the offline tools generate
di/            Hilt modules
engine_lab/    Python benchmark harness (independent of the app)
third_party/   Stockfish source, licence and checksums
tools/         fetch_stockfish.sh, export_dashboard_data.py, run_instrumented.sh, update_readme.py
```

Stack: Kotlin 2.1, Jetpack Compose (Material 3), Navigation, Hilt, Room, DataStore, coroutines and Flow,
KSP, Gradle version catalog.

## How Stockfish is integrated

* **Binary.** The official `stockfish-android-arm64-universal` release build is packaged as
  `lib/arm64-v8a/libstockfish.so`. Android extracts native libraries to a directory apps are allowed to
  execute from, so the app can start it as a child process (`useLegacyPackaging = true`). The "universal"
  build chooses the best CPU code path at run time, so it is safe on any 64-bit ARM phone.
* **Protocol.** `engine/UciEngine.kt` is a small UCI client over a `UciTransport` (stdin/stdout of the
  process). It sends `uci`, `isready`, `setoption`, `ucinewgame`, `position fen`, `go` and `stop`, parses
  `info` and `bestmove`, and finishes with `quit`. The same client is tested against a scripted fake engine
  and against the real binary.
* **Threads.** A search suspends on a channel fed by a reader thread, so it never blocks the main
  thread. The game ViewModel also runs it off the main dispatcher.
* **Lifecycle.** `SharedEngine` owns one process for the whole app, starts it on first use, serialises
  searches, and shuts it down after 45 s idle. Cancelling a search sends `stop` and drains the pending
  `bestmove`, so the next search starts clean; an engine that does not answer within its timeout is killed
  and restarted. If the engine process dies, stdin closes and Stockfish exits.
* **Safety net.** Every move the engine returns is validated by the app's own rules. If Stockfish is
  unavailable, crashes, times out or answers illegally, `ResilientAi` falls back to the built-in engine.
* **Difficulty.** Chosen through UCI options and a per-move budget:

| Level | `UCI_LimitStrength` / `UCI_Elo` | Budget per move |
|---|---|---|
| Easy | on, 1350 | 0.1 s |
| Medium | on, 1800 | 0.3 s |
| Hard | off (full strength) | 1.0 s |

The Engine lab screen additionally lets you analyse the current position to a fixed depth or time.
All of these are single-threaded with a 16 MB hash.

## What is, and is not, "AI" here

These are different things, and only the first two are in this project.

1. **AI integration.** Running Stockfish as a child process, speaking UCI, handling its lifecycle,
   cancellation and failures, validating its output, and presenting its analysis. This is what the app does.
2. **Engine configuration and benchmarking.** Choosing search budgets and strength settings and measuring
   the effect with a statistically sound match harness (`engine_lab`). The built-in minimax engine is
   original code (alpha-beta, move ordering, a small evaluation), but it is deliberately simple.
3. **Model training.** **Not done.** Stockfish's neural network (NNUE) was trained by the Stockfish project
   and is used here unchanged. No network was trained, fine-tuned or evaluated by this repository. Do not
   describe it as such.

## Build and run

Requirements: JDK 17, the Android SDK (API 36), Python 3.10+ for the lab.

```bash
tools/fetch_stockfish.sh          # downloads the official Stockfish 19 macOS binary (for the benchmarks) and checks its SHA-256
ANDROID_NDK_HOME=/path/to/android-ndk-r30 tools/build_stockfish_android.sh   # builds the 16 KB-aligned Android binary
./gradlew installDebug            # builds and installs on a connected arm64 phone
./gradlew :uci-cli:installDist    # only needed to benchmark the built-in engines
```

Without the Android binary the app still builds and runs; it then uses the built-in engine. (`tools/fetch_stockfish.sh --official-android` installs the official 4 KB-aligned binary instead, which is fine for local testing.) The APK is large
(about 94 MB) because Stockfish embeds its neural network.

## Tests

```bash
./gradlew testDebugUnitTest       # JVM tests: rules, perft, UCI client, Stockfish AI, ViewModels (includes the real engine on a Mac)
tools/run_instrumented.sh         # on a connected phone: real engine, cancellation, process cleanup, UI responsiveness, Compose UI
engine_lab/.venv/bin/python -m pytest   # harness tests (run engine_lab/run_experiment.sh once first to create the venv)
python3 tools/export_dashboard_data.py && python3 tools/update_readme.py   # refresh the app's Engine lab data and the tables below
```

`tools/run_instrumented.sh` installs the test APK next to the app and does **not** uninstall the app
afterwards, because Gradle's `connectedAndroidTest` would delete saved games.

<!-- TESTS:START -->
_Last recorded run: 2026-10-30 01:32 UTC._

| Suite | Tests | Failed | Skipped |
|---|---|---|---|
| Android JVM unit tests | 207 | 0 | 0 |
| Python engine_lab tests | 78 | 0 | 0 |
| Android instrumented tests (Pixel 8) | 23 | 0 | 0 |
<!-- TESTS:END -->

The on-device suite has 23 tests, all passing on an unlocked Pixel 8. The Compose UI tests need the screen awake and unlocked:
a run against a locked phone fails them with "No compose hierarchies found" while the engine tests still pass
(`tools/artifacts/instrumented-locked-screen-2026-10-30.txt`). Two of the 23 (`SpectatorSoakRun`, `OnDeviceCalibrationRun`)
are long runners that skip themselves unless started with their arguments.

Move generation is validated with perft (node counts of every legal move sequence to a fixed depth) on
the standard reference positions, which catches almost any rules bug.

## Reproducing the benchmarks

```bash
cd engine_lab
./run_experiment.sh exp001            # plays the experiment and writes results/exp001-*/report.md
```

See `engine_lab/README.md` for the method and for how to write your own experiment.

### Methodology

* The baseline and the candidate play every opening twice with colours swapped. Openings are 8 random plies
  filtered to be balanced (within +/-100 cp at depth 8), generated from a fixed seed and stored in the repo.
* Both engines run single-threaded with the same hash on the same machine, with a fixed node budget (which
  does not depend on machine load) or a fixed time per move.
* Results are scored per opening pair (pentanomial), which respects that the two games of a pair are
  correlated. Elo difference is shown with a 95% confidence interval; a difference is only claimed when the
  interval excludes zero, and an experiment is "preliminary" until its planned sample size is reached.
* Crashes, illegal moves and timeouts are counted per run, not hidden.

### Stability on the phone

`SpectatorSoakRun` (run with `-e soak true`) plays several full engine-vs-engine matches in a row through the real
spectator ViewModel on the Pixel 8. Latest run, phone on charge, 5 matches of built-in Hard against Stockfish Medium:
all five finished with a checkmate (62 to 125 plies), the Java heap stayed at 2 MB and native memory at 6 to 7 MB,
there was exactly one engine process while playing and none after shutdown, and the temperature stayed between 38.2
and 38.7 C with thermal status 0. The raw log is `tools/artifacts/soak-2026-10-29.txt`. That is a stability result,
not a strength result.

### Limitations

* Results come from a Mac (Apple M1), not from the phone. Time-based experiments depend on the speed of
  the machine and on other load; the node-based experiments do not.
* Fixed-node, single-threaded Stockfish is deterministic. Identical engines replay mirrored games, which
  is why the A/A control needed seeded jitter (see the notes under the results).
* Clear wins are adjudicated at 1000 cp for six plies, and games over 400 plies are drawn; both can bias
  results slightly.
* 100 opening pairs give a 95% interval about 35 Elo wide on each side (the A/A control below measured -12 to +58 around a true difference of zero), so differences smaller than that cannot be detected.
* Elo here is relative between two configurations in this harness. It is not a rating on any rating list.

## AI Performance Experiments

Update this section after every experiment: write the experiment file, run it, then run
`tools/export_dashboard_data.py` and `tools/update_readme.py`. The table is generated from the result
files and must not be edited by hand.

<!-- EXPERIMENTS:START -->
_Generated from `engine_lab/results/*/summary.json` (2026-10-30 01:32 UTC). Wins, draws and losses are from the candidate's point of view._

| Experiment | Baseline vs candidate | Games (pairs) | W / D / L | Score | Elo difference (95% CI) | Status |
|---|---|---|---|---|---|---|
| `exp000-aa-control` | Stockfish 19, 10k nodes vs Stockfish 19, 10k nodes (copy) | 200 (100) | 63 / 87 / 50 | 53.25% | +23 (-12 to +58, pentanomial) | sufficient sample |
| `exp001-doubled-nodes` | Stockfish 19, 10k nodes vs Stockfish 19, 20k nodes | 200 (100) | 96 / 91 / 13 | 70.75% | +153 (+123 to +187, pentanomial) | sufficient sample |
| `exp002-medium-vs-easy` | App Easy (Elo 1350, 0.1 s) vs App Medium (Elo 1800, 0.3 s) | 80 (40) | 77 / 1 / 2 | 96.88% | +596 (+458 to open-ended, pentanomial) | sufficient sample |
| `exp003-hard-vs-medium` | App Medium (Elo 1800, 0.3 s) vs App Hard (full strength, 1 s) | 80 (40) | 80 / 0 / 0 | 100.00% | off the scale (+530 to open-ended, pentanomial) | sufficient sample |
| `exp010-builtin-equal-time` | Built-in baseline (depth 4, as shipped Hard) vs Built-in advanced, 50 ms | 200 (100) | 193 / 7 / 0 | 98.25% | +700 (+604 to +920, pentanomial) | sufficient sample |
| `exp011-builtin-300ms` | Built-in baseline (depth 4, as shipped Hard) vs Built-in advanced, 300 ms | 100 (50) | 100 / 0 / 0 | 100.00% | off the scale (+569 to open-ended, pentanomial) | sufficient sample |
| `exp012-baseline-vs-stockfish-easy` | Stockfish 19, app Easy (Elo 1350, 0.1 s) vs Built-in baseline (depth 4, as shipped Hard) | 200 (100) | 107 / 22 / 71 | 59.00% | +63 (+18 to +111, pentanomial) | sufficient sample |
| `exp013-advanced-vs-stockfish-easy` | Stockfish 19, app Easy (Elo 1350, 0.1 s) vs Built-in advanced, 300 ms | 100 (50) | 99 / 0 / 1 | 99.00% | +798 (+608 to open-ended, pentanomial) | sufficient sample |
| `exp014-builtin-equal-time-25ms` | Built-in baseline (depth 4, as shipped Hard) vs Built-in advanced, 25 ms | 200 (100) | 187 / 13 / 0 | 96.75% | +590 (+515 to +715, pentanomial) | sufficient sample |
| `exp020-ablate-quiescence` | Without quiescence search, 50 ms vs Full improved engine, 50 ms | 200 (100) | 195 / 2 / 3 | 98.00% | +676 (+562 to +1072, pentanomial) | sufficient sample |
| `exp021-ablate-transposition-table` | Without the transposition table, 50 ms vs Full improved engine, 50 ms | 200 (100) | 109 / 30 / 61 | 62.00% | +85 (+41 to +132, pentanomial) | sufficient sample |
| `exp022-ablate-reductions` | Without late move reductions, 50 ms vs Full improved engine, 50 ms | 200 (100) | 81 / 45 / 74 | 51.75% | +12 (-30 to +55, pentanomial) | sufficient sample |
| `exp023-ablate-ordering-heuristics` | Without killer/history ordering, 50 ms vs Full improved engine, 50 ms | 200 (100) | 83 / 34 / 83 | 50.00% | -0 (-48 to +48, pentanomial) | sufficient sample |
| `exp024-ablate-check-extension` | Without check extensions, 50 ms vs Full improved engine, 50 ms | 200 (100) | 90 / 39 / 71 | 54.75% | +33 (-5 to +72, pentanomial) | sufficient sample |
| `exp025-ablate-evaluation` | With the original (material + centre) evaluation, 50 ms vs Full improved engine, 50 ms | 200 (100) | 124 / 29 / 47 | 69.25% | +141 (+94 to +194, pentanomial) | sufficient sample |
| `exp030-vs-stockfish-1800` | Stockfish 19, UCI_Elo 1800, 0.3 s vs Built-in Hard (improved engine), 0.3 s | 100 (50) | 61 / 11 / 28 | 66.50% | +119 (+52 to +196, pentanomial) | sufficient sample |
| `exp031-vs-stockfish-2200` | Stockfish 19, UCI_Elo 2200, 0.3 s vs Built-in Hard (improved engine), 0.3 s | 100 (50) | 27 / 14 / 59 | 34.00% | -115 (-189 to -50, pentanomial) | sufficient sample |
| `exp032-vs-stockfish-2600` | Stockfish 19, UCI_Elo 2600, 0.3 s vs Built-in Hard (improved engine), 0.3 s | 100 (50) | 5 / 17 / 78 | 13.50% | -323 (-438 to -246, pentanomial) | sufficient sample |
| `exp041-device-vs-stockfish-1800` | Stockfish 19, UCI_Elo 1800, 0.3 s (on the Pixel 8) vs Built-in Hard, 0.3 s (on the Pixel 8) | 60 (30) | 13 / 12 / 35 | 31.67% | -134 (-221 to -60, pentanomial) | sufficient sample |
| `exp042-device-vs-stockfish-2200` | Stockfish 19, UCI_Elo 2200, 0.3 s (on the Pixel 8) vs Built-in Hard, 0.3 s (on the Pixel 8) | 60 (30) | 9 / 8 / 43 | 21.67% | -223 (-356 to -132, pentanomial) | sufficient sample |
| `exp044-device-vs-stockfish-1350` | Stockfish 19, UCI_Elo 1350, 0.3 s (on the Pixel 8) vs Built-in Hard, 0.3 s (on the Pixel 8) | 60 (30) | 56 / 2 / 2 | 95.00% | +512 (+385 to +1124, pentanomial) | sufficient sample |
| `exp045-device-vs-stockfish-1500` | Stockfish 19, UCI_Elo 1500, 0.3 s (on the Pixel 8) vs Built-in Hard, 0.3 s (on the Pixel 8) | 60 (30) | 28 / 7 / 25 | 52.50% | +17 (-59 to +96, pentanomial) | sufficient sample |

* **A/A control: identical strength, jittered budgets**: No measurable difference: the 95% interval includes zero at this sample size.
  * Speed: baseline 862,145 nodes/s at depth 12.82, candidate 858,309 nodes/s at depth 12.93; peak engine memory 372 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Positive control: twice the search effort**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 926,204 nodes/s at depth 12.97, candidate 925,076 nodes/s at depth 14.42; peak engine memory 371 MB. Crashes 0, illegal moves 0, timeouts 0.
* **App difficulty ladder: Medium against Easy**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 870,880 nodes/s at depth 18.96, candidate 832,232 nodes/s at depth 17.28; peak engine memory 353 MB. Crashes 0, illegal moves 0, timeouts 0.
* **App difficulty ladder: Hard against Medium**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 793,778 nodes/s at depth 21.32, candidate 779,414 nodes/s at depth 30.15; peak engine memory 264 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Built-in engine: improved search at 50 ms (about twice the baseline's time)**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 515,827 nodes/s at depth 4.0, candidate 681,474 nodes/s at depth 4.48; peak engine memory 136 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Built-in engine: improved search with the app's 0.3 s budget**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 582,474 nodes/s at depth 4.0, candidate 783,831 nodes/s at depth 6.08; peak engine memory 132 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Calibration: original built-in engine against Stockfish Easy**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 1,085,418 nodes/s at depth 17.3, candidate 570,480 nodes/s at depth 4.0; peak engine memory 377 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Calibration: improved built-in engine against Stockfish Easy**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 998,529 nodes/s at depth 18.18, candidate 744,737 nodes/s at depth 5.9; peak engine memory 358 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Built-in engine: improved search at matched wall-clock time**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 487,040 nodes/s at depth 4.0, candidate 620,896 nodes/s at depth 3.72; peak engine memory 139 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Quiescence search**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 446,168 nodes/s at depth 4.84, candidate 720,221 nodes/s at depth 4.53; peak engine memory 181 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Transposition table**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 848,583 nodes/s at depth 4.48, candidate 802,162 nodes/s at depth 5.27; peak engine memory 186 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Late move reductions**: No measurable difference: the 95% interval includes zero at this sample size.
  * Speed: baseline 839,487 nodes/s at depth 4.71, candidate 790,030 nodes/s at depth 5.27; peak engine memory 189 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Killer and history move ordering**: No measurable difference: the 95% interval includes zero at this sample size.
  * Speed: baseline 904,095 nodes/s at depth 5.18, candidate 801,317 nodes/s at depth 5.34; peak engine memory 187 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Check extension**: No measurable difference: the 95% interval includes zero at this sample size.
  * Speed: baseline 783,202 nodes/s at depth 5.62, candidate 792,574 nodes/s at depth 5.24; peak engine memory 193 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Ablation: Piece-square evaluation**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 800,913 nodes/s at depth 5.23, candidate 782,654 nodes/s at depth 5.11; peak engine memory 186 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Calibration: built-in Hard against Stockfish at UCI_Elo 1800**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 1,169,078 nodes/s at depth 18.88, candidate 781,532 nodes/s at depth 6.61; peak engine memory 377 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Calibration: built-in Hard against Stockfish at UCI_Elo 2200**: The candidate measured weaker than the baseline; the 95% interval excludes zero.
  * Speed: baseline 1,177,377 nodes/s at depth 18.0, candidate 781,074 nodes/s at depth 6.78; peak engine memory 375 MB. Crashes 0, illegal moves 0, timeouts 0.
* **Calibration: built-in Hard against Stockfish at UCI_Elo 2600**: The candidate measured weaker than the baseline; the 95% interval excludes zero.
  * Speed: baseline 1,147,422 nodes/s at depth 17.61, candidate 770,583 nodes/s at depth 6.58; peak engine memory 375 MB. Crashes 0, illegal moves 0, timeouts 0.
* **On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1800**: The candidate measured weaker than the baseline; the 95% interval excludes zero.
  * Speed: baseline 264,650 nodes/s at depth 16.0, candidate 30,682 nodes/s at depth 3.55; peak engine memory 0 MB. Crashes 0, illegal moves 0, timeouts 0.
* **On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 2200**: The candidate measured weaker than the baseline; the 95% interval excludes zero.
  * Speed: baseline 249,833 nodes/s at depth 18.14, candidate 30,110 nodes/s at depth 3.38; peak engine memory 0 MB. Crashes 0, illegal moves 0, timeouts 0.
* **On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1350**: The candidate measured stronger than the baseline; the 95% interval excludes zero.
  * Speed: baseline 212,239 nodes/s at depth 16.07, candidate 28,459 nodes/s at depth 3.28; peak engine memory 0 MB. Crashes 0, illegal moves 0, timeouts 0.
* **On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1500**: No measurable difference: the 95% interval includes zero at this sample size.
  * Speed: baseline 276,409 nodes/s at depth 16.28, candidate 28,535 nodes/s at depth 3.51; peak engine memory 0 MB. Crashes 0, illegal moves 0, timeouts 0.
<!-- EXPERIMENTS:END -->

### Notes on these results

These notes are written by hand; the table above is generated. Numbers are from one Apple M1 Mac.

* **No improvement to Stockfish is claimed.** Nothing here makes Stockfish stronger. The experiments
  validate the harness (controls) and measure the app's own difficulty settings.
* **A/A control (`exp000`).** The first version used two identical, deterministic configurations. All 100
  pairs scored exactly 1-1, because single-threaded fixed-node Stockfish replays the same game with the
  colours swapped, so a 50% score was guaranteed and the run tested nothing. That was a flaw in my
  design, not a result. The harness now detects and labels that case as degenerate, and the control uses
  seeded +/-10% node jitter instead. With jitter the result is 53.2% (+23 Elo, 95% CI -12 to +58): the
  interval includes zero, so no colour or opening bias is visible at this resolution. That is not proof
  that no bias exists.
* **Positive control (`exp001`).** Giving the candidate twice the nodes (20k against 10k) measured
  +153 Elo (95% CI +123 to +187) over 200 games. A harness that cannot see this would be useless, so it
  is evidence that real differences are detected.
* **Difficulty ladder (`exp002`, `exp003`).** The app's Medium beat Easy 77-1-2 and Hard beat Medium
  80-0-0 over 40 opening pairs each. The upper end of those intervals is open-ended because the score
  reaches the edge, so the data only supports "at least +458 Elo" and "at least +530 Elo". The levels are
  clearly separated; they are not calibrated to any rating scale. These runs use time budgets on a
  Mac, so on a phone, which searches fewer nodes per second, the gaps will be smaller.
* **Engine memory.** The engine process peaked at about 264 to 372 MB resident on the Mac, mostly its
  embedded network and hash. That is worth knowing before putting it on small phones.
* **What was not measured:** playing strength on the phone itself, battery use, and any effect of Hash or
  Threads settings. The fifty-move and threefold draw rules in these games follow python-chess, not the app.

* **Improving the built-in engine (`exp010`, `exp011`, `exp012`, `exp013`, `exp014`).** This is the only real
  improvement in the project. The original engine (fixed-depth negamax with a crude evaluation) is kept
  unchanged as the baseline (`MinimaxAi`), and a new `Searcher` was written next to it: iterative deepening,
  a transposition table (Zobrist hashing), quiescence search, killer and history move ordering, late move
  reductions, repetition detection, and a piece-square-table evaluation with a separate endgame king table.
  The harness plays both through a JVM UCI front end (`uci-cli`) that compiles the app's own domain sources,
  so what is measured is the code the app runs.
  * **Matched time (`exp014`):** the candidate's average move time was 24.9 ms against the baseline's
    23.8 ms, and it won 187 games, drew 13 and lost none: +590 Elo (95% CI +515 to +715). It reached only
    depth 3.7 on average, so the gain comes from the evaluation and from not misjudging captures
    (quiescence), not from searching deeper.
  * **Not matched time (`exp010`):** my first "equal-time" run was not equal. The baseline averaged 21.6 ms per
    move and the candidate 44.8 ms. It scored +700 Elo, but that overstates the gain; `exp014` is the fair
    figure. I kept `exp010` in the table with a corrected title instead of deleting it.
  * **At the app's 0.3 s budget (`exp011`):** 100 games, 100 wins, so only a lower bound (at least +569 Elo).
* **Ablation: which technique does the work (`exp020` to `exp025`).** Each run plays the full improved engine
  against the same engine with exactly one thing switched off, both at 50 ms per move, 100 opening pairs
  (200 games). A positive number means the technique helps.

  | Technique removed from the full engine | Elo the technique adds (95% CI) | Reading |
  |---|---|---|
  | Quiescence search | +676 (+562 to +1072) | By far the largest effect |
  | Piece-square evaluation (back to the original evaluation) | +141 (+94 to +194) | Clear |
  | Transposition table | +85 (+41 to +132) | Clear |
  | Check extension | +33 (-5 to +72) | Suggestive, not established |
  | Late move reductions | +12 (-30 to +55) | No measurable difference |
  | Killer and history move ordering | 0 (-48 to +48) | No measurable difference |

  * Quiescence search alone accounts for most of the gain over the original engine. The original engine's weakness
    was the horizon effect, not a lack of depth: the improved engine reached only depth 3.7 at matched time.
  * "No measurable difference" is not "no effect". With 100 pairs the interval is about 45 Elo either way, so
    late move reductions, move ordering and check extensions could still be worth tens of Elo (or cost some). They
    are most useful at depths this 50 ms budget barely reaches, which is also why I left them in the engine.
  * Each run removes one thing from the full engine (leave-one-out), so interactions between techniques are not
    separated, and the effects do not add up to the total.

* **Calibration ladder against Stockfish (`exp030`, `exp031`, `exp032`).** The built-in Hard level (improved
  engine, 0.3 s per move) against Stockfish limited by `UCI_Elo`, also at 0.3 s per move, 50 opening pairs each:

  | Stockfish rung | Built-in Hard score | Elo difference (95% CI) |
  |---|---|---|
  | UCI_Elo 1800 | 66.5% | +119 (+52 to +196) |
  | UCI_Elo 2200 | 34.0% | -115 (-189 to -50) |
  | UCI_Elo 2600 | 13.5% | -323 (-438 to -246) |

  The built-in engine beats the 1800 rung, loses to the 2200 rung, and loses clearly to 2600. Its break-even
  point on this ladder therefore lies between the two lower rungs; a straight-line interpolation of the two
  scores puts it near 2000, but that is a rough placement, not a rating. `UCI_Elo` is calibrated by the
  Stockfish project for much longer time controls and for its own hardware, so treat the rungs as a ladder of
  increasingly strong opponents, not as ratings the built-in engine can be compared with.

* **The same calibration on the phone (`exp044`, `exp045`, `exp041`, `exp042`).** The Mac ladder above overstated how
  strong the built-in Hard level is for a player, so the match was replayed on the Pixel 8 itself, with the app's real
  built-in Hard engine (a fresh search table every move, exactly as the app plays it) against Stockfish at the same
  `UCI_Elo` rungs and the same 0.3 s per move, 30 opening pairs (60 games) per rung, with the phone on charge:

  | Stockfish rung | Built-in Hard score on the phone | Elo difference (95% CI) | On the Mac |
  |---|---|---|---|
  | UCI_Elo 1350 | 95.0% | +512 (+385 to +1124) | not run |
  | UCI_Elo 1500 | 52.5% | +17 (-59 to +96) | not run |
  | UCI_Elo 1800 | 31.7% | -134 (-221 to -60) | 66.5% |
  | UCI_Elo 2200 | 21.7% | -223 (-356 to -132) | 34.0% |

  On the phone the built-in engine is level with the 1500 rung; on the Mac it was level with a rung near 2000. The
  2600 rung was not played on the phone (it would only have added losses). The cause is speed: the built-in engine
  averaged about 29k nodes/s and reached depth 3.4 in 0.3 s on the phone, against 781k nodes/s and depth 6.7 on the
  Mac, roughly 27 times slower, while Stockfish was only 4 to 5 times slower (212k to 276k nodes/s on the phone against
  1.17M on the Mac). Two things differ between the runs and I did not separate them: the CPU (and the Android runtime
  against HotSpot), and the search table (the Mac harness kept a 32 MB table across moves; the app allocates a fresh
  small one per move). Treat "about 1500 on this ladder" as the figure for what a player faces, and the 27x slowdown as
  the biggest remaining opportunity.

  * **Calibration against Stockfish (`exp012`, `exp013`):** the original built-in engine scored 59.0% against
    Stockfish limited to 1350 Elo at 0.1 s (+63 Elo, 95% CI +18 to +111), and the improved engine at 0.3 s scored
    99% against the same opponent. These say the built-in Hard level is stronger than Stockfish's Easy preset
    in this harness. `UCI_Elo` is calibrated for much longer time controls, so do not read 1350 as a rating
    for the built-in engine.
  * **Limits of this result:** the baseline is my own earlier engine and is weak, so large margins are
    expected. Several changes were made together and were not tested one at a time (no ablation), so the gain
    cannot be attributed to any single technique. All runs are on one Mac; the phone is slower, which the
    time-limited search absorbs by searching less deep. The app now uses the improved engine for the
    built-in **Hard** level only; Easy and Medium are unchanged.

### Ideas for the next experiments

* Candidate with a patched or differently built Stockfish (keep the original source, document the patch
  under `third_party/stockfish/patches/`, and give the candidate its own `"command"`).
* Time-per-move versus strength on the phone itself (needs an on-device runner).
* Repeat the ablation with 400 opening pairs to resolve effects of about 25 Elo, and at a longer time control,
  where late move reductions and move ordering are expected to matter more.
* Calibrate the built-in Easy and Medium levels against Stockfish rungs the same way as Hard.
* Isolate the phone's CPU from the app's fresh-table-per-move choice (run the Mac match with a fresh table too).
* Make the built-in engine faster on Android: fewer allocations, a faster move generator.

## Licensing

Stockfish 19 is bundled unmodified and is licensed under the **GNU GPL version 3**. Its licence, authors,
exact version, checksums and complete corresponding source are in `third_party/stockfish/`, and the licence
text is shown in the app. See `THIRD_PARTY_NOTICES.md` for what that means when you distribute the app, and
for the one decision that is yours: which licence to give your own code. This repository does not choose one
for you, and none of this is legal advice.

## Known limitations and future work

* **Stockfish's 16 KB alignment is fixed, with one trade-off.** The official Android binary is not 16 KB
  page-aligned (Android shows a compatibility warning and the Play Store rejects it), so the app ships a build made
  from the unmodified source with `tools/build_stockfish_android.sh` and one linker flag. Stockfish's own `bench`
  reports identical node counts (2,497,913) for the official binary and for both rebuilds on the Pixel 8, which shows
  the search behaves identically, and the rebuild was 4% faster (656k vs 630k nodes/s). I ship the `armv8` build;
  the `armv8-dotprod` build was a further 16% faster (762k nodes/s) but crashes on phones without the dot-product
  instructions, so using it needs a CPU check and a second binary (about 80 MB more). Not done.
  Phones older than Android 10 use the built-in engine, because the binary is built for API 29.
* **Most benchmarks were measured on a Mac.** The ablation and the Mac calibration ran there. Only the calibration
  against Stockfish was repeated on the phone (see above), where the built-in engine is about 27 times slower in raw
  speed. The ablation's ranking of techniques was not re-measured on the phone; transposition tables and reductions
  matter more when the search is shallow, so their share may differ.
* **The built-in engine is slow on Android.** About 29k nodes/s against 781k on the Mac. Profiling its allocation
  behaviour (a new board per move) and its move generator is the obvious next step.
* The app's clock has no increment and does not treat "opponent cannot mate" as a draw on timeout.
* The engine sees a position, not its history, so repetition is tracked by the app, not by Stockfish.
* No opening book or endgame tablebases are used.
* The built-in Easy and Medium levels still use the original loose minimax, and the improved engine's
  gain has not been ablated.

## Talking about this project honestly

* "I integrated Stockfish into an Android app over UCI, with process lifecycle management, cancellation,
  failure fallback and validation against my own rules engine."
* "I built an offline harness that compares engine configurations with paired games, a pentanomial Elo
  confidence interval and failure detection, and I used controls to check it: a positive control that
  must show a gain, and an A/A control that exposed a flaw in my own first design."
* Not true, so do not say it: that you trained or fine-tuned a neural network, or that you improved
  Stockfish.
