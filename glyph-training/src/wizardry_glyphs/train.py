from __future__ import annotations

import argparse
import json
import random
from pathlib import Path

import numpy as np

from .validate_dataset import validate_dataset


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args(argv)
    config_path = Path(args.config)
    cfg = json.loads(config_path.read_text())
    base = config_path.parent

    def resolve(value):
        path = Path(value)
        return path if path.is_absolute() else base / path

    catalog = resolve(cfg["catalog"])
    seeds = resolve(cfg["seeds"])
    review = resolve(cfg["review"])
    players = resolve(cfg.get("player_data", "player-data"))
    labels = json.loads(catalog.read_text()).get("labels", []) if catalog.exists() else []
    labels = [item.get("id", item) if isinstance(item, dict) else item for item in labels]
    result = validate_dataset(labels, seeds, review, players)
    if not result.ok:
        for deficit in result.deficits:
            print(deficit)
        return 2

    random.seed(cfg.get("seed", 0))
    np.random.seed(cfg.get("seed", 0))
    # Do not import heavyweight training dependencies until a real trainer is
    # available; the hard gate must remain usable in minimal environments.

    # Real training requires reviewed player data and is intentionally not
    # synthesized here. A future trainer must write the configured artifact.
    return 2

if __name__=='__main__': raise SystemExit(main())
