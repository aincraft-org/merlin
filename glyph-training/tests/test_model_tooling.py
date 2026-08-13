import numpy as np
import pytest


def test_grouped_split_isolation_and_labels():
    from wizardry_glyphs.split import grouped_split
    rows = [
        {"id": f"a{i}", "label": i % 2, "seed_id": f"s{i//2}", "author_group": f"a{i//2}", "session_group": f"q{i//2}", "split_group": f"g{i//2}"}
        for i in range(12)
    ]
    parts = grouped_split(rows, seed=7)
    sets = [set(x["split_group"] for x in parts[name]) for name in ("train", "calibration", "test")]
    assert not (sets[0] & sets[1] or sets[0] & sets[2] or sets[1] & sets[2])
    assert all(parts[name] for name in ("train", "calibration", "test"))


def test_models_emit_finite_logits():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.model import VectorClassifier, RasterClassifier, FusedClassifier
    vectors = torch.zeros(4, 64, 32, 8)
    masks = torch.ones(4, 64, 32)
    rasters = torch.zeros(4, 1, 64, 64)
    for model, args in ((VectorClassifier(), (vectors, masks)), (RasterClassifier(), (rasters,)), (FusedClassifier(), (vectors, masks, rasters))):
        out = model(*args)
        assert out.shape == (4, 12)
        assert torch.isfinite(out).all()


def test_vector_padding_is_inert_and_stroke_order_observable():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.model import VectorClassifier
    torch.manual_seed(3)
    model = VectorClassifier().eval()
    vectors = torch.randn(1, 64, 32, 8)
    mask = torch.zeros(1, 64, 32)
    mask[:, :2, :3] = 1
    baseline = model(vectors, mask)
    changed = vectors.clone()
    changed[:, 2:] = 1000
    assert torch.allclose(baseline, model(changed, mask))
    swapped = vectors.clone()
    swapped[:, 0], swapped[:, 1] = vectors[:, 1], vectors[:, 0]
    assert not torch.allclose(baseline, model(swapped, mask))


def test_fused_head_consumes_embeddings():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.model import FusedClassifier
    model = FusedClassifier()
    assert model.head.in_features == model.vector.point[-1].out_features * 2
