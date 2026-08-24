package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class GlyphStrokeTrackerTest {
    @Test void repeatedClicksInterpolateAndPauseEndsStroke() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, 100);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertTrue(tracker.snapshot().strokes().getFirst().points().size() >= 4);
        tracker.pause(400);
        tracker.acceptClick(10, 10, 401);
        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void explicitBeginAndEndKeepDisjointStrokesSeparate() {
        var tracker = new GlyphStrokeTracker();
        tracker.beginStroke(1, 1, 0);
        tracker.appendPoint(2, 2, 10);
        tracker.endStroke(20);
        tracker.beginStroke(20, 20, 30);
        tracker.endStroke(40);
        assertEquals(2, tracker.snapshot().strokes().size());
    }

    @Test void laterVelocityDoesNotChangeEarlierSegmentWidth() {
        var tracker = new GlyphStrokeTracker();
        tracker.beginStroke(1, 1, 0);
        tracker.appendPoint(2, 2, 100);
        double first = tracker.snapshot().strokes().getFirst().segmentWidths().getFirst();
        tracker.appendPoint(3, 3, 101);
        assertEquals(first, tracker.snapshot().strokes().getFirst().segmentWidths().getFirst());
        assertNotEquals(first, tracker.snapshot().strokes().getFirst().segmentWidths().get(1));
    }
    @Test void rapidClicksRemainOneConnectedStroke() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, 100);
        tracker.acceptClick(8, 8, 200);
        assertEquals(1, tracker.snapshot().strokes().size());
        assertTrue(tracker.snapshot().strokes().getFirst().points().size() > 1);
    }

    @Test void delayedClickStartsANewStroke() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(4, 4, GlyphStrokeTracker.STROKE_PAUSE_MILLIS + 1);
        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void longContinuousStrokeDoesNotStopPaintingAtPointLimit() {
        var tracker = new GlyphStrokeTracker();
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
        var tracker = new GlyphStrokeTracker();
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
        var tracker = new GlyphStrokeTracker();
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
    @Test void stationarySamplesAdvanceTheVelocityClock() {
        var tracker = new GlyphStrokeTracker();
        tracker.beginStroke(10, 10, 0);
        tracker.appendPoint(10, 10, 100);
        tracker.appendPoint(11, 10, 101);

        assertEquals(GlyphStrokeTracker.MIN_BRUSH_WIDTH, lastWidth(tracker));
    }

    @Test void timestampOverflowEndsAnExpiredClickStroke() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(10, 10, Long.MIN_VALUE);
        tracker.acceptClick(11, 10, Long.MAX_VALUE);

        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void exactPauseThresholdStartsANewStroke() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(10, 10, 0);
        tracker.acceptClick(11, 10, GlyphStrokeTracker.STROKE_PAUSE_MILLIS);

        assertEquals(2, tracker.snapshot().strokes().size());
    }
    @Test void pointLimitRolloverStartsFreshSmoothingState() {
        var tracker = new GlyphStrokeTracker();
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
        var tracker = new GlyphStrokeTracker();
        tracker.beginStroke(30, 30, 100);
        tracker.endStroke(200);

        var stroke = tracker.snapshot().strokes().getFirst();
        assertEquals(1, stroke.points().size());
        assertTrue(stroke.segmentWidths().isEmpty());
        assertEquals(GlyphStrokeTracker.MAX_BRUSH_WIDTH, stroke.brushWidth());
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
    @Test void importedDraftPreservesSegmentWidths() {
        var draft = new dev.mintychochip.merlin.api.glyph.GlyphDraft(java.util.List.of(
                new dev.mintychochip.merlin.api.glyph.GlyphStroke(
                        java.util.List.of(
                                new dev.mintychochip.merlin.api.glyph.GlyphPoint(1, 1),
                                new dev.mintychochip.merlin.api.glyph.GlyphPoint(2, 2),
                                new dev.mintychochip.merlin.api.glyph.GlyphPoint(3, 3)),
                        4,
                        10,
                        java.util.List.of(5.5, 1.25))));

        assertEquals(draft, new GlyphStrokeTracker(draft).snapshot());
    }





    @Test void undoDuringActiveStrokeResetsPen() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(2, 2, 100);
        tracker.undo();
        tracker.acceptClick(4, 4, 101);
        assertEquals(1, tracker.snapshot().strokes().size());
    }
    @Test void explicitPauseEndsStrokeForNextClick() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(2, 2, 100);
        tracker.pause(2000);
        tracker.acceptClick(4, 4, 2001);
        assertEquals(2, tracker.snapshot().strokes().size());
    }
    private static double lastWidth(GlyphStrokeTracker tracker) {
        return tracker.snapshot().strokes().getLast().segmentWidths().getLast();
    }

}
