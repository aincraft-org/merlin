package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class MoltenHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("molten");
    private static final int FIRE_TICKS_PER_LEVEL = 60;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (damage == null || damage.isCancelled() || level <= 0 || damage.getInitialDamage() <= 0.0) return;
        LivingEntity livingAttacker = ArmorDefenseSupport.livingAttacker(attacker);
        if (livingAttacker == null) return;

        int fireTicks = (int) Math.min(Integer.MAX_VALUE, (long) FIRE_TICKS_PER_LEVEL * level);
        livingAttacker.setFireTicks(Math.max(livingAttacker.getFireTicks(), fireTicks));
    }
}
