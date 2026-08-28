package dev.mintychochip.merlin.paper.enchanting.custom;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActiveInteractTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
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

    public void dispatchEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(weapon);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof EntityHitTrigger trigger) {
                            trigger.onEntityHit(attacker, victim, damage, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchBlockBreak(Player player, Block block, ItemStack tool, CascadeScope scope) {
        if (tool == null || tool.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(tool);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof BlockBreakTrigger trigger) {
                            trigger.onBlockBreak(player, block, entry.getValue(), scope);
                        }
                    });
        }
    }

    public void dispatchEntityKill(Player killer, LivingEntity victim, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(weapon);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof EntityKillTrigger trigger) {
                            trigger.onEntityKill(killer, victim, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchBowShoot(Player shooter, Projectile projectile, float force, ItemStack bow) {
        if (bow == null || bow.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(bow);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof BowShootTrigger trigger) {
                            trigger.onBowShoot(shooter, projectile, force, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchArmorDefense(Player defender, Entity attacker, MutableDamage damage, ItemStack[] armor) {
        if (armor == null) return;
        for (ItemStack piece : armor) {
            if (piece == null || piece.isEmpty()) continue;
            Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(piece);
            for (var entry : enchants.entrySet()) {
                registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                        .ifPresent(handler -> {
                            if (handler instanceof ArmorDefenseTrigger trigger) {
                                trigger.onArmorDefense(defender, attacker, damage, entry.getValue());
                            }
                        });
            }
        }
    }

    public void dispatchActiveInteract(Player player, ItemStack item) {
        if (item == null || item.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(item);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof ActiveInteractTrigger trigger) {
                            trigger.onActiveInteract(player, item, entry.getValue());
                        }
                    });
        }
    }
}
