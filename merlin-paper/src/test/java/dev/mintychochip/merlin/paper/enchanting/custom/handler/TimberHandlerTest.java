package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class TimberHandlerTest {
    @Test
    void fellsAdjacentLog() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        Block air = mockAir();

        Block log = mockLog();
        when(log.getRelative(any(BlockFace.class))).thenReturn(air);

        Block block = mockLog();
        when(block.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        for (BlockFace face : BlockFace.values()) {
            if (face != BlockFace.SELF) {
                when(block.getRelative(face)).thenReturn(air);
            }
        }
        when(block.getRelative(BlockFace.UP)).thenReturn(log);

        ItemStack tool = mock(ItemStack.class);
        CascadeScope scope = new CascadeScope(world, player, tool, 0);

        new TimberHandler().onBlockBreak(player, block, 1, scope);

        verify(log, times(1)).breakNaturally(any(ItemStack.class), eq(true));
        verify(block, never()).breakNaturally(any(), anyBoolean());
    }

    @Test
    void respectsFourTimesLevelLimitOnAChain() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        Block air = mockAir();

        // Build a 5-log vertical chain above the broken block. Limit at level 1 is 4.
        Block log5 = mockLog();
        when(log5.getRelative(any(BlockFace.class))).thenReturn(air);

        Block log4 = mockLog();
        when(log4.getRelative(any(BlockFace.class))).thenReturn(air);
        when(log4.getRelative(BlockFace.UP)).thenReturn(log5);

        Block log3 = mockLog();
        when(log3.getRelative(any(BlockFace.class))).thenReturn(air);
        when(log3.getRelative(BlockFace.UP)).thenReturn(log4);

        Block log2 = mockLog();
        when(log2.getRelative(any(BlockFace.class))).thenReturn(air);
        when(log2.getRelative(BlockFace.UP)).thenReturn(log3);

        Block log1 = mockLog();
        when(log1.getRelative(any(BlockFace.class))).thenReturn(air);
        when(log1.getRelative(BlockFace.UP)).thenReturn(log2);

        Block block = mockLog();
        when(block.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        for (BlockFace face : BlockFace.values()) {
            if (face != BlockFace.SELF) {
                when(block.getRelative(face)).thenReturn(air);
            }
        }
        when(block.getRelative(BlockFace.UP)).thenReturn(log1);

        ItemStack tool = mock(ItemStack.class);
        new TimberHandler().onBlockBreak(player, block, 1, new CascadeScope(world, player, tool, 0));

        verify(log1, times(1)).breakNaturally(any(ItemStack.class), eq(true));
        verify(log2, times(1)).breakNaturally(any(ItemStack.class), eq(true));
        verify(log3, times(1)).breakNaturally(any(ItemStack.class), eq(true));
        verify(log4, times(1)).breakNaturally(any(ItemStack.class), eq(true));
        verify(log5, never()).breakNaturally(any(ItemStack.class), anyBoolean());
    }

    @Test
    void fellsBranchesUpToLimit() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        Block air = mockAir();

        Block up1 = mockLog();
        when(up1.getRelative(any(BlockFace.class))).thenReturn(air);

        Block north1 = mockLog();
        when(north1.getRelative(any(BlockFace.class))).thenReturn(air);

        Block south1 = mockLog();
        when(south1.getRelative(any(BlockFace.class))).thenReturn(air);

        Block upFromUp1 = mockLog();
        when(upFromUp1.getRelative(any(BlockFace.class))).thenReturn(air);
        when(up1.getRelative(BlockFace.UP)).thenReturn(upFromUp1);

        Block southFromSouth1 = mockLog();
        when(southFromSouth1.getRelative(any(BlockFace.class))).thenReturn(air);
        when(south1.getRelative(BlockFace.SOUTH)).thenReturn(southFromSouth1);

        Block block = mockLog();
        when(block.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        for (BlockFace face : BlockFace.values()) {
            if (face != BlockFace.SELF) {
                when(block.getRelative(face)).thenReturn(air);
            }
        }
        when(block.getRelative(BlockFace.UP)).thenReturn(up1);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(north1);
        when(block.getRelative(BlockFace.SOUTH)).thenReturn(south1);

        ItemStack tool = mock(ItemStack.class);
        new TimberHandler().onBlockBreak(player, block, 1, new CascadeScope(world, player, tool, 0));

        int broken = 0;
        for (Block b : new Block[]{up1, north1, south1, upFromUp1, southFromSouth1}) {
            try {
                verify(b, times(1)).breakNaturally(any(ItemStack.class), eq(true));
                broken++;
            } catch (AssertionError ignored) {
                verify(b, never()).breakNaturally(any(ItemStack.class), anyBoolean());
            }
        }

        assertEquals(4, broken, "Only 4 logs should be broken at level 1");
        verify(block, never()).breakNaturally(any(), anyBoolean());
    }

    @Test
    void ignoresNonLogBlocks() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);

        new TimberHandler().onBlockBreak(player, block, 1, new CascadeScope(world, player, null, 0));

        verify(block, never()).breakNaturally(any(), anyBoolean());
    }

    @Test
    void doesNothingWhenLevelIsZero() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.OAK_LOG);

        new TimberHandler().onBlockBreak(player, block, 0, new CascadeScope(null, player, null, 0));

        verify(block, never()).breakNaturally(any(), anyBoolean());
    }

    private static Block mockLog() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.OAK_LOG);
        when(block.breakNaturally(any(ItemStack.class), anyBoolean())).thenReturn(true);
        return block;
    }

    private static Block mockAir() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.AIR);
        return block;
    }
}
