package dev.mintychochip.merlin.paper.enchanting.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class FortuneOvercapHandlerTest {
    @Test
    void calculatesExtraDropsAboveVanillaMax() {
        FortuneOvercapHandler handler = new FortuneOvercapHandler();
        assertEquals(0, handler.calculateExtraDrops(3, 3));
        assertEquals(1, handler.calculateExtraDrops(4, 3));
        assertEquals(2, handler.calculateExtraDrops(5, 3));
    }

    @Test
    void recognizesApplicableOres() {
        FortuneOvercapHandler handler = new FortuneOvercapHandler();
        assertTrue(handler.isApplicableBlock(Material.DIAMOND_ORE));
        assertTrue(handler.isApplicableBlock(Material.DEEPSLATE_EMERALD_ORE));
        assertTrue(handler.isApplicableBlock(Material.AMETHYST_CLUSTER));
        assertFalse(handler.isApplicableBlock(Material.STONE));
        assertFalse(handler.isApplicableBlock(Material.OAK_LOG));
    }
}
