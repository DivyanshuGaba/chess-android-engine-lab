# Ablation: Piece-square evaluation

Experiment `exp025-ablate-evaluation`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: with the original (material + centre) evaluation. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Piece-square evaluation helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | With the original (material + centre) evaluation, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"Evaluation":"legacy"}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 124, draws 29, losses 47)
- Score: 69.25%
- Elo difference: +141 (95% CI +94 to +194, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 6 / 8 / 31 / 13 / 42
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":143,"insufficient_material":6,"threefold_repetition":10,"checkmate":28,"fifty_moves":13}
- Average game length: 119.9 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 800,913 | 5.2 | 44.6 | 186 |
| Candidate | 782,654 | 5.1 | 45.0 | 138 |

## Openings

Best pairs for the candidate: rnd-20261029-0001 (2/2), rnd-20261029-0004 (2/2), rnd-20261029-0005 (2/2), rnd-20261029-0006 (2/2), rnd-20261029-0007 (2/2)
Worst pairs for the candidate: rnd-20261029-0044 (0/2), rnd-20261029-0046 (0/2), rnd-20261029-0065 (0/2), rnd-20261029-0087 (0/2), rnd-20261029-0088 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp025-ablate-evaluation`
