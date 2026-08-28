# Eterna & Quanta Enchanting Table System — Design Specification

**Date:** 2026-08-28  
**Status:** Approved Design Draft  
**Target Subsystem:** `merlin-paper` (Enchanting Module)  
**Spec Location:** `docs/superpowers/specs/2026-08-28-eterna-quanta-enchanting-design.md`

---

## 1. Executive Summary & Core Philosophy

The **Eterna & Quanta Enchanting System** replaces the static vanilla enchanting table with a dynamic, in-world altar system inspired by classic modded mechanics (Apotheosis). Players build physical altar structures using various bookshelves, crystals, and esoteric blocks to tune two fundamental magical properties:

* **✦ Eterna (Power & Level Cap):** Dictates the maximum eligible enchantment tiers and unlocks over-cap levels (e.g., Sharpness VI–VII, Fortune IV).
* **⚡ Quanta (Roll Volatility & Variance):** Dictates the spread of roll power and candidate selection variance, offering high-risk, high-reward upside with tier-floor guarantees.

Enchanting takes place inside a custom 54-slot **Enchanter's Altar Matrix GUI** that provides real-time stat meters, catalyst slots, safe transaction semantics, and configurable roll offers.

```
                  ┌──────────────────────────────────────────────┐
                  │            THE ENCHANTING ALTAR              │
                  │   (Surrounding Bookshelves & Catalyst Blocks)│
                  └──────────────────────┬───────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 ▼                                               ▼
     [ ✦ ETERNA (Power Ceiling) ]                   [ ⚡ QUANTA (Variance / Risk) ]
     • Sets max enchantment tier cap                • Expands roll volatility & secondary odds
     • Unlocks over-cap tiers (e.g. Sharpness VI)   • Guaranteed non-degrading tier floor
```

---

## 2. In-World Altar Scanning

### 2.1 Bounding Box & Detection Rules
When a player interacts with an Enchanting Table:
* The plugin scans a $5 \times 3 \times 5$ cuboid centered on the table (horizontal radius: $\pm 2$ blocks; vertical offset: $-1$ to $+1$ blocks).
* **Line of Sight Requirement:** To prevent trivial dense packing, a block only contributes stats if the direct 3D line-of-sight path between the block and the enchanting table is unobstructed (only `AIR`, `CAVE_AIR`, `VOID_AIR`, carpet, or small non-solid blocks are permitted in between).

### 2.2 Configurable Block Stat Table (`config.yml`)
All block contributions and individual caps are fully externalized in `config.yml`:

```yaml
altar:
  scan_radius_horizontal: 2
  scan_radius_vertical_down: 1
  scan_radius_vertical_up: 1
  blocks:
    BOOKSHELF:
      eterna: 1.0
      quanta: 0.0
      max_eterna_cap: 15.0
    CHISELED_BOOKSHELF:
      eterna: 1.2
      quanta: 0.0
      max_eterna_cap: 18.0
    AMETHYST_BLOCK:
      eterna: 1.5
      quanta: 0.05
      max_eterna_cap: 30.0
      max_quanta_cap: 0.25
    BUDDING_AMETHYST:
      eterna: 2.0
      quanta: 0.08
      max_eterna_cap: 35.0
      max_quanta_cap: 0.35
    CRYING_OBSIDIAN:
      eterna: 2.5
      quanta: 0.15
      max_eterna_cap: 45.0
      max_quanta_cap: 0.60
    SCULK_CATALYST:
      eterna: 3.0
      quanta: 0.25
      max_eterna_cap: 50.0
      max_quanta_cap: 0.80
    CANDLE:
      eterna: 0.25
      quanta: -0.05
      max_eterna_cap: 10.0
      max_quanta_cap: -0.30
```

---

## 3. Mathematical Roll Model & Stat Separation

### 3.1 Stat Orthogonality
1. **Eterna (Eligibility Gate):**
   * An enchantment $E$ at rank $L$ requires $\text{MinEterna}(E, L)$.
   * An altar with total Eterna $T_E$ can only roll levels $L$ where $\text{MinEterna}(E, L) \le T_E$.
2. **Quanta (Candidate Selection & Secondary Enchantments):**
   * Quanta does *not* multiply an arbitrary base level down below the selected slot's guaranteed floor.
   * Instead, for a chosen Offer Tier (Tier I, II, or III), the roll selects a base candidate from the Eterna-eligible pool, then rolls a Quanta check:
     $$\text{High-Roll Chance} = \text{clamp}(Q \times \text{OfferMultiplier}, 0.0, 0.95)$$
   * If the check succeeds:
     * The candidate rank is boosted $+1$ (up to the Eterna cap).
     * An additional secondary enchantment from the eligible pool is rolled with probability proportional to Quanta.

### 3.2 Offer Tiers Configuration (`config.yml`)
Offer costs and baseline guarantees are loaded from configuration:

```yaml
enchanting_offers:
  tier_1:
    xp_level_cost: 1
    xp_level_requirement: 10
    lapis_cost: 1
    min_enchantments: 1
    max_enchantments: 1
    quanta_bonus_multiplier: 0.5
  tier_2:
    xp_level_cost: 2
    xp_level_requirement: 20
    lapis_cost: 2
    min_enchantments: 1
    max_enchantments: 2
    quanta_bonus_multiplier: 1.0
  tier_3:
    xp_level_cost: 3
    xp_level_requirement: 30
    lapis_cost: 3
    min_enchantments: 2
    max_enchantments: 3
    quanta_bonus_multiplier: 1.5
```

---

## 4. Enchantment Registry & Over-Cap Provider Architecture

### 4.1 Enchantment Definition Contract
Each registered enchantment is defined via `EnchantmentDefinition`:

```java
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
}
```

### 4.2 Over-Cap Effect Provider Interface
Rather than a single catch-all listener, over-cap mechanics are handled via modular effect providers:

```java
public interface OvercapEffectHandler {
    NamespacedKey key();
    
    // Lifecycle hooks for specific event domains
    default void onDamageDealt(EntityDamageByEntityEvent event, int level) {}
    default void onBlockBreak(BlockBreakEvent event, int level) {}
    default void onArmorHurt(EntityDamageEvent event, int level) {}
}
```

* **Standard Vanilla Levels ($\le \text{vanillaMaxLevel}$):** Handled directly by Bukkit/Minecraft vanilla enchantment logic.
* **Over-Cap Levels ($> \text{vanillaMaxLevel}$):** The handler calculates the delta bonus (e.g., Sharpness VI grants $+1.5$ damage over vanilla Sharpness V; Fortune IV adds $+1$ max drop multiplier).

### 4.3 Persistent Data Container (PDC) Storage Schema
Over-cap enchantments are serialized onto the item's `ItemStack` using a structured PDC compound tag:

* **PDC Key:** `merlin:overcap_enchantments`
* **Data Type:** `PersistentDataType.TAG_CONTAINER` (Compound sub-container)
  * Each entry key is a stringified `NamespacedKey` (e.g., `"minecraft:sharpness"`, `"minecraft:fortune"`).
  * Each entry value is an `INTEGER` (e.g., `6`, `4`).
* **Lore Synchronization:** `LoreFormatter` updates the item tooltip cleanly:
  * Vanilla enchantments rendered standard.
  * Over-cap lines rendered with custom styling (e.g. `§7Sharpness VI`, `§7Fortune IV`).

---

## 5. Altar Matrix GUI & Inventory Safety Rules

```
 0  1  2  3  4  5  6  7  8  -> Row 1: [ ] [✦ Eterna Meter] [ ] [ ] [ ] [⚡ Quanta Meter] [ ] [ ]
 9 10 11 12 13 14 15 16 17  -> Row 2: Filler Panes
18 19 20 21 22 23 24 25 26  -> Row 3: [ ] [Target Item: 20] [ ] [Lapis: 22] [ ] [Catalyst: 24] [ ]
27 28 29 30 31 32 33 34 35  -> Row 4: Filler Panes
36 37 38 39 40 41 42 43 44  -> Row 5: [ ] [Tier I: 38] [ ] [Tier II: 40] [ ] [Tier III: 42] [ ] [Reroll: 44]
45 46 47 48 49 50 51 52 53  -> Row 6: Filler Panes
```

### 5.1 Slot Ownership & Interaction Model
1. **Interactive Input Slots (Slots 20, 22, 24):**
   * **Slot 20 (Target Item):** Only accepts enchantable items (Swords, Tools, Armor, Bows, Crossbows, Tridents, Books).
   * **Slot 22 (Lapis Lazuli):** Only accepts `Material.LAPIS_LAZULI`.
   * **Slot 24 (Secondary Catalyst):** Accepts configured catalyst items (`AMETHYST_SHARD`, `ECHO_SHARD`).
2. **Action & Meter Slots (Slots 2, 6, 38, 40, 42, 44, and all filler panes):**
   * All player clicks (`PICKUP`, `PLACE`, `SWAP_WITH_CURSOR`, `HOTBAR_SWAP`, `CLONE`) are strictly cancelled.
3. **Shift-Click Routing (`InventoryClickEvent` with `isShiftClick()`):**
   * Shift-clicking from the player inventory intelligently routes:
     * Enchantable gear $\to$ Slot 20.
     * Lapis Lazuli $\to$ Slot 22.
     * Secondary Catalyst $\to$ Slot 24.
     * All other items $\to$ cancelled / no-op.
4. **Drag Protection (`InventoryDragEvent`):**
   * Any drag action touching non-input slots is completely cancelled.
5. **Safe Item Reclamation (`InventoryCloseEvent`):**
   * When the GUI closes (player presses ESC, disconnects, dies, or server stops), any items in Slots 20, 22, and 24 are immediately refunded to the player's inventory.
   * If inventory is full, items are safely dropped at the player's location (`world.dropItemNaturally`).

---

## 6. Implementation Milestones

1. **Phase 1: Core Domain & Data Structures (`merlin-paper`)**
   * `EnchantmentDefinition`, `EnchantmentRegistry`, `OvercapEffectHandler`.
   * `AltarBlockProfile` & `AltarScanner` with line-of-sight raycasts.
2. **Phase 2: Mathematical Roll Engine & Configuration**
   * `QuantaRollEngine`, `OfferCalculator`, `config.yml` loading.
   * Unit tests for roll bounds, Eterna eligibility, and non-degrading tier floors.
3. **Phase 3: Altar GUI & Safety Listeners**
   * `AltarGuiSession`, `AltarInventoryHolder`.
   * Strict shift-click, drag, and close-reclamation event handling.
4. **Phase 4: Over-Cap Enchantment Listeners & Lore Sync**
   * `PdcEnchantmentAdapter` and `OvercapEffectListener` registrations (Sharpness VI+, Fortune IV+, Protection V+).
5. **Phase 5: Integration & In-Game Verification**
   * Full test suite verification across GUI transactions, altar block detection, and roll resolution.

---

## 7. Spec Self-Review

* **Placeholder Scan:** No "TODO", "TBD", or unaddressed parameters.
* **Internal Consistency:** Roll engine math, configuration keys, GUI slot numbers, and PDC tags align precisely.
* **Scope Check:** Cleanly focused on the Enchanting Table revamp and over-cap handler architecture without conflating unrelated systems.
* **Ambiguity Check:** Explicit slot mappings, exact config format, and deterministic shift-click routing defined.