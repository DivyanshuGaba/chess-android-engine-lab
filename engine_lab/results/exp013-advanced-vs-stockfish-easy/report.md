# Calibration: improved built-in engine against Stockfish Easy

Experiment `exp013-advanced-vs-stockfish-easy`, status: **sufficient sample** (50 complete opening pairs, minimum for a conclusion: 50).

Stockfish at the app's Easy level (1350 Elo, 0.1 s) against the improved built-in engine at 0.3 s per move.

**Hypothesis:** Measures where the improved engine sits relative to the same Stockfish preset; no direction assumed.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, app Easy (Elo 1350, 0.1 s) | time 0.1 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1350}` |
| Candidate | Built-in advanced, 300 ms | time 0.3 | `{"Variant":"advanced","Hash":32}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 100 (wins 99, draws 0, losses 1)
- Score: 99.00%
- Elo difference: +798 (95% CI +608 to open-ended (the score interval reaches the extreme), pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 1 / 0 / 49
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":30,"adjudicated_win":70}
- Average game length: 50.8 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 998,529 | 18.2 | 99.6 | 358 |
| Candidate | 744,737 | 5.9 | 252.4 | 130 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0046 (2/2), rnd-20261029-0047 (2/2), rnd-20261029-0048 (2/2), rnd-20261029-0049 (2/2), rnd-20261029-0031 (1/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp013-advanced-vs-stockfish-easy`
