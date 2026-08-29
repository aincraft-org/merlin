package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.HookContactTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FireHookHandler implements OvercapEffectHandler, HookContactTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("fire_hook");
    private static final int FIRE_TICKS = 80;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onHookContact(Player player, LivingEntity hit, int level) {
        if (player == null || hit == null || level <= 0) return;
        if (hit.isDead()) return;
        hit.setFireTicks(FIRE_TICKS * level);
    }
}