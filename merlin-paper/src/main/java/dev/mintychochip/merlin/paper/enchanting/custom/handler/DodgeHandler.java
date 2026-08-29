package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import java.util.Objects;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public final class DodgeHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("dodge");
    private static final int ROLL_RANGE = 100;
    private static final int CHANCE_PER_LEVEL = 10;

    private final Random random;

    public DodgeHandler() {
        this(new Random());
    }

    public DodgeHandler(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (damage == null || damage.isCancelled() || level <= 0 || damage.getInitialDamage() <= 0.0
                || attacker instanceof Projectile || ArmorDefenseSupport.livingAttacker(attacker) == null) return;

        int chance = (int) Math.min(ROLL_RANGE, (long) CHANCE_PER_LEVEL * level);
        if (random.nextInt(ROLL_RANGE) < chance) damage.setCancelled(true);
    }
}
