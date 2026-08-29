package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import java.util.List;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableExperience;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface EntityKillTrigger {
    void onEntityKill(LivingEntity killer, LivingEntity victim, List<ItemStack> drops, MutableExperience experience, int level);
}
