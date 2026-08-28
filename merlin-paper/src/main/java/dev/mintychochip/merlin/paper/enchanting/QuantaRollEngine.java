package dev.mintychochip.merlin.paper.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public final class QuantaRollEngine {
    private final EnchantmentRegistry registry;
    private final OfferConfig config;

    public QuantaRollEngine(EnchantmentRegistry registry, OfferConfig config) {
        this.registry = registry;
        this.config = config;
    }

    public EnchantingOffer generateOffer(Material material, AltarProfile profile, int tier, Random random) {
        OfferConfig.TierSetting setting = switch (tier) {
            case 1 -> config.tier1();
            case 2 -> config.tier2();
            default -> config.tier3();
        };

        List<EnchantmentDefinition> eligible = registry.findEligible(material, profile.totalEterna());
        if (eligible.isEmpty()) {
            return new EnchantingOffer(tier, setting.xpLevelCost(), setting.xpLevelRequirement(), setting.lapisCost(), Map.of(), "None");
        }

        Map<NamespacedKey, Integer> rolled = new HashMap<>();
        EnchantmentDefinition primary = pickWeighted(eligible, random);
        int maxLevelForEterna = calculateMaxLevelForEterna(primary, profile.totalEterna());
        int baseLevel = Math.max(1, (maxLevelForEterna * tier) / 3);

        // Apply Quanta check for rank boost
        double boostChance = Math.min(0.90, profile.totalQuanta() * setting.quantaBonusMultiplier());
        if (random.nextDouble() < boostChance && baseLevel < maxLevelForEterna) {
            baseLevel++;
        }
        rolled.put(primary.key(), baseLevel);

        // Secondary enchantments
        int extraCount = setting.minEnchants() - 1;
        if (random.nextDouble() < boostChance && extraCount < (setting.maxEnchants() - 1)) {
            extraCount++;
        }

        List<EnchantmentDefinition> pool = new ArrayList<>(eligible);
        pool.remove(primary);
        for (int i = 0; i < extraCount && !pool.isEmpty(); i++) {
            EnchantmentDefinition extra = pickWeighted(pool, random);
            pool.remove(extra);
            int extraMax = calculateMaxLevelForEterna(extra, profile.totalEterna());
            int extraLevel = Math.max(1, (extraMax * tier) / 4);
            rolled.put(extra.key(), extraLevel);
        }

        String hint = primary.displayName() + " " + toRoman(baseLevel) + " (?..)";
        return new EnchantingOffer(tier, setting.xpLevelCost(), setting.xpLevelRequirement(), setting.lapisCost(), rolled, hint);
    }

    private static int calculateMaxLevelForEterna(EnchantmentDefinition def, double eterna) {
        int lvl = 1;
        while (lvl < def.absoluteMaxLevel() && def.minEternaForLevel(lvl + 1) <= eterna) {
            lvl++;
        }
        return lvl;
    }

    private static EnchantmentDefinition pickWeighted(List<EnchantmentDefinition> list, Random random) {
        int totalWeight = list.stream().mapToInt(EnchantmentDefinition::weight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        int running = 0;
        for (EnchantmentDefinition def : list) {
            running += def.weight();
            if (roll < running) return def;
        }
        return list.get(list.size() - 1);
    }

    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}
