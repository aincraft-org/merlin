package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class ReplenishHandlerTest {
    @Test
    void restoresFoodAndSaturationForOreBreaks() {
        Player player = mock(Player.class);
        when(player.getFoodLevel()).thenReturn(10);
        when(player.getSaturation()).thenReturn(2.0f);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);

        new ReplenishHandler().onBlockBreak(player, block, 2, null);

        verify(player).setFoodLevel(12);
        verify(player).setSaturation(3.0f);
    }

    @Test
    void capsFoodAndSaturation() {
        Player player = mock(Player.class);
        when(player.getFoodLevel()).thenReturn(19);
        when(player.getSaturation()).thenReturn(19.5f);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.IRON_ORE);

        new ReplenishHandler().onBlockBreak(player, block, 4, null);

        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20.0f);
    }

    @Test
    void ignoresNonOreBlocks() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);

        new ReplenishHandler().onBlockBreak(player, block, 2, mock(CascadeScope.class));

        verify(player, never()).setFoodLevel(12);
        verify(player, never()).setSaturation(1.0f);
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);

        new ReplenishHandler().onBlockBreak(player, block, 0, null);

        verify(player, never()).getFoodLevel();
    }
}
