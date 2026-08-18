package dev.mintychochip.wizardry.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.ml.Classification;
import dev.mintychochip.wizardry.api.ml.ClassificationCandidate;
import dev.mintychochip.wizardry.api.ml.Label;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class GlyphCommandTest {
    @Test void rejectedClassificationClearsPendingLabel() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {});
        screen.setPendingLabel(Label.DAMAGE);
        GlyphCommand.applyClassification(screen, Classification.rejected(List.of()));
        assertNull(screen.pendingLabel());
    }

    @Test void acceptedClassificationStoresTopLabel() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {});
        GlyphCommand.applyClassification(
                screen, new Classification(List.of(new ClassificationCandidate(Label.FIRE, 0.9f)), true));
        assertEquals(Label.FIRE, screen.pendingLabel());
    }

    @Test void hydrateFrozenSetsLabelAndPips() {
        var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {});
        GlyphCommand.hydrateFrozen(screen, Optional.of(new GlyphToken(Label.DAMAGE, 4)));
        assertEquals(Label.DAMAGE, screen.pendingLabel());
        assertEquals(4, screen.pips());
    }

    @Test void bindFailureMessageUsesDiagnosticCodeAndText() {
        var diagnostic = new Diagnostic("G0104", "more than one effect", new Span(0, 0, 2, 1));
        assertEquals("G0104: more than one effect", GlyphCommand.bindFailureMessage(Optional.of(diagnostic)));
        assertEquals("That glyph cannot bind into this tome.", GlyphCommand.bindFailureMessage(Optional.empty()));
    }
}
