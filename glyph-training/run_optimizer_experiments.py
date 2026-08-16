#!/usr/bin/env python3
"""Run the shipped optimizer sweep on the development corpus. CUDA, seed 1729."""
from __future__ import annotations

from pathlib import Path

from wizardry_glyphs.optimizer_sweep import run_optimizer_sweep
from wizardry_glyphs.schema import LABELS, load_examples
from wizardry_glyphs.train import _rows

ROOT = Path(__file__).resolve().parent
CONFIGS = [
    {"name": "adam-lr003", "optimizer": "adam", "learning_rate": 0.003, "epochs": 40, "embedding_dim": 32},
    {"name": "adam-lr001", "optimizer": "adam", "learning_rate": 0.001, "epochs": 40, "embedding_dim": 32},
    {"name": "adamw-lr003", "optimizer": "adamw", "learning_rate": 0.003, "epochs": 40, "embedding_dim": 32, "weight_decay": 0.01},
    {"name": "sgd-lr003", "optimizer": "sgd", "learning_rate": 0.003, "epochs": 40, "embedding_dim": 32, "momentum": 0.9},
    {"name": "sgd-lr03", "optimizer": "sgd", "learning_rate": 0.03, "epochs": 40, "embedding_dim": 32, "momentum": 0.9},
    {"name": "sgd-lr3", "optimizer": "sgd", "learning_rate": 0.3, "epochs": 40, "embedding_dim": 32, "momentum": 0.9},
    {"name": "rmsprop-lr003", "optimizer": "rmsprop", "learning_rate": 0.003, "epochs": 40, "embedding_dim": 32},
]


def main() -> int:
    import torch
    if not torch.cuda.is_available():
        raise RuntimeError("CUDA training requested but CUDA is unavailable")
    examples = load_examples(ROOT / "dataset/generated/dev-basic-v1/corpus.jsonl")
    rows = _rows(examples)
    result = run_optimizer_sweep(
        rows,
        list(LABELS),
        CONFIGS,
        ROOT / "artifacts/optimizer-experiments",
        seed=1729,
        folds=2,
        test_ratio=0.15,
        device="cuda",
        torch=torch,
    )
    print(result["winner"])
    for row in result["experiments"]:
        test = row["test_metrics"]
        print(
            f"{row['name']:16} cal_f1={row['calibration_macro_f1']:.4f} "
            f"seconds={row['seconds']:.2f} best_epoch={row['best_epoch']} "
            f"test_f1={None if test is None else round(test['macro_f1'], 4)}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
