"""Match statistics: score, Elo difference, confidence intervals and likelihood of superiority.

Two estimators are provided. The trinomial one treats every game as independent. The
pentanomial one treats each opening pair (the same opening played with colours swapped) as the
unit of observation, which is the correct unit here because the two games of a pair share an
opening and are therefore correlated.
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from statistics import NormalDist

_EPS = 1e-4


def score_to_elo(score: float) -> float:
    """Elo difference implied by an expected score in (0, 1)."""
    score = min(max(score, _EPS), 1 - _EPS)
    return -400.0 * math.log10(1.0 / score - 1.0)


def elo_to_score(elo: float) -> float:
    return 1.0 / (1.0 + 10.0 ** (-elo / 400.0))


@dataclass(frozen=True)
class EloEstimate:
    method: str
    samples: int          # games for trinomial, pairs for pentanomial
    score: float          # candidate score in [0, 1]
    elo: float
    elo_low: float
    elo_high: float
    score_se: float
    los: float            # probability the candidate is genuinely stronger
    confidence: float

    @property
    def excludes_zero(self) -> bool:
        return self.elo_low > 0 or self.elo_high < 0


def _interval(score: float, se: float, confidence: float) -> tuple[float, float]:
    z = NormalDist().inv_cdf(0.5 + confidence / 2)
    return score_to_elo(score - z * se), score_to_elo(score + z * se)


def _los(score: float, se: float) -> float:
    if se <= 0:
        return 0.5 if abs(score - 0.5) < 1e-12 else (1.0 if score > 0.5 else 0.0)
    return NormalDist().cdf((score - 0.5) / se)


# Added to every outcome count when the sample shows no variance at all (for example every game
# won, or every pair worth exactly one point). A zero-width interval would claim certainty that a
# finite sample cannot give, so the estimate is smoothed just enough to keep the interval honest.
_SMOOTHING = 0.5


def trinomial(wins: int, draws: int, losses: int, confidence: float = 0.95) -> EloEstimate | None:
    n = wins + draws + losses
    if n == 0:
        return None
    mean = (wins + 0.5 * draws) / n
    variance = (wins * (1 - mean) ** 2 + draws * (0.5 - mean) ** 2 + losses * (0 - mean) ** 2) / n
    if variance == 0:
        w, d, l = wins + _SMOOTHING, draws + _SMOOTHING, losses + _SMOOTHING
        total = w + d + l
        smooth_mean = (w + 0.5 * d) / total
        variance = (w * (1 - smooth_mean) ** 2 + d * (0.5 - smooth_mean) ** 2 + l * smooth_mean ** 2) / total
    se = math.sqrt(variance / n)
    low, high = _interval(mean, se, confidence)
    return EloEstimate("trinomial", n, mean, score_to_elo(mean), low, high, se, _los(mean, se), confidence)


# Pair totals are 0, 0.5, 1, 1.5 or 2 points; the index is the total multiplied by two.
PAIR_BUCKETS = (0.0, 0.5, 1.0, 1.5, 2.0)


def pentanomial(pair_counts: list[int] | tuple[int, ...], confidence: float = 0.95) -> EloEstimate | None:
    if len(pair_counts) != 5:
        raise ValueError("pair_counts needs five buckets: 0, 0.5, 1, 1.5 and 2 points")
    n = sum(pair_counts)
    if n == 0:
        return None
    mean_pair = sum(count * points for count, points in zip(pair_counts, PAIR_BUCKETS)) / n
    variance = sum(count * (points - mean_pair) ** 2 for count, points in zip(pair_counts, PAIR_BUCKETS)) / n
    if variance == 0:
        smoothed = [count + _SMOOTHING for count in pair_counts]
        total = sum(smoothed)
        smooth_mean = sum(c * p for c, p in zip(smoothed, PAIR_BUCKETS)) / total
        variance = sum(c * (p - smooth_mean) ** 2 for c, p in zip(smoothed, PAIR_BUCKETS)) / total
    score = mean_pair / 2
    se = math.sqrt(variance / n) / 2
    low, high = _interval(score, se, confidence)
    return EloEstimate("pentanomial", n, score, score_to_elo(score), low, high, se, _los(score, se), confidence)


def pair_buckets(pair_scores: list[float]) -> list[int]:
    """Counts of pair totals in the order of PAIR_BUCKETS."""
    counts = [0] * 5
    for total in pair_scores:
        counts[int(round(total * 2))] += 1
    return counts
