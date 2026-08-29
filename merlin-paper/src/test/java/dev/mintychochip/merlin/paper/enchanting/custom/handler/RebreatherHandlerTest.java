package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class RebreatherHandlerTest {
    @Test
    void restoresAirByTwentyTicksPerLevelUnderwater() {
        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(true);
        when(player.getRemainingAir()).thenReturn(100);
        when(player.getMaximumAir()).thenReturn(300);

        new RebreatherHandler().onBlockBreak(player, mock(Block.class), 2, null);

        verify(player).setRemainingAir(140);
    }

    @Test
    void capsAirAtMaximum() {
        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(true);
        when(player.getRemainingAir()).thenReturn(290);
        when(player.getMaximumAir()).thenReturn(300);

        new RebreatherHandler().onBlockBreak(player, mock(Block.class), 2, null);

        verify(player).setRemainingAir(300);
    }

    @Test
    void doesNotSetAirWhenAlreadyFull() {
        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(true);
        when(player.getRemainingAir()).thenReturn(300);
        when(player.getMaximumAir()).thenReturn(300);

        new RebreatherHandler().onBlockBreak(player, mock(Block.class), 1, null);

        verify(player, never()).setRemainingAir(300);
    }

    @Test
    void doesNotRestoreAirOnLand() {
        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(false);

        new RebreatherHandler().onBlockBreak(player, mock(Block.class), 1, null);

        verify(player, never()).getRemainingAir();
        verify(player, never()).setRemainingAir(320);
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(true);
        when(player.getRemainingAir()).thenReturn(100);
        when(player.getMaximumAir()).thenReturn(300);

        new RebreatherHandler().onBlockBreak(player, mock(Block.class), 0, null);

        verify(player, never()).isUnderWater();
    }
}
