# Ablation: Killer and history move ordering

Experiment `exp023-ablate-ordering-heuristics`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The full improved engine against the same engine with only one thing changed: without killer/history ordering. Both get 50 ms per move, so a feature that costs time but does not pay for itself shows up as a negative result. A positive Elo means the feature helps.

**Hypothesis:** Killer and history move ordering helps: the full engine scores above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Without killer/history ordering, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32,"OrderingHeuristics":false}` |
| Candidate | Full improved engine, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 83, draws 34, losses 83)
- Score: 50.00%
- Elo difference: -0 (95% CI -48 to +48, pentanomial)
- Likelihood of superiority: 50.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 21 / 13 / 31 / 15 / 20
- **Verdict:** No measurable difference: the 95% interval includes zero at this sample size.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":127,"checkmate":39,"insufficient_material":10,"threefold_repetition":20,"fifty_moves":3,"stalemate":1}
- Average game length: 124.7 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 904,095 | 5.2 | 45.1 | 187 |
| Candidate | 801,317 | 5.3 | 44.9 | 140 |

## Openings

Best pairs for the candidate: rnd-20261029-0005 (2/2), rnd-20261029-0013 (2/2), rnd-20261029-0014 (2/2), rnd-20261029-0021 (2/2), rnd-20261029-0028 (2/2)
Worst pairs for the candidate: rnd-20261029-0071 (0/2), rnd-20261029-0078 (0/2), rnd-20261029-0081 (0/2), rnd-20261029-0093 (0/2), rnd-20261029-0097 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp023-ablate-ordering-heuristics`
