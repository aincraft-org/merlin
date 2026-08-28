package dev.mintychochip.merlin.paper.enchanting.custom;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActiveInteractTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public final class CustomEnchantmentDispatcher {
    private final OvercapItemAdapter itemAdapter;
    private final EnchantmentRegistry registry;

    public CustomEnchantmentDispatcher(OvercapItemAdapter itemAdapter, EnchantmentRegistry registry) {
        this.itemAdapter = itemAdapter;
        this.registry = registry;
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

        // Higher priority executes first; secondary sort by namespaced key for deterministic stability
        resolved.sort(Comparator.<BoundTrigger<T>>comparingInt(BoundTrigger::priority).reversed()
                .thenComparing(t -> t.key().toString()));
        return resolved;
    }

    public void dispatchEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, ItemStack weapon) {
        for (var bound : resolveTriggers(weapon, EntityHitTrigger.class)) {
            bound.trigger().onEntityHit(attacker, victim, damage, bound.level());
        }
    }

    public void dispatchBlockBreak(Player player, Block block, ItemStack tool, CascadeScope scope) {
        for (var bound : resolveTriggers(tool, BlockBreakTrigger.class)) {
            bound.trigger().onBlockBreak(player, block, bound.level(), scope);
        }
    }

    public void dispatchEntityKill(Player killer, LivingEntity victim, ItemStack weapon) {
        for (var bound : resolveTriggers(weapon, EntityKillTrigger.class)) {
            bound.trigger().onEntityKill(killer, victim, bound.level());
        }
    }

    public void dispatchBowShoot(Player shooter, Projectile projectile, float force, ItemStack bow) {
        for (var bound : resolveTriggers(bow, BowShootTrigger.class)) {
            bound.trigger().onBowShoot(shooter, projectile, force, bound.level());
        }
    }

    public void dispatchArmorDefense(Player defender, Entity attacker, MutableDamage damage, ItemStack[] armor) {
        if (armor == null) return;
        List<BoundTrigger<ArmorDefenseTrigger>> all = new ArrayList<>();
        for (ItemStack piece : armor) {
            all.addAll(resolveTriggers(piece, ArmorDefenseTrigger.class));
        }
        all.sort(Comparator.<BoundTrigger<ArmorDefenseTrigger>>comparingInt(BoundTrigger::priority).reversed()
                .thenComparing(t -> t.key().toString()));
        for (var bound : all) {
            bound.trigger().onArmorDefense(defender, attacker, damage, bound.level());
        }
    }

    public void dispatchActiveInteract(Player player, ItemStack item) {
        for (var bound : resolveTriggers(item, ActiveInteractTrigger.class)) {
            bound.trigger().onActiveInteract(player, item, bound.level());
        }
    }
}
