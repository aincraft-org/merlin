# Glyph Velocity Smoothing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make calligraphic stroke width respond smoothly but promptly to drawing velocity.

**Architecture:** Keep the existing instantaneous velocity-to-width mapping as the target signal. `GlyphStrokeTracker` owns one elapsed-time-aware exponentially smoothed width for each active stroke, records that value for new segments, and resets it at every lifecycle boundary; persisted strokes and rasterization remain unchanged.

**Tech Stack:** Java 25, Gradle, JUnit 5, Paper/MapGUI integration.

## Global Constraints

- Slow movement produces a wider stroke; fast movement produces a narrower stroke.
- Existing recorded segment widths remain immutable.
- Every emitted width is finite and within `MIN_BRUSH_WIDTH..MAX_BRUSH_WIDTH`.
- The first moving segment initializes directly from its measured velocity.
- Zero, negative, and unusually large elapsed times cannot produce `NaN`, overshoot, or unbounded lag.
- `beginStroke`, `endStroke`, `clear`, and `undo` reset smoothing state.
- Do not change glyph serialization or rasterization.

---

### Task 1: Smooth calligraphic velocity transitions

**Files:**
- Modify: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java`
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java`

**Interfaces:**
- Consumes: existing `GlyphStrokeTracker.beginStroke(int, int, long)`, `appendPoint(int, int, long)`, lifecycle methods, and `widthForVelocity(double)`.
- Produces: private per-stroke smoothed-width state and a bounded elapsed-time-aware update used by `appendPoint`; public API remains unchanged.

- [ ] **Step 1: Write failing transition and reset tests**

Add focused tests that inspect the final width recorded for each appended endpoint. Use one-pixel moves so every append contributes exactly one segment:

```java
@Test void slowToFastTransitionNarrowsSmoothlyAndConverges() {
    var tracker = new GlyphStrokeTracker();
    tracker.beginStroke(10, 10, 0);
    tracker.appendPoint(11, 10, 100); // slow: initialize wide
    double slow = lastWidth(tracker);
    tracker.appendPoint(12, 10, 101); // sudden fast target
    double firstFast = lastWidth(tracker);
    tracker.appendPoint(13, 10, 102);
    double secondFast = lastWidth(tracker);

    assertTrue(firstFast < slow);
    assertTrue(firstFast > GlyphStrokeTracker.MIN_BRUSH_WIDTH);
    assertTrue(secondFast < firstFast);
}

@Test void fastToSlowTransitionWidensSmoothly() {
    var tracker = new GlyphStrokeTracker();
    tracker.beginStroke(10, 10, 0);
    tracker.appendPoint(11, 10, 1); // fast: initialize narrow
    double fast = lastWidth(tracker);
    tracker.appendPoint(12, 10, 101); // sudden slow target
    double firstSlow = lastWidth(tracker);
    tracker.appendPoint(13, 10, 201);
    double secondSlow = lastWidth(tracker);

    assertTrue(firstSlow > fast);
    assertTrue(firstSlow < GlyphStrokeTracker.MAX_BRUSH_WIDTH);
    assertTrue(secondSlow > firstSlow);
}

@Test void newStrokeDoesNotInheritPreviousSmoothedWidth() {
    var tracker = new GlyphStrokeTracker();
    tracker.beginStroke(10, 10, 0);
    tracker.appendPoint(11, 10, 100);
    tracker.appendPoint(12, 10, 101);
    tracker.endStroke(101);

    tracker.beginStroke(20, 20, 200);
    tracker.appendPoint(21, 20, 201);

    assertEquals(GlyphStrokeTracker.MIN_BRUSH_WIDTH, lastWidth(tracker));
}

@Test void irregularTimestampsKeepWidthsFiniteAndBounded() {
    var tracker = new GlyphStrokeTracker();
    tracker.beginStroke(10, 10, 100);
    tracker.appendPoint(11, 10, 100);
    tracker.appendPoint(12, 10, 99);
    tracker.appendPoint(13, 10, Long.MAX_VALUE);

    for (double width : tracker.snapshot().strokes().getFirst().segmentWidths()) {
        assertTrue(Double.isFinite(width));
        assertTrue(width >= GlyphStrokeTracker.MIN_BRUSH_WIDTH);
        assertTrue(width <= GlyphStrokeTracker.MAX_BRUSH_WIDTH);
    }
}

private static double lastWidth(GlyphStrokeTracker tracker) {
    return tracker.snapshot().strokes().getLast().segmentWidths().getLast();
}
```

If `clear` and `undo` reset behavior is not already implied by the new-stroke test, add equivalent short cases that draw a slow segment, call the lifecycle method, then verify a fast first segment equals `MIN_BRUSH_WIDTH`.

- [ ] **Step 2: Run tests and verify the transition assertions fail**

Run:

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphStrokeTrackerTest
```

Expected: slow-to-fast and fast-to-slow transition tests fail because current widths jump directly to the target; lifecycle and boundedness tests expose any stale or unsafe state.

- [ ] **Step 3: Implement bounded elapsed-time-aware smoothing**

In `GlyphStrokeTracker`, add one nullable/flagged smoothed width per active stroke and a small response constant. Initialize the first moving segment directly from `widthForVelocity(velocity)`. For subsequent segments:

```java
private static final double SMOOTHING_TIME_MILLIS = 12.0;
private boolean hasSmoothedWidth;
private double smoothedWidth;

private double smoothWidth(double targetWidth, long elapsedMillis) {
    if (!hasSmoothedWidth) {
        smoothedWidth = targetWidth;
        hasSmoothedWidth = true;
        return smoothedWidth;
    }
    double elapsed = Math.max(0.0, Math.min(1000.0, (double) elapsedMillis));
    double factor = 1.0 - Math.exp(-elapsed / SMOOTHING_TIME_MILLIS);
    factor = Math.max(0.0, Math.min(1.0, factor));
    smoothedWidth += (targetWidth - smoothedWidth) * factor;
    smoothedWidth = Math.max(MIN_BRUSH_WIDTH, Math.min(MAX_BRUSH_WIDTH, smoothedWidth));
    return smoothedWidth;
}

private void resetSmoothing() {
    hasSmoothedWidth = false;
    smoothedWidth = 0.0;
}
```

Calculate elapsed time safely before updating `lastAt`: subtraction must not overflow. Clamp timestamp ordering by deriving a non-negative bounded elapsed value with comparisons rather than raw overflowing subtraction. Pass the smoothed width to each interpolated segment created for the append. Invoke `resetSmoothing()` from `beginStroke`, `endStroke`/`finishActive`, `clear`, and `undo`. When the point-limit rollover creates a continuation stroke within one gesture, preserve the current smoothed width so visual continuity is not broken.

- [ ] **Step 4: Run focused tests and tune only the response constant if needed**

Run:

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphStrokeTrackerTest
```

Expected: PASS. The transition tests must show monotonic movement toward the target without an instantaneous jump, while the second transitioned sample is closer to the target than the first.

- [ ] **Step 5: Run the integration module tests**

Run:

```bash
./gradlew :mapgui-integration:test
```

Expected: `BUILD SUCCESSFUL` with all existing stroke lifecycle and screen integration behavior preserved.

- [ ] **Step 6: Commit the behavior and tests atomically**

```bash
git add mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java
git diff --cached --check
git commit -m "feat: smooth calligraphy stroke velocity"
```
