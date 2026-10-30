# Calibration: original built-in engine against Stockfish Easy

Experiment `exp012-baseline-vs-stockfish-easy`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

Stockfish limited to 1350 Elo at 0.1 s per move (the app's Easy level) is the baseline; the candidate is the original built-in engine at depth 4. A score below 50% means the built-in engine is weaker than that Stockfish preset.

**Hypothesis:** Measures where the original engine sits relative to a Stockfish preset; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, app Easy (Elo 1350, 0.1 s) | time 0.1 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1350}` |
| Candidate | Built-in baseline (depth 4, as shipped Hard) | depth 4 | `{"Variant":"baseline","Hash":32,"Depth":4}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 200 (wins 107, draws 22, losses 71)
- Score: 59.00%
- Elo difference: +63 (95% CI +18 to +111, pentanomial)
- Likelihood of superiority: 99.7%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 13 / 8 / 37 / 14 / 28
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":87,"checkmate":91,"threefold_repetition":22}
- Average game length: 68.3 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 1,085,418 | 17.3 | 100.2 | 377 |
| Candidate | 570,480 | 4.0 | 18.2 | 145 |

## Openings

Best pairs for the candidate: rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2), rnd-20261029-0005 (2/2)
Worst pairs for the candidate: rnd-20261029-0064 (0/2), rnd-20261029-0068 (0/2), rnd-20261029-0073 (0/2), rnd-20261029-0082 (0/2), rnd-20261029-0086 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp012-baseline-vs-stockfish-easy`
