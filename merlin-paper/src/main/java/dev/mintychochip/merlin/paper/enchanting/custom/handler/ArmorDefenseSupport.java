package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

final class ArmorDefenseSupport {
    private ArmorDefenseSupport() {}

    static boolean attackerHolds(LivingEntity attacker, String materialSuffix) {
        if (attacker == null || materialSuffix == null) return false;
        EntityEquipment equipment = attacker.getEquipment();
        if (equipment == null) return false;
        ItemStack item = equipment.getItemInMainHand();
        return item != null && !item.isEmpty() && item.getType() != null
                && item.getType().name().endsWith(materialSuffix);
    }

    static LivingEntity livingAttacker(Entity attacker) {
        if (attacker instanceof LivingEntity living) return living;
        if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    static double reductionMultiplier(int level, double reductionPerLevel) {
        if (level <= 0 || !Double.isFinite(reductionPerLevel) || reductionPerLevel <= 0.0) return 1.0;
        return Math.max(0.0, 1.0 - reductionPerLevel * level);
    }
}
