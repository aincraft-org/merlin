import numpy as np
import pytest


def test_grouped_split_isolation_and_labels():
    from wizardry_glyphs.split import grouped_split
    rows = [
        {"id": f"{label}-{group}", "label": label, "seed_id": f"s{label}-{group}", "author_group": f"a{label}-{group}", "session_group": f"q{label}-{group}", "split_group": f"g{label}-{group}"}
        for label in range(2)
        for group in range(6)
    ]
    parts = grouped_split(rows, seed=7)
    sets = [set(x["split_group"] for x in parts[name]) for name in ("train", "calibration", "test")]
    assert not (sets[0] & sets[1] or sets[0] & sets[2] or sets[1] & sets[2])
    assert all(parts[name] for name in ("train", "calibration", "test"))

def test_grouped_split_stratifies_labels_with_three_groups_each():
    from wizardry_glyphs.split import grouped_split
    rows = [
        {"id": f"{label}-{group}-{sample}", "label": label, "split_group": f"{label}-g{group}"}
        for label in ("fire", "frost", "reject")
        for group in range(3)
        for sample in range(4)
    ]
    parts = grouped_split(rows, seed=9)
    assert all({row["label"] for row in parts[name]} == {"fire", "frost", "reject"} for name in parts)
    group_sets = [{row["split_group"] for row in parts[name]} for name in ("train", "calibration", "test")]
    assert not (group_sets[0] & group_sets[1] or group_sets[0] & group_sets[2] or group_sets[1] & group_sets[2])


def test_grouped_split_rejects_groups_spanning_labels():
    from wizardry_glyphs.split import grouped_split

    rows = [
        {"id": "fire", "label": "fire", "split_group": "shared"},
        {"id": "frost", "label": "frost", "split_group": "shared"},
    ]
    with pytest.raises(ValueError, match="contains multiple labels"):
        grouped_split(rows)


def test_models_emit_finite_logits():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.model import VectorClassifier, RasterClassifier, FusedClassifier
    vectors = torch.zeros(4, 64, 32, 8)
    masks = torch.ones(4, 64, 32)
    rasters = torch.zeros(4, 1, 64, 64)
    for model, args in ((VectorClassifier(), (vectors, masks)), (RasterClassifier(), (rasters,)), (FusedClassifier(), (vectors, masks, rasters))):
        out = model(*args)
        from wizardry_glyphs.schema import LABELS
        assert out.shape == (4, len(LABELS))
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
    assert model.vector_head.in_features == model.vector.point[-1].out_features
    assert model.raster_head.in_features == model.raster.projection.out_features


def test_raster_encoder_preserves_spatial_layout():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.model import RasterEncoder

    torch.manual_seed(4)
    encoder = RasterEncoder(8).eval()
    left = torch.zeros(1, 1, 64, 64)
    right = torch.zeros(1, 1, 64, 64)
    left[:, :, 16:48, 8:16] = 1
    right[:, :, 16:48, 48:56] = 1
    assert not torch.allclose(encoder(left), encoder(right))


def test_fit_learns_balanced_classes_from_imbalanced_rows():
    torch = pytest.importorskip("torch")
    from wizardry_glyphs.train import _fit

    rows = []
    for label, count, x in (("rare", 4, 0.0), ("common", 40, 1.0)):
        for _ in range(count):
            rows.append({
                "label": label,
                "vectors": np.full((64, 32, 8), x, dtype=np.float32),
                "mask": np.ones((64, 32), dtype=np.float32),
                "raster": np.full((1, 64, 64), x, dtype=np.float32),
            })

    class Tiny(torch.nn.Module):
        def __init__(self):
            super().__init__()
            self.head = torch.nn.Linear(1, 2)

        def forward(self, vectors, mask, raster):
            return self.head(vectors[:, 0, 0, :1])

    torch.manual_seed(5)
    model = _fit(Tiny(), rows, ["rare", "common"], {"epochs": 40, "learning_rate": 0.1}, torch, torch.device("cpu"))
    predictions = model(
        torch.tensor([[[[0.0] * 8] * 32] * 64, [[[1.0] * 8] * 32] * 64]),
        torch.ones(2, 64, 32),
        torch.zeros(2, 1, 64, 64),
    ).argmax(dim=1)
    assert predictions.tolist() == [0, 1]


def test_model_selection_uses_calibration_macro_f1_and_prefers_fused_on_tie():
    from wizardry_glyphs.train import _select_model
    scores = {
        "vector": {"macro_f1": 0.7},
        "raster": {"macro_f1": 0.9},
        "fused": {"macro_f1": 0.9},
    }
    assert _select_model(scores) == "fused"
