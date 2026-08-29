package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class TrenchHandlerTest {

    @Test
    void breaksThreeByThreePlaneSkippingCenter() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location playerLoc = new Location(world, 10, 0, 0);
        when(player.getLocation()).thenReturn(playerLoc);

        Set<String> seen = new HashSet<>();

        Block block = mock(Block.class);
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getType()).thenReturn(Material.STONE);

        when(block.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);

            if (x == 0 && y == 0 && z == 0) {
                return block;
            }

            seen.add("%d,%d,%d".formatted(x, y, z));

            Block neighbor = mock(Block.class);
            when(neighbor.getType()).thenReturn(Material.STONE);
            when(neighbor.isEmpty()).thenReturn(false);
            when(neighbor.breakNaturally(any(ItemStack.class), anyBoolean())).thenReturn(true);
            return neighbor;
        });

        new TrenchHandler().onBlockBreak(player, block, 1, new CascadeScope(world, player, mock(ItemStack.class), 0));

        assertEquals(8, seen.size(), "Trench should break exactly 8 neighbors in a 3x3 plane");
        // Player is far along +X, so the dominant axis is X and the broken plane is YZ.
        // Expected offsets are all (0, y, z) except (0,0,0).
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                if (y == 0 && z == 0) continue;
                String key = "0,%d,%d".formatted(y, z);
                assertEquals(true, seen.contains(key), "Expected YZ plane offset " + key);
            }
        }

        verify(block, never()).breakNaturally(any(), anyBoolean());
    }

    @Test
    void picksHorizontalPlaneWhenLookingDown() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location playerLoc = new Location(world, 0, 10, 0);
        when(player.getLocation()).thenReturn(playerLoc);

        Set<String> seen = new HashSet<>();

        Block block = mock(Block.class);
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getType()).thenReturn(Material.STONE);

        when(block.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);

            if (x == 0 && y == 0 && z == 0) return block;

            seen.add("%d,%d,%d".formatted(x, y, z));

            Block neighbor = mock(Block.class);
            when(neighbor.getType()).thenReturn(Material.STONE);
            when(neighbor.isEmpty()).thenReturn(false);
            when(neighbor.breakNaturally(any(ItemStack.class), anyBoolean())).thenReturn(true);
            return neighbor;
        });

        new TrenchHandler().onBlockBreak(player, block, 1, new CascadeScope(world, player, mock(ItemStack.class), 0));

        assertEquals(8, seen.size());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                String key = "%d,0,%d".formatted(x, z);
                assertEquals(true, seen.contains(key), "Expected XZ plane offset " + key);
            }
        }
    }

    @Test
    void doesNothingWhenLevelIsZero() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);

        new TrenchHandler().onBlockBreak(player, block, 0, new CascadeScope(null, player, null, 0));

        verify(block, never()).getRelative(anyInt(), anyInt(), anyInt());
    }
}
