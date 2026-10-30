# Built-in engine: improved search at 50 ms (about twice the baseline's time)

Experiment `exp010-builtin-equal-time`, status: **sufficient sample** (100 complete opening pairs, minimum for a conclusion: 100).

The original fixed-depth minimax at depth 4 against the improved engine limited to 50 ms per move. The baseline turned out to average about 22 ms per move and the candidate about 45 ms, so this is NOT equal time: see exp014 for the matched-time run.

**Hypothesis:** The improved engine scores clearly above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Built-in baseline (depth 4, as shipped Hard) | depth 4 | `{"Variant":"baseline","Hash":32,"Depth":4}` |
| Candidate | Built-in advanced, 50 ms | time 0.05 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 200 (wins 193, draws 7, losses 0)
- Score: 98.25%
- Elo difference: +700 (95% CI +604 to +920, pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 0 / 7 / 93
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":122,"checkmate":71,"threefold_repetition":7}
- Average game length: 65.5 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 515,827 | 4.0 | 21.6 | 136 |
| Candidate | 681,474 | 4.5 | 44.8 | 136 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0005 (2/2)
Worst pairs for the candidate: rnd-20261029-0025 (1.5/2), rnd-20261029-0072 (1.5/2), rnd-20261029-0080 (1.5/2), rnd-20261029-0091 (1.5/2), rnd-20261029-0092 (1.5/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp010-builtin-equal-time`
