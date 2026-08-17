# Glyph Stroke Boundaries and Calligraphic Width Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make glyph drawing support separate strokes and preserve speed-derived brush width per segment.

**Architecture:** Extend the glyph model with immutable per-segment width samples while retaining uniform-width compatibility. Make `GlyphStrokeTracker` lifecycle-driven and route screen actions through explicit begin/end behavior. Rasterize each segment using interpolated local widths.

**Tech Stack:** Java 25, Gradle, JUnit 5, Paper/MapGUI integration.

## Global Constraints

- Preserve old serialized glyphs with a uniform brush width.
- Never mutate widths of already-recorded segments.
- Keep the 128x128 canvas and existing point/stroke limits.
- Do not infer stroke boundaries from elapsed time.

### Task 1: Add failing model and raster tests

**Files:**
- Modify: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphTest.java`
- Modify: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java`

- [ ] Test explicit begin/end creates two independent strokes.
- [ ] Test later width updates do not alter earlier segment widths.
- [ ] Test rasterization of a mixed-width stroke has distinct local thicknesses.
- [ ] Run focused tests and confirm they fail for the missing model behavior.

### Task 2: Implement per-segment width model

**Files:**
- Modify: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphStroke.java`
- Modify: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphCaptureSession.java`
- Modify: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphRasterizer.java`

- [ ] Add an immutable width profile whose length is zero for a point stroke and otherwise equals `points.size()-1`.
- [ ] Keep the existing `(points, brushWidth, startedAtMillis)` constructor as uniform legacy behavior.
- [ ] Record one width per appended segment, validate all widths, and rasterize with interpolated widths.
- [ ] Run focused compiler tests and confirm green.

### Task 3: Implement explicit tracker lifecycle and UI routing

**Files:**
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java`
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphScreen.java`
- Modify: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphScreenModelTest.java`

- [ ] Add synchronized `beginStroke`, `appendPoint`, and `endStroke` methods.
- [ ] Make `acceptClick` append to the active stroke without timeout-based separation.
- [ ] Ensure clear, undo, menu, save, and close end an active stroke before operating.
- [ ] Add an explicit screen action used by the existing callback path to end the current stroke and verify two disjoint strokes.
- [ ] Run integration tests.

### Task 4: Preserve storage compatibility and verify end-to-end

**Files:**
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapter.java` only if model encoding requires a profile.
- Modify: relevant tests.

- [ ] Read old uniform glyph records into the new model without changing their appearance.
- [ ] Encode width profiles deterministically when present.
- [ ] Run `./gradlew :java-compiler:test :mapgui-integration:test`.
- [ ] Run the actual server smoke path only after stopping or isolating any existing Paper process, and verify the loaded MapGUI version is `1.0.0-SNAPSHOT`.
