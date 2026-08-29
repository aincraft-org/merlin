package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ShearEntityTrigger;
import java.util.Random;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class PrismaticHandler implements OvercapEffectHandler, ShearEntityTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("prismatic");

    private final Random random;

    public PrismaticHandler() {
        this(new Random());
    }

    public PrismaticHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onShearEntity(Player player, Entity shearedEntity, ItemStack shears,
                               EquipmentSlot hand, int level) {
        if (player == null || !(shearedEntity instanceof Sheep sheep) || shears == null
                || shears.getType() != Material.SHEARS || level <= 0) return;

        DyeColor[] colors = DyeColor.values();
        sheep.setColor(colors[random.nextInt(colors.length)]);
    }
}
