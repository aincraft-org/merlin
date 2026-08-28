package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class AltarScannerTest {
    @Test
    void computesStatsWithBlockCaps() {
        Map<Material, AltarBlockStats> configBlocks = Map.of(
                Material.BOOKSHELF, new AltarBlockStats(1.0, 0.0, 15.0, 0.0),
                Material.CRYING_OBSIDIAN, new AltarBlockStats(2.5, 0.20, 45.0, 0.60)
        );
        AltarConfig config = new AltarConfig(2, 1, 1, configBlocks);

        Map<Material, Integer> scannedBlocks = Map.of(
                Material.BOOKSHELF, 20, // 20 * 1.0 = 20 -> capped at 15.0
                Material.CRYING_OBSIDIAN, 2 // 2 * 2.5 = 5.0 Eterna, 2 * 0.20 = 0.40 Quanta
        );

        AltarProfile profile = AltarScanner.calculateProfile(config, scannedBlocks);
        assertEquals(20.0, profile.totalEterna(), 0.001); // 15.0 + 5.0
        assertEquals(0.40, profile.totalQuanta(), 0.001); // 0.40
    }

    @Test
    void handlesNegativeQuantaStabilizers() {
        Map<Material, AltarBlockStats> configBlocks = Map.of(
                Material.CANDLE, new AltarBlockStats(0.25, -0.05, 10.0, -0.30)
        );
        AltarConfig config = new AltarConfig(2, 1, 1, configBlocks);

        Map<Material, Integer> scannedBlocks = Map.of(
                Material.CANDLE, 4 // 4 * 0.25 = 1.0 Eterna, 4 * -0.05 = -0.20 Quanta
        );

        AltarProfile profile = AltarScanner.calculateProfile(config, scannedBlocks);
        assertEquals(1.0, profile.totalEterna(), 0.001);
        assertEquals(-0.20, profile.totalQuanta(), 0.001);
    }
}
