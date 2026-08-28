package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class MutableDamageTest {
    @Test
    void calculatesBonusesAndMultipliersCorrectly() {
        MutableDamage dmg = new MutableDamage(10.0);
        assertEquals(10.0, dmg.getFinalDamage());

        dmg.addBonus(2.5); // 12.5
        assertEquals(12.5, dmg.getFinalDamage());

        dmg.multiply(1.5); // 12.5 * 1.5 = 18.75
        assertEquals(18.75, dmg.getFinalDamage(), 0.001);

        dmg.setCancelled(true);
        assertEquals(0.0, dmg.getFinalDamage());
    }
}
