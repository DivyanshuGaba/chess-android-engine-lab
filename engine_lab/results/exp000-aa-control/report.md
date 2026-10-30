# A/A control: identical strength, jittered budgets

Experiment `exp000-aa-control`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

Both sides have the same expected strength: the same options and an average budget of 10,000 nodes, with each move's budget randomly varied by up to 10% (seeded). Fixed-node Stockfish is deterministic, so without the jitter identical engines would replay mirrored games and the control would prove nothing. A sound harness must report a score near 50%; a large gap would reveal a bias in colours, openings or scoring.

**Hypothesis:** Score is 50% and the Elo interval includes zero.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, 10k nodes | nodes 10000 (jitter +/-10%) | `{"Threads":1,"Hash":16}` |
| Candidate | Stockfish 19, 10k nodes (copy) | nodes 10000 (jitter +/-10%) | `{"Threads":1,"Hash":16}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 200 (wins 63, draws 87, losses 50)
- Score: 53.25%
- Elo difference: +23 (95% CI -12 to +58, pentanomial)
- Likelihood of superiority: 89.7%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 6 / 20 / 38 / 27 / 9
- **Verdict:** No measurable difference: the 95% interval includes zero at this sample size.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"threefold_repetition":64,"checkmate":9,"adjudicated_win":104,"fifty_moves":6,"insufficient_material":14,"stalemate":3}
- Average game length: 126.1 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 862,145 | 12.8 | 12.9 | 372 |
| Candidate | 858,309 | 12.9 | 13.0 | 371 |

## Openings

Best pairs for the candidate: rnd-20261029-0014 (2/2), rnd-20261029-0025 (2/2), rnd-20261029-0026 (2/2), rnd-20261029-0027 (2/2), rnd-20261029-0052 (2/2)
Worst pairs for the candidate: rnd-20261029-0007 (0/2), rnd-20261029-0013 (0/2), rnd-20261029-0042 (0/2), rnd-20261029-0072 (0/2), rnd-20261029-0093 (0/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp000-aa-control`
