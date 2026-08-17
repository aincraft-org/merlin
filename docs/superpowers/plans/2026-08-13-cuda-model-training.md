# CUDA Model Training Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce GPU-only glyph-model training when CUDA is requested and save a verified synthetic development model bundle.

**Architecture:** Resolve an explicit training device before data preprocessing. Move models and training/evaluation tensors to that device, retain the current atomic export flow, and move only the selected completed model and golden export batch to CPU for ONNX packaging.

**Tech Stack:** Python 3.12, PyTorch 2.x with CUDA, NumPy, ONNX, ONNX Runtime, pytest.

## Global Constraints

- `device: "cuda"` must never fall back to CPU.
- Every optimization and evaluation forward pass must run on the selected CUDA device.
- ONNX export may use CPU after training has completed.
- The synthetic artifact must remain `release_ready: false`.
- Existing atomic artifact replacement must remain intact.

---

### Task 1: Explicit training device

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/train.py`
- Modify: `glyph-training/tests/test_train_end_to_end.py`

**Interfaces:**
- Produces: `_resolve_device(config: dict, torch) -> torch.device`
- Produces: `_tensor(rows, torch, device)` returning three tensors on `device`
- Produces: `_fit(..., device)` and `_logits(..., device)` with all model passes on `device`

- [ ] **Step 1: Write failing device tests**

Add tests asserting that `device: "cuda"` raises when unavailable and resolves `torch.device("cuda")` when available. Add a focused tensor test asserting every `_tensor` result has the requested device.

- [ ] **Step 2: Run the focused tests**

Run: `uv run pytest tests/test_train_end_to_end.py -q`
Expected: FAIL because `_resolve_device` and device-aware `_tensor` do not exist.

- [ ] **Step 3: Implement explicit device placement**

Resolve `config.get("device", "cpu")`, reject values outside `cpu` and `cuda`, and raise `RuntimeError("CUDA training requested but CUDA is unavailable")` when necessary. Pass the device through `_tensor`, `_fit`, and `_logits`; call `model.to(device)` before optimizer construction and construct labels/class weights on the same device.

- [ ] **Step 4: Preserve CPU-only export**

After model selection and all evaluation, call `selected.to("cpu")` and create the golden export tensors explicitly on CPU before `export_bundle`.

- [ ] **Step 5: Run focused tests**

Run: `uv run pytest tests/test_train_end_to_end.py -q`
Expected: PASS.

### Task 2: Device provenance and CUDA config

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/train.py`
- Modify: `glyph-training/train-dev-basic-v1.json`
- Modify: `glyph-training/tests/test_train_end_to_end.py`

**Interfaces:**
- Manifest metadata adds `training_device` and nullable `gpu_name`.
- Development config requires `"device": "cuda"`.

- [ ] **Step 1: Write failing provenance assertion**

Extend the end-to-end test to assert CPU runs record `training_device == "cpu"` and `gpu_name is None`.

- [ ] **Step 2: Implement provenance**

Record `device.type`; for CUDA record `torch.cuda.get_device_name(device)`. Add `"device": "cuda"` to `train-dev-basic-v1.json`.

- [ ] **Step 3: Run training tests**

Run: `uv run pytest tests/test_train_end_to_end.py tests/test_train_export_tooling.py -q`
Expected: PASS.

### Task 3: Train and verify saved weights

**Files:**
- Replace atomically through trainer: `glyph-training/artifacts/dev-basic-v1/`

**Interfaces:**
- Consumes: `glyph-training/train-dev-basic-v1.json`
- Produces: `model.onnx`, `manifest.json`, and golden fixture files in the configured artifact directory.

- [ ] **Step 1: Verify CUDA runtime**

Run a PyTorch probe that asserts CUDA availability and prints the selected device and GPU name. Expected GPU: NVIDIA GeForce RTX 3080.

- [ ] **Step 2: Run CUDA-only training**

Run: `CUDA_VISIBLE_DEVICES=0 uv run python -m wizardry_glyphs.train --config train-dev-basic-v1.json`
Expected: exit 3 because synthetic development bundles are intentionally not release-ready, with the artifact directory successfully replaced.

- [ ] **Step 3: Verify artifact structure and provenance**

Assert `model.onnx` and `manifest.json` exist; manifest profile is `synthetic-development`, `release_ready` is false, `training_device` is `cuda`, and `gpu_name` identifies the RTX 3080.

- [ ] **Step 4: Verify model weights load and execute**

Run `onnx.checker.check_model`, create an ONNX Runtime session, load the saved golden inputs, execute inference, and assert finite logits with shape `[1, 12]`.

- [ ] **Step 5: Report exact metrics**

Read the saved manifest and report selected model, test accuracy, reject false-accept rate, output path, model SHA-256, and verification commands.