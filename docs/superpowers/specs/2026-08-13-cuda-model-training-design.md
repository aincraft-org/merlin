# CUDA Model Training Design

## Goal

Train the existing glyph classifiers on the local synthetic development corpus using the NVIDIA GPU for every optimization and evaluation pass, then save and verify a Java-compatible model bundle.

## Device contract

The training configuration declares `"device": "cuda"`. The trainer accepts `cpu` and `cuda`; requesting CUDA when PyTorch cannot use it raises an error before preprocessing or optimization. There is no automatic fallback.

Models, input tensors, expected labels, and class weights live on the selected device throughout fitting and scoring. The selected trained model and one golden input batch move to CPU only after training and evaluation complete, immediately before ONNX export. This packaging step does not perform optimization.

The exported manifest records the requested device and, for CUDA runs, the GPU name reported by PyTorch.

## Data and artifacts

This run uses `glyph-training/train-dev-basic-v1.json` and its generated synthetic corpus. The resulting bundle remains marked `synthetic-development` and `release_ready: false`; it must not be represented as a production model.

The existing atomic output replacement remains unchanged. A successful export replaces `glyph-training/artifacts/dev-basic-v1`; a failed export preserves the previous bundle.

## Verification

Focused tests cover explicit CUDA selection, unavailable-CUDA failure without CPU fallback, tensor/model device placement, and manifest metadata. Before training, a runtime probe must show `torch.cuda.is_available() == true` and identify the RTX 3080. After training, the saved ONNX file must pass `onnx.checker.check_model`, load in ONNX Runtime, and produce logits for the saved golden fixture with the declared input schema.