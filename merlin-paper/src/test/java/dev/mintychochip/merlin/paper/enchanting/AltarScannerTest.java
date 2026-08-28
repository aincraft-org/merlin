package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

    @Test
    void ddaRaycastDetectsObstructionsAlongPath() {
        World world = mock(World.class);

        // Ray from (0, 0, 0) to (2, 0, 1) passes through (1, 0, 0) or (1, 0, 1)
        Block clearBlock = mock(Block.class);
        when(clearBlock.isEmpty()).thenReturn(true);
        when(clearBlock.getType()).thenReturn(Material.AIR);

        Block solidBlock = mock(Block.class);
        when(solidBlock.isEmpty()).thenReturn(false);
        when(solidBlock.getType()).thenReturn(Material.STONE);

        // When (1, 0, 0) is air -> unobstructed
        when(world.getBlockAt(1, 0, 0)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 0, 1)).thenReturn(clearBlock);
        assertTrue(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 0, 1));

        // When (1, 0, 0) is solid stone -> obstructed
        when(world.getBlockAt(1, 0, 0)).thenReturn(solidBlock);
        assertFalse(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 0, 1));
    }

    @Test
    void supercoverCatchesTiedAxisBorderVoxels() {
        World world = mock(World.class);

        Block clearBlock = mock(Block.class);
        when(clearBlock.isEmpty()).thenReturn(true);
        when(clearBlock.getType()).thenReturn(Material.AIR);

        Block solidBlock = mock(Block.class);
        when(solidBlock.isEmpty()).thenReturn(false);
        when(solidBlock.getType()).thenReturn(Material.STONE);

        // 45-degree ray from (0, 0, 0) to (2, 0, 2) has X/Z ties
        when(world.getBlockAt(1, 0, 0)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 0, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 0, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(2, 0, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 0, 2)).thenReturn(clearBlock);

        assertTrue(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 0, 2));

        // Obstructing a bordering tied voxel (1, 0, 0) must be caught by supercover
        when(world.getBlockAt(1, 0, 0)).thenReturn(solidBlock);
        assertFalse(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 0, 2));
    }

    @Test
    void supercoverCatchesThreeAxisCornerTies() {
        World world = mock(World.class);

        Block clearBlock = mock(Block.class);
        when(clearBlock.isEmpty()).thenReturn(true);
        when(clearBlock.getType()).thenReturn(Material.AIR);

        Block solidBlock = mock(Block.class);
        when(solidBlock.isEmpty()).thenReturn(false);
        when(solidBlock.getType()).thenReturn(Material.STONE);

        // Ray from (0, 0, 0) to (2, 2, 2) ties X, Y, and Z
        when(world.getBlockAt(1, 0, 0)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 1, 0)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 0, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 1, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(2, 1, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 2, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(1, 1, 2)).thenReturn(clearBlock);

        assertTrue(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 2, 2));

        // Obstructing side Y (0, 1, 0) must block line-of-sight
        when(world.getBlockAt(0, 1, 0)).thenReturn(solidBlock);
        assertFalse(AltarScanner.hasLineOfSight(world, 0, 0, 0, 2, 2, 2));
    }

    @Test
    void supercoverCatchesYZPairwiseTies() {
        World world = mock(World.class);

        Block clearBlock = mock(Block.class);
        when(clearBlock.isEmpty()).thenReturn(true);
        when(clearBlock.getType()).thenReturn(Material.AIR);

        Block solidBlock = mock(Block.class);
        when(solidBlock.isEmpty()).thenReturn(false);
        when(solidBlock.getType()).thenReturn(Material.STONE);

        // Ray from (0, 0, 0) to (0, 2, 2) ties Y and Z
        when(world.getBlockAt(0, 1, 0)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 0, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 1, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 2, 1)).thenReturn(clearBlock);
        when(world.getBlockAt(0, 1, 2)).thenReturn(clearBlock);

        assertTrue(AltarScanner.hasLineOfSight(world, 0, 0, 0, 0, 2, 2));

        // Obstructing side Z (0, 0, 1) must block line-of-sight
        when(world.getBlockAt(0, 0, 1)).thenReturn(solidBlock);
        assertFalse(AltarScanner.hasLineOfSight(world, 0, 0, 0, 0, 2, 2));
    }
}
