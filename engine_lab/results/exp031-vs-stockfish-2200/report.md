# Calibration: built-in Hard against Stockfish at UCI_Elo 2200

Experiment `exp031-vs-stockfish-2200`, status: **sufficient sample** (50 complete opening pairs, minimum for a conclusion: 50).

The built-in Hard engine (0.3 s per move, the app's setting) against Stockfish limited to 2200 Elo with the same 0.3 s per move. UCI_Elo is calibrated for long time controls, so treat it as a ladder rung, not a rating for the built-in engine.

**Hypothesis:** Finds the rung of this ladder where the built-in engine stops winning; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, UCI_Elo 2200, 0.3 s | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":2200}` |
| Candidate | Built-in Hard (improved engine), 0.3 s | time 0.3 | `{"Variant":"advanced","Hash":32}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 100 (wins 27, draws 14, losses 59)
- Score: 34.00%
- Elo difference: -115 (95% CI -189 to -50, pentanomial)
- Likelihood of superiority: 0.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 19 / 6 / 17 / 4 / 4
- **Verdict:** The candidate measured weaker than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":77,"threefold_repetition":12,"insufficient_material":2,"checkmate":9}
- Average game length: 96.0 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 1,177,377 | 18.0 | 298.9 | 375 |
| Candidate | 781,074 | 6.8 | 249.2 | 135 |

## Openings

Best pairs for the candidate: rnd-20261029-0018 (2/2), rnd-20261029-0033 (2/2), rnd-20261029-0045 (2/2), rnd-20261029-0048 (2/2), rnd-20261029-0008 (1.5/2)
Worst pairs for the candidate: rnd-20261029-0039 (0/2), rnd-20261029-0041 (0/2), rnd-20261029-0044 (0/2), rnd-20261029-0046 (0/2), rnd-20261029-0047 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp031-vs-stockfish-2200`
