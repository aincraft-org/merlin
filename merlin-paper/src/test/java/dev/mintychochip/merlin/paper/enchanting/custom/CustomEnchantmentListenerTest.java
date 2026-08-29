package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentListenerTest {
    @Test
    void adaptsEntityDamageEventAndDispatches() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player attacker = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(attacker.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(sword);

        LivingEntity victim = mock(LivingEntity.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamage()).thenReturn(10.0);

        final double[] modifiedDamage = new double[]{10.0};
        org.mockito.Mockito.doAnswer(invocation -> {
            modifiedDamage[0] = invocation.getArgument(0);
            return null;
        }).when(event).setDamage(org.mockito.ArgumentMatchers.anyDouble());

        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(1, CascadeGuard.getDepth());
            MutableDamage dmg = invocation.getArgument(2);
            dmg.addBonus(5.0);
            return null;
        }).when(dispatcher).dispatchEntityHit(eq(attacker), eq(victim), any(MutableDamage.class), eq(sword));

        listener.onAttack(event);
        verify(dispatcher).dispatchEntityHit(eq(attacker), eq(victim), any(MutableDamage.class), eq(sword));
        assertEquals(15.0, modifiedDamage[0], 0.001);
    }

    @Test
    void adaptsVictimHitToEquippedEntityTriggers() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack helmet = mock(ItemStack.class);
        when(victim.getEquipment()).thenReturn(equipment);
        when(equipment.getArmorContents()).thenReturn(new ItemStack[]{helmet});

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamage()).thenReturn(10.0);

        final double[] modifiedDamage = new double[]{10.0};
        org.mockito.Mockito.doAnswer(invocation -> {
            modifiedDamage[0] = invocation.getArgument(0);
            return null;
        }).when(event).setDamage(org.mockito.ArgumentMatchers.anyDouble());
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(1, CascadeGuard.getDepth());
            MutableDamage damage = invocation.getArgument(2);
            damage.addBonus(2.0);
            return null;
        }).when(dispatcher).dispatchEntityHitByEntity(
                eq(victim), eq(attacker), any(MutableDamage.class), any(ItemStack[].class));

        listener.onAttack(event);

        verify(dispatcher).dispatchEntityHitByEntity(
                eq(victim), eq(attacker), any(MutableDamage.class), any(ItemStack[].class));
        assertEquals(12.0, modifiedDamage[0], 0.001);
    }

    @Test
    void preservesOffenseModifierWhenApplyingVictimModifier() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player attacker = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(attacker.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(sword);

        LivingEntity victim = mock(LivingEntity.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        when(victim.getEquipment()).thenReturn(equipment);
        when(equipment.getArmorContents()).thenReturn(new ItemStack[0]);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamage()).thenReturn(10.0);
        final double[] modifiedDamage = new double[]{10.0};
        org.mockito.Mockito.doAnswer(invocation -> {
            modifiedDamage[0] = invocation.getArgument(0);
            return null;
        }).when(event).setDamage(org.mockito.ArgumentMatchers.anyDouble());
        org.mockito.Mockito.doAnswer(invocation -> {
            MutableDamage damage = invocation.getArgument(2);
            damage.addBonus(3.0);
            return null;
        }).when(dispatcher).dispatchEntityHit(
                eq(attacker), eq(victim), any(MutableDamage.class), eq(sword));
        org.mockito.Mockito.doAnswer(invocation -> {
            MutableDamage damage = invocation.getArgument(2);
            damage.addBonus(2.0);
            return null;
        }).when(dispatcher).dispatchEntityHitByEntity(
                eq(victim), eq(attacker), any(MutableDamage.class), any(ItemStack[].class));

        listener.onAttack(event);

        assertEquals(15.0, modifiedDamage[0], 0.001);
    }

    @Test
    void adaptsEntityItemDamageEventAndWritesModifiedAmount() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Entity entity = mock(Entity.class);
        ItemStack item = mock(ItemStack.class);
        EntityDamageItemEvent event = mock(EntityDamageItemEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getItem()).thenReturn(item);
        when(event.getDamage()).thenReturn(4);
        when(dispatcher.dispatchEntityItemDamage(entity, item, 4)).thenReturn(1);

        listener.onEntityItemDamage(event);

        verify(dispatcher).dispatchEntityItemDamage(entity, item, 4);
        org.mockito.Mockito.verify(event).setDamage(1);
    }

    @Test
    void dispatchesActivationAlongsideInteractionOnRightClick() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        ItemStack item = mock(ItemStack.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(dispatcher.dispatchActivate(player, item)).thenReturn(true);

        listener.onInteract(event);

        verify(dispatcher).dispatchActiveInteract(player, Action.RIGHT_CLICK_AIR, null, item);
        verify(dispatcher).dispatchActivate(player, item);
    }

    @Test
    void doesNotActivateOnLeftClick() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        ItemStack item = mock(ItemStack.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

        listener.onInteract(event);

        verify(dispatcher).dispatchActiveInteract(player, Action.LEFT_CLICK_AIR, null, item);
        verify(dispatcher, never()).dispatchActivate(any(), any());
    }

    @Test
    void usesTheLaunchingBowWhenProjectileHitsAfterHandChanges() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack originalBow = mock(ItemStack.class);
        ItemStack launchSnapshot = mock(ItemStack.class);
        ItemStack replacement = mock(ItemStack.class);
        Projectile projectile = mock(Projectile.class);
        Entity hitEntity = mock(Entity.class);
        Block hitBlock = mock(Block.class);
        UUID projectileId = UUID.randomUUID();
        when(player.getInventory()).thenReturn(inventory);
        when(projectile.getUniqueId()).thenReturn(projectileId);
        when(projectile.getShooter()).thenReturn(player);

        EntityShootBowEvent shoot = mock(EntityShootBowEvent.class);
        when(shoot.getEntity()).thenReturn(player);
        when(shoot.getBow()).thenReturn(originalBow);
        when(originalBow.clone()).thenReturn(launchSnapshot);
        when(shoot.getProjectile()).thenReturn(projectile);
        when(shoot.getForce()).thenReturn(1.0f);
        listener.onBowShoot(shoot);
        listener.onBowShootFinal(shoot);
        verify(dispatcher).dispatchBowShoot(
                eq(player), eq(projectile), eq(launchSnapshot), eq(1.0f));

        when(inventory.getItemInMainHand()).thenReturn(replacement);
        ProjectileHitEvent hit = mock(ProjectileHitEvent.class);
        when(hit.getEntity()).thenReturn(projectile);
        when(hit.getHitEntity()).thenReturn(hitEntity);
        when(hit.getHitBlock()).thenReturn(hitBlock);
        listener.onProjectileHit(hit);

        verify(dispatcher).dispatchProjectileHit(
                eq(player), eq(projectile), eq(hitEntity), eq(hitBlock), eq(launchSnapshot));
    }

    @Test
    void dispatchesProjectileHitForLivingEntityShooters() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        LivingEntity shooter = mock(LivingEntity.class);
        Projectile projectile = mock(Projectile.class);
        ItemStack bow = mock(ItemStack.class);
        ItemStack source = mock(ItemStack.class);
        Entity hitEntity = mock(Entity.class);
        Block hitBlock = mock(Block.class);
        when(bow.clone()).thenReturn(source);
        when(projectile.getUniqueId()).thenReturn(UUID.randomUUID());
        when(projectile.getShooter()).thenReturn(shooter);

        EntityShootBowEvent shoot = mock(EntityShootBowEvent.class);
        when(shoot.getEntity()).thenReturn(shooter);
        when(shoot.getBow()).thenReturn(bow);
        when(shoot.getProjectile()).thenReturn(projectile);
        listener.onBowShoot(shoot);
        listener.onBowShootFinal(shoot);

        ProjectileHitEvent hit = mock(ProjectileHitEvent.class);
        when(hit.getEntity()).thenReturn(projectile);
        when(hit.getHitEntity()).thenReturn(hitEntity);
        when(hit.getHitBlock()).thenReturn(hitBlock);
        listener.onProjectileHit(hit);

        verify(dispatcher).dispatchProjectileHit(
                eq(shooter), eq(projectile), eq(hitEntity), eq(hitBlock), eq(source));
    }

    @Test
    void discardsBowSnapshotWhenShootIsCancelled() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        LivingEntity shooter = mock(LivingEntity.class);
        Projectile projectile = mock(Projectile.class);
        ItemStack bow = mock(ItemStack.class);
        ItemStack source = mock(ItemStack.class);
        when(bow.clone()).thenReturn(source);
        when(projectile.getUniqueId()).thenReturn(UUID.randomUUID());
        when(projectile.getShooter()).thenReturn(shooter);

        EntityShootBowEvent shoot = mock(EntityShootBowEvent.class);
        when(shoot.getEntity()).thenReturn(shooter);
        when(shoot.getBow()).thenReturn(bow);
        when(shoot.getProjectile()).thenReturn(projectile);
        listener.onBowShoot(shoot);
        when(shoot.isCancelled()).thenReturn(true);
        listener.onBowShootFinal(shoot);

        ProjectileHitEvent hit = mock(ProjectileHitEvent.class);
        when(hit.getEntity()).thenReturn(projectile);
        listener.onProjectileHit(hit);

        verify(dispatcher, never()).dispatchProjectileHit(
                any(LivingEntity.class), any(Projectile.class), any(Entity.class),
                any(Block.class), any(ItemStack.class));
    }

    @Test
    void followsProjectileReplacementAtFinalBowEventPriority() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        LivingEntity shooter = mock(LivingEntity.class);
        Projectile initialProjectile = mock(Projectile.class);
        Projectile launchedProjectile = mock(Projectile.class);
        ItemStack bow = mock(ItemStack.class);
        ItemStack source = mock(ItemStack.class);
        Entity hitEntity = mock(Entity.class);
        Block hitBlock = mock(Block.class);
        when(bow.clone()).thenReturn(source);
        when(launchedProjectile.getUniqueId()).thenReturn(UUID.randomUUID());
        when(launchedProjectile.getShooter()).thenReturn(shooter);

        EntityShootBowEvent shoot = mock(EntityShootBowEvent.class);
        when(shoot.getEntity()).thenReturn(shooter);
        when(shoot.getBow()).thenReturn(bow);
        when(shoot.getProjectile()).thenReturn(initialProjectile, launchedProjectile);
        listener.onBowShoot(shoot);
        listener.onBowShootFinal(shoot);

        ProjectileHitEvent hit = mock(ProjectileHitEvent.class);
        when(hit.getEntity()).thenReturn(launchedProjectile);
        when(hit.getHitEntity()).thenReturn(hitEntity);
        when(hit.getHitBlock()).thenReturn(hitBlock);
        listener.onProjectileHit(hit);

        verify(dispatcher).dispatchProjectileHit(
                eq(shooter), eq(launchedProjectile), eq(hitEntity), eq(hitBlock), eq(source));
    }

    @Test
    void clearsProjectileSnapshotWhenHitIsCancelled() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        LivingEntity shooter = mock(LivingEntity.class);
        Projectile projectile = mock(Projectile.class);
        ItemStack bow = mock(ItemStack.class);
        ItemStack source = mock(ItemStack.class);
        Entity hitEntity = mock(Entity.class);
        Block hitBlock = mock(Block.class);
        UUID projectileId = UUID.randomUUID();
        when(bow.clone()).thenReturn(source);
        when(projectile.getUniqueId()).thenReturn(projectileId);
        when(projectile.getShooter()).thenReturn(shooter);

        EntityShootBowEvent shoot = mock(EntityShootBowEvent.class);
        when(shoot.getEntity()).thenReturn(shooter);
        when(shoot.getBow()).thenReturn(bow);
        when(shoot.getProjectile()).thenReturn(projectile);
        listener.onBowShoot(shoot);
        listener.onBowShootFinal(shoot);

        ProjectileHitEvent hit = mock(ProjectileHitEvent.class);
        when(hit.getEntity()).thenReturn(projectile);
        when(hit.getHitEntity()).thenReturn(hitEntity);
        when(hit.getHitBlock()).thenReturn(hitBlock);
        when(hit.isCancelled()).thenReturn(true);
        listener.onProjectileHit(hit);
        listener.onProjectileHitFinal(hit);

        when(hit.isCancelled()).thenReturn(false);
        listener.onProjectileHit(hit);

        verify(dispatcher, never()).dispatchProjectileHit(
                eq(shooter), eq(projectile), eq(hitEntity), eq(hitBlock), eq(source));
    }


    @Test
    void keepsTheFishingRodAcrossFishingStates() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack rod = mock(ItemStack.class);
        ItemStack rodSnapshot = mock(ItemStack.class);
        when(rod.clone()).thenReturn(rodSnapshot);
        when(rod.getAmount()).thenReturn(2);
        when(rodSnapshot.getAmount()).thenReturn(1);
        ItemStack replacement = mock(ItemStack.class);
        FishHook hook = mock(FishHook.class);
        Entity caught = mock(Entity.class);
        UUID hookId = UUID.randomUUID();
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(replacement);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(rod);
        when(hook.getUniqueId()).thenReturn(hookId);

        PlayerFishEvent cast = mock(PlayerFishEvent.class);
        when(cast.getPlayer()).thenReturn(player);
        when(cast.getHook()).thenReturn(hook);
        when(cast.getCaught()).thenReturn(null);
        when(cast.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(cast.getState()).thenReturn(PlayerFishEvent.State.FISHING);
        listener.onFish(cast);

        when(inventory.getItemInMainHand()).thenReturn(replacement);
        PlayerFishEvent catchEvent = mock(PlayerFishEvent.class);
        when(catchEvent.getPlayer()).thenReturn(player);
        when(catchEvent.getHook()).thenReturn(hook);
        when(catchEvent.getCaught()).thenReturn(caught);
        when(catchEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(catchEvent.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_ENTITY);
        listener.onFish(catchEvent);

        ArgumentCaptor<ItemStack> capturedRod = ArgumentCaptor.forClass(ItemStack.class);
        verify(dispatcher).dispatchPlayerFish(
                eq(player), eq(hook), eq(caught), eq(PlayerFishEvent.State.CAUGHT_ENTITY), capturedRod.capture());
        verify(rod).clone();
        assertNotSame(rod, capturedRod.getValue());
        assertEquals(1, capturedRod.getValue().getAmount());
    }

    @Test
    void clearsFishingRodWhenCastIsCancelled() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack rod = mock(ItemStack.class);
        ItemStack rodSnapshot = mock(ItemStack.class);
        FishHook hook = mock(FishHook.class);
        Entity caught = mock(Entity.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(rod);
        when(rod.clone()).thenReturn(rodSnapshot);
        when(hook.getUniqueId()).thenReturn(UUID.randomUUID());

        PlayerFishEvent cast = mock(PlayerFishEvent.class);
        when(cast.getPlayer()).thenReturn(player);
        when(cast.getHook()).thenReturn(hook);
        when(cast.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(cast.getState()).thenReturn(PlayerFishEvent.State.FISHING);
        listener.onFish(cast);
        when(cast.isCancelled()).thenReturn(true);
        listener.onFishFinal(cast);

        PlayerFishEvent terminal = mock(PlayerFishEvent.class);
        when(terminal.getPlayer()).thenReturn(player);
        when(terminal.getHook()).thenReturn(hook);
        when(terminal.getCaught()).thenReturn(caught);
        when(terminal.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_ENTITY);
        listener.onFish(terminal);

        verify(dispatcher, never()).dispatchPlayerFish(
                eq(player), eq(hook), eq(caught), eq(PlayerFishEvent.State.CAUGHT_ENTITY), eq(rodSnapshot));
    }

    @Test
    void dispatchesMovementWithinTheSameBlock() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getExtraContents()).thenReturn(null);
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        when(from.getBlockX()).thenReturn(1);
        when(from.getBlockY()).thenReturn(2);
        when(from.getBlockZ()).thenReturn(3);
        when(to.getBlockX()).thenReturn(1);
        when(to.getBlockY()).thenReturn(2);
        when(to.getBlockZ()).thenReturn(3);

        PlayerMoveEvent playerMove = mock(PlayerMoveEvent.class);
        when(playerMove.getPlayer()).thenReturn(player);
        when(playerMove.getFrom()).thenReturn(from);
        when(playerMove.getTo()).thenReturn(to);
        listener.onPlayerMove(playerMove);

        EntityMoveEvent entityMove = mock(EntityMoveEvent.class);
        when(entityMove.getEntity()).thenReturn(entity);
        when(entityMove.getFrom()).thenReturn(from);
        when(entityMove.getTo()).thenReturn(to);
        listener.onEntityMove(entityMove);

        verify(dispatcher).dispatchPlayerMove(eq(player), eq(from), eq(to), any(ItemStack[].class));
        verify(dispatcher).dispatchEntityMove(eq(entity), eq(from), eq(to), any(ItemStack[].class));
    }

    @Test
    void includesArmoredHorseEquipmentInEntityMovement() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Horse horse = mock(Horse.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        HorseInventory horseInventory = mock(HorseInventory.class);
        ItemStack horseArmor = mock(ItemStack.class);
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        when(horse.getEquipment()).thenReturn(equipment);
        when(equipment.getArmorContents()).thenReturn(new ItemStack[0]);
        when(horse.getInventory()).thenReturn(horseInventory);
        when(horseInventory.getArmor()).thenReturn(horseArmor);
        when(horseInventory.getSaddle()).thenReturn(null);
        when(from.getBlockX()).thenReturn(0);
        when(to.getBlockX()).thenReturn(1);
        when(from.getBlockY()).thenReturn(0);
        when(to.getBlockY()).thenReturn(0);
        when(from.getBlockZ()).thenReturn(0);
        when(to.getBlockZ()).thenReturn(0);

        EntityMoveEvent event = mock(EntityMoveEvent.class);
        when(event.getEntity()).thenReturn(horse);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        listener.onEntityMove(event);

        var captor = org.mockito.ArgumentCaptor.forClass(ItemStack[].class);
        verify(dispatcher).dispatchEntityMove(eq(horse), eq(from), eq(to), captor.capture());
        assertTrue(Arrays.asList(captor.getValue()).contains(horseArmor));
    }

    @Test
    void resolvesExperienceTriggersFromAllPlayerEquipment() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack heldItem = mock(ItemStack.class);
        ItemStack extraItem = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getEquipment()).thenReturn(equipment);
        when(inventory.getItemInMainHand()).thenReturn(heldItem);
        when(inventory.getArmorContents()).thenReturn(new ItemStack[0]);
        when(inventory.getExtraContents()).thenReturn(new ItemStack[]{extraItem});
        when(equipment.getItemInMainHand()).thenReturn(heldItem);
        when(equipment.getItemInOffHand()).thenReturn(extraItem);
        when(equipment.getArmorContents()).thenReturn(new ItemStack[0]);

        PlayerExpChangeEvent event = mock(PlayerExpChangeEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAmount()).thenReturn(3);
        when(dispatcher.dispatchExpGain(eq(player), eq(3), any(ItemStack[].class))).thenReturn(4);
        listener.onExpGain(event);

        var captor = org.mockito.ArgumentCaptor.forClass(ItemStack[].class);
        verify(dispatcher).dispatchExpGain(eq(player), eq(3), captor.capture());
        assertTrue(Arrays.asList(captor.getValue()).contains(heldItem));
        long extraOccurrences = Arrays.stream(captor.getValue()).filter(item -> item == extraItem).count();
        assertEquals(1L, extraOccurrences);
    }

    @Test
    void adaptsBlockBreakEventAndDispatches() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack pickaxe = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(pickaxe);

        Block block = mock(Block.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        listener.onBlockBreak(event);
        verify(dispatcher).dispatchBlockBreak(eq(player), eq(block), eq(pickaxe), any(CascadeScope.class));
    }

    @Test
    void skipsDispatchWhenCascadeDepthMaxed() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player player = mock(Player.class);
        Block block = mock(Block.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        CascadeGuard.runInScope(() -> {
            CascadeGuard.runInScope(() -> {
                CascadeGuard.runInScope(() -> {
                    assertEquals(3, CascadeGuard.getDepth());
                    listener.onBlockBreak(event);
                    verify(dispatcher, never()).dispatchBlockBreak(any(), any(), any(), any());
                });
            });
        });
    }
}
