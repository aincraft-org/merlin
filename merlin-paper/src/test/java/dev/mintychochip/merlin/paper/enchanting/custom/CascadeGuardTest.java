package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CascadeGuardTest {
    @Test
    void tracksDepthAndBoundsRecursion() {
        assertEquals(0, CascadeGuard.getDepth());
        assertTrue(CascadeGuard.canCascade());

        CascadeGuard.runInScope(() -> {
            assertEquals(1, CascadeGuard.getDepth());
            CascadeGuard.runInScope(() -> {
                assertEquals(2, CascadeGuard.getDepth());
                CascadeGuard.runInScope(() -> {
                    assertEquals(3, CascadeGuard.getDepth());
                    assertFalse(CascadeGuard.canCascade());

                    // Nested 4th attempt should be blocked
                    CascadeGuard.runInScope(() -> {
                        assertEquals(3, CascadeGuard.getDepth());
                    });
                });
                assertEquals(2, CascadeGuard.getDepth());
            });
            assertEquals(1, CascadeGuard.getDepth());
        });
        assertEquals(0, CascadeGuard.getDepth());
    }
}
