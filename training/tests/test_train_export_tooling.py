import json
from pathlib import Path


def test_train_refuses_empty_real_data(tmp_path, capsys):
    from wizardry_glyphs.train import main
    config = {
        "catalog": "catalog-v1.json", "seeds": "seeds.jsonl", "review": "review.json", "player_data": "players", "output": str(tmp_path / "artifact")
    }
    cfg = tmp_path / "config.json"; cfg.write_text(json.dumps(config))
    assert main(["--config", str(cfg)]) == 2
    assert not (tmp_path / "artifact").exists()
def test_invalid_config_returns_before_model_construction(tmp_path, monkeypatch):
    from wizardry_glyphs.train import main
    config = {"catalog": "catalog-v1.json", "seeds": "seeds.jsonl", "review": "review.json", "player_data": "players", "output": str(tmp_path / "artifact"), "folds": 1}
    cfg = tmp_path / "config.json"; cfg.write_text(json.dumps(config))
    assert main(["--config", str(cfg)]) == 2
