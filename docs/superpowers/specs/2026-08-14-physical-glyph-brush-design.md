# Physical Glyph Brush Design

## Problem

Glyph strokes currently read as mechanically straight marks. Width can respond to sampled velocity, but rasterization does not model the start, continuous footprint, direction, and lift of a physical brush. In particular, the end cap reflects only the final stored radius rather than how the brush arrived and left the canvas.

## Decision

Render each gesture as the trace of a round brush whose deposited footprint is derived from recorded motion. Do not apply a fixed decorative taper or chisel-nib effect.

A gesture containing one point is a dab. It produces one round pressure footprint and no synthetic tail. A moving gesture uses velocity-derived width throughout its path: slower movement deposits a wider mark and faster movement deposits a narrower mark. Width changes remain smoothed so acceleration and deceleration do not create abrupt mechanical steps.

The rasterizer interpolates position and radius continuously between samples. Sampling density is based on both distance and radius change, keeping adjacent footprints overlapping without gaps while avoiding unnecessary stamps. Direction comes from the local path tangent; it determines where the footprint advances but does not turn the round brush into an angled nib.

The beginning and ending are consequences of gesture state:

- A stationary start retains the initial round pressure footprint.
- Movement transitions continuously from that footprint into the velocity-derived body.
- A slow final movement leaves a fuller, rounder ending.
- A fast final movement reaches a narrower final radius and therefore produces a finer tail.
- Ending a stationary dab never invents movement or a taper.

No unconditional end taper is synthesized after input ends. Doing so would make every lift look alike and would contradict the requirement that the brush follow the gesture.

## Architecture

`GlyphStrokeTracker` remains responsible for converting timed input points into bounded, smoothed segment widths. It must preserve enough endpoint information for the last deposited radius to reflect final velocity without mutating earlier segments.

`GlyphRasterizer` remains responsible for turning points and width samples into pixels. Its stroke renderer will treat a point stroke as one circular footprint and a moving stroke as an interpolated sequence of circular footprints. Start and end caps use their local endpoint radii rather than one uniform stroke radius.

The persisted glyph model does not gain a separate decorative cap or taper field. Existing point geometry and width profiles contain the required information, keeping saved glyphs deterministic and avoiding renderer-only state that recognition cannot reproduce.

## Invariants

- A one-point stroke rasterizes as one round dot.
- No-motion input never creates a tail.
- Slow segments are wider than fast segments within configured brush bounds.
- Recorded segment widths remain immutable.
- Width and radius interpolation are finite, bounded, and deterministic.
- Consecutive footprints overlap sufficiently to avoid holes on diagonal or curved paths.
- The final footprint uses the final local width; it does not revert to a uniform width.
- Existing uniform-width strokes retain their current appearance.
- Raster output remains bounded to the 128 by 128 canvas.

## Verification

Focused raster tests will cover:

- a single-point dab producing a compact round footprint with no trailing pixels;
- a slow stroke ending wider than an otherwise equivalent fast stroke;
- a fast final section producing a finer endpoint than a slow final section;
- mixed-width curved and diagonal strokes remaining continuous without gaps;
- endpoint pixels reflecting local start and end widths;
- legacy uniform-width strokes remaining deterministic.

Tracker tests will continue to verify finite bounded width generation, monotonic smoothed response to velocity changes, and lifecycle reset behavior. The compiler and MapGUI module test suites will verify model and integration compatibility. The actual glyph drawing surface will be exercised to confirm that a dab looks like a dot and moving strokes visibly respond to speed through their endings.
