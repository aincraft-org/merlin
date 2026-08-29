package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.FireworkBoostTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class MomentumHandler implements OvercapEffectHandler, FireworkBoostTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("momentum");
    private static final double BOOST_PER_LEVEL = 0.5;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onFireworkBoost(Player player, org.bukkit.entity.LivingEntity shooter, ItemStack elytra, int level) {
        if (player == null || elytra == null || level <= 0) return;
        if (shooter == null || !shooter.equals(player)) return;
        if (!player.isGliding()) return;

        Vector velocity = player.getVelocity();
        if (velocity == null) return;
        player.setVelocity(velocity.multiply(1.0 + BOOST_PER_LEVEL * level));
    }
}