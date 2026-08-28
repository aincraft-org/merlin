package dev.mintychochip.merlin.paper.enchanting;

import org.bukkit.configuration.ConfigurationSection;

public record OfferConfig(
        TierSetting tier1,
        TierSetting tier2,
        TierSetting tier3
) {
    public record TierSetting(
            int xpLevelCost,
            int xpLevelRequirement,
            int lapisCost,
            int minEnchants,
            int maxEnchants,
            double quantaBonusMultiplier
    ) {}

    public static OfferConfig fromSection(ConfigurationSection section) {
        if (section == null) {
            return defaultConfig();
        }
        return new OfferConfig(
                loadTier(section.getConfigurationSection("tier_1"), 1, 10, 1, 1, 1, 0.5),
                loadTier(section.getConfigurationSection("tier_2"), 2, 20, 2, 1, 2, 1.0),
                loadTier(section.getConfigurationSection("tier_3"), 3, 30, 3, 2, 3, 1.5)
        );
    }

    private static TierSetting loadTier(ConfigurationSection sec, int defCost, int defReq, int defLapis, int defMin, int defMax, double defQuanta) {
        if (sec == null) {
            return new TierSetting(defCost, defReq, defLapis, defMin, defMax, defQuanta);
        }
        return new TierSetting(
                sec.getInt("xp_level_cost", defCost),
                sec.getInt("xp_level_requirement", defReq),
                sec.getInt("lapis_cost", defLapis),
                sec.getInt("min_enchantments", defMin),
                sec.getInt("max_enchantments", defMax),
                sec.getDouble("quanta_bonus_multiplier", defQuanta)
        );
    }

    public static OfferConfig defaultConfig() {
        return new OfferConfig(
                new TierSetting(1, 10, 1, 1, 1, 0.5),
                new TierSetting(2, 20, 2, 1, 2, 1.0),
                new TierSetting(3, 30, 3, 2, 3, 1.5)
        );
    }
}
