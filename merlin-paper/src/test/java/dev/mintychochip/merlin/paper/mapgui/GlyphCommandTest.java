package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.dsl.Diagnostic;
import dev.mintychochip.merlin.api.dsl.Span;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.ml.Classification;
import dev.mintychochip.merlin.api.ml.ClassificationCandidate;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class GlyphCommandTest {
    @Test void rejectedClassificationClearsPendingLabel() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        screen.setPendingLabel(Label.DAMAGE);
        GlyphCommand.applyClassification(screen, Classification.rejected(List.of()));
        assertNull(screen.pendingLabel());
    }

    @Test void acceptedClassificationStoresTopLabel() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        GlyphCommand.applyClassification(
                screen, new Classification(List.of(new ClassificationCandidate(Label.FLAME, 0.9f)), true));
        assertEquals(Label.FLAME, screen.pendingLabel());
    }

    @Test void hydrateFrozenSetsLabelAndPips() {
        var screen = new GlyphScreen(GlyphStrokeTracker.unconstrained(), () -> {}, () -> {});
        GlyphCommand.hydrateFrozen(screen, Optional.of(new GlyphToken(Label.DAMAGE, 4)));
        assertEquals(Label.DAMAGE, screen.pendingLabel());
        assertEquals(4, screen.pips());
    }

    @Test void parseInkAcceptsElementNames() {
        assertEquals(GlyphElement.FLAME, GlyphCommand.parseInk(new String[]{"ink", "flame"}).orElseThrow());
        assertEquals(GlyphElement.FLAME, GlyphCommand.parseInk(new String[]{"ink", "fire"}).orElseThrow());
        assertTrue(GlyphCommand.parseInk(new String[]{"ink", "ember"}).isEmpty());
        assertTrue(GlyphCommand.parseInk(new String[]{"ink", "nope"}).isEmpty());
        assertTrue(GlyphCommand.parseInk(new String[]{"stamp", "flame"}).isEmpty());
    }

    @Test void bareInkCommandGivesEveryElement() {
        assertEquals(
                List.of(GlyphElement.PHYSICAL, GlyphElement.FLAME, GlyphElement.FROST, GlyphElement.ARCANE),
                GlyphCommand.inkElements(new String[]{"ink"}));
    }

    @Test void unknownInkNameGivesNoElements() {
        assertTrue(GlyphCommand.inkElements(new String[]{"ink", "bone"}).isEmpty());
    }

    @Test void inkCountReadsOptionalAmount() {
        assertEquals(1, GlyphCommand.inkCount(new String[]{"ink", "flame"}));
        assertEquals(26, GlyphCommand.inkCount(new String[]{"ink", "flame", "26"}));
        assertEquals(1, GlyphCommand.inkCount(new String[]{"ink", "flame", "nope"}));
    }

    @Test void bindFailureMessageUsesDiagnosticCodeAndText() {
        var diagnostic = new Diagnostic("G0104", "more than one effect", new Span(0, 0, 2, 1));
        assertEquals("G0104: more than one effect", GlyphCommand.bindFailureMessage(Optional.of(diagnostic)));
        assertEquals("That glyph cannot bind into this tome.", GlyphCommand.bindFailureMessage(Optional.empty()));
    }
}
