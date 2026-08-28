package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
