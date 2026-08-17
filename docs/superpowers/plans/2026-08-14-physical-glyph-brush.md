# Physical Glyph Brush Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render glyph gestures as continuous velocity-dependent round-brush marks whose dabs, bodies, and endings follow recorded motion.

**Architecture:** Keep velocity interpretation and smoothing in `GlyphStrokeTracker`; keep persistence unchanged. Refine `GlyphRasterizer` so each stroke is a sequence of overlapping circular brush footprints, with interpolation density determined by movement distance and local radius change. Point strokes remain a single circular dab, while moving stroke endpoints use their local widths.

**Tech Stack:** Java 25, Gradle, JUnit 5, Paper/MapGUI integration.

## Global Constraints

- A one-point gesture produces one round dot and no synthetic tail.
- Slow movement is wider than fast movement within existing brush limits.
- Stroke width changes are continuous and deterministic.
- No fixed decorative taper or chisel-nib geometry is introduced.
- Existing uniform-width glyphs remain compatible.
- Raster output remains bounded to the 128 by 128 canvas.

---

### Task 1: Specify Physical Brush Raster Contracts

**Files:**
- Modify: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphTest.java`

**Interfaces:**
- Consumes: `GlyphRasterizer.renderFull(GlyphDraft): GlyphBitmap`, `GlyphStroke(List<GlyphPoint>, double, long, List<Double>)`.
- Produces: behavioral coverage for dab geometry, local endpoint width, and continuous curved/diagonal marks.

- [ ] **Step 1: Add a dab contract test**

Create a one-point stroke centered away from canvas boundaries. Assert that its center and symmetric cardinal pixels are inked, that equal-radius horizontal and vertical extents match, and that no pixels occur outside the expected circular footprint. This proves the dab is a dot rather than a zero-length line or synthetic tail.

```java
@Test void stationaryDabProducesOneRoundFootprintWithoutTail() {
    var draft = new GlyphDraft(List.of(new GlyphStroke(
            List.of(new GlyphPoint(64, 64)), 6, 0)));
    var pixels = GlyphRasterizer.renderFull(draft).pixels();

    assertInk(pixels, 64, 64);
    assertInk(pixels, 61, 64);
    assertInk(pixels, 67, 64);
    assertInk(pixels, 64, 61);
    assertInk(pixels, 64, 67);
    assertBlank(pixels, 68, 64);
    assertBlank(pixels, 64, 68);
}
```

Add helpers:

```java
private static void assertInk(byte[] pixels, int x, int y) {
    assertNotEquals(0, pixels[y * 128 + x] & 0xff);
}

private static void assertBlank(byte[] pixels, int x, int y) {
    assertEquals(0, pixels[y * 128 + x] & 0xff);
}
```

- [ ] **Step 2: Add endpoint-velocity contract tests**

Construct two geometrically identical strokes with different final local widths. Assert that the slow-ending stroke has a thicker final cross-section while both start from the same footprint.

```java
@Test void finalFootprintUsesFinalVelocityDerivedWidth() {
    var slow = new GlyphDraft(List.of(new GlyphStroke(
            List.of(new GlyphPoint(20, 30), new GlyphPoint(40, 30), new GlyphPoint(60, 30)),
            4, 0, List.of(4.0, 6.0))));
    var fast = new GlyphDraft(List.of(new GlyphStroke(
            List.of(new GlyphPoint(20, 50), new GlyphPoint(40, 50), new GlyphPoint(60, 50)),
            4, 0, List.of(4.0, 1.0))));

    var slowPixels = GlyphRasterizer.renderFull(slow).pixels();
    var fastPixels = GlyphRasterizer.renderFull(fast).pixels();
    assertTrue(columnInk(slowPixels, 60) > columnInk(fastPixels, 60));
}

private static int columnInk(byte[] pixels, int x) {
    int count = 0;
    for (int y = 0; y < 128; y++) if ((pixels[y * 128 + x] & 0xff) != 0) count++;
    return count;
}
```

- [ ] **Step 3: Add continuity coverage for sparse diagonal width changes**

Use a long diagonal segment changing from width 6 to width 1. For each integer projection along the centerline, assert at least one pixel in its 3 by 3 neighborhood is inked. This catches gaps caused by under-sampling position or radius.

```java
@Test void sparseDiagonalWithChangingRadiusRemainsContinuous() {
    var draft = new GlyphDraft(List.of(new GlyphStroke(
            List.of(new GlyphPoint(16, 16), new GlyphPoint(96, 96)),
            6, 0, List.of(1.0))));
    var pixels = GlyphRasterizer.renderFull(draft).pixels();

    for (int coordinate = 16; coordinate <= 96; coordinate++) {
        assertTrue(hasInkNear(pixels, coordinate, coordinate));
    }
}

private static boolean hasInkNear(byte[] pixels, int cx, int cy) {
    for (int y = Math.max(0, cy - 1); y <= Math.min(127, cy + 1); y++)
        for (int x = Math.max(0, cx - 1); x <= Math.min(127, cx + 1); x++)
            if ((pixels[y * 128 + x] & 0xff) != 0) return true;
    return false;
}
```

- [ ] **Step 4: Run focused tests before implementation**

Run:

```bash
./gradlew :java-compiler:test --tests dev.mintychochip.wizardry.glyph.GlyphTest
```

Expected: the new tests expose any mismatch in dab bounds, endpoint-local width, or sparse interpolation. Existing raster tests must remain green. If the current implementation already satisfies an individual observable contract, retain that test as regression coverage rather than weakening it.

---

### Task 2: Render Continuous Round-Brush Footprints

**Files:**
- Modify: `java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphRasterizer.java`
- Modify: `java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphTest.java`

**Interfaces:**
- Consumes: immutable `GlyphStroke.points()`, `GlyphStroke.brushWidth()`, and `GlyphStroke.widthAtSegment(int)`.
- Produces: unchanged public `renderFull` and `renderNormalized` APIs with physical round-brush raster behavior.

- [ ] **Step 1: Centralize canvas bounds and endpoint width selection**

Replace literal `127` and `128` indexing in `stamp` with `GlyphLimits.CANVAS_WIDTH` and `GlyphLimits.CANVAS_HEIGHT`. Preserve the established endpoint convention: the first point uses `brushWidth()`, and each segment endpoint uses its recorded `widthAtSegment` value.

```java
private static void stamp(byte[] pixels, GlyphPoint point, double width) {
    double radius = width / 2.0;
    int bounds = Math.max(0, (int) Math.ceil(radius + 0.5));
    int cx = (int) Math.round(point.x());
    int cy = (int) Math.round(point.y());
    int maxX = GlyphLimits.CANVAS_WIDTH - 1;
    int maxY = GlyphLimits.CANVAS_HEIGHT - 1;
    for (int y = Math.max(0, cy - bounds); y <= Math.min(maxY, cy + bounds); y++) {
        for (int x = Math.max(0, cx - bounds); x <= Math.min(maxX, cx + bounds); x++) {
            if (Math.hypot(x - point.x(), y - point.y()) <= radius + 0.5) {
                pixels[y * GlyphLimits.CANVAS_WIDTH + x] = (byte) 255;
            }
        }
    }
}
```

- [ ] **Step 2: Derive interpolation density from travel and radius change**

Replace the fixed `distance * 2` rule with a helper that guarantees at most half-pixel center travel and at most quarter-pixel radius change per stamp. This preserves overlap for narrow diagonal strokes and smooths rapidly changing widths without allocating intermediate point lists.

```java
private static int interpolationSteps(
        GlyphPoint start, GlyphPoint end, double startWidth, double endWidth) {
    double distance = Math.hypot(end.x() - start.x(), end.y() - start.y());
    double radiusDelta = Math.abs(endWidth - startWidth) / 2.0;
    int movementSteps = (int) Math.ceil(distance / 0.5);
    int radiusSteps = (int) Math.ceil(radiusDelta / 0.25);
    return Math.max(1, Math.max(movementSteps, radiusSteps));
}
```

Use it directly in `drawLine`; interpolate position and width as doubles and stamp immediately.

```java
private static void drawLine(
        byte[] pixels, GlyphPoint start, GlyphPoint end,
        double startWidth, double endWidth) {
    int steps = interpolationSteps(start, end, startWidth, endWidth);
    for (int index = 0; index <= steps; index++) {
        double t = (double) index / steps;
        double x = start.x() + (end.x() - start.x()) * t;
        double y = start.y() + (end.y() - start.y()) * t;
        double width = startWidth + (endWidth - startWidth) * t;
        stamp(pixels, new GlyphPoint(x, y), width);
    }
}
```

Do not add a post-stroke taper. The final stamp already represents final velocity through `endWidth`; a point stroke continues to call `stamp` exactly once.

- [ ] **Step 3: Run focused raster tests**

Run:

```bash
./gradlew :java-compiler:test --tests dev.mintychochip.wizardry.glyph.GlyphTest
```

Expected: PASS. The dab remains round with no tail, sparse changing-width diagonals contain no gaps, and final cross-sections reflect local width.

- [ ] **Step 4: Run the compiler module suite**

Run:

```bash
./gradlew :java-compiler:test
```

Expected: `BUILD SUCCESSFUL`; normalized rendering, ML preprocessing contracts, and legacy uniform-width behavior remain compatible.

- [ ] **Step 5: Commit raster behavior and tests atomically**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/glyph/GlyphRasterizer.java \
  java-compiler/src/test/java/dev/mintychochip/wizardry/glyph/GlyphTest.java
git diff --cached --check
git commit -m "feat: render glyphs as physical brush strokes"
```

---

### Task 3: Verify Velocity-to-Brush Integration

**Files:**
- Modify: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java` only if existing coverage does not assert the final stored width.
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java` only if focused tests show final velocity is not recorded correctly.

**Interfaces:**
- Consumes: `GlyphStrokeTracker.beginStroke(int, int, long)`, `appendPoint(int, int, long)`, `endStroke(long)`, and `snapshot()`.
- Produces: a persisted final segment width that the rasterizer can use as the physical ending footprint.

- [ ] **Step 1: Confirm tracker tests cover a dot and contrasting final velocities**

Ensure existing tests prove that beginning and ending without movement creates one point with no segment widths, and that otherwise identical final movements with longer elapsed time produce a wider final recorded segment than short elapsed time. Add only missing observable coverage:

```java
@Test void stationaryGestureRemainsAOnePointDab() {
    var tracker = new GlyphStrokeTracker();
    tracker.beginStroke(30, 30, 100);
    tracker.endStroke(200);

    var stroke = tracker.snapshot().strokes().getFirst();
    assertEquals(1, stroke.points().size());
    assertTrue(stroke.segmentWidths().isEmpty());
}

@Test void finalRecordedWidthReflectsFinalVelocity() {
    var slow = new GlyphStrokeTracker();
    slow.beginStroke(10, 10, 0);
    slow.appendPoint(11, 10, 100);
    slow.appendPoint(12, 10, 200);

    var fast = new GlyphStrokeTracker();
    fast.beginStroke(10, 20, 0);
    fast.appendPoint(11, 20, 100);
    fast.appendPoint(12, 20, 101);

    assertTrue(lastWidth(slow) > lastWidth(fast));
}
```

Reuse the suite's existing `lastWidth` helper if present.

- [ ] **Step 2: Run focused tracker tests**

Run:

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphStrokeTrackerTest
```

Expected: PASS if current velocity smoothing already records physical endpoint width. If it fails, fix the source calculation rather than adding renderer heuristics; preserve bounded elapsed-time smoothing and lifecycle resets.

- [ ] **Step 3: Run both affected module suites**

Run:

```bash
./gradlew :java-compiler:test :mapgui-integration:test
```

Expected: `BUILD SUCCESSFUL` with glyph model, storage, screen, and raster tests green.

- [ ] **Step 4: Commit only if integration behavior changed**

If tests required tracker production or test changes:

```bash
git add mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java \
  mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java
git diff --cached --check
git commit -m "fix: preserve glyph brush endpoint velocity"
```

If existing behavior already satisfies the contract and no files changed, do not create an empty commit.

---

### Task 4: Exercise the Actual Drawing Surface

**Files:**
- No source changes expected.

**Interfaces:**
- Consumes: built MapGUI integration plugin and its existing glyph drawing screen.
- Produces: behavioral evidence from the real surface rather than test-only confidence.

- [ ] **Step 1: Build the plugin artifact used by the server**

Run:

```bash
./gradlew :mapgui-integration:build
```

Expected: `BUILD SUCCESSFUL` and the integration JAR is produced under `mapgui-integration/build/libs/`.

- [ ] **Step 2: Launch the project’s existing Paper smoke environment**

Use the repository's established server launch path. Stop or isolate any existing Paper process first, verify the loaded MapGUI plugin is the newly built `1.0.0-SNAPSHOT`, and open the glyph drawing screen.

- [ ] **Step 3: Exercise physical brush scenarios**

On the actual surface:

1. Press and release without moving; observe one round dot with no tail.
2. Draw a slow moving stroke; observe a broad continuous mark and full ending.
3. Draw the same path quickly; observe a narrower mark and finer ending.
4. Draw a curved or diagonal path; observe no gaps, blocky circles, or abrupt width jumps.

Expected: all four visual behaviors match the approved design. If the current MapGUI input API provides only discrete clicks rather than continuous movement, record that runtime limitation exactly and verify the same scenarios at the finest input granularity the surface exposes; do not claim pointer-level behavior that was not exercised.

- [ ] **Step 4: Run final focused verification**

Run:

```bash
./gradlew :java-compiler:test :mapgui-integration:test
```

Expected: `BUILD SUCCESSFUL` after the runtime exercise, with no source changes introduced by smoke testing.
