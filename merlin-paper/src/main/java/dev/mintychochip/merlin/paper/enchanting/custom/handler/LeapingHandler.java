package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.HorseJumpTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractHorse;

public final class LeapingHandler implements OvercapEffectHandler, HorseJumpTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("leaping");
    private static final float API_MAXIMUM = 1.0f;
    private static final float POWER_PER_LEVEL = 0.1f;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public float onHorseJump(AbstractHorse horse, float power, int level) {
        if (horse == null || level <= 0) return power;
        return Math.min(API_MAXIMUM, power + POWER_PER_LEVEL * level);
    }
}
