# Positive control: twice the search effort

Experiment `exp001-doubled-nodes`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The candidate searches 20,000 nodes per move against the baseline's 10,000. More search is known to help, so the harness should detect a clear advantage. This proves that the framework can see a real difference.

**Hypothesis:** The candidate scores clearly above 50% and the Elo interval excludes zero.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Stockfish 19, 10k nodes | nodes 10000 | `{"Threads":1,"Hash":16}` |
| Candidate | Stockfish 19, 20k nodes | nodes 20000 | `{"Threads":1,"Hash":16}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 200 (wins 96, draws 91, losses 13)
- Score: 70.75%
- Elo difference: +153 (95% CI +123 to +187, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 3 / 31 / 46 / 20
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":102,"threefold_repetition":76,"stalemate":3,"checkmate":7,"insufficient_material":10,"fifty_moves":2}
- Average game length: 118.7 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 926,204 | 13.0 | 12.0 | 371 |
| Candidate | 925,076 | 14.4 | 22.8 | 370 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0012 (2/2), rnd-20261029-0021 (2/2), rnd-20261029-0026 (2/2), rnd-20261029-0029 (2/2)
Worst pairs for the candidate: rnd-20261029-0095 (1/2), rnd-20261029-0099 (1/2), rnd-20261029-0016 (0.5/2), rnd-20261029-0059 (0.5/2), rnd-20261029-0061 (0.5/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp001-doubled-nodes`
