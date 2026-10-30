# On the Pixel 8: built-in Hard against Stockfish at UCI_Elo 1500

Experiment `exp045-device-vs-stockfish-1500`, status: **sufficient sample** (30 complete opening pairs, minimum for a conclusion: 30).

The same match as the Mac calibration, but played on the phone: the app's real built-in Hard engine (0.3 s per move, a fresh search table every move, exactly as the app plays it) against Stockfish limited to 1500 Elo, also 0.3 s per move, both running on the Pixel 8. The phone's CPU is much slower than the Mac for the built-in engine, so this measures what a player actually faces.

**Hypothesis:** Finds where the built-in engine stops winning on the phone itself; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 1500, 0.3 s (on the Pixel 8) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1500}` |
| Candidate | Built-in Hard, 0.3 s (on the Pixel 8) | time 0.3 | `{"Variant":"advanced"}` |

Engine: Stockfish 19 and built-in advanced, on device.

## Result (candidate point of view)

- Games: 60 (wins 28, draws 7, losses 25)
- Score: 52.50%
- Elo difference: +17 (95% CI -59 to +96, pentanomial)
- Likelihood of superiority: 67.3%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 4 / 3 / 15 / 2 / 6
- **Verdict:** No measurable difference: the 95% interval includes zero at this sample size.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"threefold_repetition":7,"checkmate":19,"adjudicated_win":34}
- Average game length: 82.8 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 276,409 | 16.3 | 308.8 | n/a |
| Candidate | 28,535 | 3.5 | 222.7 | n/a |

## Openings

Best pairs for the candidate: rnd-20261029-0002 (2/2), rnd-20261029-0008 (2/2), rnd-20261029-0011 (2/2), rnd-20261029-0013 (2/2), rnd-20261029-0026 (2/2)
Worst pairs for the candidate: rnd-20261029-0028 (0.5/2), rnd-20261029-0009 (0/2), rnd-20261029-0018 (0/2), rnd-20261029-0019 (0/2), rnd-20261029-0023 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp045-device-vs-stockfish-1500`
