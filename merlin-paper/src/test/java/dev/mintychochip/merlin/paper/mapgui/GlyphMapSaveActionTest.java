package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class GlyphMapSaveActionTest {
    @Test void replacementRequiresMatchingIdentityAtBothChecks() {
        UUID expected = UUID.randomUUID();
        var replaced = new AtomicBoolean();
        boolean saved = GlyphMapSaveAction.replaceIfStillHeld(
                expected,
                () -> expected,
                () -> Optional.of("replacement"),
                ignored -> replaced.set(true));
        assertTrue(saved);
        assertTrue(replaced.get());
    }

    @Test void changedHeldItemIsNeverReplaced() {
        UUID expected = UUID.randomUUID();
        var replaced = new AtomicBoolean();
        boolean saved = GlyphMapSaveAction.replaceIfStillHeld(
                expected,
                UUID::randomUUID,
                () -> Optional.of("replacement"),
                ignored -> replaced.set(true));
        assertFalse(saved);
        assertFalse(replaced.get());
    }
    @Test void changedIdentityAfterPreparationSkipsReplacement() {
        UUID expected = UUID.randomUUID();
        var checks = new java.util.concurrent.atomic.AtomicInteger();
        var prepared = new AtomicBoolean();
        var replaced = new AtomicBoolean();
        boolean saved = GlyphMapSaveAction.replaceIfStillHeld(
                expected,
                () -> checks.getAndIncrement() == 0 ? expected : UUID.randomUUID(),
                () -> {
                    prepared.set(true);
                    return Optional.of("replacement");
                },
                ignored -> replaced.set(true));
        assertFalse(saved);
        assertTrue(prepared.get());
        assertFalse(replaced.get());
    }


    @Test void failedPreparationLeavesHeldItemUntouched() {
        UUID expected = UUID.randomUUID();
        var replaced = new AtomicBoolean();
        boolean saved = GlyphMapSaveAction.<String>replaceIfStillHeld(
                expected,
                () -> expected,
                Optional::empty,
                ignored -> replaced.set(true));
        assertFalse(saved);
        assertFalse(replaced.get());
    }
}
