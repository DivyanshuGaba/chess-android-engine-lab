# On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1800

Experiment `exp041-device-vs-stockfish-1800`, status: **sufficient sample** (30 complete opening pairs, minimum for a conclusion: 30).

The same match as the Mac calibration, but played on the phone: the app's real built-in Hard engine (0.3 s per move, a fresh search table every move, exactly as the app plays it) against Stockfish limited to 1800 Elo, also 0.3 s per move, both running on the Pixel 8. The phone's CPU is much slower than the Mac for the built-in engine, so this measures what a player actually faces.

**Hypothesis:** Finds where the built-in engine stops winning on the phone itself; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 1800, 0.3 s (on the Pixel 8) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1800}` |
| Candidate | Built-in Hard, 0.3 s (on the Pixel 8) | time 0.3 | `{"Variant":"advanced"}` |

Engine: Stockfish 19 and built-in advanced, on device.

## Result (candidate point of view)

- Games: 60 (wins 13, draws 12, losses 35)
- Score: 31.67%
- Elo difference: -134 (95% CI -221 to -60, pentanomial)
- Likelihood of superiority: 0.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 10 / 6 / 11 / 2 / 1
- **Verdict:** The candidate measured weaker than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":23,"checkmate":25,"threefold_repetition":11,"insufficient_material":1}
- Average game length: 90.7 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 264,650 | 16.0 | 306.8 | n/a |
| Candidate | 30,682 | 3.5 | 224.6 | n/a |

## Openings

Best pairs for the candidate: rnd-20261029-0006 (2/2), rnd-20261029-0016 (1.5/2), rnd-20261029-0023 (1.5/2), rnd-20261029-0002 (1/2), rnd-20261029-0003 (1/2)
Worst pairs for the candidate: rnd-20261029-0012 (0/2), rnd-20261029-0013 (0/2), rnd-20261029-0014 (0/2), rnd-20261029-0017 (0/2), rnd-20261029-0026 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp041-device-vs-stockfish-1800`
