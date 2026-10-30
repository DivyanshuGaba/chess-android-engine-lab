# Ablation: Check extension

Experiment `exp024-ablate-check-extension`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: without check extensions. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Check extension helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Without check extensions, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"CheckExtension":false}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 90, draws 39, losses 71)
- Score: 54.75%
- Elo difference: +33 (95% CI -5 to +72, pentanomial)
- Likelihood of superiority: 95.5%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 8 / 15 / 43 / 18 / 16
- **Verdict:** No measurable difference: the 95% interval includes zero at this sample size.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":40,"adjudicated_win":121,"insufficient_material":8,"threefold_repetition":27,"fifty_moves":4}
- Average game length: 117.8 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 783,202 | 5.6 | 44.3 | 193 |
| Candidate | 792,574 | 5.2 | 44.8 | 157 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0011 (2/2), rnd-20261029-0028 (2/2), rnd-20261029-0031 (2/2), rnd-20261029-0040 (2/2)
Worst pairs for the candidate: rnd-20261029-0039 (0/2), rnd-20261029-0070 (0/2), rnd-20261029-0079 (0/2), rnd-20261029-0089 (0/2), rnd-20261029-0092 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp024-ablate-check-extension`
