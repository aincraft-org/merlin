"""Versioned JSONL glyph dataset validation."""
from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType
from typing import Any

LABELS = (
    "target-ray", "damage", "heal", "push", "cooldown", "self", "target",
    "physical", "fire", "frost", "arcane", "reject"
)
LABEL_SET = frozenset(LABELS)
SOURCES = frozenset({"canonical", "synthetic", "player", "reject"})


def _finite(value: Any, field: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        raise ValueError(f"{field} must be finite")
    return float(value)


def _text(value: Any, field: str, *, optional: bool = False) -> str | None:
    if value is None and optional:
        return None
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field} must be a non-empty string")
    return value


@dataclass(frozen=True, slots=True)
class GlyphPointData:
    x: float
    y: float

    def __post_init__(self) -> None:
        x, y = _finite(self.x, "point.x"), _finite(self.y, "point.y")
        if not (0 <= x < 128 and 0 <= y < 128):
            raise ValueError("point coordinates must be in [0, 128)")
        object.__setattr__(self, "x", x)
        object.__setattr__(self, "y", y)


@dataclass(frozen=True, slots=True)
class GlyphStrokeData:
    points: tuple[GlyphPointData, ...]
    brush_width: float
    started_at_millis: int

    def __post_init__(self) -> None:
        points = tuple(self.points)
        if not points or len(points) > 256:
            raise ValueError("stroke points must contain 1..256 points")
        width = _finite(self.brush_width, "brush_width")
        if not 0 < width <= 32:
            raise ValueError("brush_width must be in (0, 32]")
        if isinstance(self.started_at_millis, bool) or not isinstance(self.started_at_millis, int):
            raise ValueError("started_at_millis must be an integer")
        object.__setattr__(self, "points", points)
        object.__setattr__(self, "brush_width", width)


@dataclass(frozen=True, slots=True)
class GlyphExample:
    schema_version: str
    example_id: str
    label: str
    source: str
    lineage_group: str
    seed_id: str | None
    author_group: str
    session_group: str
    split_group: str
    consent: bool | None
    strokes: tuple[GlyphStrokeData, ...]
    generation: MappingProxyType | None = None

    def __post_init__(self) -> None:
        _text(self.schema_version, "schema_version")
        _text(self.example_id, "example_id")
        if self.label not in LABEL_SET:
            raise ValueError(f"unknown label: {self.label}")
        if self.source not in SOURCES:
            raise ValueError(f"unknown source: {self.source}")
        for field in ("lineage_group", "author_group", "session_group", "split_group"):
            _text(getattr(self, field), field)
        seed = _text(self.seed_id, "seed_id", optional=True)
        consent = self.consent
        if consent is not None and not isinstance(consent, bool):
            raise ValueError("consent must be a boolean")
        if self.source == "player" and consent is None:
            raise ValueError("player examples require consent")
        if self.source == "synthetic" and seed is None:
            raise ValueError("synthetic examples require seed_id")
        if self.source == "reject" and self.label != "reject":
            raise ValueError("reject source requires reject label")
        if self.label == "reject" and self.source not in {"reject", "player"}:
            raise ValueError("reject label requires reject or player source")
        strokes = tuple(self.strokes)
        if self.label != "reject" and not strokes:
            raise ValueError("positive examples require strokes")
        if len(strokes) > 64:
            raise ValueError("strokes cannot exceed 64")
        generation = self.generation
        if generation is not None:
            if not isinstance(generation, dict):
                raise ValueError("generation must be an object")
            generation = MappingProxyType(dict(generation))
        object.__setattr__(self, "seed_id", seed)
        object.__setattr__(self, "consent", consent)
        object.__setattr__(self, "strokes", strokes)
        object.__setattr__(self, "generation", generation)


def _record(raw: Any, line: int) -> GlyphExample:
    if not isinstance(raw, dict):
        raise ValueError(f"line {line}: record must be an object")
    required = ("schema_version", "example_id", "label", "source", "lineage_group", "author_group", "session_group", "split_group", "strokes")
    missing = [key for key in required if key not in raw]
    if missing:
        raise ValueError(f"line {line}: missing required field {missing[0]}")
    try:
        strokes = []
        for stroke in raw["strokes"]:
            if not isinstance(stroke, dict):
                raise ValueError("stroke must be an object")
            points = tuple(GlyphPointData(p["x"], p["y"]) for p in stroke["points"])
            strokes.append(GlyphStrokeData(points, stroke["brush_width"], stroke["started_at_millis"]))
        return GlyphExample(raw["schema_version"], raw["example_id"], raw["label"], raw["source"], raw["lineage_group"], raw.get("seed_id"), raw["author_group"], raw["session_group"], raw["split_group"], raw.get("consent"), tuple(strokes), raw.get("generation"))
    except (KeyError, TypeError) as exc:
        raise ValueError(f"line {line}: malformed record field {exc}") from exc
    except ValueError as exc:
        raise ValueError(f"line {line}: {exc}") from exc


def load_examples(path: Path) -> list[GlyphExample]:
    examples: list[GlyphExample] = []
    seen: set[str] = set()
    try:
        handle = Path(path).open(encoding="utf-8")
    except OSError as exc:
        raise ValueError(f"cannot read dataset: {exc}") from exc
    with handle:
        for line_number, line in enumerate(handle, 1):
            if not line.strip():
                continue
            try:
                raw = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"line {line_number}: invalid JSON") from exc
            example = _record(raw, line_number)
            if example.example_id in seen:
                raise ValueError(f"line {line_number}: duplicate example_id {example.example_id}")
            seen.add(example.example_id)
            examples.append(example)
    return examples
