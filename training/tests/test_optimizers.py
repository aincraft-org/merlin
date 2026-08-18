import pytest


def test_make_optimizer_selects_named_engines():
    import torch
    from wizardry_glyphs.train import _make_optimizer

    model = torch.nn.Linear(4, 2)
    assert type(_make_optimizer(model, {}, torch)).__name__ == "Adam"
    assert type(_make_optimizer(model, {"optimizer": "sgd", "momentum": 0.9}, torch)).__name__ == "SGD"
    assert type(_make_optimizer(model, {"optimizer": "adamw"}, torch)).__name__ == "AdamW"
    assert type(_make_optimizer(model, {"optimizer": "rmsprop"}, torch)).__name__ == "RMSprop"


def test_unknown_optimizer_is_rejected():
    import torch
    from wizardry_glyphs.train import _make_optimizer

    with pytest.raises(ValueError, match="unsupported optimizer"):
        _make_optimizer(torch.nn.Linear(2, 2), {"optimizer": "lion"}, torch)


def test_batched_fit_takes_one_step_per_batch():
    import numpy as np
    import torch
    from wizardry_glyphs.model import FusedClassifier
    from wizardry_glyphs.train import _fit, _make_optimizer

    rows = [{
        "label": "a" if index % 2 == 0 else "b",
        "vectors": np.zeros((64, 32, 8), dtype="float32"),
        "mask": np.ones((64, 32), dtype="float32"),
        "raster": np.zeros((3, 64, 64), dtype="float32"),
    } for index in range(4)]
    steps = {"count": 0}
    original = _make_optimizer

    def counting(model, config, torch_mod):
        optimizer = original(model, config, torch_mod)
        inner_step = optimizer.step
        def step(*args, **kwargs):
            steps["count"] += 1
            return inner_step(*args, **kwargs)
        optimizer.step = step
        return optimizer

    import wizardry_glyphs.train as train
    train._make_optimizer = counting
    try:
        _fit(FusedClassifier(classes=2, embedding_dim=2), rows, ["a", "b"],
             {"epochs": 1, "optimizer": "sgd", "learning_rate": 0.1, "batch_size": 2}, torch)
    finally:
        train._make_optimizer = original
    assert steps["count"] == 2


def test_fit_honors_configured_optimizer():
    import numpy as np
    import torch
    from wizardry_glyphs.model import FusedClassifier
    from wizardry_glyphs.train import _fit

    rows = [{
        "label": "a",
        "vectors": np.zeros((64, 32, 8), dtype="float32"),
        "mask": np.ones((64, 32), dtype="float32"),
        "raster": np.zeros((3, 64, 64), dtype="float32"),
    }]
    model = FusedClassifier(classes=2, embedding_dim=2)
    before = {key: value.detach().clone() for key, value in model.state_dict().items()}
    _fit(model, rows, ["a", "b"], {"epochs": 1, "optimizer": "sgd", "learning_rate": 0.1}, torch)
    assert any(not torch.equal(before[key], value) for key, value in model.state_dict().items())
