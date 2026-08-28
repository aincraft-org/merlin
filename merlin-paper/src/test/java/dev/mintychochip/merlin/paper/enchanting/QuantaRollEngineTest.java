package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class QuantaRollEngineTest {
    @Test
    void generatesOffersRespectingEternaBounds() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        AltarProfile profile = new AltarProfile(30.0, 0.5, Map.of());
        OfferConfig offersConfig = OfferConfig.defaultConfig();
        QuantaRollEngine engine = new QuantaRollEngine(registry, offersConfig);

        EnchantingOffer tier3 = engine.generateOffer(Material.DIAMOND_SWORD, profile, 3, new Random(42));
        assertNotNull(tier3);
        assertFalse(tier3.enchantments().isEmpty());
        // Verify all rolled levels require <= profile eterna
        for (var entry : tier3.enchantments().entrySet()) {
            EnchantmentDefinition def = registry.get(entry.getKey()).orElseThrow();
            assertTrue(def.minEternaForLevel(entry.getValue()) <= profile.totalEterna());
        }
    }

    @Test
    void toRomanConvertsCorrectly() {
        assertEquals("I", QuantaRollEngine.toRoman(1));
        assertEquals("II", QuantaRollEngine.toRoman(2));
        assertEquals("III", QuantaRollEngine.toRoman(3));
        assertEquals("IV", QuantaRollEngine.toRoman(4));
        assertEquals("V", QuantaRollEngine.toRoman(5));
        assertEquals("VI", QuantaRollEngine.toRoman(6));
        assertEquals("VII", QuantaRollEngine.toRoman(7));
    }
}
