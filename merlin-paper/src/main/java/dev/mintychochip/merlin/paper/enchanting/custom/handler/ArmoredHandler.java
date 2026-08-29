package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ArmoredHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("armored");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (damage == null || damage.isCancelled() || level <= 0 || damage.getInitialDamage() <= 0.0
                || !(attacker instanceof LivingEntity living)
                || !ArmorDefenseSupport.attackerHolds(living, "_SWORD")) return;
        damage.multiply(ArmorDefenseSupport.reductionMultiplier(level, 0.10));
    }
}
