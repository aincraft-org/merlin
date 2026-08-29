package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public final class EquilibriumHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("equilibrium");

    private final Random random;
    private boolean applyingSelfDamage;

    public EquilibriumHandler() {
        this(new Random());
    }

    public EquilibriumHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (applyingSelfDamage || attacker == null || victim == null || damage == null || level <= 0) return;

        int bonus = CustomEnchantmentSupport.randomPerRank(random, 1, 3, level);
        damage.addBonus(bonus);
        if (bonus <= 0) return;

        applyingSelfDamage = true;
        try {
            attacker.damage(bonus / 2.0);
        } finally {
            applyingSelfDamage = false;
        }
    }
}
