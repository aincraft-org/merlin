package dev.mintychochip.merlin.paper.enchanting.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SharpnessOvercapHandlerTest {
    @Test
    void calculatesBonusDamageForLevel() {
        SharpnessOvercapHandler handler = new SharpnessOvercapHandler();
        // Vanilla Sharpness V is max. Level 6 = +1.5 extra damage, Level 7 = +3.0 extra damage
        assertEquals(0.0, handler.calculateBonusDamage(5, 5), 0.001);
        assertEquals(1.5, handler.calculateBonusDamage(6, 5), 0.001);
        assertEquals(3.0, handler.calculateBonusDamage(7, 5), 0.001);
    }
}
