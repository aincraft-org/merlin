package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class AngelicHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("angelic");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (defender == null || damage == null || level <= 0 || damage.isCancelled()
                || damage.getFinalDamage() <= 0.0) return;
        CustomEnchantmentSupport.healToMax(defender, level);
    }
}
