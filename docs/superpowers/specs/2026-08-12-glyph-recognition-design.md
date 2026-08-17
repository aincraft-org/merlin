# Glyph Recognition and Map Capture Design

## Goal

Implement the first Glyphcraft recognition core and Paper-side capture seam without coupling the repository to a live MapGUI API or introducing a heavy ML stack.

## Representation

`GlyphDraft` stores vector strokes on a full 128×128 canvas. Each stroke contains ordered points, brush width, and timing metadata. Vector strokes are authoritative for undo, normalization, stroke-order analysis, and deterministic recognition.

The system derives two bitmap views:

1. **Full canvas bitmap:** a 128×128 raster representing the complete MapGUI-sized drawing. It preserves placement, multiple marks, and preview/export fidelity.
2. **Normalized crop bitmap:** the non-empty drawing bounding box, padded by a fixed margin and resampled to a fixed recognition grid. It makes recognition robust to translation and unused canvas space.

Bitmaps are derived artifacts. They are never the source of truth and can be regenerated from strokes.

## Recognition

The first recognizer is deterministic and constrained to a known glyph catalog. A glyph template contains normalized vector strokes, expected stroke count/direction, optional intersection/end-point/enclosure constraints, and a derived bitmap feature signature. Recognition:

```text
strokes
  -> validate bounds and finite coordinates
  -> rasterize full canvas
  -> crop/pad/resample normalized bitmap
  -> normalize vector geometry
  -> calculate geometry and bitmap distances
  -> rank known templates
  -> return confidence and ambiguity candidates
```

Confidence selects recognition only; it never changes effect strength or cost. A low-confidence or tied result is ambiguous and must be confirmed/corrected by the caller.

The first release uses deterministic template/features recognition and ships no trained neural model. The recognizer boundary can later accept a lightweight classifier using the same derived full-canvas/cropped bitmap and vector feature record.

## Paper capture seam

Add a Paper-independent capture/session model that accepts MapGUI-like point events and produces `GlyphDraft` snapshots. Add a Paper-side store seam for persisting the serialized draft in plugin-owned data. Do not import MapGUI classes until its dependency/version/API is confirmed. The seam must support begin stroke, append point, end stroke, undo, clear, snapshot, and close/preserve.

## Validation

Reject non-finite points, points outside the canvas, empty strokes, excessive point/stroke counts, and oversized serialized drafts. Rasterization clips only at the explicit canvas boundary; it does not silently alter invalid input. Recognition is deterministic for the same draft and template catalog version.

## Testing

Cover:

- full-canvas raster dimensions and stable pixel output;
- crop/resample invariance under translation and scale;
- vector authority and regeneration of bitmaps;
- bounds and non-finite point rejection;
- undo/clear/session lifecycle;
- template ranking, confidence, and ambiguity;
- deterministic output across repeated runs.
