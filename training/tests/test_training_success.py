import numpy as np

from wizardry_glyphs.evaluate import calibrate_temperature, evaluate, select_thresholds


def test_calibration_thresholds_and_metrics_are_measured():
    logits = np.array([
        [8.0, 1.0, 0.0],
        [0.0, 8.0, 1.0],
        [0.0, 1.0, 8.0],
        [7.0, 1.0, 0.0],
        [0.0, 7.0, 1.0],
        [0.0, 1.0, 7.0],
    ])
    labels = np.array([0, 1, 2, 0, 1, 2])
    temperature = calibrate_temperature(logits, labels)
    top, margin = select_thresholds(logits, labels, reject_id=2, temperature=temperature)
    metrics = evaluate(logits, labels, temperature, reject_id=2, top_threshold=top, margin=margin)
    assert 0.25 <= temperature <= 4.0
    assert top > 0
    assert margin > 0
    assert metrics["count"] == 6
    assert len(metrics["confusion"]) == 3
    assert len(metrics["per_class"]) == 3
    assert all(item["f1"] == 1.0 for item in metrics["per_class"])
    assert metrics["macro_f1"] == 1.0
    assert metrics["weighted_f1"] == 1.0
    assert metrics["reject_false_accept_rate"] == 0
