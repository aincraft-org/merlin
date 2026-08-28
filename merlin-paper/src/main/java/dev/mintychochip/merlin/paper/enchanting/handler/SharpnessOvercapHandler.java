package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Over-cap handler for Sharpness VI+.
 *
 * <p>Deliberate balance design:
 * In vanilla Minecraft, Sharpness adds {@code 0.5 * level + 0.5} damage (Sharpness V = +3.0 damage).
 * For transcendent over-cap tiers (level > 5), each additional rank contributes a deliberate
 * flat bonus of +1.5 damage (Level 6 = +4.5 total bonus, Level 7 = +6.0 total bonus).
 */
public final class SharpnessOvercapHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = NamespacedKey.minecraft("sharpness");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    public double calculateBonusDamage(int level, int vanillaMax) {
        int extra = Math.max(0, level - vanillaMax);
        return extra * 1.5;
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, int level) {
        if (level > 5) {
            double bonus = calculateBonusDamage(level, 5);
            event.setDamage(event.getDamage() + bonus);
        }
    }
}
