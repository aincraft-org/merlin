package dev.jlo.wizardry.mapgui;

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
    @Test void rejectsInvalidCursor() {
        var tracker = new GlyphStrokeTracker();
        assertThrows(IllegalArgumentException.class, () -> tracker.acceptClick(128, 0, 0));
    }
    @Test void undoDuringActiveStrokeResetsPen() {
        var tracker = new GlyphStrokeTracker();
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(2, 2, 100);
        tracker.undo();
        tracker.acceptClick(4, 4, 101);
        assertEquals(1, tracker.snapshot().strokes().size());
    }
}
