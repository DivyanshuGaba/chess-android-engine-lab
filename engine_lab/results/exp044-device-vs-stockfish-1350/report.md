# On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1350

Experiment `exp044-device-vs-stockfish-1350`, status: **sufficient sample** (30 complete opening pairs, minimum for a conclusion: 30).

The same match as the Mac calibration, but played on the phone: the app's real built-in Hard engine (0.3 s per move, a fresh search table every move, exactly as the app plays it) against Stockfish limited to 1350 Elo, also 0.3 s per move, both running on the Pixel 8. The phone's CPU is much slower than the Mac for the built-in engine, so this measures what a player actually faces.

**Hypothesis:** Finds where the built-in engine stops winning on the phone itself; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 1350, 0.3 s (on the Pixel 8) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1350}` |
| Candidate | Built-in Hard, 0.3 s (on the Pixel 8) | time 0.3 | `{"Variant":"advanced"}` |

Engine: Stockfish 19 and built-in advanced, on device.

## Result (candidate point of view)

- Games: 60 (wins 56, draws 2, losses 2)
- Score: 95.00%
- Elo difference: +512 (95% CI +385 to +1124, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 2 / 2 / 26
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":16,"adjudicated_win":42,"threefold_repetition":2}
- Average game length: 60.9 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 212,239 | 16.1 | 309.6 | n/a |
| Candidate | 28,459 | 3.3 | 232.4 | n/a |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0029 (2/2), rnd-20261029-0010 (1.5/2), rnd-20261029-0014 (1.5/2), rnd-20261029-0020 (1/2), rnd-20261029-0026 (1/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp044-device-vs-stockfish-1350`
