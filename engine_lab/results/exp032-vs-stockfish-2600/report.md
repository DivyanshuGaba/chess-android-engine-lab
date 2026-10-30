# Calibration: built-in Hard against Stockfish at UCI_Elo 2600

Experiment `exp032-vs-stockfish-2600`, status: **sufficient sample** (50 complete opening pairs, minimum for a conclusion: 50).

The built-in Hard engine (0.3 s per move, the app's setting) against Stockfish limited to 2600 Elo with the same 0.3 s per move. UCI_Elo is calibrated for long time controls, so treat it as a ladder rung, not a rating for the built-in engine.

**Hypothesis:** Finds the rung of this ladder where the built-in engine stops winning; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 2600, 0.3 s | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":2600}` |
| Candidate | Built-in Hard (improved engine), 0.3 s | time 0.3 | `{"Variant":"advanced","Hash":32}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 100 (wins 5, draws 17, losses 78)
- Score: 13.50%
- Elo difference: -323 (95% CI -438 to -246, pentanomial)
- Likelihood of superiority: 0.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 33 / 10 / 4 / 3 / 0
- **Verdict:** The candidate measured weaker than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":72,"checkmate":11,"threefold_repetition":13,"fifty_moves":1,"insufficient_material":3}
- Average game length: 90.2 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 1,147,422 | 17.6 | 299.4 | 375 |
| Candidate | 770,583 | 6.6 | 248.0 | 142 |

## Openings

Best pairs for the candidate: rnd-20261029-0020 (1.5/2), rnd-20261029-0027 (1.5/2), rnd-20261029-0041 (1.5/2), rnd-20261029-0003 (1/2), rnd-20261029-0019 (1/2)
Worst pairs for the candidate: rnd-20261029-0044 (0/2), rnd-20261029-0045 (0/2), rnd-20261029-0046 (0/2), rnd-20261029-0048 (0/2), rnd-20261029-0049 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp032-vs-stockfish-2600`
