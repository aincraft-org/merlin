package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableExperience;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public final class PlunderHandler implements OvercapEffectHandler, EntityKillTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("plunder");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityKill(
            LivingEntity killer,
            LivingEntity victim,
            List<ItemStack> drops,
            MutableExperience experience,
            int level) {
        if (killer == null || victim == null || drops == null || drops.isEmpty() || experience == null || level <= 0) {
            return;
        }

        List<ItemStack> originalDrops = new ArrayList<>(drops);
        for (int rank = 0; rank < level; rank++) {
            for (ItemStack drop : originalDrops) {
                if (drop != null && !drop.isEmpty()) drops.add(drop.clone());
            }
        }
    }
}
