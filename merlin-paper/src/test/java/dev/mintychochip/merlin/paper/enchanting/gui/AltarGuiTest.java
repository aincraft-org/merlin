package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class AltarGuiTest {
    @Test
    void verifiesSlotConstants() {
        assertEquals(2, AltarGuiSession.SLOT_ETERNA_METER);
        assertEquals(6, AltarGuiSession.SLOT_QUANTA_METER);
        assertEquals(20, AltarGuiSession.SLOT_TARGET);
        assertEquals(22, AltarGuiSession.SLOT_LAPIS);
        assertEquals(24, AltarGuiSession.SLOT_CATALYST);
        assertEquals(38, AltarGuiSession.SLOT_TIER_1);
        assertEquals(40, AltarGuiSession.SLOT_TIER_2);
        assertEquals(42, AltarGuiSession.SLOT_TIER_3);
        assertEquals(44, AltarGuiSession.SLOT_REROLL);
    }
}
