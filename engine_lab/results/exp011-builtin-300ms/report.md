# Built-in engine: improved search with the app's 0.3 s budget

Experiment `exp011-builtin-300ms`, status: **sufficient sample** (50 complete opening pairs, minimum for a conclusion: 50).

Same baseline, but the improved engine gets 0.3 s per move, the time budget the app can afford. This is NOT an equal-time test: it shows what the extra time buys on top of exp010.

**Hypothesis:** The improved engine scores far above 50%.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | Built-in baseline (depth 4, as shipped Hard) | depth 4 | `{"Variant":"baseline","Hash":32,"Depth":4}` |
| Candidate | Built-in advanced, 300 ms | time 0.3 | `{"Variant":"advanced","Hash":32}` |

Engine: BuiltIn-UCI 1.0.

## Result (candidate point of view)

- Games: 100 (wins 100, draws 0, losses 0)
- Score: 100.00%
- Elo difference: off the scale (95% CI +569 to open-ended (the score interval reaches the extreme), pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 0 / 0 / 50
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":66,"checkmate":34}
- Average game length: 56.3 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 582,474 | 4.0 | 20.1 | 132 |
| Candidate | 783,831 | 6.1 | 250.4 | 137 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0045 (2/2), rnd-20261029-0046 (2/2), rnd-20261029-0047 (2/2), rnd-20261029-0048 (2/2), rnd-20261029-0049 (2/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp011-builtin-300ms`
