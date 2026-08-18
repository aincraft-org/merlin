from wizardry_glyphs.optimizer_sweep import run_optimizer_sweep


def _row(label, lineage):
    import numpy as np
    return {
        "label": label,
        "lineage_group": lineage,
        "independent_source": lineage,
        "vectors": np.zeros((64, 32, 8), dtype="float32"),
        "mask": np.ones((64, 32), dtype="float32"),
        "raster": np.zeros((3, 64, 64), dtype="float32"),
    }


def test_optimizer_sweep_selects_on_calibration_and_writes_loss_curves(tmp_path):
    import torch

    labels = ["damage", "heal"]
    rows = []
    for label in labels:
        for lineage in range(4):
            rows.extend(_row(label, f"{label}-{lineage}") for _ in range(3))
    result = run_optimizer_sweep(
        rows,
        labels,
        [
            {"name": "adam-tiny", "optimizer": "adam", "epochs": 2, "learning_rate": 0.05, "embedding_dim": 8},
            {"name": "sgd-tiny", "optimizer": "sgd", "epochs": 2, "learning_rate": 0.05, "embedding_dim": 8},
        ],
        tmp_path,
        seed=1729,
        folds=2,
        test_ratio=0.25,
        device="cpu",
        torch=torch,
    )
    assert result["winner"] in {"adam-tiny", "sgd-tiny"}
    assert (tmp_path / "loss-curve-adam-tiny.svg").is_file()
    assert (tmp_path / "loss-curve-sgd-tiny.svg").is_file()
    assert (tmp_path / "loss-overlay.svg").is_file()
    assert (tmp_path / "f1-overlay.svg").is_file()
    assert (tmp_path / "experiments.json").is_file()
    payload = (tmp_path / "sweep.json").read_text()
    assert '"selection": "calibration_macro_f1"' in payload
    winner = next(row for row in result["experiments"] if row["name"] == result["winner"])
    assert winner["test_metrics"] is not None
    assert all(row["test_metrics"] is None or row["name"] == result["winner"] for row in result["experiments"])
