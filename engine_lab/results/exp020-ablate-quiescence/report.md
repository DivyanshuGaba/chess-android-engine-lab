# Ablation: Quiescence search

Experiment `exp020-ablate-quiescence`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: without quiescence search. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Quiescence search helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Without quiescence search, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"Quiescence":false}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 195, draws 2, losses 3)
- Score: 98.00%
- Elo difference: +676 (95% CI +562 to +1072, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 3 / 2 / 95
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":17,"adjudicated_win":181,"threefold_repetition":1,"insufficient_material":1}
- Average game length: 74.6 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 446,168 | 4.8 | 47.0 | 181 |
| Candidate | 720,221 | 4.5 | 45.8 | 143 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0010 (1.5/2), rnd-20261029-0074 (1.5/2), rnd-20261029-0056 (1/2), rnd-20261029-0081 (1/2), rnd-20261029-0088 (1/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp020-ablate-quiescence`
