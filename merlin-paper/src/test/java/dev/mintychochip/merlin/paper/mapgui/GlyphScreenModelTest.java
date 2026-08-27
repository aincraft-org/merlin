package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.*;

import de.flog99.mapgui.Click;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class GlyphScreenModelTest {
    @Test void clearAndUndoModifyTrackerAndSaveCallbackRuns() {
        var tracker = GlyphStrokeTracker.unconstrained();
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

    @Test void closingTheScreenSavesTheDraft() {
        var saves = new AtomicInteger();
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), saves::incrementAndGet, () -> {});
        screen.beginStroke(10, 10);
        screen.detach();
        assertEquals(1, saves.get());
        assertFalse(screen.draft().strokes().isEmpty());
    }

    @Test void closingEndsAnInProgressHoldBeforeSaving() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.held(10, 10);
        screen.held(20, 20);
        screen.detach();
        assertEquals(1, screen.draft().strokes().size());
        screen.held(80, 80);
        assertEquals(2, screen.draft().strokes().size());
    }
    @Test void menuSaveInvokesCallbackAndClosesMenu() {
        var saves = new AtomicInteger();
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), saves::incrementAndGet, () -> {});

        screen.toggleMenu();
        assertTrue(screen.menuOpen());
        screen.saveFromMenu();

        assertEquals(1, saves.get());
        assertFalse(screen.menuOpen());
    }

    @Test void explicitScreenStrokeControlsKeepDisjointComponentsSeparate() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.beginStroke(10, 10);
        screen.endStroke();
        screen.beginStroke(100, 100);
        screen.endStroke();
        assertEquals(2, screen.draft().strokes().size());
    }
    @Test void canvasClickCreatesIndependentStroke() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.beginStroke(10, 10);
        screen.endStroke();
        screen.beginStroke(20, 20);
        screen.endStroke();
        assertEquals(2, screen.draft().strokes().size());
    }

    @Test void drawingUsesMapGuiHoldInsteadOfClickGuessing() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        assertTrue(screen.holdable());
        screen.held(10, 10);
        screen.held(20, 20);
        assertEquals(1, screen.draft().strokes().size());
        screen.holdEnded();
        screen.held(80, 80);
        screen.holdEnded();
        assertEquals(2, screen.draft().strokes().size());
    }

    @Test void holdDoesNotDrawThroughOpenMenu() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.toggleMenu();
        screen.held(10, 10);
        screen.holdEnded();
        assertTrue(screen.draft().strokes().isEmpty());
    }

    @Test void holdIgnoresCursorLossCoordinates() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.held(-1, -1);
        screen.holdEnded();
        assertTrue(screen.draft().strokes().isEmpty());
    }

    @Test void pipClickerStepsAndSneakJumps() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {}, ignored -> {}, () -> false);
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

    @Test void clickingAPipDotSetsThatRank() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        assertEquals(1, screen.pips());
        screen.clickPip(4);
        assertEquals(5, screen.pips());
        screen.clickPip(2);
        assertEquals(3, screen.pips());
        screen.clickPip(0);
        assertEquals(1, screen.pips());
    }

    @Test void clickingTheCurrentRankTogglesThatPipOff() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.clickPip(4);
        assertEquals(5, screen.pips());
        screen.clickPip(4);
        assertEquals(4, screen.pips());
        screen.clickPip(4);
        assertEquals(5, screen.pips());
        screen.clickPip(0);
        assertEquals(1, screen.pips());
        screen.clickPip(0);
        assertEquals(1, screen.pips());
    }

    @Test void pipClickerDoesNotTouchDraft() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {}, ignored -> {}, () -> false);
        screen.beginStroke(10, 10);
        screen.endStroke();
        int strokes = screen.draft().strokes().size();
        screen.clickPip(3);
        assertEquals(strokes, screen.draft().strokes().size());
        assertEquals(4, screen.pips());
    }

    @Test void rightClickHoldDrawsOnTheCanvas() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.click(10, 10, Click.RIGHT);
        screen.held(10, 10);
        screen.holdEnded();
        assertEquals(1, screen.draft().strokes().size());
    }

    @Test void pipClickDoesNotDrawInkOnTheFollowingHold() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.clickPip(4);
        screen.held(64, 110);
        screen.holdEnded();
        assertTrue(screen.draft().strokes().isEmpty());
        assertEquals(5, screen.pips());
    }
    @Test void inkSelectionDoesNotBlockFollowingHold() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setAvailableInks(List.of(GlyphElement.FLAME, GlyphElement.FROST));
        screen.selectInk(GlyphElement.FLAME);
        screen.held(64, 110);
        screen.holdEnded();
        assertEquals(1, screen.draft().strokes().size());
        assertEquals(GlyphElement.FLAME, screen.selectedInk());
    }


    @Test void pendingLabelIsStored() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        assertNull(screen.pendingLabel());
        screen.setPendingLabel(Label.DAMAGE);
        assertEquals(Label.DAMAGE, screen.pendingLabel());
    }

    @Test void clearPendingLabelDropsStoredLabel() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setPendingLabel(Label.DAMAGE);
        screen.clearPendingLabel();
        assertNull(screen.pendingLabel());
    }

    @Test void setPipsClampsToOneThroughFive() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setPips(3);
        assertEquals(3, screen.pips());
        screen.setPips(0);
        assertEquals(1, screen.pips());
        screen.setPips(9);
        assertEquals(5, screen.pips());
    }

    @Test void inkChipsHideEmptyAndSelectClickedColor() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setAvailableInks(List.of(GlyphElement.FLAME, GlyphElement.FROST));
        assertEquals(List.of(GlyphElement.FLAME, GlyphElement.FROST), screen.availableInks());
        assertEquals(GlyphElement.FLAME, screen.selectedInk());
        screen.selectInk(GlyphElement.FROST);
        assertEquals(GlyphElement.FROST, screen.selectedInk());
        screen.selectInk(GlyphElement.ARCANE);
        assertEquals(GlyphElement.FROST, screen.selectedInk());
    }

    @Test void lastSpentColorMovesSelectionToNextChip() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setAvailableInks(List.of(GlyphElement.FLAME, GlyphElement.ARCANE));
        screen.selectInk(GlyphElement.FLAME);
        screen.setAvailableInks(List.of(GlyphElement.ARCANE));
        assertEquals(List.of(GlyphElement.ARCANE), screen.availableInks());
        assertEquals(GlyphElement.ARCANE, screen.selectedInk());
        screen.setAvailableInks(List.of());
        assertTrue(screen.availableInks().isEmpty());
        assertNull(screen.selectedInk());
    }

    @Test void rankAndInkHoversShareOneCaption() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setInks(List.of(new MagicalInk(GlyphElement.FROST, 1, 5)));
        screen.showHover("Rank 1", GlyphScreen.HoverAnchor.RANK);
        assertEquals("Rank 1", screen.hoverCaption());
        assertEquals(GlyphScreen.HoverAnchor.RANK, screen.hoverAnchor());
        screen.showHover(screen.inkCaption(GlyphElement.FROST), GlyphScreen.HoverAnchor.INK);
        assertEquals("Frost Ink 1/5", screen.hoverCaption());
        assertEquals(GlyphScreen.HoverAnchor.INK, screen.hoverAnchor());
        screen.showHover(null, null);
        assertNull(screen.hoverCaption());
        assertNull(screen.hoverAnchor());
    }

    @Test void hoveringInkChipReportsRemainingFill() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setInks(List.of(
                new MagicalInk(GlyphElement.FLAME, 12, 64),
                new MagicalInk(GlyphElement.FROST, 8, 64),
                new MagicalInk(GlyphElement.ARCANE, 0, 64)));
        assertEquals(List.of(GlyphElement.FLAME, GlyphElement.FROST), screen.availableInks());
        assertEquals("Flame Ink 12/64", screen.inkCaption(GlyphElement.FLAME));
        assertEquals("Frost Ink 8/64", screen.inkCaption(GlyphElement.FROST));
        assertNull(screen.inkCaption(GlyphElement.ARCANE));
        assertEquals("Flame Ink 12/64", screen.selectedInkCaption());
        screen.selectInk(GlyphElement.FROST);
        assertEquals("Frost Ink 8/64", screen.selectedInkCaption());
        screen.setInks(List.of(new MagicalInk(GlyphElement.FROST, 7, 64)));
        assertEquals("Frost Ink 7/64", screen.inkCaption(GlyphElement.FROST));
        assertEquals("Frost Ink 7/64", screen.selectedInkCaption());
    }

    @Test void classifyInvokesClassifyAction() {
        var classifications = new AtomicInteger();
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {}, ignored -> classifications.incrementAndGet(), () -> false);
        screen.classify();
        assertEquals(1, classifications.get());
    }
}
