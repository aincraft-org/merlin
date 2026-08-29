package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeGuard;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ReflectHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("reflect");
    private static final double DAMAGE_PER_LEVEL = 0.10;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (defender == null || damage == null || damage.isCancelled() || level <= 0
                || damage.getInitialDamage() <= 0.0) return;
        LivingEntity livingAttacker = ArmorDefenseSupport.livingAttacker(attacker);
        if (livingAttacker == null) return;

        double reflectedDamage = damage.getInitialDamage() * DAMAGE_PER_LEVEL * level;
        if (Double.isFinite(reflectedDamage) && reflectedDamage > 0.0) {
            if (CascadeGuard.canCascade()) {
                CascadeGuard.runInScope(() -> livingAttacker.damage(reflectedDamage, defender));
            }
        }
    }
}
