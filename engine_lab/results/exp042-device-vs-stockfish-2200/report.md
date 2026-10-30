# On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 2200

Experiment `exp042-device-vs-stockfish-2200`, status: **sufficient sample** (30 complete opening pairs, minimum for a conclusion: 30).

The same match as the Mac calibration, but played on the phone: the app's real built-in Hard engine (0.3 s per move, a fresh search table every move, exactly as the app plays it) against Stockfish limited to 2200 Elo, also 0.3 s per move, both running on the Pixel 8. The phone's CPU is much slower than the Mac for the built-in engine, so this measures what a player actually faces.

**Hypothesis:** Finds where the built-in engine stops winning on the phone itself; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 2200, 0.3 s (on the Pixel 8) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":2200}` |
| Candidate | Built-in Hard, 0.3 s (on the Pixel 8) | time 0.3 | `{"Variant":"advanced"}` |

Engine: Stockfish 19 and built-in advanced, on device.

## Result (candidate point of view)

- Games: 60 (wins 9, draws 8, losses 43)
- Score: 21.67%
- Elo difference: -223 (95% CI -356 to -132, pentanomial)
- Likelihood of superiority: 0.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 17 / 4 / 6 / 2 / 1
- **Verdict:** The candidate measured weaker than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":19,"adjudicated_win":33,"threefold_repetition":8}
- Average game length: 78.7 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 249,833 | 18.1 | 306.2 | n/a |
| Candidate | 30,110 | 3.4 | 225.0 | n/a |

## Openings

Best pairs for the candidate: rnd-20261029-0029 (2/2), rnd-20261029-0002 (1.5/2), rnd-20261029-0023 (1.5/2), rnd-20261029-0001 (1/2), rnd-20261029-0004 (1/2)
Worst pairs for the candidate: rnd-20261029-0022 (0/2), rnd-20261029-0025 (0/2), rnd-20261029-0026 (0/2), rnd-20261029-0027 (0/2), rnd-20261029-0028 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp042-device-vs-stockfish-2200`
