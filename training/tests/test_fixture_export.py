import json
import numpy as np
import pytest
torch = pytest.importorskip("torch")
from wizardry_glyphs.export import export_bundle
from wizardry_glyphs.model import FusedClassifier


def test_fixture_bundle_manifest_and_onnx_parity(tmp_path):
    torch.manual_seed(11)
    model = FusedClassifier().eval()
    inputs = (torch.randn(1, 64, 32, 8), torch.ones(1, 64, 32), torch.randn(1, 3, 64, 64))
    manifest = export_bundle(model, inputs, tmp_path, [f"label-{i}" for i in range(12)])
    required = {"schema_id", "model_id", "catalog_id", "preprocessing_id", "training_id", "dataset_id", "input_schema", "output_schema", "labels", "calibration", "files", "metrics", "golden_fixture", "opset"}
    assert required <= manifest.keys()
    assert manifest["files"]["model.onnx"]
    import onnxruntime as ort
    session = ort.InferenceSession(str(tmp_path / "model.onnx"), providers=["CPUExecutionProvider"])
    ort_out = session.run(None, {name: value.numpy() for name, value in zip(("vectors", "mask", "raster"), inputs)})[0]
    with torch.no_grad():
        torch_out = model(*inputs).numpy()
    np.testing.assert_allclose(ort_out, torch_out, rtol=1e-4, atol=1e-5)
    loaded = json.loads((tmp_path / "manifest.json").read_text())
    assert loaded == manifest
