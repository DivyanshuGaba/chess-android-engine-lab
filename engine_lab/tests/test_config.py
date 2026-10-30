import json

import pytest

from enginelab.config import EngineConfig, Experiment

from conftest import LAB_DIR


def test_every_shipped_experiment_loads():
    files = sorted((LAB_DIR / "experiments").glob("*.json"))
    assert len(files) >= 4
    for path in files:
        experiment = Experiment.load(path)
        assert experiment.id == path.stem
        experiment.baseline.chess_limit()
        experiment.candidate.chess_limit()


def test_limits_are_translated():
    config = EngineConfig.from_json({"name": "x", "limits": {"nodes": 5000, "time": 0.5}})
    limit = config.chess_limit()
    assert limit.nodes == 5000 and limit.time == 0.5
    assert config.describe_limits() == "nodes 5000, time 0.5"


def test_a_config_without_limits_is_rejected():
    with pytest.raises(ValueError):
        EngineConfig.from_json({"name": "x", "limits": {}})


def test_unknown_limit_keys_are_rejected():
    with pytest.raises(ValueError):
        EngineConfig.from_json({"name": "x", "limits": {"movetime": 100}})


def test_the_a_a_control_really_is_identical():
    experiment = Experiment.load(LAB_DIR / "experiments" / "exp000-aa-control.json")
    assert experiment.baseline.options == experiment.candidate.options
    assert experiment.baseline.limits == experiment.candidate.limits


def test_experiments_share_one_opening_set():
    seeds = {
        (e.openings.seed, e.openings.count, e.openings.plies)
        for e in (Experiment.load(p) for p in (LAB_DIR / "experiments").glob("*.json"))
    }
    assert len(seeds) == 1


def test_jitter_is_bounded_and_reproducible():
    import random

    from enginelab.config import apply_jitter

    limit = EngineConfig.from_json({"name": "x", "limits": {"nodes": 10000, "time": 1.0}}).chess_limit()
    first = [apply_jitter(limit, 0.1, random.Random("seed")).nodes for _ in range(3)]
    assert len(set(first)) == 1
    values = [apply_jitter(limit, 0.1, random.Random(i)) for i in range(200)]
    assert all(9000 <= v.nodes <= 11000 for v in values)
    assert all(0.9 <= v.time <= 1.1 for v in values)
    assert len({v.nodes for v in values}) > 50
    assert apply_jitter(limit, 0.0, random.Random(1)) is limit


def test_jitter_must_be_a_fraction():
    with pytest.raises(ValueError):
        EngineConfig.from_json({"name": "x", "limits": {"nodes": 1}, "jitter": 1.5})
