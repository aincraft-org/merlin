package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphLimits;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;
import dev.mintychochip.merlin.api.glyph.GlyphStroke;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GlyphStrokeTrackerTest {
    @Test void repeatedClicksInterpolateAndPauseEndsStroke() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, 100);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertTrue(tracker.snapshot().strokes().getFirst().points().size() >= 4);
        tracker.pause(400);
        tracker.acceptClick(10, 10, 401);
        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void explicitBeginAndEndKeepDisjointStrokesSeparate() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(1, 1, 0);
        tracker.appendPoint(2, 2, 10);
        tracker.endStroke(20);
        tracker.beginStroke(20, 20, 30);
        tracker.endStroke(40);
        assertEquals(2, tracker.snapshot().strokes().size());
    }

    @Test void laterVelocityDoesNotChangeEarlierSegmentWidth() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(1, 1, 0);
        tracker.appendPoint(2, 2, 100);
        double first = tracker.snapshot().strokes().getFirst().segmentWidths().getFirst();
        tracker.appendPoint(3, 3, 101);
        assertEquals(first, tracker.snapshot().strokes().getFirst().segmentWidths().getFirst());
        assertNotEquals(first, tracker.snapshot().strokes().getFirst().segmentWidths().get(1));
    }
    @Test void rapidClicksRemainOneConnectedStroke() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, 100);
        tracker.acceptClick(8, 8, 200);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertTrue(tracker.snapshot().strokes().getFirst().points().size() > 1);
    }

    @Test void delayedClickStartsANewStroke() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, GlyphStrokeTracker.STROKE_PAUSE_MILLIS + 1);
        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void longContinuousStrokeDoesNotStopPaintingAtPointLimit() {
        var tracker = GlyphStrokeTracker.unconstrained();
        for (int i = 0; i < 400; i++) {
            tracker.acceptClick(i % 128, (i * 3) % 128, i);
        }

        assertDoesNotThrow(() -> tracker.snapshot());
        assertTrue(tracker.snapshot().strokes().size() > 1);
    }
    @Test void velocityControlsBrushWidthForCalligraphy() {
        assertEquals(GlyphStrokeTracker.MAX_BRUSH_WIDTH, GlyphStrokeTracker.widthForVelocity(0));
        assertEquals(GlyphStrokeTracker.MIN_BRUSH_WIDTH, GlyphStrokeTracker.widthForVelocity(1));
        assertTrue(GlyphStrokeTracker.widthForVelocity(0.25) > GlyphStrokeTracker.widthForVelocity(0.5));
    }
    @Test void slowToFastTransitionNarrowsSmoothly() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(11, 10, 100);
        double slow = lastWidth(tracker);
        tracker.appendPoint(12, 10, 101);
        double firstFast = lastWidth(tracker);
        tracker.appendPoint(13, 10, 102);
        double secondFast = lastWidth(tracker);

        assertTrue(firstFast < slow);
        assertTrue(firstFast > GlyphStrokeTracker.MIN_BRUSH_WIDTH);
        assertTrue(secondFast < firstFast);
    }

    @Test void fastToSlowTransitionWidensSmoothly() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(11, 10, 1);
        double fast = lastWidth(tracker);
        tracker.appendPoint(12, 10, 101);
        double firstSlow = lastWidth(tracker);
        tracker.appendPoint(13, 10, 201);
        double secondSlow = lastWidth(tracker);

        assertTrue(firstSlow > fast);
        assertTrue(firstSlow < GlyphStrokeTracker.MAX_BRUSH_WIDTH);
        assertTrue(secondSlow > firstSlow);
    }

    @Test void newStrokeInitializesWidthIndependently() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(11, 10, 100);
        tracker.appendPoint(12, 10, 101);
        tracker.endStroke(101);

        tracker.beginStroke(20, 20, 200);
        tracker.appendPoint(21, 20, 201);

        assertEquals(GlyphStrokeTracker.MIN_BRUSH_WIDTH, lastWidth(tracker));
    }

    @Test void irregularTimestampsKeepWidthsFiniteAndBounded() {
        var tracker = GlyphStrokeTracker.unconstrained();
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
    @Test void stationarySamplesAdvanceTheVelocityClock() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(10, 10, 100);
        tracker.appendPoint(11, 10, 101);

        assertEquals(GlyphStrokeTracker.MIN_BRUSH_WIDTH, lastWidth(tracker));
    }

    @Test void timestampOverflowEndsAnExpiredClickStroke() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(10, 10, Long.MIN_VALUE);
        tracker.acceptClick(11, 10, Long.MAX_VALUE);

        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void exactPauseThresholdStartsANewStroke() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(10, 10, 0);
        tracker.acceptClick(11, 10, GlyphStrokeTracker.STROKE_PAUSE_MILLIS);

        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void pointLimitRolloverStartsFreshSmoothingState() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(0, 0, 0);
        for (int i = 1; i < 256; i++) {
            tracker.appendPoint(i % 2, 0, i * 100L);
        }

        tracker.appendPoint(0, 0, 25_500);

        assertEquals(2, tracker.snapshot().strokes().size());
        assertEquals(
                GlyphStrokeTracker.widthForVelocity(1),
                tracker.snapshot().strokes().getLast().segmentWidths().getFirst());
    }
    @Test void stationaryGestureRemainsAOnePointDab() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(30, 30, 100);
        tracker.endStroke(200);

        var stroke = tracker.snapshot().strokes().getFirst();
        assertEquals(1, stroke.points().size());
        assertTrue(stroke.segmentWidths().isEmpty());
        assertEquals(GlyphStrokeTracker.MAX_BRUSH_WIDTH, stroke.brushWidth());
    }
    @Test void finalRecordedWidthReflectsFinalVelocity() {
        var slow = GlyphStrokeTracker.unconstrained();
        slow.beginStroke(10, 10, 0);
        slow.appendPoint(11, 10, 100);
        slow.appendPoint(12, 10, 200);

        var fast = GlyphStrokeTracker.unconstrained();
        fast.beginStroke(10, 20, 0);
        fast.appendPoint(11, 20, 100);
        fast.appendPoint(12, 20, 101);

        assertTrue(lastWidth(slow) > lastWidth(fast));
    }
    @Test void importedDraftPreservesSegmentWidths() {
        var draft = new GlyphDraft(List.of(
                new GlyphStroke(
                        List.of(new GlyphPoint(1, 1), new GlyphPoint(2, 2), new GlyphPoint(3, 3)),
                        4,
                        10,
                        List.of(5.5, 1.25))));

        assertEquals(draft, new GlyphStrokeTracker(draft).snapshot());
    }

    @Test void emptySupplyDoesNotStroke() {
        var box = new MagicalInk[1];
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        assertTrue(tracker.snapshot().strokes().isEmpty());
        assertNull(box[0]);
    }

    @Test void emptyFillDoesNotStroke() {
        var box = new MagicalInk[] { new MagicalInk(GlyphElement.FLAME, 0, 64) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        assertTrue(tracker.snapshot().strokes().isEmpty());
        assertEquals(0, box[0].remaining());
    }

    @Test void fullEmberPaintsFireAndSpendsOne() {
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FLAME) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertEquals(GlyphElement.FLAME, tracker.snapshot().strokes().getFirst().element());
        assertEquals(63, box[0].remaining());
    }

    @Test void secondStrokeReReadsSupply() {
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FLAME) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        tracker.endStroke(10);
        box[0] = MagicalInk.full(GlyphElement.FROST);
        tracker.acceptClick(20, 20, 20);
        var strokes = tracker.snapshot().strokes();
        assertEquals(2, strokes.size());
        assertEquals(GlyphElement.FLAME, strokes.get(0).element());
        assertEquals(GlyphElement.FROST, strokes.get(1).element());
    }

    @Test void midStrokeSwapDoesNotRecolor() {
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FLAME) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        box[0] = MagicalInk.full(GlyphElement.ARCANE);
        tracker.acceptClick(4, 4, 50);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertEquals(GlyphElement.FLAME, tracker.snapshot().strokes().getFirst().element());
    }

    @Test void undoDoesNotRefund() {
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FLAME) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.acceptClick(1, 1, 0);
        tracker.endStroke(10);
        tracker.undo();
        assertTrue(tracker.snapshot().strokes().isEmpty());
        assertEquals(63, box[0].remaining());
    }

    @Test void rehydrateDoesNotSpend() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(1, 1)), 2, 0, List.of(), GlyphElement.FLAME)));
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FROST) };
        var tracker = new GlyphStrokeTracker(draft, GlyphStrokeTracker.boxSupply(box));
        assertEquals(GlyphElement.FLAME, tracker.snapshot().strokes().getFirst().element());
        assertEquals(64, box[0].remaining());
    }





    @Test void undoDuringActiveStrokeResetsPen() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(2, 2, 100);
        tracker.undo();
        tracker.acceptClick(4, 4, 101);
        assertEquals(1, tracker.snapshot().strokes().size());
    }
    @Test void explicitPauseEndsStrokeForNextClick() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(2, 2, 100);
        tracker.pause(2000);
        tracker.acceptClick(4, 4, 2001);
        assertEquals(2, tracker.snapshot().strokes().size());
    }

    @Test void sparseRightAnglePollSamplesRoundTheCorner() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(50, 10, 50);
        tracker.appendPoint(50, 50, 100);
        assertRoundedRightAngle(tracker.snapshot().strokes().getFirst());

        var clicked = GlyphStrokeTracker.unconstrained();
        clicked.acceptClick(10, 10, 0);
        clicked.acceptClick(50, 10, 50);
        clicked.acceptClick(50, 50, 100);
        assertRoundedRightAngle(clicked.snapshot().strokes().getFirst());
    }

    @Test void onePixelPrefixStillRoundsALaterSparseTurn() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(11, 10, 20);
        tracker.appendPoint(50, 10, 70);
        tracker.appendPoint(50, 50, 120);
        var points = tracker.snapshot().strokes().getFirst().points();
        assertTrue(
                hasPointInsideCorner(points),
                () -> "1px prefix must not disable spline on a later sparse turn: " + points);
        assertTrue(reachesSample(points, 10, 10), () -> "missed start sample: " + points);
        assertTrue(reachesSample(points, 50, 50), () -> "missed end sample: " + points);

        var twoSteps = GlyphStrokeTracker.unconstrained();
        twoSteps.beginStroke(10, 10, 0);
        twoSteps.appendPoint(11, 10, 20);
        twoSteps.appendPoint(12, 10, 40);
        twoSteps.appendPoint(50, 10, 90);
        twoSteps.appendPoint(50, 50, 140);
        assertTrue(
                hasPointInsideCorner(twoSteps.snapshot().strokes().getFirst().points()),
                () -> "two 1px samples must not disable a later sparse turn: "
                        + twoSteps.snapshot().strokes().getFirst().points());
    }

    @Test void collinearPollSamplesStayOnTheLine() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(40, 10, 50);
        tracker.appendPoint(80, 10, 100);
        var points = tracker.snapshot().strokes().getFirst().points();

        for (var point : points) {
            assertEquals(10.0, point.y(), 0.75, () -> "lateral deviation off the line: " + points);
        }
        assertTrue(reachesSample(points, 10, 10), () -> "missed start: " + points);
        assertTrue(reachesSample(points, 40, 10), () -> "missed mid sample: " + points);
        assertTrue(reachesSample(points, 80, 10), () -> "missed end: " + points);
    }

    @Test void interpolatedTurningStrokeSpendsOneInk() {
        var box = new MagicalInk[] { MagicalInk.full(GlyphElement.FLAME) };
        var tracker = GlyphStrokeTracker.withSupply(box);
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(50, 10, 50);
        tracker.appendPoint(50, 50, 100);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertTrue(tracker.snapshot().strokes().getFirst().points().size() > 3);
        assertEquals(63, box[0].remaining());
    }

    @Test void pauseAndEndStrokeDoNotSplineAcrossStrokes() {
        var paused = GlyphStrokeTracker.unconstrained();
        paused.acceptClick(10, 10, 0);
        paused.acceptClick(50, 10, 50);
        paused.pause(100);
        paused.acceptClick(50, 50, 101);
        assertEquals(2, paused.snapshot().strokes().size());
        assertFalse(hasPointInsideCorner(paused.snapshot().strokes().getFirst().points()));

        var ended = GlyphStrokeTracker.unconstrained();
        ended.beginStroke(10, 10, 0);
        ended.appendPoint(50, 10, 50);
        ended.endStroke(60);
        ended.beginStroke(50, 50, 70);
        ended.appendPoint(50, 90, 120);
        assertEquals(2, ended.snapshot().strokes().size());
        assertFalse(hasPointInsideCorner(ended.snapshot().strokes().getFirst().points()));

        var delayed = GlyphStrokeTracker.unconstrained();
        delayed.acceptClick(10, 10, 0);
        delayed.acceptClick(50, 10, GlyphStrokeTracker.STROKE_PAUSE_MILLIS);
        assertEquals(2, delayed.snapshot().strokes().size());
    }

    @Test void sparseInterpolatedStrokeKeepsSlowWiderThanFast() {
        var slow = GlyphStrokeTracker.unconstrained();
        slow.beginStroke(10, 10, 0);
        slow.appendPoint(50, 10, 200);
        slow.appendPoint(90, 10, 400);

        var fast = GlyphStrokeTracker.unconstrained();
        fast.beginStroke(10, 20, 0);
        fast.appendPoint(50, 20, 20);
        fast.appendPoint(90, 20, 40);

        assertTrue(lastWidth(slow) > lastWidth(fast));
        double first = slow.snapshot().strokes().getFirst().segmentWidths().getFirst();
        slow.appendPoint(90, 50, 401);
        assertEquals(first, slow.snapshot().strokes().getFirst().segmentWidths().getFirst());
        for (double width : slow.snapshot().strokes().getFirst().segmentWidths()) {
            assertTrue(width >= GlyphStrokeTracker.MIN_BRUSH_WIDTH);
            assertTrue(width <= GlyphStrokeTracker.MAX_BRUSH_WIDTH);
        }
    }

    @Test void pointLimitRolloversStayWithinMaxStrokes() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(0, 0, 0);
        for (int i = 1; i < GlyphLimits.MAX_POINTS_PER_STROKE * (GlyphLimits.MAX_STROKES + 4); i++) {
            tracker.appendPoint(i % 2, 0, i);
        }
        var draft = assertDoesNotThrow(tracker::snapshot);
        assertTrue(draft.strokes().size() <= GlyphLimits.MAX_STROKES);
        assertTrue(GlyphDraftStoreAdapter.encode(draft).length <= GlyphLimits.MAX_SERIALIZED_BYTES);
    }

    @Test void aLongHoldStillEncodesUnderTheSaveLimit() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(0, 64, 0);
        int x = 0;
        int y = 64;
        int dir = 1;
        long t = 0;
        for (int i = 0; i < 400; i++) {
            t += 50;
            x += dir * 8;
            if (x >= 127) { x = 127; dir = -1; y = Math.min(127, y + 6); }
            if (x <= 0) { x = 0; dir = 1; y = Math.min(127, y + 6); }
            tracker.appendPoint(x, y, t);
        }
        tracker.endStroke(t);
        var draft = tracker.snapshot();
        int points = 0;
        for (var stroke : draft.strokes()) points += stroke.points().size();
        byte[] encoded = GlyphDraftStoreAdapter.encode(draft);
        assertTrue(
                encoded.length <= GlyphLimits.MAX_SERIALIZED_BYTES,
                "long hold encoded to " + encoded.length + " bytes from " + draft.strokes().size()
                        + " strokes / " + points + " points");
    }

    @Test void inBetweenSplinePointsStayOnTheCanvas() {
        var tracker = GlyphStrokeTracker.unconstrained();
        tracker.beginStroke(0, 0, 0);
        tracker.appendPoint(0, 40, 50);
        tracker.appendPoint(40, 40, 100);
        tracker.appendPoint(127, 0, 150);
        tracker.appendPoint(127, 127, 200);
        assertDoesNotThrow(tracker::snapshot);
        for (var stroke : tracker.snapshot().strokes()) {
            for (var point : stroke.points()) {
                assertTrue(point.x() >= 0 && point.x() < 128);
                assertTrue(point.y() >= 0 && point.y() < 128);
            }
        }
    }

    private static double lastWidth(GlyphStrokeTracker tracker) {
        return tracker.snapshot().strokes().getLast().segmentWidths().getLast();
    }

    private static void assertRoundedRightAngle(GlyphStroke stroke) {
        var points = stroke.points();
        assertTrue(
                hasPointInsideCorner(points),
                () -> "expected a path point inside the corner, not only the two legs: " + points);
        assertTrue(reachesSample(points, 10, 10), () -> "missed start sample: " + points);
        assertTrue(reachesSample(points, 50, 50), () -> "missed end sample: " + points);
        assertTrue(reachesSample(points, 50, 10) || minDistanceToPath(points, 50, 10) < 16,
                () -> "stroke should still reach the poll vertex: " + points);
        for (double width : stroke.segmentWidths()) {
            assertTrue(width >= GlyphStrokeTracker.MIN_BRUSH_WIDTH);
            assertTrue(width <= GlyphStrokeTracker.MAX_BRUSH_WIDTH);
        }
    }

    private static boolean hasPointInsideCorner(List<GlyphPoint> points) {
        for (var point : points) {
            if (point.x() < 49.0 && point.y() > 11.0) return true;
        }
        return false;
    }

    private static boolean reachesSample(List<GlyphPoint> points, double x, double y) {
        return minDistanceToPath(points, x, y) < 1.0;
    }

    private static double minDistanceToPath(List<GlyphPoint> points, double x, double y) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            var point = points.get(i);
            min = Math.min(min, Math.hypot(point.x() - x, point.y() - y));
            if (i == 0) continue;
            min = Math.min(min, distanceToSegment(points.get(i - 1), point, x, y));
        }
        return min;
    }

    private static double distanceToSegment(GlyphPoint start, GlyphPoint end, double x, double y) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) return Math.hypot(start.x() - x, start.y() - y);
        double t = Math.max(0, Math.min(1, ((x - start.x()) * dx + (y - start.y()) * dy) / lengthSq));
        return Math.hypot(start.x() + t * dx - x, start.y() + t * dy - y);
    }

}
