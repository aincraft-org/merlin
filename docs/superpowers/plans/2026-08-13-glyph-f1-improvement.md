# Glyph F1 Improvement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve held-out synthetic glyph macro F1 through controlled CUDA experiments and save reproducible visual comparisons.

**Architecture:** Add F1 to evaluation, select model families on calibration macro F1 instead of test accuracy, support bounded training-only augmentation and label smoothing, and run a serialized CUDA experiment matrix. A dependency-free reporting module writes JSON, CSV, and SVG charts.

**Tech Stack:** Python 3.12, PyTorch CUDA, NumPy, ONNX, SVG.

## Global Constraints

- Never use test metrics to select architecture or hyperparameters.
- Preserve split-group isolation.
- Training runs use CUDA without CPU fallback.
- Synthetic artifacts remain non-release-ready.
- Experiments use seed 1729 and report exact configuration.

---

### Task 1: F1 metrics and leakage-free selection

- [ ] Add per-class F1, macro F1, and weighted F1 to `evaluate()` with failing tests first.
- [ ] Select vector/raster/fused using calibration macro F1; evaluate test only after selection.
- [ ] Persist baseline calibration scores and explicit hyperparameters in manifest.
- [ ] Run focused evaluation and end-to-end tests.

### Task 2: Controlled training variants

- [ ] Fix augmentation brush-width assignment with a regression test.
- [ ] Add configurable training-only augmentation count and label smoothing with tests.
- [ ] Keep calibration and test examples unaugmented.
- [ ] Run preprocessing/augmentation and training tests.

### Task 3: Experiment observability

- [ ] Add a report module producing `experiments.json`, `experiments.csv`, `macro-f1.svg`, `per-class-f1.svg`, and `confusion-matrix.svg`.
- [ ] Test schemas, labels, and SVG content without pixel snapshots.
- [ ] Run a bounded CUDA matrix: baseline, 100 epochs, 100 epochs with wider embedding, 100 epochs with label smoothing, and 100 epochs with training-only augmentation.
- [ ] Select by calibration macro F1, then parameter count, then runtime; report test metrics only after selection.

### Task 4: Retrain winner and verify

- [ ] Update development config to the winning controlled configuration.
- [ ] Retrain through the main entrypoint on CUDA.
- [ ] Verify F1 improvement, checkpoint reload, hashes, ONNX validity, golden parity, graphs, and full test suite.