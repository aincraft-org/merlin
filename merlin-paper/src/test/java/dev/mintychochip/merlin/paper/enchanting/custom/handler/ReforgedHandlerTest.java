package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ReforgedHandlerTest {
    @Test
    void reducesDamageByTheEnchantmentLevel() {
        ReforgedHandler handler = new ReforgedHandler();

        assertEquals(3, handler.onItemDamage(null, null, 5, 2));
        assertEquals(0, handler.onItemDamage(null, null, 1, 1));
    }

    @Test
    void clampsReducedDamageAtZero() {
        assertEquals(0, new ReforgedHandler().onItemDamage(null, null, 2, 5));
    }

    @Test
    void preservesDamageWhenLevelIsZero() {
        assertEquals(5, new ReforgedHandler().onItemDamage(null, null, 5, 0));
    }
}
