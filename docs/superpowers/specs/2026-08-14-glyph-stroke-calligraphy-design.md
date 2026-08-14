# Glyph Stroke Boundaries and Calligraphic Width Design

## Problem
Glyph input currently appends every discrete canvas click to one active stroke until `pause` is called. This makes disjoint glyphs difficult to draw. Brush width is stored once on `GlyphStroke`; changing it while drawing retroactively resizes the entire stroke.

## Design
Keep each logical stroke as a separate `GlyphStroke`, but store width samples for each point-to-point segment. The existing serialized `GlyphStroke` shape remains compatible by adding a width profile with a default uniform profile for old data.

The tracker exposes real lifecycle methods: `beginStroke`, `appendPoint`, and `endStroke`. The screen maps the available explicit gesture to these methods: the draw button starts a stroke, subsequent draw clicks append, and the menu/clear/undo actions end the stroke. Until MapGUI provides pointer movement and release events, discrete draw clicks remain the available input granularity; no timeout ends a held stroke.

Each segment receives a target width derived from the velocity between its endpoints. To avoid mechanical jumps, the tracker keeps one smoothed width per active stroke and moves it toward each target with elapsed-time-aware exponential smoothing. The first moving segment initializes the value directly from its target. Elapsed time and the smoothing factor are clamped to finite bounds so duplicate or irregular timestamps cannot produce `NaN`, overshoot, or unbounded lag.

The smoothed value resets on `beginStroke`, `endStroke`, `clear`, and `undo`; one stroke can never influence the next. The response constant favors quick convergence while preserving a visible transition across sudden speed changes. The rasterizer interpolates recorded widths along each segment and stamps each sample at its local width. Width changes therefore affect only newly drawn segments, and end caps naturally taper when the final segment is narrow. Old strokes loaded from storage use a uniform width profile.

## Correctness
- Starting a new stroke never appends to the previous stroke.
- Ending a stroke commits it once; ending twice is harmless.
- A segment’s width is immutable after it is recorded.
- Widths are finite and within the existing brush limits.
- The first moving segment initializes width deterministically from its measured velocity.
- Smoothing remains finite and bounded for zero, negative, or unusually large elapsed times.
- A stroke never inherits smoothed velocity or width state from an earlier stroke.
- Existing glyph serialization remains readable; new profiles are encoded only when needed.
- Rasterization remains deterministic and bounded to the 128x128 canvas.

## Testing
Add failing-first tests for explicit stroke separation, per-segment width preservation, uniform legacy behavior, and raster output showing narrow/large segments independently. Transition tests verify that slow-to-fast motion narrows progressively, fast-to-slow motion widens progressively, both directions converge without losing responsiveness, irregular timestamps stay bounded, and a new stroke initializes independently. Integration tests exercise the tracker path used by `GlyphScreen` actions.
