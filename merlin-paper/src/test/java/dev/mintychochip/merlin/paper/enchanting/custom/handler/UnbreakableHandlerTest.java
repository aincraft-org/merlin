package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class UnbreakableHandlerTest {
    @Test
    void removesAllDamageForActiveLevel() {
        assertEquals(0, new UnbreakableHandler().onItemDamage(null, null, 5, 1));
    }

    @Test
    void preservesDamageWhenLevelIsZero() {
        assertEquals(5, new UnbreakableHandler().onItemDamage(null, null, 5, 0));
    }
}
