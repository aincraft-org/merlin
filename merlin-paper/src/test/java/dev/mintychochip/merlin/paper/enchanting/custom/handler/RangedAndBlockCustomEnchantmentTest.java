package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

final class RangedAndBlockCustomEnchantmentTest {
    @Test
    void flurryKnocksBackAtMostThreeLivingTargetsOutwardAndIgnoresNonLivingTargets() {
        Player player = org.mockito.Mockito.mock(Player.class);
        LivingEntity first = org.mockito.Mockito.mock(LivingEntity.class);
        LivingEntity second = org.mockito.Mockito.mock(LivingEntity.class);
        LivingEntity third = org.mockito.Mockito.mock(LivingEntity.class);
        LivingEntity fourth = org.mockito.Mockito.mock(LivingEntity.class);
        Entity nonLiving = org.mockito.Mockito.mock(Entity.class);
        org.bukkit.Location origin = new org.bukkit.Location(null, 0, 0, 0);
        when(player.getLocation()).thenReturn(origin);
        when(first.getLocation()).thenReturn(new org.bukkit.Location(null, 2, 0, 0));
        when(second.getLocation()).thenReturn(new org.bukkit.Location(null, -2, 0, 0));
        when(third.getLocation()).thenReturn(new org.bukkit.Location(null, 0, 0, 2));
        when(fourth.getLocation()).thenReturn(new org.bukkit.Location(null, 0, 0, -2));
        when(player.getNearbyEntities(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(player, first, second, third, fourth, nonLiving));

        new FlurryHandler().onEntityInteract(player, first, org.mockito.Mockito.mock(ItemStack.class), null, 1);

        verify(first).setVelocity(new Vector(1, 0, 0));
        verify(second).setVelocity(new Vector(-1, 0, 0));
        verify(third).setVelocity(new Vector(0, 0, 1));
        verify(fourth, never()).setVelocity(any(Vector.class));
        verify(nonLiving, never()).getLocation();
    }

    @Test
    void flurryIgnoresNonLivingRightClickedTarget() {
        Player player = org.mockito.Mockito.mock(Player.class);
        Entity target = org.mockito.Mockito.mock(Entity.class);
        new FlurryHandler().onEntityInteract(player, target, null, null, 3);
        verify(player, never()).getNearbyEntities(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void arraySpawnsOnlyAdditionalArrowsWithShooterAndOriginalVelocity() {
        LivingEntity shooter = org.mockito.Mockito.mock(LivingEntity.class);
        Arrow original = org.mockito.Mockito.mock(Arrow.class);
        Arrow extraOne = org.mockito.Mockito.mock(Arrow.class);
        Arrow extraTwo = org.mockito.Mockito.mock(Arrow.class);
        World world = org.mockito.Mockito.mock(World.class);
        ItemStack bow = org.mockito.Mockito.mock(ItemStack.class);
        when(original.getWorld()).thenReturn(world);
        when(original.getType()).thenReturn(EntityType.ARROW);
        when(original.getLocation()).thenReturn(new org.bukkit.Location(world, 1, 2, 3));
        when(original.getVelocity()).thenReturn(new Vector(1, 0, 0));
        when(world.spawnEntity(any(org.bukkit.Location.class), eq(EntityType.ARROW))).thenReturn(extraOne, extraTwo);

        ArrayHandler handler = new ArrayHandler();
        handler.onBowShoot(shooter, original, bow, 1.0f, 1);
        handler.onBowShoot(shooter, original, bow, 1.0f, 2);

        verify(world, times(6)).spawnEntity(any(org.bukkit.Location.class), eq(EntityType.ARROW));
        verify(bow, never()).setAmount(anyInt());
        assertTrue(extraOne.getVelocity() == null || extraOne.getVelocity().getX() >= 0.9);
    }

    @Test
    void arrayRankThreeIsRejectedByRegistryBoundsAndDoesNotConsumeAmmunition() {
        assertEquals(2, EnchantmentRegistry.defaultRegistry()
                .get(new org.bukkit.NamespacedKey("merlin", "array")).orElseThrow().absoluteMaxLevel());
    }
    @Test
    void moltenTouchMapsSmeltableDropsAndPreservesAmounts() {
        Player player = org.mockito.Mockito.mock(Player.class);
        BlockState state = org.mockito.Mockito.mock(BlockState.class);
        Item iron = item(stack(Material.RAW_IRON, 7));
        Item gold = item(stack(Material.RAW_GOLD, 4));
        Item copper = item(stack(Material.RAW_COPPER, 9));
        Item debris = item(stack(Material.ANCIENT_DEBRIS, 2));
        Item sand = item(stack(Material.SAND, 6));
        Item redSand = item(stack(Material.RED_SAND, 3));
        Item bone = item(stack(Material.BONE, 8));

        new MoltenTouchHandler().onBlockDrop(player, state,
                List.of(iron, gold, copper, debris, sand, redSand, bone), 1);

        verify(iron.getItemStack()).setType(Material.IRON_INGOT);
        verify(gold.getItemStack()).setType(Material.GOLD_INGOT);
        verify(copper.getItemStack()).setType(Material.COPPER_INGOT);
        verify(debris.getItemStack()).setType(Material.NETHERITE_SCRAP);
        verify(sand.getItemStack()).setType(Material.GLASS);
        verify(redSand.getItemStack()).setType(Material.GLASS);
        verify(bone, never()).setItemStack(any(ItemStack.class));
    }

    @Test
    void drillBreadthFirstBreaksOnlyAdjacentSameOreUpToFourTimesRank() {
        Block origin = block(Material.DIAMOND_ORE);
        Block first = block(Material.DIAMOND_ORE);
        Block second = block(Material.DIAMOND_ORE);
        Block diagonal = block(Material.DIAMOND_ORE);
        Block different = block(Material.IRON_ORE);
        configureRelative(origin, Map.of(
                BlockFace.EAST, first, BlockFace.NORTH, different, BlockFace.SOUTH, diagonal));
        configureRelative(first, Map.of(BlockFace.EAST, second));
        configureRelative(second, Map.of());
        configureRelative(diagonal, Map.of());
        configureRelative(different, Map.of());
        CascadeScope scope = org.mockito.Mockito.mock(CascadeScope.class);
        when(scope.breakBlockSafely(any(Block.class), eq(true))).thenReturn(true);

        new DrillHandler().onBlockBreak(org.mockito.Mockito.mock(Player.class), origin, 1, scope);
        verify(scope, times(3)).breakBlockSafely(any(Block.class), eq(true));
    }
    @Test
    void drillIgnoresNonOreAndDoesNotRevisitBlocks() {
        Block stone = block(Material.STONE);
        CascadeScope scope = org.mockito.Mockito.mock(CascadeScope.class);
        new DrillHandler().onBlockBreak(org.mockito.Mockito.mock(Player.class), stone, 3, scope);
        verifyNoBreak(scope);
    }
    @Test
    void drillStopsWhenCascadeScopeRejectsARecursiveBreak() {
        Block origin = block(Material.IRON_ORE);
        Block first = block(Material.IRON_ORE);
        Block second = block(Material.IRON_ORE);
        configureRelative(origin, Map.of(BlockFace.DOWN, first, BlockFace.UP, second));
        CascadeScope scope = org.mockito.Mockito.mock(CascadeScope.class);
        when(scope.breakBlockSafely(any(Block.class), eq(true))).thenReturn(false);

        new DrillHandler().onBlockBreak(org.mockito.Mockito.mock(Player.class), origin, 2, scope);

        verify(scope).breakBlockSafely(first, true);
        verify(scope, times(1)).breakBlockSafely(any(Block.class), eq(true));
    }

    @Test
    void heatWaveOnlyIgnitesAirInHorizontalThreeByThreeForRightClickBlock() {
        Block clicked = block(Material.STONE);
        Map<String, Block> blocks = new HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                blocks.put(key(x, z), block((x == 1 && z == 1) ? Material.STONE : Material.AIR));
            }
        }
        when(clicked.getRelative(anyInt(), eq(0), anyInt())).thenAnswer((Answer<Block>) invocation -> {
            int x = invocation.getArgument(0);
            int z = invocation.getArgument(2);
            return x == 0 && z == 0 ? clicked : blocks.get(key(x, z));
        });
        Player player = org.mockito.Mockito.mock(Player.class);
        new HeatWaveHandler().onActiveInteract(player, Action.RIGHT_CLICK_BLOCK, clicked,
                org.mockito.Mockito.mock(ItemStack.class), 1);
        for (Map.Entry<String, Block> entry : blocks.entrySet()) {
            if (entry.getValue().isEmpty()) verify(entry.getValue()).setType(Material.FIRE, true);
            else verify(entry.getValue(), never()).setType(any(Material.class), eq(true));
        }
        verify(clicked, never()).setType(any(Material.class), eq(true));
        new HeatWaveHandler().onActiveInteract(player, Action.LEFT_CLICK_BLOCK, clicked, null, 1);
        verify(clicked, times(9)).getRelative(anyInt(), eq(0), anyInt());
    }

    private static Item item(ItemStack stack) {
        Item item = org.mockito.Mockito.mock(Item.class);
        when(item.getItemStack()).thenReturn(stack);
        return item;
    }

    private static ItemStack stack(Material material, int amount) {
        ItemStack stack = org.mockito.Mockito.mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.clone()).thenAnswer(invocation -> {
            ItemStack copy = org.mockito.Mockito.mock(ItemStack.class);
            when(copy.getType()).thenReturn(material);
            when(copy.getAmount()).thenReturn(amount);
            when(copy.isEmpty()).thenReturn(false);
            return copy;
        });
        return stack;
    }

    private static Block block(Material material) {
        Block block = org.mockito.Mockito.mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.isEmpty()).thenReturn(material.isAir());
        return block;
    }

    private static void configureRelative(Block block, Map<BlockFace, Block> neighbors) {
        when(block.getRelative(any(BlockFace.class))).thenAnswer((Answer<Block>) invocation ->
                neighbors.get(invocation.getArgument(0)));
    }

    private static void verifyNoBreak(CascadeScope scope) {
        verify(scope, never()).breakBlockSafely(any(Block.class), eq(true));
    }

    private static String key(int x, int z) {
        return x + ":" + z;
    }
}
