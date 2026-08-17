import json
from pathlib import Path

from wizardry_glyphs.dev_corpus import generate_corpus, validate_development_corpus
from wizardry_glyphs.schema import LABELS

ROOT = Path(__file__).resolve().parents[1]
COMPOSITION_LABELS = (
    "on-hit", "on-hurt", "on-use", "periodic",
    "if-health", "if-undead", "if-outdoors",
    "shield", "attacker", "area", "repeat", "charges",
)


def test_schema_and_catalog_include_composition_roles():
    catalog = json.loads((ROOT / "catalog-v1.json").read_text())
    ids = [item["id"] for item in catalog["labels"]]
    assert ids == list(LABELS)
    assert ids[-1] == "reject"
    for label in COMPOSITION_LABELS:
        assert label in ids


def test_geometry_catalog_has_six_independent_templates_per_label():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    for label in LABELS:
        templates = geometry["glyphs"][label]["templates"]
        assert len(templates) >= 6, label
        ids = [item["id"] for item in templates]
        sources = [item["independent_source"] for item in templates]
        assert len(set(ids)) == len(ids)
        assert len(set(sources)) == len(sources)


def test_geometry_catalog_generates_a_valid_development_corpus(tmp_path):
    manifest = generate_corpus(
        ROOT / "catalog-geometry-v1.json",
        tmp_path / "out",
        seed_variants=0,
        derivatives_per_label=0,
        reject_count=0,
    )
    errors = validate_development_corpus(tmp_path / "out" / "corpus.jsonl", tmp_path / "out" / "manifest.json")
    assert errors == []
    assert manifest["lineage_counts"][COMPOSITION_LABELS[0]] >= 6
    assert all(manifest["lineage_counts"][label] >= 6 for label in LABELS)


def test_training_config_records_selected_optimizer():
    config = json.loads((ROOT / "train-dev-basic-v1.json").read_text())
    assert config["optimizer"] == "rmsprop"
    assert config["learning_rate"] == 0.003
    assert config["epochs"] == 100
    assert config["embedding_dim"] == 32
