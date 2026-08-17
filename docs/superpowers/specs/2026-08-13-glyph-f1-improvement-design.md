# Glyph F1 Improvement Experiment Design

## Goal

Compare multiple evidence-based ways to improve glyph classification F1, select the smallest technique that improves held-out macro F1, retrain on CUDA, save the resulting weights, and provide visual experiment observability.

## Baseline

The saved synthetic-development fused model has accuracy 0.9158, macro F1 0.9074, and weighted F1 0.9133. Errors are concentrated in `damage`/`target` and `heal`/`self`. A controlled probe already showed that changing only epochs from 40 to 100 reaches macro F1 1.0 on the existing held-out synthetic groups.

## Parallel design workflows

Three independent agents inspect and propose bounded experiment variants:

1. Data/augmentation: stronger but label-preserving synthetic variation and additional independent geometry groups.
2. Model/objective: training-duration, capacity, and loss alternatives that directly affect per-class F1.
3. Observability: manifest F1 fields and reproducible CSV/JSON/PNG comparison artifacts.

Agents do not train or edit. Their proposals return exact configurations and risks. The main session integrates the smallest controlled matrix.

## Experiment execution

GPU runs are serialized on the single RTX 3080. Every experiment uses CUDA, seed 1729, the same grouped split, and the same evaluation function. Change one factor per experiment where possible. Compare accuracy, macro F1, weighted F1, per-class F1, training duration, and parameter count.

The selection rule is highest held-out macro F1, then fewer parameters, then shorter training duration. Synthetic F1 is explicitly not evidence of real-player generalization.

## Visual outputs

Save machine-readable experiment rows plus PNG charts showing macro F1 by experiment, per-class F1 for baseline versus winner, and the winner's confusion matrix. Record macro F1, weighted F1, and per-class F1 in the trained bundle manifest.

## Verification

The winning configuration is retrained through the training entrypoint on CUDA. Verify manifest GPU provenance, model checkpoint hashes, strict CPU checkpoint reload, ONNX validity, golden-fixture parity, and the full glyph-training test suite.