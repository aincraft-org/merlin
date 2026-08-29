package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ItemDamageTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class UnbreakableHandler implements OvercapEffectHandler, ItemDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("unbreakable");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public int onItemDamage(Player player, ItemStack item, int originalDamageAmount, int level) {
        return level > 0 ? 0 : originalDamageAmount;
    }
}
