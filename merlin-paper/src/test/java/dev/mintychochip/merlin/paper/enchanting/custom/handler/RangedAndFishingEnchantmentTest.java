package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

final class RangedAndFishingEnchantmentTest {

    private static LivingEntity shooter() {
        return mock(LivingEntity.class);
    }

    private static org.bukkit.entity.Projectile projectileWithVelocity(Vector velocity) {
        org.bukkit.entity.Projectile projectile = mock(org.bukkit.entity.Projectile.class);
        when(projectile.getVelocity()).thenReturn(velocity);
        return projectile;
    }

    @Test
    void archerIncreasesProjectileVelocityPerLevel() {
        Vector velocity = new Vector(1, 0, 0);
        org.bukkit.entity.Projectile projectile = projectileWithVelocity(velocity);
        org.bukkit.inventory.ItemStack bow = mock(org.bukkit.inventory.ItemStack.class);

        new ArcherHandler().onBowShoot(shooter(), projectile, bow, 1.0f, 2);

        verify(projectile).setVelocity(velocity);
        org.junit.jupiter.api.Assertions.assertEquals(1.2, velocity.getX(), 1e-9);
    }

    @Test
    void marksmanIncreasesProjectileVelocityPerLevel() {
        Vector velocity = new Vector(1, 0, 0);
        org.bukkit.entity.Projectile projectile = projectileWithVelocity(velocity);
        org.bukkit.inventory.ItemStack bow = mock(org.bukkit.inventory.ItemStack.class);

        new MarksmanHandler().onBowShoot(shooter(), projectile, bow, 1.0f, 2);

        verify(projectile).setVelocity(velocity);
        org.junit.jupiter.api.Assertions.assertEquals(1.16, velocity.getX(), 1e-9);
    }

    @Test
    void rangedHandlersIgnoreNullLevelsAndNonProjectiles() {
        org.bukkit.entity.Projectile projectile = projectileWithVelocity(new Vector(1, 0, 0));
        new ArcherHandler().onBowShoot(null, projectile, null, 1.0f, 1);
        new ArcherHandler().onBowShoot(shooter(), projectile, null, 1.0f, 0);
        verify(projectile, never()).setVelocity(ArgumentMatchers.any());
    }

    @Test
    void sniperDealsHeadshotDamage() {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.isDead()).thenReturn(false);
        Location eye = mock(Location.class);
        when(eye.getY()).thenReturn(10.0);
        when(victim.getEyeLocation()).thenReturn(eye);
        Location impact = mock(Location.class);
        when(impact.getY()).thenReturn(9.9);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getLocation()).thenReturn(impact);
        when(arrow.getDamage()).thenReturn(4.0);

        new SniperHandler().onProjectileHit(shooter(), arrow, victim, null, 2);

        verify(victim).damage(ArgumentMatchers.eq(8.0), ArgumentMatchers.<org.bukkit.entity.Entity>any());
    }

    @Test
    void sniperSkipsBodyShotsAndNonLivingTargets() {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.isDead()).thenReturn(false);
        Location eye = mock(Location.class);
        when(eye.getY()).thenReturn(10.0);
        when(victim.getEyeLocation()).thenReturn(eye);
        Location body = mock(Location.class);
        when(body.getY()).thenReturn(8.0);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getLocation()).thenReturn(body);
        when(arrow.getDamage()).thenReturn(4.0);

        new SniperHandler().onProjectileHit(shooter(), arrow, victim, null, 1);
        verify(victim, never()).damage(ArgumentMatchers.anyDouble(), ArgumentMatchers.<org.bukkit.entity.Entity>any());

        Entity nonLiving = mock(Entity.class);
        new SniperHandler().onProjectileHit(shooter(), arrow, nonLiving, null, 1);
        verify(victim, never()).damage(ArgumentMatchers.anyDouble(), ArgumentMatchers.<org.bukkit.entity.Entity>any());
    }

    @Test
    void autoReelPullsHookedEntityOnBite() {
        Player player = mock(Player.class);
        FishHook hook = mock(FishHook.class);
        AutoReelHandler handler = new AutoReelHandler();

        handler.onPlayerFish(player, hook, null, State.BITE, 1);
        verify(hook).pullHookedEntity();

        handler.onPlayerFish(player, hook, null, State.CAUGHT_FISH, 1);
        handler.onPlayerFish(null, hook, null, State.BITE, 1);
        verify(hook, times(1)).pullHookedEntity();
    }

    @Test
    void baitDropsOneClonePerLevel() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        Location location = mock(Location.class);
        Item item = mock(Item.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getMaxStackSize()).thenReturn(64);
        ItemStack copy = mock(ItemStack.class);
        when(stack.clone()).thenReturn(copy);
        when(copy.getAmount()).thenReturn(1);
        when(item.getItemStack()).thenReturn(stack);
        when(item.getLocation()).thenReturn(location);
        when(world.dropItemNaturally(location, copy)).thenReturn(item);

        new BaitHandler().onPlayerFish(player, mock(FishHook.class), item, State.CAUGHT_FISH, 2);

        verify(world, times(2)).dropItemNaturally(location, copy);
    }

    @Test
    void hookGrantsExperienceOnCatchPerLevel() {
        Player player = mock(Player.class);
        FishHook hook = mock(FishHook.class);
        Entity caught = mock(Entity.class);

        new HookHandler().onPlayerFish(player, hook, caught, State.CAUGHT_ENTITY, 3);
        verify(player).giveExp(12);

        new HookHandler().onPlayerFish(player, hook, caught, State.BITE, 1);
        verify(player, times(1)).giveExp(ArgumentMatchers.anyInt());
    }

    @Test
    void snapTeleportsCaughtEntity() {
        Player player = mock(Player.class);
        Location playerLocation = mock(Location.class);
        when(player.getLocation()).thenReturn(playerLocation);
        LivingEntity caught = mock(LivingEntity.class);
        when(caught.isDead()).thenReturn(false);

        new SnapHandler().onPlayerFish(player, mock(FishHook.class), caught, State.CAUGHT_ENTITY, 1);
        verify(caught).teleport(playerLocation);

        when(caught.isDead()).thenReturn(true);
        new SnapHandler().onPlayerFish(player, mock(FishHook.class), caught, State.CAUGHT_ENTITY, 1);
        verify(caught, times(1)).teleport(ArgumentMatchers.any(Location.class));
    }

    @Test
    void lavaWalkerConvertsSurroundingLavaToObsidianOnGround() {
        Player player = mock(Player.class);
        when(player.isOnGround()).thenReturn(true);
        World world = mock(World.class);
        Location to = mock(Location.class);
        when(to.getWorld()).thenReturn(world);
        when(to.getBlockX()).thenReturn(0);
        when(to.getBlockY()).thenReturn(0);
        when(to.getBlockZ()).thenReturn(0);

        Block center = mock(Block.class);
        when(center.getType()).thenReturn(Material.LAVA);
        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.STONE);
        when(center.getRelative(org.bukkit.block.BlockFace.DOWN)).thenReturn(below);
        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> {
                    int x = invocation.getArgument(0);
                    int y = invocation.getArgument(1);
                    int z = invocation.getArgument(2);
                    return (x == 0 && y == 0 && z == 0) ? center : air;
                });

        new LavaWalkerHandler().onPlayerMove(player, null, to, 1);

        verify(center).setType(Material.OBSIDIAN, false);
    }

    @Test
    void waterWalkerConvertsSurroundingWaterToFrostedIce() {
        Player player = mock(Player.class);
        when(player.isOnGround()).thenReturn(true);
        World world = mock(World.class);
        Location to = mock(Location.class);
        when(to.getWorld()).thenReturn(world);
        when(to.getBlockX()).thenReturn(0);
        when(to.getBlockY()).thenReturn(0);
        when(to.getBlockZ()).thenReturn(0);

        Block center = mock(Block.class);
        when(center.getType()).thenReturn(Material.WATER);
        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.STONE);
        when(center.getRelative(org.bukkit.block.BlockFace.DOWN)).thenReturn(below);
        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> {
                    int x = invocation.getArgument(0);
                    int y = invocation.getArgument(1);
                    int z = invocation.getArgument(2);
                    return (x == 0 && y == 0 && z == 0) ? center : air;
                });

        new WaterWalkerHandler().onPlayerMove(player, null, to, 1);

        verify(center).setType(Material.FROSTED_ICE, false);
    }

    @Test
    void plummetDamagesNearbyLivingEntitiesOnFall() {
        Player player = mock(Player.class);
        when(player.isDead()).thenReturn(false);
        LivingEntity nearby = mock(LivingEntity.class);
        when(nearby.isDead()).thenReturn(false);
        when(player.getNearbyEntities(3.0, 3.0, 3.0)).thenReturn(List.of(nearby));

        new PlummetHandler().onEnvironmentalDamage(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL,
                new MutableDamage(5.0), 2);

        verify(nearby).damage(4.0, player);
    }

    @Test
    void plummetIgnoresNonFallAndDeadPlayer() {
        Player player = mock(Player.class);
        when(player.isDead()).thenReturn(false);
        new PlummetHandler().onEnvironmentalDamage(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE,
                new MutableDamage(5.0), 1);
        verify(player, never()).getNearbyEntities(ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble());

        Player dead = mock(Player.class);
        when(dead.isDead()).thenReturn(true);
        new PlummetHandler().onEnvironmentalDamage(dead, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL,
                new MutableDamage(5.0), 1);
        verify(dead, never()).getNearbyEntities(ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble());
    }

    @Test
    void jellyLegsCancelsFallDamageOnly() {
        Player player = mock(Player.class);
        MutableDamage fall = new MutableDamage(10.0);
        new JellyLegsHandler().onEnvironmentalDamage(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL, fall, 1);
        assertTrue(fall.isCancelled());

        MutableDamage fire = new MutableDamage(10.0);
        new JellyLegsHandler().onEnvironmentalDamage(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE, fire, 1);
        assertEquals(10.0, fire.getFinalDamage());
    }
}