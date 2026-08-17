package dev.mintychochip.wizardry.paper.mapgui;

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
    @Test void menuSaveInvokesCallbackAndClosesMenu() {
        var saves = new AtomicInteger();
        var screen = new GlyphScreen(new GlyphStrokeTracker(), saves::incrementAndGet, () -> {});

        screen.toggleMenu();
        assertTrue(screen.menuOpen());
        screen.saveFromMenu();

        assertEquals(1, saves.get());
        assertFalse(screen.menuOpen());
    }

    @Test void explicitScreenStrokeControlsKeepDisjointComponentsSeparate() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {});
        screen.beginStroke(10, 10);
        screen.endStroke();
        screen.beginStroke(100, 100);
        screen.endStroke();
        assertEquals(2, screen.draft().strokes().size());
    }
    @Test void canvasClickCreatesIndependentStroke() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {});
        screen.beginStroke(10, 10);
        screen.endStroke();
        screen.beginStroke(20, 20);
        screen.endStroke();
        assertEquals(2, screen.draft().strokes().size());
    }

    @Test void pipClickerStepsAndSneakJumps() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {}, ignored -> {}, () -> false);
        assertEquals(1, screen.pips());
        screen.stepPips(1);
        assertEquals(2, screen.pips());
        screen.stepPips(1);
        screen.stepPips(1);
        screen.stepPips(1);
        screen.stepPips(1);
        assertEquals(5, screen.pips());
        screen.stepPips(-1);
        assertEquals(4, screen.pips());
        screen.jumpPips(1);
        assertEquals(5, screen.pips());
        screen.jumpPips(-1);
        assertEquals(1, screen.pips());
    }

    @Test void pipClickerDoesNotTouchDraft() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {}, ignored -> {}, () -> false);
        screen.beginStroke(10, 10);
        screen.endStroke();
        int strokes = screen.draft().strokes().size();
        screen.stepPips(1);
        assertEquals(strokes, screen.draft().strokes().size());
    }
}
