# App difficulty ladder: Hard against Medium

Experiment `exp003-hard-vs-medium`, status: **sufficient sample** (40 complete opening pairs, minimum for a conclusion: 40).

The app's Hard level (no strength cap, 1 s per move) against its Medium level (UCI_Elo 1800, 0.3 s per move).

**Hypothesis:** Hard beats Medium; the Elo interval excludes zero.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | App Medium (Elo 1800, 0.3 s) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1800}` |
| Candidate | App Hard (full strength, 1 s) | time 1.0 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":false}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 80 (wins 80, draws 0, losses 0)
- Score: 100.00%
- Elo difference: off the scale (95% CI +530 to open-ended (the score interval reaches the extreme), pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 0 / 0 / 40
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"checkmate":20,"adjudicated_win":60}
- Average game length: 47.0 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 793,778 | 21.3 | 294.0 | 264 |
| Candidate | 779,414 | 30.1 | 970.6 | 368 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0035 (2/2), rnd-20261029-0036 (2/2), rnd-20261029-0037 (2/2), rnd-20261029-0038 (2/2), rnd-20261029-0039 (2/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp003-hard-vs-medium`
