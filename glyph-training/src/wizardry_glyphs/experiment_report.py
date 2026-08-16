from __future__ import annotations

import csv
import html
import json
from pathlib import Path


def _svg(width: int, height: int, body: str) -> str:
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}"><rect width="100%" height="100%" fill="#111827"/>{body}</svg>\n'


def _text(x, y, value, *, size=14, anchor="start", color="#e5e7eb"):
    return f'<text x="{x}" y="{y}" font-family="sans-serif" font-size="{size}" text-anchor="{anchor}" fill="{color}">{html.escape(str(value))}</text>'


def _bar_chart(rows):
    width, height = 900, 460
    left, top, bottom = 180, 55, 45
    usable = width - left - 35
    row_height = (height - top - bottom) / max(len(rows), 1)
    body = _text(width / 2, 28, "Calibration macro F1 by experiment", size=20, anchor="middle")
    for index, row in enumerate(rows):
        y = top + index * row_height
        score = float(row["calibration_macro_f1"])
        body += _text(left - 10, y + row_height * .62, row["name"], anchor="end")
        body += f'<rect x="{left}" y="{y + 5}" width="{usable * score:.2f}" height="{max(8, row_height - 12):.2f}" fill="#38bdf8"/>'
        body += _text(left + usable * score + 8, y + row_height * .62, f"{score:.4f}")
    body += _text(left, height - 12, "0", anchor="middle") + _text(left + usable, height - 12, "1.0", anchor="middle")
    return _svg(width, height, body)


def _per_class_chart(baseline, winner, labels, winner_name):
    width, height = 1000, 520
    left, top, bottom = 160, 60, 120
    usable = width - left - 30
    values = []
    for index, label in enumerate(labels):
        values.append((label, float(baseline[index]["f1"]), float(winner[index]["f1"])))
    step = usable / max(len(values), 1)
    body = _text(width / 2, 28, f"Per-class F1: baseline vs {winner_name}", size=20, anchor="middle")
    chart_height = height - top - bottom
    for index, (label, before, after) in enumerate(values):
        x = left + index * step
        bar = max(5, step * .32)
        body += f'<rect x="{x:.2f}" y="{top + chart_height * (1-before):.2f}" width="{bar:.2f}" height="{chart_height * before:.2f}" fill="#f59e0b"/>'
        body += f'<rect x="{x + bar:.2f}" y="{top + chart_height * (1-after):.2f}" width="{bar:.2f}" height="{chart_height * after:.2f}" fill="#22c55e"/>'
        body += f'<text transform="translate({x + bar:.2f},{height - bottom + 18}) rotate(55)" font-family="sans-serif" font-size="12" fill="#e5e7eb">{html.escape(label)}</text>'
    body += _text(20, height - 36, "baseline", color="#f59e0b") + _text(110, height - 36, winner_name, color="#22c55e")
    return _svg(width, height, body)


def _confusion_chart(matrix, labels, winner_name):
    size = len(labels)
    cell = 42
    left, top = 150, 90
    width, height = left + cell * size + 30, top + cell * size + 140
    maximum = max(max(row) for row in matrix) or 1
    body = _text(width / 2, 28, f"Confusion matrix: {winner_name}", size=20, anchor="middle")
    body += _text(width / 2, 52, "expected rows, predicted columns", anchor="middle", color="#94a3b8")
    for i, label in enumerate(labels):
        body += _text(left - 8, top + i * cell + 27, label, anchor="end", size=11)
        body += f'<text transform="translate({left + i * cell + 26},{top - 8}) rotate(-55)" font-family="sans-serif" font-size="11" fill="#e5e7eb">{html.escape(label)}</text>'
        for j, value in enumerate(matrix[i]):
            intensity = int(30 + 190 * value / maximum)
            body += f'<rect x="{left + j*cell}" y="{top + i*cell}" width="{cell-1}" height="{cell-1}" fill="rgb(15,{intensity},180)"/>'
            body += _text(left + j*cell + cell/2, top + i*cell + 27, value, anchor="middle", size=11)
    return _svg(width, height, body)


def _polyline(points, color):
    if not points:
        return ""
    rendered = " ".join(f"{x:.2f},{y:.2f}" for x, y in points)
    return f'<polyline points="{rendered}" fill="none" stroke="{color}" stroke-width="2"/>'


def write_loss_curve(path: Path, history: list[dict], *, title: str, best_epoch: int | None = None) -> None:
    path = Path(path)
    width, height = 900, 420
    left, top, right, bottom = 70, 40, 860, 360
    trains = [float(row["train_loss"]) for row in history]
    vals = [float(row["val_loss"]) for row in history if "val_loss" in row]
    epochs = [int(row["epoch"]) for row in history]
    values = trains + vals
    lo, hi = (min(values), max(values)) if values else (0.0, 1.0)
    if hi <= lo:
        hi = lo + 1e-6
    def x_at(epoch):
        if len(epochs) <= 1:
            return left
        return left + (epoch - epochs[0]) / (epochs[-1] - epochs[0]) * (right - left)
    def y_at(value):
        return top + (hi - value) / (hi - lo) * (bottom - top)
    train_pts = [(x_at(row["epoch"]), y_at(row["train_loss"])) for row in history]
    val_pts = [(x_at(row["epoch"]), y_at(row["val_loss"])) for row in history if "val_loss" in row]
    body = _text(width / 2, 28, f"Loss curve ({title})", size=18, anchor="middle")
    body += f'<line x1="{left}" y1="{top}" x2="{left}" y2="{bottom}" stroke="#94a3b8"/>'
    body += f'<line x1="{left}" y1="{bottom}" x2="{right}" y2="{bottom}" stroke="#94a3b8"/>'
    body += _polyline(train_pts, "#38bdf8")
    body += _polyline(val_pts, "#f59e0b")
    if best_epoch is not None and epochs:
        x = x_at(best_epoch)
        body += f'<line x1="{x:.2f}" y1="{top}" x2="{x:.2f}" y2="{bottom}" stroke="#22c55e" stroke-dasharray="4,4" stroke-width="1.5"/>'
    body += _text(60, 52, f"{hi:.4f}", size=12, anchor="end")
    body += _text(60, bottom, f"{lo:.4f}", size=12, anchor="end")
    body += _text(left, 405, str(epochs[0] if epochs else 1), size=12, anchor="middle")
    body += _text(right, 405, str(epochs[-1] if epochs else 1), size=12, anchor="middle")
    body += _text(90, 385, "train", size=12, color="#38bdf8")
    body += _text(150, 385, "validation", size=12, color="#f59e0b")
    if trains:
        body += _text(280, 385, f"{trains[0]:.4g} → {trains[-1]:.4g}", size=12, color="#94a3b8")
    path.write_text(_svg(width, height, body))


def write_metric_overlay(path: Path, experiments: list[dict], *, metric: str, title: str) -> None:
    path = Path(path)
    width, height = 1000, 460
    left, top, right, bottom = 70, 50, 780, 390
    colors = ("#38bdf8", "#f59e0b", "#22c55e", "#e879f9", "#f43f5e", "#a3e635", "#818cf8")
    series = []
    values = []
    max_epoch = 1
    for row in experiments:
        history = row.get("history") or []
        points = [(int(item["epoch"]), float(item[metric])) for item in history if metric in item]
        series.append((row["name"], points))
        values.extend(value for _, value in points)
        if points:
            max_epoch = max(max_epoch, points[-1][0])
    lo, hi = (min(values), max(values)) if values else (0.0, 1.0)
    if hi <= lo:
        hi = lo + 1e-6
    def x_at(epoch):
        return left + (epoch - 1) / max(max_epoch - 1, 1) * (right - left)
    def y_at(value):
        return top + (hi - value) / (hi - lo) * (bottom - top)
    body = _text(width / 2, 28, title, size=18, anchor="middle")
    body += f'<line x1="{left}" y1="{top}" x2="{left}" y2="{bottom}" stroke="#94a3b8"/>'
    body += f'<line x1="{left}" y1="{bottom}" x2="{right}" y2="{bottom}" stroke="#94a3b8"/>'
    for index, (name, points) in enumerate(series):
        color = colors[index % len(colors)]
        body += _polyline([(x_at(epoch), y_at(value)) for epoch, value in points], color)
        body += _text(800, 80 + index * 24, name, size=14, color=color)
    body += _text(60, 52, f"{hi:.4f}", size=12, anchor="end")
    body += _text(60, bottom, f"{lo:.4f}", size=12, anchor="end")
    body += _text(left, 430, "1", size=12, anchor="middle")
    body += _text(right, 430, str(max_epoch), size=12, anchor="middle")
    body += _text(width / 2, 448, f"epoch / {metric}", size=12, anchor="middle", color="#94a3b8")
    path.write_text(_svg(width, height, body))


def write_loss_overlay(path: Path, experiments: list[dict]) -> None:
    write_metric_overlay(path, experiments, metric="val_loss", title="Validation loss by optimizer")


def write_f1_overlay(path: Path, experiments: list[dict]) -> None:
    write_metric_overlay(path, experiments, metric="val_macro_f1", title="Validation macro F1 by optimizer")


def write_report(output: Path, experiments: list[dict], labels: list[str], *, winner: str) -> None:
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    winner_row = next(row for row in experiments if row["name"] == winner)
    baseline_metrics = experiments[0].get("test_metrics") or winner_row["test_metrics"]
    payload = {"winner": winner, "labels": labels, "experiments": experiments}
    (output / "experiments.json").write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
    with (output / "experiments.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=("name", "calibration_macro_f1", "parameters", "seconds", "config"))
        writer.writeheader()
        for row in experiments:
            writer.writerow({key: json.dumps(row[key], sort_keys=True) if key == "config" else row[key] for key in writer.fieldnames})
    (output / "macro-f1.svg").write_text(_bar_chart(experiments))
    (output / "per-class-f1.svg").write_text(_per_class_chart(baseline_metrics["per_class"], winner_row["test_metrics"]["per_class"], labels, winner))
    (output / "confusion-matrix.svg").write_text(_confusion_chart(winner_row["test_metrics"]["confusion"], labels, winner))
