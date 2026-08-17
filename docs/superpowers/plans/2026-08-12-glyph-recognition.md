# Glyph Recognition and Map Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement vector-authoritative Glyphcraft drafts, full-map and normalized-crop bitmap derivation, deterministic template recognition, and a Paper-independent capture seam.

**Architecture:** Add a dependency-free glyph core under `java-compiler` or a focused shared Java module so recognition remains independent of Bukkit. `GlyphDraft` stores bounded vector strokes; rasterization derives the full 128×128 bitmap and normalized crop. A deterministic recognizer ranks catalog templates behind an interface that can later accept a lightweight classifier. Paper only owns capture/session and persistence wiring; MapGUI is not imported until its API is confirmed.

**Tech Stack:** Java 21, Gradle, JUnit 5, Paper API only in the Paper module, no ML dependency in the first release.

## Global Constraints

- Vector strokes are authoritative; bitmaps are derived artifacts.
- The full canvas is 128×128 and preserves map placement.
- Recognition uses a normalized crop plus vector geometry features.
- Reject non-finite/out-of-bounds points, empty strokes, excessive strokes/points, and oversized drafts.
- Recognition is deterministic and confidence never changes enchantment power or cost.
- No live MapGUI dependency or heavy ML model in this milestone.

---

### Task 1: Add bounded glyph data model

**Files:**
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphPoint.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphStroke.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphDraft.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphLimits.java`
- Test: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphDraftTest.java`

**Interfaces:**
- `GlyphPoint(double x, double y)` validates finite coordinates and canvas bounds.
- `GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis)` validates non-empty points, finite positive brush width, and point count.
- `GlyphDraft(List<GlyphStroke> strokes)` is immutable and validates stroke count and serialized-size assumptions.
- `GlyphDraft` exposes defensive immutable lists and `canvasWidth()`/`canvasHeight()` constants.

- [ ] **Step 1: Write failing tests**

Cover finite/in-range points, rejection of NaN/infinity/out-of-bounds values, empty strokes, excessive points/strokes, defensive copies, and stable equality.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :java-compiler:test --tests '*GlyphDraftTest*'`; expected failure because the types do not exist.

- [ ] **Step 3: Implement immutable model**

Use records where validation fits; copy all lists with `List.copyOf`, enforce exact limits in `GlyphLimits`, and expose no mutable backing state.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/glyph java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphDraftTest.java
git commit -m "feat: add bounded glyph draft model"
```

### Task 2: Derive full-map and normalized-crop bitmaps

**Files:**
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphBitmap.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphRasterizer.java`
- Test: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphRasterizerTest.java`

**Interfaces:**
- `GlyphBitmap(int width, int height, byte[] pixels)` stores immutable grayscale pixels.
- `GlyphRasterizer.renderFull(GlyphDraft): GlyphBitmap` returns 128×128 output.
- `GlyphRasterizer.renderNormalized(GlyphDraft, int size, int padding): GlyphBitmap` crops non-empty bounds, pads deterministically, and resamples to `size×size`.
- `GlyphRasterizer.features(GlyphDraft): GlyphFeatures` returns full bitmap, normalized bitmap, bounding box, stroke count, and geometric summaries.

- [ ] **Step 1: Write failing tests**

Test stable pixel output for a horizontal/diagonal stroke, full dimensions, translation invariance of normalized crops, scale invariance within bounds, empty-draft behavior, and no mutation of the draft.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :java-compiler:test --tests '*GlyphRasterizerTest*'`; expected failure because rasterization is absent.

- [ ] **Step 3: Implement deterministic rasterization**

Rasterize line segments with a fixed integer coverage rule and brush width; derive the full canvas first, calculate non-empty bounds, add fixed padding, and use deterministic nearest-neighbor or area averaging for the normalized crop. Ensure identical input produces identical byte arrays.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/glyph java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphRasterizerTest.java
git commit -m "feat: derive glyph map and crop bitmaps"
```

### Task 3: Implement deterministic template recognition

**Files:**
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphTemplate.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphMatch.java`
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphRecognizer.java`
- Test: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphRecognizerTest.java`

**Interfaces:**
- `GlyphTemplate(String id, GlyphDraft canonicalDraft, double acceptanceThreshold)` stores canonical geometry and derived features.
- `GlyphMatch(String templateId, double confidence, boolean accepted)` is immutable.
- `GlyphRecognizer(List<GlyphTemplate> catalog).recognize(GlyphDraft): List<GlyphMatch>` returns deterministic descending-ranked candidates.

- [ ] **Step 1: Write failing tests**

Create simple line, angle, and closed-loop templates. Assert translated/scaled inputs rank the expected template first, unrelated shapes rank lower, ties are stable, low confidence is rejected, and repeated recognition returns byte-for-byte-equivalent results.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :java-compiler:test --tests '*GlyphRecognizerTest*'`; expected failure.

- [ ] **Step 3: Implement scoring**

Combine normalized bitmap distance, stroke count mismatch, endpoint/direction distance, and enclosure/intersection penalties with fixed weights. Clamp confidence to `[0,1]`; mark acceptance only when the top score crosses the template threshold and beats the runner-up by the ambiguity margin.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/glyph java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphRecognizerTest.java
git commit -m "feat: recognize glyphs with deterministic templates"
```

### Task 4: Add Paper-independent capture/session seam

**Files:**
- Create: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphCaptureSession.java`
- Test: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphCaptureSessionTest.java`
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/glyph/GlyphDraftStore.java`
- Test: `paper/src/test/java/dev/mintychochip/wizardry/paper/glyph/GlyphDraftStoreTest.java`

**Interfaces:**
- `GlyphCaptureSession.beginStroke(long now)`, `appendPoint(GlyphPoint)`, `endStroke()`, `undo()`, `clear()`, `snapshot()`, and `close()`.
- `GlyphDraftStore.save(ItemStack, GlyphDraft): boolean`, `load(ItemStack): Optional<GlyphDraft>`, and `clear(ItemStack): boolean` use plugin-owned PDC data.

- [ ] **Step 1: Write failing tests**

Test stroke lifecycle, append bounds, undo, clear, close preserving the last snapshot, immutable snapshots, exact marked-item checks, and round-trip persistence.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :java-compiler:test --tests '*GlyphCaptureSessionTest*' :paper:test --tests '*GlyphDraftStoreTest*'`; expected failure.

- [ ] **Step 3: Implement capture and store**

Keep session mutations local and bounded. Serialize a versioned compact representation with fixed numeric encoding; reject oversized PDC payloads. Do not reference MapGUI types. Require a marked glyph item before PDC mutation.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/glyph paper/src/main/java/dev/mintychochip/wizardry/paper/glyph java-compiler/src/test/java paper/src/test/java
git commit -m "feat: add glyph capture and Paper persistence seam"
```

### Task 5: Verify integration and document model boundary

**Files:**
- Modify: `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`
- Modify: `docs/superpowers/specs/2026-08-12-glyph-recognition-design.md`

- [ ] **Step 1: Document full-canvas bitmap semantics**

State that the 128×128 full bitmap is a derived map-sized artifact, while vector strokes remain authoritative and the normalized crop is recognition input. State that no trained model ships in the first release.

- [ ] **Step 2: Run complete verification**

Run `./gradlew :java-compiler:test :paper:test :paper:build`.

- [ ] **Step 3: Inspect artifacts**

Confirm glyph classes are present in the Java compiler and Paper jars, and no MapGUI or ML dependency was introduced.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-08-12-glyph-recognition-design.md docs/superpowers/specs/2026-08-12-glyph-recognition-design.md
git commit -m "docs: define glyph bitmap and recognizer boundaries"
```
