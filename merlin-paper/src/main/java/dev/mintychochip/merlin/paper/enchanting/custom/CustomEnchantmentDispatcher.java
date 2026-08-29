package dev.mintychochip.merlin.paper.enchanting.custom;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class CustomEnchantmentDispatcher {
    private final OvercapItemAdapter itemAdapter;
    private final EnchantmentRegistry registry;
    private final ActivationCooldowns activationCooldowns;

    public CustomEnchantmentDispatcher(OvercapItemAdapter itemAdapter, EnchantmentRegistry registry) {
        this(itemAdapter, registry, new ActivationCooldowns());
    }

    public CustomEnchantmentDispatcher(
            OvercapItemAdapter itemAdapter,
            EnchantmentRegistry registry,
            ActivationCooldowns activationCooldowns) {
        this.itemAdapter = itemAdapter;
        this.registry = registry;
        this.activationCooldowns = activationCooldowns;
    }

    public void purgeActivationCooldowns() {
        activationCooldowns.purgeExpired();
    }

    void clearActivationCooldowns(UUID playerId) {
        activationCooldowns.clearPlayer(playerId);
    }

    private record BoundTrigger<T>(T trigger, int level, int priority, NamespacedKey key) {}

    private <T> List<BoundTrigger<T>> resolveTriggers(ItemStack item, Class<T> triggerType) {
        if (item == null || item.isEmpty()) return List.of();
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(item);
        List<BoundTrigger<T>> resolved = new ArrayList<>();

        for (var entry : enchants.entrySet()) {
            NamespacedKey key = entry.getKey();
            int level = entry.getValue();
            var defOpt = registry.get(key);
            if (defOpt.isPresent() && defOpt.get().overcapHandler().isPresent()) {
                OvercapEffectHandler handler = defOpt.get().overcapHandler().get();
                if (triggerType.isInstance(handler)) {
                    resolved.add(new BoundTrigger<>(triggerType.cast(handler), level, handler.priority(), key));
                }
            }
        }

        resolved.sort(Comparator.<BoundTrigger<T>>comparingInt(BoundTrigger::priority).reversed()
                .thenComparing(t -> t.key().toString()));
        return resolved;
    }

    private <T> List<BoundTrigger<T>> resolveAllEquipped(ItemStack[] equipment, Class<T> triggerType) {
        if (equipment == null) return List.of();
        List<BoundTrigger<T>> all = new ArrayList<>();
        for (ItemStack piece : equipment) {
            all.addAll(resolveTriggers(piece, triggerType));
        }
        all.sort(Comparator.<BoundTrigger<T>>comparingInt(BoundTrigger::priority).reversed()
                .thenComparing(t -> t.key().toString()));
        return all;
    }

    // 1. Combat (Offense & Defense)
    public void dispatchEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, ItemStack weapon) {
        for (var bound : resolveTriggers(weapon, EntityHitTrigger.class)) {
            bound.trigger().onEntityHit(attacker, victim, damage, bound.level());
        }
    }

    public void dispatchEntityHitByEntity(LivingEntity victim, Entity attacker, MutableDamage damage, ItemStack[] equipment) {
        for (var bound : resolveAllEquipped(equipment, EntityHitByEntityTrigger.class)) {
            bound.trigger().onEntityHitByEntity(victim, attacker, damage, bound.level());
        }
    }

    public void dispatchArmorDefense(Player defender, Entity attacker, MutableDamage damage, ItemStack[] armor) {
        for (var bound : resolveAllEquipped(armor, ArmorDefenseTrigger.class)) {
            bound.trigger().onArmorDefense(defender, attacker, damage, bound.level());
        }
    }
    public void dispatchEntityKill(
            LivingEntity killer,
            LivingEntity victim,
            List<ItemStack> drops,
            MutableExperience experience,
            ItemStack weapon) {
        for (var bound : resolveTriggers(weapon, EntityKillTrigger.class)) {
            bound.trigger().onEntityKill(killer, victim, drops, experience, bound.level());
        }
    }

    public boolean dispatchPlayerDrop(Player player, ItemStack item) {
        boolean cancelled = false;
        for (var bound : resolveTriggers(item, PlayerDropItemTrigger.class)) {
            cancelled |= bound.trigger().shouldCancelDrop(player, item, bound.level());
        }
        return cancelled;
    }

    public int dispatchFoodLevelChange(
            Player player,
            int currentFoodLevel,
            int proposedFoodLevel,
            ItemStack[] equipment) {
        int current = proposedFoodLevel;
        for (var bound : resolveAllEquipped(equipment, FoodLevelChangeTrigger.class)) {
            current = bound.trigger().onFoodLevelChange(player, currentFoodLevel, current, bound.level());
        }
        return current;
    }

    public float dispatchHorseJump(AbstractHorse horse, float power, ItemStack[] equipment) {
        float current = power;
        for (var bound : resolveAllEquipped(equipment, HorseJumpTrigger.class)) {
            current = bound.trigger().onHorseJump(horse, current, bound.level());
        }
        return current;
    }

    public void dispatchEntityEnvironmentalDamage(
            LivingEntity entity,
            DamageCause cause,
            MutableDamage damage,
            ItemStack[] equipment) {
        for (var bound : resolveAllEquipped(equipment, EntityEnvironmentalDamageTrigger.class)) {
            bound.trigger().onEnvironmentalDamage(entity, cause, damage, bound.level());
        }
    }

    public void dispatchEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, ItemStack[] armor) {
        for (var bound : resolveAllEquipped(armor, EnvironmentalDamageTrigger.class)) {
            bound.trigger().onEnvironmentalDamage(player, cause, damage, bound.level());
        }
    }

    // 2. Ranged & Projectiles
    public void dispatchBowShoot(LivingEntity shooter, Entity projectile, ItemStack bow, float force) {
        for (var bound : resolveTriggers(bow, BowShootTrigger.class)) {
            bound.trigger().onBowShoot(shooter, projectile, bow, force, bound.level());
        }
    }

    public void dispatchProjectileHit(LivingEntity shooter, Projectile projectile, Entity hitEntity, Block hitBlock, ItemStack weapon) {
        for (var bound : resolveTriggers(weapon, ProjectileHitTrigger.class)) {
            bound.trigger().onProjectileHit(shooter, projectile, hitEntity, hitBlock, bound.level());
        }
    }

    // 3. Mining, Harvesting & Blocks
    public void dispatchBlockBreak(Player player, Block block, ItemStack tool, CascadeScope scope) {
        for (var bound : resolveTriggers(tool, BlockBreakTrigger.class)) {
            bound.trigger().onBlockBreak(player, block, bound.level(), scope);
        }
    }

    public void dispatchBlockDrop(Player player, BlockState state, List<Item> items, ItemStack tool) {
        for (var bound : resolveTriggers(tool, BlockDropTrigger.class)) {
            bound.trigger().onBlockDrop(player, state, items, bound.level());
        }
    }

    public void dispatchBlockPlace(Player player, Block placed, Block against, ItemStack handItem) {
        for (var bound : resolveTriggers(handItem, BlockPlaceTrigger.class)) {
            bound.trigger().onBlockPlace(player, placed, against, handItem, bound.level());
        }
    }

    // 4. Gathering & Utility Tools
    public void dispatchPlayerFish(Player player, FishHook hook, Entity caught, State state, ItemStack rod) {
        for (var bound : resolveTriggers(rod, PlayerFishTrigger.class)) {
            bound.trigger().onPlayerFish(player, hook, caught, state, bound.level());
        }
    }

    public void dispatchShearEntity(Player player, Entity sheared, ItemStack shears, EquipmentSlot hand) {
        for (var bound : resolveTriggers(shears, ShearEntityTrigger.class)) {
            bound.trigger().onShearEntity(player, sheared, shears, hand, bound.level());
        }
    }

    public boolean dispatchBucketEmpty(
            Player player, Block clicked, BlockFace face, ItemStack bucket, EquipmentSlot hand) {
        var resolved = resolveTriggers(bucket, BucketEmptyTrigger.class);
        for (var bound : resolved) {
            bound.trigger().onBucketEmpty(player, clicked, face, bucket, hand, bound.level());
        }
        return !resolved.isEmpty();
    }

    public boolean dispatchBucketFill(
            Player player, Block clicked, BlockFace face, ItemStack bucket, EquipmentSlot hand) {
        var resolved = resolveTriggers(bucket, BucketFillTrigger.class);
        for (var bound : resolved) {
            bound.trigger().onBucketFill(player, clicked, face, bucket, hand, bound.level());
        }
        return !resolved.isEmpty();
    }

    public int dispatchItemDamage(Player player, ItemStack item, int amount) {
        int current = amount;
        for (var bound : resolveTriggers(item, ItemDamageTrigger.class)) {
            current = bound.trigger().onItemDamage(player, item, current, bound.level());
        }
        return current;
    }

    public int dispatchEntityItemDamage(Entity entity, ItemStack item, int amount) {
        int current = amount;
        for (var bound : resolveTriggers(item, EntityItemDamageTrigger.class)) {
            current = bound.trigger().onEntityItemDamage(entity, item, current, bound.level());
        }
        return current;
    }

    public void dispatchItemConsume(Player player, ItemStack item) {
        for (var bound : resolveTriggers(item, ItemConsumeTrigger.class)) {
            bound.trigger().onItemConsume(player, item, bound.level());
        }
    }

    public void dispatchActiveInteract(Player player, Action action, Block clicked, ItemStack item) {
        for (var bound : resolveTriggers(item, ActiveInteractTrigger.class)) {
            bound.trigger().onActiveInteract(player, action, clicked, item, bound.level());
        }
    }

    public boolean dispatchActivate(Player player, ItemStack item) {
        boolean activated = false;
        for (var bound : resolveTriggers(item, ActivateTrigger.class)) {
            if (!activationCooldowns.tryAcquire(player.getUniqueId(), bound.key(), bound.trigger().activationCooldown())) {
                continue;
            }

            boolean accepted;
            try {
                accepted = bound.trigger().onActivate(bound.level(), player, item);
            } catch (RuntimeException | Error failure) {
                activationCooldowns.release(player.getUniqueId(), bound.key());
                throw failure;
            }
            if (!accepted) {
                activationCooldowns.release(player.getUniqueId(), bound.key());
            } else {
                activated = true;
            }
        }
        return activated;
    }

    // 5. Movement & Traversal
    public void dispatchPlayerMove(Player player, Location from, Location to, ItemStack[] equipment) {
        for (var bound : resolveAllEquipped(equipment, PlayerMoveTrigger.class)) {
            bound.trigger().onPlayerMove(player, from, to, bound.level());
        }
    }

    public void dispatchEntityMove(LivingEntity entity, Location from, Location to, ItemStack[] equipment) {
        for (var bound : resolveAllEquipped(equipment, EntityMoveTrigger.class)) {
            bound.trigger().onEntityMove(entity, from, to, bound.level());
        }
    }

    public void dispatchPlayerJump(Player player, Location from, Location to, ItemStack boots) {
        for (var bound : resolveTriggers(boots, PlayerJumpTrigger.class)) {
            bound.trigger().onPlayerJump(player, from, to, bound.level());
        }
    }

    public void dispatchToggleGlide(Player player, boolean isGliding, ItemStack chestplate) {
        for (var bound : resolveTriggers(chestplate, PlayerToggleGlideTrigger.class)) {
            bound.trigger().onToggleGlide(player, isGliding, bound.level());
        }
    }

    public void dispatchToggleSneak(Player player, boolean isSneaking, ItemStack[] armor) {
        for (var bound : resolveAllEquipped(armor, PlayerToggleSneakTrigger.class)) {
            bound.trigger().onToggleSneak(player, isSneaking, bound.level());
        }
    }

    public void dispatchToggleSprint(Player player, boolean isSprinting, ItemStack boots) {
        for (var bound : resolveTriggers(boots, PlayerToggleSprintTrigger.class)) {
            bound.trigger().onToggleSprint(player, isSprinting, bound.level());
        }
    }

    // 6. Entity Interaction & Exp
    public void dispatchEntityInteract(Player player, Entity entity, ItemStack item, EquipmentSlot hand) {
        for (var bound : resolveTriggers(item, EntityInteractTrigger.class)) {
            bound.trigger().onEntityInteract(player, entity, item, hand, bound.level());
        }
    }

    public int dispatchExpGain(Player player, int originalAmount, ItemStack[] allEquipped) {
        int current = originalAmount;
        for (var bound : resolveAllEquipped(allEquipped, ExpGainTrigger.class)) {
            current = bound.trigger().onExpGain(player, current, bound.level());
        }
        return current;
    }
}
