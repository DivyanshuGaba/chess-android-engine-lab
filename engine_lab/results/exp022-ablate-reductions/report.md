# Ablation: Late move reductions

Experiment `exp022-ablate-reductions`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: without late move reductions. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Late move reductions helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Without late move reductions, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"Reductions":false}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 81, draws 45, losses 74)
- Score: 51.75%
- Elo difference: +12 (95% CI -30 to +55, pentanomial)
- Likelihood of superiority: 71.5%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 12 / 18 / 40 / 11 / 19
- **Verdict:** No measurable difference: the 95% interval includes zero at this sample size.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":40,"adjudicated_win":115,"threefold_repetition":29,"insufficient_material":13,"fifty_moves":3}
- Average game length: 124.9 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 839,487 | 4.7 | 46.7 | 189 |
| Candidate | 790,030 | 5.3 | 45.0 | 141 |

## Openings

Best pairs for the candidate: rnd-20261029-0001 (2/2), rnd-20261029-0005 (2/2), rnd-20261029-0006 (2/2), rnd-20261029-0011 (2/2), rnd-20261029-0016 (2/2)
Worst pairs for the candidate: rnd-20261029-0069 (0/2), rnd-20261029-0070 (0/2), rnd-20261029-0082 (0/2), rnd-20261029-0086 (0/2), rnd-20261029-0097 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp022-ablate-reductions`
