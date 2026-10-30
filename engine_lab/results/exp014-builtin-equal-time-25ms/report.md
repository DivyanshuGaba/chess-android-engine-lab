# Built-in engine: improved search at matched wall-clock time

Experiment `exp014-builtin-equal-time-25ms`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

exp010 labelled itself equal-time, but the baseline averaged 21.6 ms per move and the candidate 44.8 ms, so the candidate had about twice the time. Here the candidate's budget is cut to 25 ms so its measured average move time matches the baseline's (the report shows the actual figures).

**Hypothesis:** The improved engine still scores clearly above 50% when its measured time per move matches the baseline's.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Built-in baseline (depth 4, as shipped Hard) | depth 4 | `{"Variant":"baseline","Hash":32,"Depth":4}` |
| Candidate | Built-in advanced, 25 ms | time 0.025 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 187, draws 13, losses 0)
- Score: 96.75%
- Elo difference: +590 (95% CI +515 to +715, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 0 / 13 / 87
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":112,"checkmate":75,"threefold_repetition":13}
- Average game length: 65.2 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 487,040 | 4.0 | 23.8 | 139 |
| Candidate | 620,896 | 3.7 | 24.9 | 131 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0005 (2/2), rnd-20261029-0006 (2/2)
Worst pairs for the candidate: rnd-20261029-0091 (1.5/2), rnd-20261029-0092 (1.5/2), rnd-20261029-0096 (1.5/2), rnd-20261029-0097 (1.5/2), rnd-20261029-0099 (1.5/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp014-builtin-equal-time-25ms`
