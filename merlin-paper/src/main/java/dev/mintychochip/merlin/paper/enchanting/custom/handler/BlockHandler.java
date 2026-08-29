package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public final class BlockHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("block");

    private final Random random;

    public BlockHandler() {
        this(new Random());
    }

    public BlockHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (attacker == null || victim == null || damage == null || level <= 0) return;
        if (damage.isCancelled()) return;
        if (random.nextDouble() >= 0.08 * level) return;

        damage.multiply(0.0);
    }
}