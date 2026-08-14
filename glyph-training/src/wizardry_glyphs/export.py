from __future__ import annotations
import hashlib
import json
from pathlib import Path


def _sha256(path: Path) -> str: return hashlib.sha256(path.read_bytes()).hexdigest()


def export_bundle(model, inputs, output: Path, labels, *, metadata=None):
    import torch
    output = Path(output); output.mkdir(parents=True, exist_ok=True); onnx_path = output / "model.onnx"; model.eval()
    torch.onnx.export(model, inputs, onnx_path, opset_version=17, input_names=["vectors", "mask", "raster"], output_names=["logits"], dynamic_axes={"vectors": {0: "batch"}, "mask": {0: "batch"}, "raster": {0: "batch"}, "logits": {0: "batch"}})
    with torch.no_grad(): golden = model(*inputs).detach().cpu().tolist()
    files = {"model.onnx": _sha256(onnx_path)}; metadata = metadata or {}
    manifest = {"schema_id": metadata.get("schema_id", "glyph-bundle-v1"), "model_id": metadata.get("model_id", "fused-glyph-v1-fixture"), "catalog_id": metadata.get("catalog_id", "catalog-fixture"), "preprocessing_id": metadata.get("preprocessing_id", "preprocessing-fixture"), "training_id": metadata.get("training_id", "train-fixture"), "dataset_id": metadata.get("dataset_id", "dataset-fixture"), "release_ready": bool(metadata.get("release_ready", False)), "labels": list(labels), "input_schema": {"vectors": {"shape": [None, 64, 32, 8], "dtype": "float32"}, "mask": {"shape": [None, 64, 32], "dtype": "float32"}, "raster": {"shape": [None, 1, 64, 64], "dtype": "float32"}}, "output_schema": {"logits": {"shape": [None, len(labels)], "dtype": "float32"}}, "calibration": {"temperature": float(metadata.get("temperature", 1.0)), "top_threshold": metadata.get("top_threshold"), "margin": metadata.get("margin")}, "files": files, "metrics": metadata.get("metrics", {"fixture": True, "onnx_parity": {"rtol": 1e-4, "atol": 1e-5}}), "selected_candidate": metadata.get("selected_candidate"), "cross_validation": metadata.get("cross_validation"), "partition": metadata.get("partition"), "benchmark_warning": metadata.get("benchmark_warning"), "golden_fixture": {"inputs": [x.detach().cpu().tolist() for x in inputs], "logits": golden}, "opset": 17}
    (output / "manifest.json").write_text(json.dumps(manifest, sort_keys=True, indent=2)); (output / "sha256sums.json").write_text(json.dumps(files, sort_keys=True, indent=2)); return manifest
