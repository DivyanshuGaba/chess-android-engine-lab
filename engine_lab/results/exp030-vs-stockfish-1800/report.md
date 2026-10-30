# Calibration: built-in Hard against Stockfish at UCI_Elo 1800

Experiment `exp030-vs-stockfish-1800`, status: **sufficient sample** (50 complete opening pairs, minimum for a conclusion: 50).

The built-in Hard engine (0.3 s per move, the app's setting) against Stockfish limited to 1800 Elo with the same 0.3 s per move. UCI_Elo is calibrated for long time controls, so treat it as a ladder rung, not a rating for the built-in engine.

**Hypothesis:** Finds the rung of this ladder where the built-in engine stops winning; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 1800, 0.3 s | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1800}` |
| Candidate | Built-in Hard (improved engine), 0.3 s | time 0.3 | `{"Variant":"advanced","Hash":32}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 100 (wins 61, draws 11, losses 28)
- Score: 66.50%
- Elo difference: +119 (95% CI +52 to +196, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 3 / 7 / 15 / 4 / 21
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":71,"checkmate":18,"threefold_repetition":10,"fifty_moves":1}
- Average game length: 90.0 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 1,169,078 | 18.9 | 297.6 | 377 |
| Candidate | 781,532 | 6.6 | 254.7 | 138 |

## Openings

Best pairs for the candidate: rnd-20261029-0002 (2/2), rnd-20261029-0005 (2/2), rnd-20261029-0008 (2/2), rnd-20261029-0010 (2/2), rnd-20261029-0013 (2/2)
Worst pairs for the candidate: rnd-20261029-0048 (0.5/2), rnd-20261029-0049 (0.5/2), rnd-20261029-0004 (0/2), rnd-20261029-0006 (0/2), rnd-20261029-0011 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp030-vs-stockfish-1800`
