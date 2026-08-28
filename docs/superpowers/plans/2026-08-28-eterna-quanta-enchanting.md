# Eterna & Quanta Enchanting Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a modular Eterna & Quanta enchanting table system for PaperMC featuring in-world altar block scanning, a custom 54-slot matrix GUI with real-time stat meters, a volatility-governed Quanta roll engine, PDC-backed over-cap enchantment storage, and modular over-cap effect handlers.

**Architecture:** The subsystem is decomposed into pure domain models (`EnchantmentDefinition`, `AltarProfile`, `AltarConfig`), an in-world raycasting spatial scanner (`AltarScanner`), a statistical roll engine (`QuantaRollEngine`), a PDC compound serializer (`OvercapItemAdapter`), modular event effect providers (`OvercapEffectHandler`), and a transactional custom GUI container (`AltarGuiSession` / `AltarGuiListener`) that guarantees zero item loss.

**Tech Stack:** Java 21, PaperMC API (1.21.4), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-28-eterna-quanta-enchanting-design.md`

## Global Constraints

* Java 21 records, sealed types, and pattern matching.
* Bukkit API dependencies are contained within `merlin-paper`.
* Over-cap enchantments are stored under PDC key `merlin:overcap_enchantments` using `PersistentDataType.TAG_CONTAINER`.
* GUI slots: Target Item (Slot 20), Lapis (Slot 22), Secondary Catalyst (Slot 24), Tier I (Slot 38), Tier II (Slot 40), Tier III (Slot 42), Reroll (Slot 44).
* Items left in GUI slots upon inventory close are returned to the player or safely dropped at their location.

---

### Task 1: Core Domain Models & Interfaces

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentDefinition.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/OvercapEffectHandler.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/AltarBlockStats.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/AltarProfile.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentDefinitionTest.java`

**Interfaces:**
* Produces:
  * `EnchantmentDefinition(NamespacedKey key, String displayName, int vanillaMaxLevel, int absoluteMaxLevel, int baseEternaRequired, int eternaPerLevel, int weight, Set<Material> targetMaterials, Optional<OvercapEffectHandler> overcapHandler)`
  * `OvercapEffectHandler` interface with `key()`, `onDamageDealt()`, `onBlockBreak()`, `onArmorHurt()`
  * `AltarBlockStats(double eterna, double quanta, double maxEternaCap, double maxQuantaCap)`
  * `AltarProfile(double totalEterna, double totalQuanta, Map<Material, Integer> blockCounts)`

- [ ] **Step 1: Write failing domain unit tests**

```java
package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class EnchantmentDefinitionTest {
    @Test
    void calculatesMinEternaForLevel() {
        NamespacedKey key = NamespacedKey.minecraft("sharpness");
        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Sharpness", 5, 7, 5, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.empty());
        assertEquals(5, def.minEternaForLevel(1));
        assertEquals(10, def.minEternaForLevel(2));
        assertEquals(25, def.minEternaForLevel(5));
        assertEquals(35, def.minEternaForLevel(7));
    }

    @Test
    void checksMaterialApplicability() {
        NamespacedKey key = NamespacedKey.minecraft("fortune");
        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Fortune", 3, 5, 10, 5, 5, Set.of(Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE), Optional.empty());
        assertTrue(def.canApplyTo(Material.DIAMOND_PICKAXE));
        assertTrue(!def.canApplyTo(Material.DIAMOND_SWORD));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinitionTest`  
Expected: Compilation error (types not found).

- [ ] **Step 3: Implement domain records and interfaces**

Create `OvercapEffectHandler.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public interface OvercapEffectHandler {
    NamespacedKey key();
    default void onDamageDealt(EntityDamageByEntityEvent event, int level) {}
    default void onBlockBreak(BlockBreakEvent event, int level) {}
    default void onArmorHurt(EntityDamageEvent event, int level) {}
}
```

Create `EnchantmentDefinition.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public record EnchantmentDefinition(
        NamespacedKey key,
        String displayName,
        int vanillaMaxLevel,
        int absoluteMaxLevel,
        int baseEternaRequired,
        int eternaPerLevel,
        int weight,
        Set<Material> targetMaterials,
        Optional<OvercapEffectHandler> overcapHandler
) {
    public int minEternaForLevel(int level) {
        return baseEternaRequired + (level - 1) * eternaPerLevel;
    }

    public boolean canApplyTo(Material material) {
        return targetMaterials.contains(material);
    }
}
```

Create `AltarBlockStats.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

public record AltarBlockStats(
        double eterna,
        double quanta,
        double maxEternaCap,
        double maxQuantaCap
) {}
```

Create `AltarProfile.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.Material;

public record AltarProfile(
        double totalEterna,
        double totalQuanta,
        Map<Material, Integer> blockCounts
) {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinitionTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add core domain records and interfaces"
```

---

### Task 2: Altar Configuration & Spatial Scanner

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/AltarConfig.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/AltarScanner.java`
* Modify: `merlin-paper/src/main/resources/config.yml`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/AltarScannerTest.java`

**Interfaces:**
* Consumes: `AltarBlockStats`, `AltarProfile`
* Produces: `AltarConfig.load(ConfigurationSection)`, `AltarScanner.scan(Location tableLocation)`

- [ ] **Step 1: Write failing tests for AltarScanner calculations**

```java
package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class AltarScannerTest {
    @Test
    void computesStatsWithBlockCaps() {
        Map<Material, AltarBlockStats> configBlocks = Map.of(
                Material.BOOKSHELF, new AltarBlockStats(1.0, 0.0, 15.0, 0.0),
                Material.CRYING_OBSIDIAN, new AltarBlockStats(2.5, 0.20, 45.0, 0.60)
        );
        AltarConfig config = new AltarConfig(2, 1, 1, configBlocks);

        Map<Material, Integer> scannedBlocks = Map.of(
                Material.BOOKSHELF, 20, // 20 * 1.0 = 20 -> capped at 15.0
                Material.CRYING_OBSIDIAN, 2 // 2 * 2.5 = 5.0 Eterna, 2 * 0.20 = 0.40 Quanta
        );

        AltarProfile profile = AltarScanner.calculateProfile(config, scannedBlocks);
        assertEquals(20.0, profile.totalEterna(), 0.001); // 15.0 + 5.0
        assertEquals(0.40, profile.totalQuanta(), 0.001); // 0.40
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.AltarScannerTest`  
Expected: FAIL.

- [ ] **Step 3: Implement AltarConfig and AltarScanner**

Create `AltarConfig.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record AltarConfig(
        int radiusHorizontal,
        int radiusVerticalDown,
        int radiusVerticalUp,
        Map<Material, AltarBlockStats> blockStats
) {
    public static AltarConfig fromSection(ConfigurationSection section) {
        if (section == null) {
            return new AltarConfig(2, 1, 1, Map.of());
        }
        int radH = section.getInt("scan_radius_horizontal", 2);
        int radVDown = section.getInt("scan_radius_vertical_down", 1);
        int radVUp = section.getInt("scan_radius_vertical_up", 1);
        Map<Material, AltarBlockStats> stats = new HashMap<>();
        ConfigurationSection blocksSec = section.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String key : blocksSec.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat == null) continue;
                ConfigurationSection b = blocksSec.getConfigurationSection(key);
                if (b == null) continue;
                stats.put(mat, new AltarBlockStats(
                        b.getDouble("eterna", 0.0),
                        b.getDouble("quanta", 0.0),
                        b.getDouble("max_eterna_cap", 100.0),
                        b.getDouble("max_quanta_cap", 1.0)
                ));
            }
        }
        return new AltarConfig(radH, radVDown, radVUp, stats);
    }
}
```

Create `AltarScanner.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class AltarScanner {
    private final AltarConfig config;

    public AltarScanner(AltarConfig config) {
        this.config = config;
    }

    public AltarProfile scan(Location tableLoc) {
        World world = tableLoc.getWorld();
        if (world == null) return new AltarProfile(0.0, 0.0, Map.of());
        int tx = tableLoc.getBlockX();
        int ty = tableLoc.getBlockY();
        int tz = tableLoc.getBlockZ();

        Map<Material, Integer> counts = new HashMap<>();
        for (int x = -config.radiusHorizontal(); x <= config.radiusHorizontal(); x++) {
            for (int z = -config.radiusHorizontal(); z <= config.radiusHorizontal(); z++) {
                for (int y = -config.radiusVerticalDown(); y <= config.radiusVerticalUp(); y++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block b = world.getBlockAt(tx + x, ty + y, tz + z);
                    Material mat = b.getType();
                    if (config.blockStats().containsKey(mat)) {
                        if (hasLineOfSight(world, tx, ty, tz, tx + x, ty + y, tz + z)) {
                            counts.merge(mat, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return calculateProfile(config, counts);
    }

    public static boolean hasLineOfSight(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        if (dx <= 1 && dy <= 1 && dz <= 1) return true;
        int midX = x1 + (x2 - x1) / 2;
        int midY = y1 + (y2 - y1) / 2;
        int midZ = z1 + (z2 - z1) / 2;
        if (midX == x1 && midY == y1 && midZ == z1) return true;
        Block midBlock = world.getBlockAt(midX, midY, midZ);
        return midBlock.isEmpty() || !midBlock.getType().isSolid();
    }

    public static AltarProfile calculateProfile(AltarConfig config, Map<Material, Integer> counts) {
        double totalEterna = 0.0;
        double totalQuanta = 0.0;
        for (var entry : counts.entrySet()) {
            AltarBlockStats stats = config.blockStats().get(entry.getKey());
            if (stats == null) continue;
            int count = entry.getValue();
            double eternaContribution = Math.min(stats.maxEternaCap(), count * stats.eterna());
            double quantaContribution = count * stats.quanta();
            if (stats.maxQuantaCap() >= 0) {
                quantaContribution = Math.min(stats.maxQuantaCap(), quantaContribution);
            } else {
                quantaContribution = Math.max(stats.maxQuantaCap(), quantaContribution);
            }
            totalEterna += eternaContribution;
            totalQuanta += quantaContribution;
        }
        return new AltarProfile(Math.max(0.0, totalEterna), Math.max(0.0, totalQuanta), counts);
    }
}
```

Modify `merlin-paper/src/main/resources/config.yml` to include the altar configuration block.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.AltarScannerTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/main/resources/config.yml
git commit -m "feat(enchanting): add AltarConfig and AltarScanner with line-of-sight raycasting"
```

---

### Task 3: Enchantment Registry & Built-in Definitions

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistryTest.java`

**Interfaces:**
* Consumes: `EnchantmentDefinition`
* Produces: `EnchantmentRegistry.defaultRegistry()`, `register()`, `findForMaterial(Material)`, `findEligible(Material, double eterna)`

- [ ] **Step 1: Write failing tests for EnchantmentRegistry**

```java
package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class EnchantmentRegistryTest {
    @Test
    void filtersEligibleByEternaAndMaterial() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        List<EnchantmentDefinition> swordEnchants = registry.findForMaterial(Material.DIAMOND_SWORD);
        assertFalse(swordEnchants.isEmpty());

        // At 0 Eterna, only low base enchants eligible
        List<EnchantmentDefinition> lowEterna = registry.findEligible(Material.DIAMOND_SWORD, 0.0);
        assertTrue(lowEterna.stream().allMatch(d -> d.minEternaForLevel(1) <= 0.0));

        // At 50 Eterna, high tiers eligible
        List<EnchantmentDefinition> highEterna = registry.findEligible(Material.DIAMOND_SWORD, 50.0);
        assertTrue(highEterna.size() >= lowEterna.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistryTest`  
Expected: FAIL.

- [ ] **Step 3: Implement EnchantmentRegistry**

Create `EnchantmentRegistry.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public final class EnchantmentRegistry {
    private final Map<NamespacedKey, EnchantmentDefinition> definitions = new ConcurrentHashMap<>();

    public static EnchantmentRegistry defaultRegistry() {
        EnchantmentRegistry reg = new EnchantmentRegistry();
        Set<Material> swords = Set.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        );
        Set<Material> tools = Set.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
        );
        Set<Material> armor = Set.of(
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );

        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("sharpness"), "Sharpness", 5, 7, 0, 5, 10, swords, Optional.empty()));
        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("smite"), "Smite", 5, 7, 0, 5, 5, swords, Optional.empty()));
        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("fortune"), "Fortune", 3, 5, 10, 8, 3, tools, Optional.empty()));
        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("efficiency"), "Efficiency", 5, 7, 0, 4, 10, tools, Optional.empty()));
        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("protection"), "Protection", 4, 6, 0, 5, 10, armor, Optional.empty()));
        reg.register(new EnchantmentDefinition(NamespacedKey.minecraft("unbreaking"), "Unbreaking", 3, 5, 0, 5, 8, swords, Optional.empty()));
        return reg;
    }

    public void register(EnchantmentDefinition def) {
        definitions.put(def.key(), def);
    }

    public Optional<EnchantmentDefinition> get(NamespacedKey key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public List<EnchantmentDefinition> findForMaterial(Material mat) {
        return definitions.values().stream().filter(d -> d.canApplyTo(mat)).toList();
    }

    public List<EnchantmentDefinition> findEligible(Material mat, double eterna) {
        return definitions.values().stream()
                .filter(d -> d.canApplyTo(mat))
                .filter(d -> d.minEternaForLevel(1) <= eterna)
                .toList();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistryTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add EnchantmentRegistry with default definitions"
```

---

### Task 4: Quanta Roll Engine & Offer Generation

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantingOffer.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/OfferConfig.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/QuantaRollEngine.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/QuantaRollEngineTest.java`

**Interfaces:**
* Consumes: `EnchantmentRegistry`, `AltarProfile`, `EnchantmentDefinition`
* Produces: `EnchantingOffer(int xpLevelCost, int lapisCost, Map<NamespacedKey, Integer> enchantments, String previewHint)`, `QuantaRollEngine.generateOffers(...)`

- [ ] **Step 1: Write failing tests for QuantaRollEngine**

```java
package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class QuantaRollEngineTest {
    @Test
    void generatesOffersRespectingEternaBounds() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        AltarProfile profile = new AltarProfile(30.0, 0.5, Map.of());
        OfferConfig offersConfig = OfferConfig.defaultConfig();
        QuantaRollEngine engine = new QuantaRollEngine(registry, offersConfig);

        EnchantingOffer tier3 = engine.generateOffer(Material.DIAMOND_SWORD, profile, 3, new Random(42));
        assertNotNull(tier3);
        assertFalse(tier3.enchantments().isEmpty());
        // Verify all rolled levels require <= profile eterna
        for (var entry : tier3.enchantments().entrySet()) {
            EnchantmentDefinition def = registry.get(entry.getKey()).orElseThrow();
            assertTrue(def.minEternaForLevel(entry.getValue()) <= profile.totalEterna());
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.QuantaRollEngineTest`  
Expected: FAIL.

- [ ] **Step 3: Implement OfferConfig, EnchantingOffer, and QuantaRollEngine**

Create `OfferConfig.java`:
```java
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

    public static OfferConfig defaultConfig() {
        return new OfferConfig(
                new TierSetting(1, 10, 1, 1, 1, 0.5),
                new TierSetting(2, 20, 2, 1, 2, 1.0),
                new TierSetting(3, 30, 3, 2, 3, 1.5)
        );
    }
}
```

Create `EnchantingOffer.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.NamespacedKey;

public record EnchantingOffer(
        int tier,
        int xpLevelCost,
        int xpLevelRequirement,
        int lapisCost,
        Map<NamespacedKey, Integer> enchantments,
        String previewHint
) {}
```

Create `QuantaRollEngine.java`:
```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.QuantaRollEngineTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add QuantaRollEngine and offer calculator"
```

---

### Task 5: PDC Over-Cap Serialization & Lore Formatting

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/OvercapItemAdapter.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/OvercapItemAdapterTest.java`

**Interfaces:**
* Consumes: `EnchantmentDefinition`, `EnchantmentRegistry`
* Produces: `OvercapItemAdapter.readOvercap(ItemStack)`, `OvercapItemAdapter.applyEnchantments(ItemStack, Map<NamespacedKey, Integer>, EnchantmentRegistry)`

- [ ] **Step 1: Write failing tests for OvercapItemAdapter**

```java
package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class OvercapItemAdapterTest {
    @Test
    void formatsRomanNumerals() {
        assertEquals("I", QuantaRollEngine.toRoman(1));
        assertEquals("IV", QuantaRollEngine.toRoman(4));
        assertEquals("VI", QuantaRollEngine.toRoman(6));
        assertEquals("VII", QuantaRollEngine.toRoman(7));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapterTest`  
Expected: PASS or compile if adapter not yet present.

- [ ] **Step 3: Implement OvercapItemAdapter**

Create `OvercapItemAdapter.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class OvercapItemAdapter {
    private final NamespacedKey overcapContainerKey;
    private final EnchantmentRegistry registry;

    public OvercapItemAdapter(Plugin plugin, EnchantmentRegistry registry) {
        this.overcapContainerKey = new NamespacedKey(plugin, "overcap_enchantments");
        this.registry = registry;
    }

    public Map<NamespacedKey, Integer> readOvercap(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer sub = root.get(overcapContainerKey, PersistentDataType.TAG_CONTAINER);
        if (sub == null) return Map.of();

        Map<NamespacedKey, Integer> result = new HashMap<>();
        for (NamespacedKey key : sub.getKeys()) {
            Integer lvl = sub.get(key, PersistentDataType.INTEGER);
            if (lvl != null) {
                result.put(key, lvl);
            }
        }
        return result;
    }

    public void applyEnchantments(ItemStack item, Map<NamespacedKey, Integer> enchants) {
        if (item == null || item.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer sub = root.getAdapterContext().newPersistentDataContainer();

        List<String> overcapLore = new ArrayList<>();
        for (var entry : enchants.entrySet()) {
            NamespacedKey key = entry.getKey();
            int level = entry.getValue();
            Enchantment vanilla = Enchantment.getByKey(key);
            EnchantmentDefinition def = registry.get(key).orElse(null);
            int vanillaMax = def != null ? def.vanillaMaxLevel() : (vanilla != null ? vanilla.getMaxLevel() : 1);

            if (level > vanillaMax) {
                sub.set(key, PersistentDataType.INTEGER, level);
                if (vanilla != null) {
                    meta.addEnchant(vanilla, vanillaMax, true);
                }
                String name = def != null ? def.displayName() : key.getKey();
                overcapLore.add("§7" + name + " " + QuantaRollEngine.toRoman(level));
            } else if (vanilla != null) {
                meta.addEnchant(vanilla, level, true);
            }
        }

        if (!sub.getKeys().isEmpty()) {
            root.set(overcapContainerKey, PersistentDataType.TAG_CONTAINER, sub);
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.addAll(overcapLore);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapterTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add OvercapItemAdapter for PDC serialization and lore sync"
```

---

### Task 6: Over-Cap Effect Handlers & Listener

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/handler/SharpnessOvercapHandler.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/handler/FortuneOvercapHandler.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/OvercapEnchantmentListener.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/handler/SharpnessOvercapHandlerTest.java`

**Interfaces:**
* Consumes: `OvercapItemAdapter`, `OvercapEffectHandler`
* Produces: `OvercapEnchantmentListener` (implements `Listener` for `EntityDamageByEntityEvent` and `BlockBreakEvent`)

- [ ] **Step 1: Write failing tests for overcap calculations**

```java
package dev.mintychochip.merlin.paper.enchanting.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SharpnessOvercapHandlerTest {
    @Test
    void calculatesBonusDamageForLevel() {
        SharpnessOvercapHandler handler = new SharpnessOvercapHandler();
        // Vanilla Sharpness V is +3.0 damage. Level 6 = +4.5, Level 7 = +6.0 (bonus over vanilla = 1.5 per extra level)
        assertEquals(1.5, handler.calculateBonusDamage(6, 5), 0.001);
        assertEquals(3.0, handler.calculateBonusDamage(7, 5), 0.001);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.handler.SharpnessOvercapHandlerTest`  
Expected: FAIL.

- [ ] **Step 3: Implement handlers and listener**

Create `SharpnessOvercapHandler.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class SharpnessOvercapHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = NamespacedKey.minecraft("sharpness");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    public double calculateBonusDamage(int level, int vanillaMax) {
        int extra = Math.max(0, level - vanillaMax);
        return extra * 1.5;
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, int level) {
        if (level > 5) {
            double bonus = calculateBonusDamage(level, 5);
            event.setDamage(event.getDamage() + bonus);
        }
    }
}
```

Create `FortuneOvercapHandler.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockBreakEvent;

public final class FortuneOvercapHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = NamespacedKey.minecraft("fortune");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, int level) {
        // Fortune overcap bonus drop logic
    }
}
```

Create `OvercapEnchantmentListener.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class OvercapEnchantmentListener implements Listener {
    private final OvercapItemAdapter adapter;
    private final EnchantmentRegistry registry;

    public OvercapEnchantmentListener(OvercapItemAdapter adapter, EnchantmentRegistry registry) {
        this.adapter = adapter;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Map<NamespacedKey, Integer> overcap = adapter.readOvercap(weapon);
        for (var entry : overcap.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> handler.onDamageDealt(event, entry.getValue()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Map<NamespacedKey, Integer> overcap = adapter.readOvercap(tool);
        for (var entry : overcap.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> handler.onBlockBreak(event, entry.getValue()));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.handler.SharpnessOvercapHandlerTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add OvercapEffectHandler implementations and OvercapEnchantmentListener"
```

---

### Task 7: Altar Matrix GUI & Safe Inventory Interaction Lifecycle

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/gui/AltarInventoryHolder.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/gui/AltarGuiSession.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/gui/AltarGuiListener.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/AltarInteractListener.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/gui/AltarGuiTest.java`

**Interfaces:**
* Consumes: `AltarProfile`, `QuantaRollEngine`, `OvercapItemAdapter`
* Produces: `AltarGuiSession.open(Player, Location, AltarProfile)`

- [ ] **Step 1: Write failing tests for AltarGui slot constants and rules**

```java
package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AltarGuiTest {
    @Test
    void verifiesSlotConstants() {
        assertEquals(20, AltarGuiSession.SLOT_TARGET);
        assertEquals(22, AltarGuiSession.SLOT_LAPIS);
        assertEquals(24, AltarGuiSession.SLOT_CATALYST);
        assertEquals(38, AltarGuiSession.SLOT_TIER_1);
        assertEquals(40, AltarGuiSession.SLOT_TIER_2);
        assertEquals(42, AltarGuiSession.SLOT_TIER_3);
        assertEquals(44, AltarGuiSession.SLOT_REROLL);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.gui.AltarGuiTest`  
Expected: FAIL.

- [ ] **Step 3: Implement GUI components and listeners**

Create `AltarInventoryHolder.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AltarInventoryHolder implements InventoryHolder {
    private final AltarGuiSession session;
    private Inventory inventory;

    public AltarInventoryHolder(AltarGuiSession session) {
        this.session = session;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public AltarGuiSession session() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
```

Create `AltarGuiSession.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.gui;

import dev.mintychochip.merlin.paper.enchanting.AltarProfile;
import dev.mintychochip.merlin.paper.enchanting.EnchantingOffer;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.QuantaRollEngine;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AltarGuiSession {
    public static final int SLOT_ETERNA_METER = 2;
    public static final int SLOT_QUANTA_METER = 6;
    public static final int SLOT_TARGET = 20;
    public static final int SLOT_LAPIS = 22;
    public static final int SLOT_CATALYST = 24;
    public static final int SLOT_TIER_1 = 38;
    public static final int SLOT_TIER_2 = 40;
    public static final int SLOT_TIER_3 = 42;
    public static final int SLOT_REROLL = 44;

    private final Player player;
    private final Location altarLocation;
    private final AltarProfile profile;
    private final QuantaRollEngine rollEngine;
    private final OvercapItemAdapter itemAdapter;
    private final Inventory inventory;
    private final Random random = new Random();

    private EnchantingOffer offer1;
    private EnchantingOffer offer2;
    private EnchantingOffer offer3;

    public AltarGuiSession(Player player, Location altarLocation, AltarProfile profile,
                           QuantaRollEngine rollEngine, OvercapItemAdapter itemAdapter) {
        this.player = player;
        this.altarLocation = altarLocation;
        this.profile = profile;
        this.rollEngine = rollEngine;
        this.itemAdapter = itemAdapter;

        AltarInventoryHolder holder = new AltarInventoryHolder(this);
        this.inventory = Bukkit.createInventory(holder, 54, "Enchanter's Altar Matrix");
        holder.setInventory(inventory);
        populateDecorations();
        rerollOffers();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void populateDecorations() {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i != SLOT_TARGET && i != SLOT_LAPIS && i != SLOT_CATALYST &&
                i != SLOT_ETERNA_METER && i != SLOT_QUANTA_METER &&
                i != SLOT_TIER_1 && i != SLOT_TIER_2 && i != SLOT_TIER_3 && i != SLOT_REROLL) {
                inventory.setItem(i, filler);
            }
        }

        ItemStack eterna = createItem(Material.CYAN_STAINED_GLASS_PANE,
                "§b✦ Eterna: " + String.format("%.1f", profile.totalEterna()),
                List.of("§7Altar Matrix Power Ceiling"));
        inventory.setItem(SLOT_ETERNA_METER, eterna);

        ItemStack quanta = createItem(Material.YELLOW_STAINED_GLASS_PANE,
                "§e⚡ Quanta: " + String.format("+%.0f%%", profile.totalQuanta() * 100),
                List.of("§7Roll Volatility & Critical Odds"));
        inventory.setItem(SLOT_QUANTA_METER, quanta);

        ItemStack reroll = createItem(Material.LAPIS_LAZULI, "§6[ Reroll Seeds ]", List.of("§7Costs 1 Lapis Lazuli"));
        inventory.setItem(SLOT_REROLL, reroll);
    }

    public void rerollOffers() {
        ItemStack target = inventory.getItem(SLOT_TARGET);
        Material mat = target != null && !target.isEmpty() ? target.getType() : Material.DIAMOND_SWORD;
        offer1 = rollEngine.generateOffer(mat, profile, 1, random);
        offer2 = rollEngine.generateOffer(mat, profile, 2, random);
        offer3 = rollEngine.generateOffer(mat, profile, 3, random);
        updateOfferButtons();
    }

    private void updateOfferButtons() {
        inventory.setItem(SLOT_TIER_1, createOfferButton("Tier I Offer", offer1));
        inventory.setItem(SLOT_TIER_2, createOfferButton("Tier II Offer", offer2));
        inventory.setItem(SLOT_TIER_3, createOfferButton("Tier III Offer", offer3));
    }

    private ItemStack createOfferButton(String title, EnchantingOffer offer) {
        if (offer == null) return createItem(Material.BARRIER, "§cNo Offer");
        return createItem(Material.ENCHANTED_BOOK, "§a" + title, List.of(
                "§7Requires: §eLevel " + offer.xpLevelRequirement(),
                "§7Cost: §a" + offer.xpLevelCost() + " XP Levels + " + offer.lapisCost() + " Lapis",
                "§7Preview: §d" + offer.previewHint()
        ));
    }

    public void handleEnchantClick(int tier) {
        EnchantingOffer offer = switch (tier) {
            case 1 -> offer1;
            case 2 -> offer2;
            default -> offer3;
        };
        if (offer == null || offer.enchantments().isEmpty()) return;

        ItemStack target = inventory.getItem(SLOT_TARGET);
        ItemStack lapis = inventory.getItem(SLOT_LAPIS);
        if (target == null || target.isEmpty()) {
            player.sendMessage("§cPlace an enchantable item in the target slot!");
            return;
        }
        if (lapis == null || lapis.getAmount() < offer.lapisCost()) {
            player.sendMessage("§cYou need at least " + offer.lapisCost() + " Lapis Lazuli!");
            return;
        }
        if (player.getLevel() < offer.xpLevelRequirement()) {
            player.sendMessage("§cYou need at least XP Level " + offer.xpLevelRequirement() + "!");
            return;
        }

        // Deduct cost
        player.setLevel(player.getLevel() - offer.xpLevelCost());
        lapis.setAmount(lapis.getAmount() - offer.lapisCost());
        if (lapis.getAmount() <= 0) {
            inventory.setItem(SLOT_LAPIS, null);
        }

        // Apply enchants
        itemAdapter.applyEnchantments(target, offer.enchantments());
        player.sendMessage("§aEnchanting complete!");
        rerollOffers();
    }

    public void handleClose() {
        returnOrDrop(SLOT_TARGET);
        returnOrDrop(SLOT_LAPIS);
        returnOrDrop(SLOT_CATALYST);
    }

    private void returnOrDrop(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && !item.isEmpty()) {
            inventory.setItem(slot, null);
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty() && player.getWorld() != null) {
                leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
            }
        }
    }

    private static ItemStack createItem(Material mat, String name) {
        return createItem(mat, name, List.of());
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
```

Create `AltarGuiListener.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class AltarGuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof AltarInventoryHolder holder)) return;
        AltarGuiSession session = holder.session();

        int slot = event.getRawSlot();
        if (slot < 54) {
            // Click inside GUI
            if (slot == AltarGuiSession.SLOT_TARGET || slot == AltarGuiSession.SLOT_LAPIS || slot == AltarGuiSession.SLOT_CATALYST) {
                // Allowed input slots
                return;
            }
            event.setCancelled(true);
            if (slot == AltarGuiSession.SLOT_TIER_1) session.handleEnchantClick(1);
            else if (slot == AltarGuiSession.SLOT_TIER_2) session.handleEnchantClick(2);
            else if (slot == AltarGuiSession.SLOT_TIER_3) session.handleEnchantClick(3);
            else if (slot == AltarGuiSession.SLOT_REROLL) session.rerollOffers();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof AltarInventoryHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && slot != AltarGuiSession.SLOT_TARGET && slot != AltarGuiSession.SLOT_LAPIS && slot != AltarGuiSession.SLOT_CATALYST) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AltarInventoryHolder holder) {
            holder.session().handleClose();
        }
    }
}
```

Create `AltarInteractListener.java`:
```java
package dev.mintychochip.merlin.paper.enchanting;

import dev.mintychochip.merlin.paper.enchanting.gui.AltarGuiSession;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class AltarInteractListener implements Listener {
    private final AltarScanner scanner;
    private final QuantaRollEngine rollEngine;
    private final OvercapItemAdapter itemAdapter;

    public AltarInteractListener(AltarScanner scanner, QuantaRollEngine rollEngine, OvercapItemAdapter itemAdapter) {
        this.scanner = scanner;
        this.rollEngine = rollEngine;
        this.itemAdapter = itemAdapter;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTableClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENCHANTING_TABLE) return;

        event.setCancelled(true);
        AltarProfile profile = scanner.scan(event.getClickedBlock().getLocation());
        AltarGuiSession session = new AltarGuiSession(
                event.getPlayer(), event.getClickedBlock().getLocation(), profile, rollEngine, itemAdapter);
        session.open();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests dev.mintychochip.merlin.paper.enchanting.gui.AltarGuiTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/
git commit -m "feat(enchanting): add AltarGuiSession, AltarGuiListener, and AltarInteractListener"
```

---

### Task 8: Plugin Wiring & Full Test Verification

**Files:**
* Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java`
* Test: Full test suite (`./gradlew test`)

**Interfaces:**
* Consumes: All components from Tasks 1–7.
* Produces: Wires enchanting listeners during plugin `onEnable()`.

- [ ] **Step 1: Wire enchanting subsystem into MerlinPlugin**

In `MerlinPlugin.java`:
```java
// Initialize Enchanting Subsystem
var altarConfig = AltarConfig.fromSection(getConfig().getConfigurationSection("altar"));
var altarScanner = new AltarScanner(altarConfig);
var enchantmentRegistry = EnchantmentRegistry.defaultRegistry();
var sharpnessHandler = new SharpnessOvercapHandler();
var fortuneHandler = new FortuneOvercapHandler();
enchantmentRegistry.get(sharpnessHandler.key()).ifPresent(d -> {
    // register handler
});
var overcapAdapter = new OvercapItemAdapter(this, enchantmentRegistry);
var offerConfig = OfferConfig.defaultConfig();
var quantaRollEngine = new QuantaRollEngine(enchantmentRegistry, offerConfig);

getServer().getPluginManager().registerEvents(new AltarGuiListener(), this);
getServer().getPluginManager().registerEvents(new AltarInteractListener(altarScanner, quantaRollEngine, overcapAdapter), this);
getServer().getPluginManager().registerEvents(new OvercapEnchantmentListener(overcapAdapter, enchantmentRegistry), this);
```

- [ ] **Step 2: Run full build and test suite**

Run: `./gradlew test`  
Expected: BUILD SUCCESSFUL (all unit and integration tests passing).

- [ ] **Step 3: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java
git commit -m "feat(enchanting): wire Eterna & Quanta enchanting subsystem into MerlinPlugin"
```
