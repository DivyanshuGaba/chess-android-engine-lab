import math

import pytest

from enginelab import stats


def test_even_score_is_zero_elo():
    assert stats.score_to_elo(0.5) == pytest.approx(0.0)


def test_known_score_to_elo_values():
    assert stats.score_to_elo(0.64) == pytest.approx(100.0, abs=1.0)
    assert stats.score_to_elo(0.75) == pytest.approx(190.85, abs=0.1)
    assert stats.score_to_elo(0.25) == pytest.approx(-190.85, abs=0.1)


def test_elo_and_score_are_inverses():
    for elo in (-300, -50, 0, 25, 400):
        assert stats.score_to_elo(stats.elo_to_score(elo)) == pytest.approx(elo, abs=1e-6)


def test_extreme_scores_stay_finite():
    assert math.isfinite(stats.score_to_elo(0.0))
    assert math.isfinite(stats.score_to_elo(1.0))


def test_trinomial_symmetric_result_has_zero_elo_and_symmetric_interval():
    estimate = stats.trinomial(30, 40, 30)
    assert estimate.score == pytest.approx(0.5)
    assert estimate.elo == pytest.approx(0.0)
    assert estimate.elo_low == pytest.approx(-estimate.elo_high, abs=1e-6)
    assert not estimate.excludes_zero
    assert estimate.los == pytest.approx(0.5)


def test_interval_contains_the_estimate_and_narrows_with_more_games():
    small = stats.trinomial(12, 10, 8)
    large = stats.trinomial(120, 100, 80)
    assert small.elo_low < small.elo < small.elo_high
    assert (large.elo_high - large.elo_low) < (small.elo_high - small.elo_low)


def test_a_dominant_result_excludes_zero():
    estimate = stats.trinomial(80, 15, 5)
    assert estimate.excludes_zero and estimate.elo > 0
    assert estimate.los > 0.999


def test_empty_results_give_no_estimate():
    assert stats.trinomial(0, 0, 0) is None
    assert stats.pentanomial([0, 0, 0, 0, 0]) is None


def test_pentanomial_needs_five_buckets():
    with pytest.raises(ValueError):
        stats.pentanomial([1, 2, 3])


def test_pair_buckets_count_pair_totals():
    assert stats.pair_buckets([0.0, 0.5, 1.0, 1.0, 2.0, 1.5]) == [1, 1, 2, 1, 1]


def test_pentanomial_matches_trinomial_score_and_gives_wider_interval_for_correlated_pairs():
    # Every pair splits 1-1: the games are perfectly anti-correlated within a pair.
    buckets = [0, 0, 50, 0, 0]
    penta = stats.pentanomial(buckets)
    tri = stats.trinomial(50, 0, 50)
    assert penta.score == pytest.approx(0.5)
    assert penta.score_se < tri.score_se  # colour-balanced pairs remove variance


def test_pentanomial_dominant_candidate():
    estimate = stats.pentanomial([0, 0, 5, 10, 85])
    assert estimate.excludes_zero and estimate.elo > 0
    assert estimate.method == "pentanomial"


def test_zero_variance_samples_still_get_a_nonzero_interval():
    all_split = stats.pentanomial([0, 0, 100, 0, 0])
    assert all_split.elo_low < 0 < all_split.elo_high
    sweep = stats.trinomial(40, 0, 0)
    assert sweep.elo_high > sweep.elo_low
    assert sweep.excludes_zero


def test_smoothing_does_not_touch_samples_that_have_variance():
    plain = stats.trinomial(30, 40, 30)
    # 30 wins, 40 draws, 30 losses: variance 0.15 per game over 100 games.
    assert plain.score_se == pytest.approx(math.sqrt(0.15 / 100), rel=1e-9)
