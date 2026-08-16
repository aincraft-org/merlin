import csv
import json
from pathlib import Path

from wizardry_glyphs.experiment_report import write_report


def test_report_writes_machine_readable_results_and_visual_graphs(tmp_path):
    labels = ["damage", "heal"]
    rows = [
        {"name": "baseline", "config": {"epochs": 40}, "calibration_macro_f1": 0.7, "test_metrics": None, "parameters": 100, "seconds": 2.0},
        {"name": "winner", "config": {"epochs": 100}, "calibration_macro_f1": 0.9, "test_metrics": {
            "macro_f1": 0.88,
            "weighted_f1": 0.89,
            "per_class": [
                {"label": 0, "precision": 0.8, "recall": 1.0, "f1": 0.8889, "count": 10},
                {"label": 1, "precision": 1.0, "recall": 0.8, "f1": 0.8889, "count": 10},
            ],
            "confusion": [[10, 0], [2, 8]],
        }, "parameters": 100, "seconds": 4.0},
    ]

    write_report(tmp_path, rows, labels, winner="winner")

    parsed = json.loads((tmp_path / "experiments.json").read_text())
    assert parsed["winner"] == "winner"
    assert len(parsed["experiments"]) == 2
    with (tmp_path / "experiments.csv").open() as handle:
        assert [row["name"] for row in csv.DictReader(handle)] == ["baseline", "winner"]
    for name in ("macro-f1.svg", "per-class-f1.svg", "confusion-matrix.svg"):
        text = (tmp_path / name).read_text()
        assert text.startswith("<svg")
        assert "winner" in text or name == "confusion-matrix.svg"


def test_loss_curve_svg_plots_train_and_validation_from_history(tmp_path):
    from wizardry_glyphs.experiment_report import write_loss_curve

    history = [
        {"epoch": 1, "train_loss": 2.4, "val_loss": 2.5, "val_macro_f1": 0.1},
        {"epoch": 2, "train_loss": 1.1, "val_loss": 1.3, "val_macro_f1": 0.4},
        {"epoch": 3, "train_loss": 0.4, "val_loss": 0.6, "val_macro_f1": 0.8},
    ]
    path = tmp_path / "loss-curve.svg"
    write_loss_curve(path, history, title="adam-lr003", best_epoch=3)
    text = path.read_text()
    assert text.startswith("<svg")
    assert "adam-lr003" in text
    assert "train" in text and "validation" in text
    assert "2.4" in text and "0.4" in text


def test_overlay_loss_curves_include_each_experiment_name(tmp_path):
    from wizardry_glyphs.experiment_report import write_loss_overlay

    path = tmp_path / "loss-overlay.svg"
    write_loss_overlay(path, [
        {"name": "adam", "history": [{"epoch": 1, "train_loss": 2.0, "val_loss": 2.1}, {"epoch": 2, "train_loss": 1.0, "val_loss": 1.2}]},
        {"name": "sgd", "history": [{"epoch": 1, "train_loss": 2.2, "val_loss": 2.3}, {"epoch": 2, "train_loss": 1.8, "val_loss": 1.9}]},
    ])
    text = path.read_text()
    assert text.startswith("<svg")
    assert "adam" in text and "sgd" in text
    assert "val_loss" in text or "validation" in text
