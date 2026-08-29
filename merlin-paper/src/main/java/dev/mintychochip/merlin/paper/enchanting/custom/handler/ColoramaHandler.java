package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ItemDamageTrigger;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public final class ColoramaHandler implements OvercapEffectHandler, ItemDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("colorama");
    private static final int COLOR_BOUND = 256;

    private final Random random;

    public ColoramaHandler() {
        this(new Random());
    }

    public ColoramaHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public int onItemDamage(Player player, ItemStack item, int originalDamageAmount, int level) {
        if (player == null || item == null || level <= 0) return originalDamageAmount;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta leatherMeta)) return originalDamageAmount;

        leatherMeta.setColor(Color.fromRGB(
                random.nextInt(COLOR_BOUND), random.nextInt(COLOR_BOUND), random.nextInt(COLOR_BOUND)));
        item.setItemMeta(leatherMeta);
        return originalDamageAmount;
    }
}
