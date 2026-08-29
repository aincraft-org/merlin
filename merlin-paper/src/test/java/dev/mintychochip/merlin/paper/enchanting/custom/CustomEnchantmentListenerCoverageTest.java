package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.*;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentListenerCoverageTest {
    @Test
    void adaptsEverySupportedPaperEvent() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        EntityEquipment playerEquipment = mock(EntityEquipment.class);
        ItemStack item = mock(ItemStack.class);
        ItemStack fishingRod = mock(ItemStack.class);
        ItemStack fishingRodSnapshot = mock(ItemStack.class);
        when(fishingRod.clone()).thenReturn(fishingRodSnapshot);
        ItemStack launchSnapshot = mock(ItemStack.class);
        when(item.clone()).thenReturn(launchSnapshot);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        when(player.getEquipment()).thenReturn(playerEquipment);
        when(inventory.getItemInMainHand()).thenReturn(item);
        when(inventory.getItemInOffHand()).thenReturn(item);
        when(inventory.getArmorContents()).thenReturn(new ItemStack[]{item});
        when(inventory.getExtraContents()).thenReturn(new ItemStack[]{item});
        when(inventory.getBoots()).thenReturn(item);
        when(inventory.getChestplate()).thenReturn(item);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(item);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(fishingRod);
        when(playerEquipment.getItemInMainHand()).thenReturn(item);
        when(playerEquipment.getItemInOffHand()).thenReturn(item);
        when(playerEquipment.getArmorContents()).thenReturn(new ItemStack[]{item});

        LivingEntity living = mock(LivingEntity.class);
        EntityEquipment livingEquipment = mock(EntityEquipment.class);
        when(living.getEquipment()).thenReturn(livingEquipment);
        when(livingEquipment.getItemInMainHand()).thenReturn(item);
        when(livingEquipment.getItemInOffHand()).thenReturn(item);
        when(livingEquipment.getArmorContents()).thenReturn(new ItemStack[]{item});

        Entity entity = mock(Entity.class);
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(player);
        FishHook hook = mock(FishHook.class);
        Block block = mock(Block.class);
        BlockState blockState = mock(BlockState.class);
        Item dropEntity = mock(Item.class);
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        when(from.getBlockX()).thenReturn(0);
        when(from.getBlockY()).thenReturn(0);
        when(from.getBlockZ()).thenReturn(0);
        when(to.getBlockX()).thenReturn(1);
        when(to.getBlockY()).thenReturn(0);
        when(to.getBlockZ()).thenReturn(0);

        EntityDamageByEntityEvent attack = mock(EntityDamageByEntityEvent.class);
        when(attack.getDamager()).thenReturn(player);
        when(attack.getEntity()).thenReturn(living);
        when(attack.getDamage()).thenReturn(10.0);
        listener.onAttack(attack);
        verify(dispatcher).dispatchEntityHit(eq(player), eq(living), any(MutableDamage.class), eq(item));
        verify(dispatcher).dispatchEntityHitByEntity(
                eq(living), eq(player), any(MutableDamage.class), any(ItemStack[].class));

        EntityDamageEvent environmental = mock(EntityDamageEvent.class);
        when(environmental.getEntity()).thenReturn(player);
        when(environmental.getCause()).thenReturn(EntityDamageEvent.DamageCause.FIRE);
        when(environmental.getDamage()).thenReturn(4.0);
        listener.onEnvironmentalDamage(environmental);
        EntityDamageEvent entityEnvironmental = mock(EntityDamageEvent.class);
        when(entityEnvironmental.getEntity()).thenReturn(living);
        when(entityEnvironmental.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(entityEnvironmental.getDamage()).thenReturn(6.0);
        listener.onEnvironmentalDamage(entityEnvironmental);
        verify(dispatcher).dispatchEntityEnvironmentalDamage(
                eq(living), eq(EntityDamageEvent.DamageCause.FALL), any(MutableDamage.class), any(ItemStack[].class));

        when(dropEntity.getItemStack()).thenReturn(item);
        PlayerDropItemEvent drop = mock(PlayerDropItemEvent.class);
        when(drop.getPlayer()).thenReturn(player);
        when(drop.getItemDrop()).thenReturn(dropEntity);
        when(dispatcher.dispatchPlayerDrop(player, item)).thenReturn(true);
        listener.onDrop(drop);
        verify(dispatcher).dispatchPlayerDrop(player, item);
        verify(drop).setCancelled(true);

        when(player.getFoodLevel()).thenReturn(18);
        FoodLevelChangeEvent food = mock(FoodLevelChangeEvent.class);
        when(food.getEntity()).thenReturn(player);
        when(food.getFoodLevel()).thenReturn(5);
        when(dispatcher.dispatchFoodLevelChange(eq(player), eq(18), eq(5), any(ItemStack[].class))).thenReturn(30);
        listener.onFoodLevelChange(food);
        verify(food).setFoodLevel(20);

        org.bukkit.entity.AbstractHorse horse = mock(org.bukkit.entity.AbstractHorse.class);
        org.bukkit.inventory.HorseInventory horseInventory = mock(org.bukkit.inventory.HorseInventory.class);
        when(horse.getInventory()).thenReturn(horseInventory);
        when(horseInventory.getSaddle()).thenReturn(item);
        EntityEquipment horseEquipment = mock(EntityEquipment.class);
        when(horse.getEquipment()).thenReturn(horseEquipment);
        when(horseEquipment.getItemInMainHand()).thenReturn(item);
        HorseJumpEvent horseJump = mock(HorseJumpEvent.class);
        when(horseJump.getEntity()).thenReturn(horse);
        when(horseJump.getPower()).thenReturn(0.5f);
        when(dispatcher.dispatchHorseJump(eq(horse), eq(0.5f), any(ItemStack[].class))).thenReturn(2.0f);
        listener.onHorseJump(horseJump);
        verify(horseJump).setPower(1.0f);

        DamageSource source = mock(DamageSource.class);
        when(source.getCausingEntity()).thenReturn(player);
        EntityDeathEvent death = mock(EntityDeathEvent.class);
        List<ItemStack> drops = List.of(item);
        when(death.getDamageSource()).thenReturn(source);
        when(death.getEntity()).thenReturn(living);
        when(death.getDrops()).thenReturn(drops);
        when(death.getDroppedExp()).thenReturn(5);
        doAnswer(invocation -> {
            MutableExperience experience = invocation.getArgument(3);
            experience.add(4);
            return null;
        }).when(dispatcher).dispatchEntityKill(
                eq(player), eq(living), eq(drops), any(MutableExperience.class), eq(item));
        listener.onKill(death);
        verify(dispatcher).dispatchEntityKill(eq(player), eq(living), eq(drops), any(MutableExperience.class), eq(item));
        verify(death).setDroppedExp(9);

        EntityShootBowEvent bow = mock(EntityShootBowEvent.class);
        when(bow.getEntity()).thenReturn(living);
        when(bow.getBow()).thenReturn(item);
        when(bow.getProjectile()).thenReturn(projectile);
        when(bow.getForce()).thenReturn(0.75f);
        listener.onBowShoot(bow);
        listener.onBowShootFinal(bow);
        verify(dispatcher).dispatchBowShoot(eq(living), eq(projectile), eq(launchSnapshot), eq(0.75f));

        ProjectileHitEvent projectileHit = mock(ProjectileHitEvent.class);
        when(projectileHit.getEntity()).thenReturn(projectile);
        when(projectileHit.getHitEntity()).thenReturn(entity);
        when(projectileHit.getHitBlock()).thenReturn(block);
        listener.onProjectileHit(projectileHit);
        listener.onProjectileHitFinal(projectileHit);
        verify(dispatcher).dispatchProjectileHit(eq(player), eq(projectile), eq(entity), eq(block), eq(launchSnapshot));

        BlockBreakEvent blockBreak = mock(BlockBreakEvent.class);
        when(blockBreak.getPlayer()).thenReturn(player);
        when(blockBreak.getBlock()).thenReturn(block);
        listener.onBlockBreak(blockBreak);
        verify(dispatcher).dispatchBlockBreak(eq(player), eq(block), eq(item), any(CascadeScope.class));

        BlockDropItemEvent blockDrop = mock(BlockDropItemEvent.class);
        when(blockDrop.getPlayer()).thenReturn(player);
        when(blockDrop.getBlockState()).thenReturn(blockState);
        when(blockDrop.getItems()).thenReturn(List.of(dropEntity));
        listener.onBlockDrop(blockDrop);
        verify(dispatcher).dispatchBlockDrop(eq(player), eq(blockState), eq(List.of(dropEntity)), eq(item));

        BlockPlaceEvent blockPlace = mock(BlockPlaceEvent.class);
        when(blockPlace.getPlayer()).thenReturn(player);
        when(blockPlace.getBlockPlaced()).thenReturn(block);
        when(blockPlace.getBlockAgainst()).thenReturn(block);
        when(blockPlace.getItemInHand()).thenReturn(item);
        listener.onBlockPlace(blockPlace);
        verify(dispatcher).dispatchBlockPlace(eq(player), eq(block), eq(block), eq(item));

        PlayerFishEvent fish = mock(PlayerFishEvent.class);
        when(fish.getPlayer()).thenReturn(player);
        when(fish.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(fish.getHook()).thenReturn(hook);
        when(hook.getUniqueId()).thenReturn(UUID.randomUUID());
        when(fish.getCaught()).thenReturn(entity);
        when(fish.getState()).thenReturn(PlayerFishEvent.State.FISHING, PlayerFishEvent.State.CAUGHT_ENTITY);
        listener.onFish(fish);
        listener.onFish(fish);
        listener.onFishFinal(fish);
        verify(dispatcher).dispatchPlayerFish(
                eq(player), eq(hook), eq(entity), eq(PlayerFishEvent.State.CAUGHT_ENTITY), eq(fishingRodSnapshot));
        assertTrue(java.util.Arrays.stream(CustomEnchantmentListener.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("onFish")
                        && method.isAnnotationPresent(org.bukkit.event.EventHandler.class)));

        PlayerShearEntityEvent shear = mock(PlayerShearEntityEvent.class);
        when(shear.getPlayer()).thenReturn(player);
        when(shear.getEntity()).thenReturn(entity);
        when(shear.getItem()).thenReturn(item);
        when(shear.getHand()).thenReturn(EquipmentSlot.HAND);
        listener.onShear(shear);
        verify(dispatcher).dispatchShearEntity(eq(player), eq(entity), eq(item), eq(EquipmentSlot.HAND));

        PlayerBucketEmptyEvent bucketEmpty = mock(PlayerBucketEmptyEvent.class);
        when(bucketEmpty.getPlayer()).thenReturn(player);
        when(bucketEmpty.getBlock()).thenReturn(block);
        when(bucketEmpty.getBlockFace()).thenReturn(BlockFace.UP);
        when(bucketEmpty.getHand()).thenReturn(EquipmentSlot.HAND);
        when(bucketEmpty.getItemStack()).thenReturn(item);
        listener.onBucketEmpty(bucketEmpty);
        verify(dispatcher).dispatchBucketEmpty(eq(player), eq(block), eq(BlockFace.UP), eq(item), eq(EquipmentSlot.HAND));

        PlayerBucketFillEvent bucketFill = mock(PlayerBucketFillEvent.class);
        when(bucketFill.getPlayer()).thenReturn(player);
        when(bucketFill.getBlock()).thenReturn(block);
        when(bucketFill.getBlockFace()).thenReturn(BlockFace.UP);
        when(bucketFill.getHand()).thenReturn(EquipmentSlot.HAND);
        when(bucketFill.getItemStack()).thenReturn(item);
        listener.onBucketFill(bucketFill);
        verify(dispatcher).dispatchBucketFill(eq(player), eq(block), eq(BlockFace.UP), eq(item), eq(EquipmentSlot.HAND));

        PlayerItemDamageEvent playerItemDamage = mock(PlayerItemDamageEvent.class);
        when(playerItemDamage.getPlayer()).thenReturn(player);
        when(playerItemDamage.getItem()).thenReturn(item);
        when(playerItemDamage.getDamage()).thenReturn(3);
        when(dispatcher.dispatchItemDamage(player, item, 3)).thenReturn(2);
        listener.onItemDamage(playerItemDamage);
        verify(dispatcher).dispatchItemDamage(player, item, 3);

        EntityDamageItemEvent entityItemDamage = mock(EntityDamageItemEvent.class);
        when(entityItemDamage.getEntity()).thenReturn(entity);
        when(entityItemDamage.getItem()).thenReturn(item);
        when(entityItemDamage.getDamage()).thenReturn(3);
        when(dispatcher.dispatchEntityItemDamage(entity, item, 3)).thenReturn(2);
        listener.onEntityItemDamage(entityItemDamage);
        verify(dispatcher).dispatchEntityItemDamage(entity, item, 3);

        PlayerItemConsumeEvent consume = mock(PlayerItemConsumeEvent.class);
        when(consume.getPlayer()).thenReturn(player);
        when(consume.getItem()).thenReturn(item);
        listener.onItemConsume(consume);
        verify(dispatcher).dispatchItemConsume(player, item);

        PlayerInteractEvent interact = mock(PlayerInteractEvent.class);
        when(interact.getPlayer()).thenReturn(player);
        when(interact.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(interact.getClickedBlock()).thenReturn(block);
        when(interact.getItem()).thenReturn(item);
        listener.onInteract(interact);
        verify(dispatcher).dispatchActiveInteract(player, Action.RIGHT_CLICK_BLOCK, block, item);
        verify(dispatcher).dispatchActivate(player, item);

        PlayerMoveEvent playerMove = mock(PlayerMoveEvent.class);
        when(playerMove.getPlayer()).thenReturn(player);
        when(playerMove.getFrom()).thenReturn(from);
        when(playerMove.getTo()).thenReturn(to);
        listener.onPlayerMove(playerMove);
        verify(dispatcher).dispatchPlayerMove(eq(player), eq(from), eq(to), any(ItemStack[].class));

        EntityMoveEvent entityMove = mock(EntityMoveEvent.class);
        when(entityMove.getEntity()).thenReturn(living);
        when(entityMove.getFrom()).thenReturn(from);
        when(entityMove.getTo()).thenReturn(to);
        listener.onEntityMove(entityMove);
        verify(dispatcher).dispatchEntityMove(eq(living), eq(from), eq(to), any(ItemStack[].class));

        PlayerJumpEvent jump = mock(PlayerJumpEvent.class);
        when(jump.getPlayer()).thenReturn(player);
        when(jump.getFrom()).thenReturn(from);
        when(jump.getTo()).thenReturn(to);
        listener.onPlayerJump(jump);
        verify(dispatcher).dispatchPlayerJump(eq(player), eq(from), eq(to), eq(item));

        EntityToggleGlideEvent glide = mock(EntityToggleGlideEvent.class);
        when(glide.getEntity()).thenReturn(player);
        when(glide.isGliding()).thenReturn(true);
        listener.onToggleGlide(glide);
        verify(dispatcher).dispatchToggleGlide(player, true, item);

        org.bukkit.event.player.PlayerToggleSneakEvent sneak = mock(org.bukkit.event.player.PlayerToggleSneakEvent.class);
        when(sneak.getPlayer()).thenReturn(player);
        when(sneak.isSneaking()).thenReturn(true);
        listener.onToggleSneak(sneak);
        verify(dispatcher).dispatchToggleSneak(player, true, new ItemStack[]{item});

        org.bukkit.event.player.PlayerToggleSprintEvent sprint = mock(org.bukkit.event.player.PlayerToggleSprintEvent.class);
        when(sprint.getPlayer()).thenReturn(player);
        when(sprint.isSprinting()).thenReturn(true);
        listener.onToggleSprint(sprint);
        verify(dispatcher).dispatchToggleSprint(player, true, item);

        PlayerInteractEntityEvent interactEntity = mock(PlayerInteractEntityEvent.class);
        when(interactEntity.getPlayer()).thenReturn(player);
        when(interactEntity.getRightClicked()).thenReturn(entity);
        when(interactEntity.getHand()).thenReturn(EquipmentSlot.HAND);
        listener.onInteractEntity(interactEntity);
        verify(dispatcher).dispatchEntityInteract(player, entity, item, EquipmentSlot.HAND);

        PlayerExpChangeEvent exp = mock(PlayerExpChangeEvent.class);
        when(exp.getPlayer()).thenReturn(player);
        when(exp.getAmount()).thenReturn(3);
        when(dispatcher.dispatchExpGain(eq(player), eq(3), any(ItemStack[].class))).thenReturn(4);
        listener.onExpGain(exp);
        verify(dispatcher).dispatchExpGain(eq(player), eq(3), any(ItemStack[].class));

        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        listener.onPlayerQuit(quit);
        verify(dispatcher).clearActivationCooldowns(any(UUID.class));
        verify(dispatcher).purgeActivationCooldowns();
    }
}
