package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.FoodLevelChangeTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public final class QuenchingHandler implements OvercapEffectHandler, FoodLevelChangeTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("quenching");
    private static final int MAX_FOOD_LEVEL = 20;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public int onFoodLevelChange(Player player, int currentFoodLevel, int proposedFoodLevel, int level) {
        if (player == null || level <= 0 || proposedFoodLevel >= currentFoodLevel) return proposedFoodLevel;
        return Math.min(MAX_FOOD_LEVEL, proposedFoodLevel + level);
    }
}
