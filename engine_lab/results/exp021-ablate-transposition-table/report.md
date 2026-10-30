# Ablation: Transposition table

Experiment `exp021-ablate-transposition-table`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: without the transposition table. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Transposition table helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Without the transposition table, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"TranspositionTable":false}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 109, draws 30, losses 61)
- Score: 62.00%
- Elo difference: +85 (95% CI +41 to +132, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 8 / 11 / 36 / 15 / 30
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":35,"adjudicated_win":135,"threefold_repetition":17,"insufficient_material":12,"fifty_moves":1}
- Average game length: 119.8 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 848,583 | 4.5 | 45.9 | 186 |
| Candidate | 802,162 | 5.3 | 44.8 | 160 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0006 (2/2), rnd-20261029-0009 (2/2), rnd-20261029-0012 (2/2), rnd-20261029-0016 (2/2)
Worst pairs for the candidate: rnd-20261029-0069 (0/2), rnd-20261029-0080 (0/2), rnd-20261029-0084 (0/2), rnd-20261029-0086 (0/2), rnd-20261029-0091 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp021-ablate-transposition-table`
