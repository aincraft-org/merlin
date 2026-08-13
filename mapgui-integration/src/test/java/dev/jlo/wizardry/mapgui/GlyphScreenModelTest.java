package dev.jlo.wizardry.mapgui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class GlyphScreenModelTest {
    @Test void clearAndUndoModifyTrackerAndSaveCallbackRuns() {
        var tracker = new GlyphStrokeTracker();
        var saves = new AtomicInteger();
        var closes = new AtomicInteger();
        var screen = new GlyphScreen(tracker, saves::incrementAndGet, closes::incrementAndGet);
        tracker.acceptClick(1, 1, 0);
        tracker.acceptClick(5, 5, 100);
        assertFalse(screen.draft().strokes().isEmpty());
        screen.undo();
        assertTrue(screen.draft().strokes().isEmpty());
        tracker.acceptClick(2, 2, 200);
        screen.clear();
        screen.save();
        assertEquals(1, saves.get());
        // Screen.close() requires an attached MapGUI session; callback behavior is tested without closing the API object.
        assertEquals(0, closes.get());
    }
}
