# App difficulty ladder: Medium against Easy

Experiment `exp002-medium-vs-easy`, status: **sufficient sample** (40 complete opening pairs, minimum for a conclusion: 40).

The exact Stockfish settings the app uses for its Easy (UCI_Elo 1350, 0.1 s per move) and Medium (UCI_Elo 1800, 0.3 s per move) levels. Tests whether the levels differ in practice.

**Hypothesis:** Medium beats Easy; the Elo interval excludes zero.

## Configurations

| | Name | Search limit | UCI options |
|---|---|---|---|
| Baseline | App Easy (Elo 1350, 0.1 s) | time 0.1 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1350}` |
| Candidate | App Medium (Elo 1800, 0.3 s) | time 0.3 | `{"Threads":1,"Hash":16,"UCI_LimitStrength":true,"UCI_Elo":1800}` |

Engine: Stockfish 19.

## Result (candidate point of view)

- Games: 80 (wins 77, draws 1, losses 2)
- Score: 96.88%
- Elo difference: +596 (95% CI +458 to open-ended (the score interval reaches the extreme), pentanomial)
- Likelihood of superiority: 100.0%
- Pair totals 0 / 0.5 / 1 / 1.5 / 2: 0 / 0 / 2 / 1 / 37
- **Verdict:** The candidate measured stronger than the baseline; the 95% interval excludes zero.

## Reliability

- Crashes: 0, illegal moves: 0, timeouts: 0, incomplete pairs: 0
- Terminations: {"adjudicated_win":61,"checkmate":18,"threefold_repetition":1}
- Average game length: 67.8 plies

## Engine speed

| | Avg nodes/s | Avg depth | Avg ms/move (wall) | Peak RSS (MB) |
|---|---|---|---|---|
| Baseline | 870,880 | 19.0 | 99.3 | 353 |
| Candidate | 832,232 | 17.3 | 298.8 | 359 |

## Openings

Best pairs for the candidate: rnd-20261029-0000 (2/2), rnd-20261029-0001 (2/2), rnd-20261029-0002 (2/2), rnd-20261029-0003 (2/2), rnd-20261029-0004 (2/2)
Worst pairs for the candidate: rnd-20261029-0038 (2/2), rnd-20261029-0039 (2/2), rnd-20261029-0035 (1.5/2), rnd-20261029-0015 (1/2), rnd-20261029-0016 (1/2)

## Method and limits

- Every opening is played twice with colours swapped; the opening pair is the statistical unit (pentanomial).
- Engines run single-threaded with identical hash settings unless the options above differ.
- Clear wins are adjudicated when both sides evaluate the position beyond the configured threshold for several plies.
- A confidence interval that includes zero means the experiment could not tell the engines apart.
- Reproduce: `cd engine_lab && ./run_experiment.sh exp002-medium-vs-easy`
