package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public final class CustomEnchantmentSupport {
    private static final Set<EntityType> NATIVE_NETHER_MOBS = Set.of(
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.HOGLIN,
            EntityType.MAGMA_CUBE,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.WITHER_SKELETON,
            EntityType.ZOGLIN,
            EntityType.ZOMBIFIED_PIGLIN
    );

    private CustomEnchantmentSupport() {}

    public static NamespacedKey customKey(String key) {
        return new NamespacedKey("merlin", key);
    }

    public static int randomPerRank(Random random, int min, int max, int level) {
        if (random == null || level <= 0 || min > max) return 0;

        long range = (long) max - min + 1L;
        long total = 0L;
        for (int rank = 0; rank < level; rank++) {
            int roll;
            if (range <= Integer.MAX_VALUE) {
                roll = random.nextInt((int) range) + min;
            } else {
                long candidate = (long) (random.nextDouble() * range);
                roll = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, candidate + min));
            }
            total += roll;
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (total <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        }
        return (int) total;
    }

    public static boolean isNativeNetherMob(Entity entity) {
        return entity != null && NATIVE_NETHER_MOBS.contains(entity.getType());
    }

    public static Material mobHeadMaterial(EntityType type) {
        if (type == null) return null;
        return switch (type) {
            case CREEPER -> Material.CREEPER_HEAD;
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            default -> null;
        };
    }

    public static void healToMax(LivingEntity entity, double amount) {
        if (entity == null || !Double.isFinite(amount) || amount <= 0.0) return;

        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) return;
        double maxHealth = maxHealthAttribute.getValue();
        double currentHealth = entity.getHealth();
        if (!Double.isFinite(maxHealth) || !Double.isFinite(currentHealth) || maxHealth <= currentHealth) return;

        entity.setHealth(Math.min(maxHealth, currentHealth + amount));
    }
}
