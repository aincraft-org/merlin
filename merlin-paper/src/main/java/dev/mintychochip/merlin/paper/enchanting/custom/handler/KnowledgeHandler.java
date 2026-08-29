package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableExperience;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
import java.util.List;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public final class KnowledgeHandler implements OvercapEffectHandler, EntityKillTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("knowledge");

    private final Random random;

    public KnowledgeHandler() {
        this(new Random());
    }

    public KnowledgeHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

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
        experience.add(CustomEnchantmentSupport.randomPerRank(random, 3, 4, level));
    }
}
