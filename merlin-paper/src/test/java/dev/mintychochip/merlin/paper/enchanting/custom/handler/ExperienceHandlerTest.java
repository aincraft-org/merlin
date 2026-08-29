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

final class ExperienceHandlerTest {
    @Test
    void grantsBonusExperienceForOreBreaks() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);

        new ExperienceHandler().onBlockBreak(player, block, 3, null);

        verify(player).giveExp(3);
    }

    @Test
    void ignoresNonOreBreaks() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);

        new ExperienceHandler().onBlockBreak(player, block, 3, mock(CascadeScope.class));

        verify(player, never()).giveExp(3);
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);

        new ExperienceHandler().onBlockBreak(player, block, 0, null);

        verify(player, never()).giveExp(0);
    }
}
