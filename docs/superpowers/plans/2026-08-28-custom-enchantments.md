# Archived Custom Enchantments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all 24 custom enchantments listed by the archived Mayhem Multiverse Enchanter page in Merlin's Paper enchanting subsystem.

**Architecture:** Extend the existing `EnchantmentRegistry` → `OvercapItemAdapter` → trigger dispatcher pipeline. Each active enchantment is a focused `OvercapEffectHandler`; Paper events are adapted into narrow trigger interfaces and remain bounded by `CascadeGuard`. Custom definitions use explicit non-vanilla persistence and the `merlin` namespace.

**Tech Stack:** Java 25, Paper API `26.2.build.84-stable`, JUnit 5, Mockito, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-28-custom-enchantments-design.md`

## Global Constraints

- Implement the archived **Custom** table only; leave the separate vanilla list and its current handlers intact.
- Register exactly 24 custom definitions; Array's maximum rank is II because the archived table has no Rank III entry.
- Expertise is metadata-only because the archived source gives it no description.
- Custom definitions use `vanillaMaxLevel = 0` and persist positive levels through `merlin:overcap_enchantments`, including Rank I.
- Preserve `CascadeGuard.MAX_CASCADE_DEPTH = 3`, dispatcher priority/key ordering, and cancelled-event behavior.
- Do not add a third-party dependency or a second item metadata format.
- Every implementation task writes a failing behavioral test before production code and skips project-wide validation until the final verification task.

## File map

### Existing files to modify

- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java` — register all custom definitions and target material sets.
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/OvercapItemAdapter.java` — persist registered custom keys at Rank I and above.
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcher.java` — dispatch new event contracts and mutable kill experience.
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentListener.java` — adapt new Paper events and write mutable results back.
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/EntityKillTrigger.java` — accept mutable dropped experience.
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcherTest.java` — update entity-kill contract calls.
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcherCoverageTest.java` — update coverage handler signatures.
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentListenerCoverageTest.java` — cover new event adapters.

### New framework files

- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/MutableExperience.java`
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/PlayerDropItemTrigger.java`
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/FoodLevelChangeTrigger.java`
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/HorseJumpTrigger.java`
- `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/EntityEnvironmentalDamageTrigger.java`

### New handler files

All handlers live in `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/`:

- `CustomEnchantmentSupport.java`
- `StickyGripHandler.java`
- `EquilibriumHandler.java`
- `NetherScourgeHandler.java`
- `ColdAspectHandler.java`
- `ConfusingAspectHandler.java`
- `ToxinAspectHandler.java`
- `KnowledgeHandler.java`
- `VorpalHandler.java`
- `VampirismHandler.java`
- `FlurryHandler.java`
- `ArrayHandler.java`
- `PlunderHandler.java`
- `WisdomHandler.java`
- `MoltenTouchHandler.java`
- `DrillHandler.java`
- `QuenchingHandler.java`
- `ColoramaHandler.java`
- `LeapingHandler.java`
- `FeatherHoovesHandler.java`
- `PrismaticHandler.java`
- `OverflowingHandler.java`
- `VacuumHandler.java`
- `HeatWaveHandler.java`

### New behavioral tests

- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/CustomEnchantmentRegistryTest.java`
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/CustomEnchantmentPersistenceTest.java`
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/CombatCustomEnchantmentTest.java`
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/RangedAndBlockCustomEnchantmentTest.java`
- `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ArmorAndUtilityCustomEnchantmentTest.java`

---

### Task 1: Add mutable kill experience and event contracts

**Files:**
- Create the framework file and four trigger interfaces listed above.
- Modify `EntityKillTrigger.java`, `CustomEnchantmentDispatcher.java`, and `CustomEnchantmentListener.java`.
- Modify the three existing dispatcher/listener coverage tests.
- Test in `CustomEnchantmentListenerCoverageTest.java` and a new `MutableExperienceTest.java`.

**Interfaces:**

```java
public final class MutableExperience {
    public MutableExperience(int amount);
    public int getAmount();
    public void setAmount(int amount);
    public void add(int amount);
}

public interface PlayerDropItemTrigger {
    boolean shouldCancelDrop(Player player, ItemStack item, int level);
}

public interface FoodLevelChangeTrigger {
    int onFoodLevelChange(Player player, int currentFoodLevel, int proposedFoodLevel, int level);
}

public interface HorseJumpTrigger {
    float onHorseJump(AbstractHorse horse, float power, int level);
}

public interface EntityEnvironmentalDamageTrigger {
    void onEnvironmentalDamage(LivingEntity entity, DamageCause cause, MutableDamage damage, int level);
}
```

- Change `EntityKillTrigger.onEntityKill` to receive `MutableExperience experience` instead of an `int droppedExp`; update every existing test implementation and call.
- Add dispatcher methods `dispatchPlayerDrop`, `dispatchFoodLevelChange`, `dispatchHorseJump`, and `dispatchEntityEnvironmentalDamage`. Each resolves the matching interface through the existing PDC path and preserves the current priority/key order.
- In the listener, adapt `PlayerDropItemEvent`, `FoodLevelChangeEvent`, `HorseJumpEvent`, and non-player `EntityDamageEvent`. For entity death, construct `MutableExperience(event.getDroppedExp())`, dispatch it, then call `event.setDroppedExp(experience.getAmount())`.
- Keep the existing player environmental path; additionally dispatch `EntityEnvironmentalDamageTrigger` for every `LivingEntity` with its complete available equipment so saddles can protect horses. Write back cancellation and bounded damage once.
- Clamp food levels to `[0, 20]`, horse power to the event/API range, and experience to `>= 0`.

**Test steps:**

- [ ] Add a `MutableExperience` test proving add/set operations never expose a negative amount.
- [ ] Add listener coverage proving entity death writes modified XP, item drop cancellation is written back, food level is bounded, horse jump power is written back, and a horse environmental event reaches the entity-specific trigger.
- [ ] Run only the affected custom listener tests and confirm they fail before implementation, then pass after implementation.
- [ ] Commit only framework files and their tests with `feat(enchanting): add custom enchantment event contracts`.

---

### Task 2: Register every custom definition and fix custom PDC persistence

**Files:**
- Modify `EnchantmentRegistry.java` and `OvercapItemAdapter.java`.
- Create `CustomEnchantmentRegistryTest.java` and `CustomEnchantmentPersistenceTest.java`.

**Interfaces:**
- `EnchantmentRegistry.defaultRegistry()` remains the public factory.
- Keys are `new NamespacedKey("merlin", "<lower_snake_case>")`.
- Each active definition's `overcapHandler` contains the handler implementing its event contract; Expertise uses `Optional.empty()`.

**Registration data:**

- Weapons/tools: Sticky Grip rank I.
- Weapons: Equilibrium V, Nether's Scourge VI, Cold Aspect III, Confusing Aspect III, Toxin Aspect III, Knowledge III, Vorpal III, Vampirism V.
- Swords: Flurry III.
- Bows: Array II, Plunder III, Wisdom III.
- Tools: Molten Touch I.
- Pickaxes: Drill III, Expertise III.
- Leggings: Quenching IV.
- Leather armor: Colorama I.
- Saddles: Leaping III, Feather Hooves I.
- Shears: Prismatic I.
- Buckets: Overflowing I and Vacuum I.
- Flint and steel: Heat Wave I.

Use complete material sets for the categories: all six sword materials; all six pickaxes; all six axes, shovels, and hoes for weapons/tools; all vanilla leggings for Quenching; all leather armor pieces for Colorama; `SADDLE`, `SHEARS`, `BUCKET`, `WATER_BUCKET`, and `FLINT_AND_STEEL` for the utility entries. Keep target sets explicit and immutable.

- Set `vanillaMaxLevel = 0` on every custom definition. Use the existing linear Eterna fields and the source's minimum unlock tier as the definition's base eligibility without adding a second job-level model.
- In `applyEnchantments`, treat `def != null && vanilla == null` as a registered custom key. Persist any positive level in the PDC and lore, rather than comparing it to the fallback vanilla maximum. Never call `meta.addEnchant` for a custom key.
- Do not alter vanilla entries or the existing over-cap lore behavior.

**Test steps:**

- [ ] Assert all 24 keys exist, exact display names and maximum levels match the table, Expertise has no handler, and Array's maximum is II.
- [ ] Assert every source target category accepts a representative material and rejects a known incompatible material.
- [ ] With a real `ItemStack` and mocked `Plugin`, apply a custom Rank I enchantment, read it back from PDC, and assert its lore contains `Sticky Grip I`.
- [ ] Run only the two new registry/persistence test classes; confirm the rank-I test fails before the adapter branch and passes after it.
- [ ] Commit only registry, adapter, and tests with `feat(enchanting): register archived custom enchantments`.

---

### Task 3: Add shared handler support and combat enchantments

**Files:**
- Create `CustomEnchantmentSupport.java` and the six combat handlers plus four kill handlers: `EquilibriumHandler`, `NetherScourgeHandler`, `ColdAspectHandler`, `ConfusingAspectHandler`, `ToxinAspectHandler`, `VampirismHandler`, `KnowledgeHandler`, `VorpalHandler`, `PlunderHandler`, and `WisdomHandler`.
- Create `CombatCustomEnchantmentTest.java`.

**Interfaces:**
- Every handler implements `OvercapEffectHandler` and its matching trigger interface.
- Constructors accept `Random` where behavior is random and provide a default constructor using a fresh `Random`.
- `CustomEnchantmentSupport` supplies `NamespacedKey customKey(String)`, `int randomPerRank(Random,int,int,int)`, native Nether-mob detection, mob-head material mapping, and safe max-health healing.

**Implementation:**

- Use one bounded random roll for each rank and sum the rolls. Equilibrium adds the sum to `MutableDamage`, then applies half the sum to the attacker through `LivingEntity.damage`; a handler-local recursion guard prevents the self-damage event from applying Equilibrium again. Nether's Scourge adds the same roll only for native Nether hostile entity types.
- Cold Aspect raises freeze ticks to at least `20 * level`; Confusing Aspect applies Nausea for `100 * level` ticks; Toxin Aspect applies Poison for `80 * level` ticks. Preserve stronger existing effects by using the larger duration.
- Vampirism checks `random.nextDouble() < 0.10 * level` and heals exactly one health point, capped by `Attribute.MAX_HEALTH`.
- Knowledge adds `randomPerRank(random, 3, 4, level)` to mutable death experience.
- Vorpal checks `random.nextDouble() < 0.005 * level` and appends the matching `CREEPER_HEAD`, `ZOMBIE_HEAD`, `SKELETON_SKULL`, or `WITHER_SKELETON_SKULL` drop. It does nothing for unsupported entities.
- Plunder appends one cloned copy of each current drop for every rank. Wisdom adds `originalExperience * level` to mutable death experience.
- All handlers no-op for incompatible entities, empty drops, null contexts, or non-positive levels.

**Test steps:**

- [ ] Write tests for each handler's positive effect and incompatible-context no-op. Use a deterministic `Random` test double to force damage, chance, and head branches.
- [ ] Assert damage bonus and self-damage, Nether filtering, durations, freeze ticks, healing cap, XP additions, head mapping, drop copies, and zero-XP behavior.
- [ ] Run only `CombatCustomEnchantmentTest`; confirm failure before handler implementations and pass afterward.
- [ ] Commit handlers, support, and tests with `feat(enchanting): add combat custom enchantments`.

---

### Task 4: Add ranged, block, and interaction enchantments

**Files:**
- Create `FlurryHandler.java`, `ArrayHandler.java`, `MoltenTouchHandler.java`, `DrillHandler.java`, and `HeatWaveHandler.java`.
- Create `RangedAndBlockCustomEnchantmentTest.java`.

**Interfaces:**
- Flurry implements `EntityInteractTrigger`.
- Array implements `BowShootTrigger`.
- Molten Touch implements `BlockDropTrigger`.
- Drill implements `BlockBreakTrigger`.
- Heat Wave implements `ActiveInteractTrigger`.

**Implementation:**

- Flurry ignores non-living right-click targets, selects nearby living mobs excluding the player, and applies outward velocity to at most `3 * level` targets. Normalize zero-length vectors before multiplying knockback.
- Array maps Rank I/II to 3/5 total arrows. Spawn only `total - 1` additional projectiles with the original shooter and velocity plus bounded spread; never mutate inventory or consume additional arrows. Rank III is rejected by registry bounds.
- Molten Touch maps furnace-smeltable ore outputs (iron, gold, copper, ancient debris) and sand/red sand to their smelted item while preserving each item entity's amount. Leave non-smeltable drops unchanged.
- Drill recognizes ore blocks, breadth-first traverses six adjacent faces of the same ore material, and calls `CascadeScope.breakBlockSafely(neighbor, true)` until `4 * level` total blocks including the original are reached. Maintain a per-dispatch visited set and never revisit a block.
- Heat Wave responds only to right-click block actions and sets fire on valid air blocks in a horizontal 3×3 centered on the clicked block, leaving occupied blocks unchanged.

**Test steps:**

- [ ] Test Flurry target cap/direction, Array arrow count and one-shot ammunition invariant, molten output mapping, Drill cap/adjacency, and Heat Wave's 3×3 valid-block filtering.
- [ ] Test wrong action, non-living target, non-ore, non-smeltable drop, and cascade-limit no-op paths.
- [ ] Run only `RangedAndBlockCustomEnchantmentTest`; confirm failure before implementation and pass afterward.
- [ ] Commit these handlers and tests with `feat(enchanting): add ranged and block custom enchantments`.

---

### Task 5: Add armor, mount, and utility enchantments

**Files:**
- Create `StickyGripHandler.java`, `QuenchingHandler.java`, `ColoramaHandler.java`, `LeapingHandler.java`, `FeatherHoovesHandler.java`, `PrismaticHandler.java`, `OverflowingHandler.java`, `VacuumHandler.java`, and `ArmorAndUtilityCustomEnchantmentTest.java`.

**Interfaces:**
- Sticky Grip implements `PlayerDropItemTrigger`.
- Quenching implements `FoodLevelChangeTrigger`.
- Colorama implements `ItemDamageTrigger`.
- Leaping implements `HorseJumpTrigger`.
- Feather Hooves implements `EntityEnvironmentalDamageTrigger`.
- Prismatic implements `ShearEntityTrigger`.
- Overflowing implements `BucketEmptyTrigger`.
- Vacuum implements `BucketFillTrigger`.

**Implementation:**

- Sticky Grip returns true for every drop event carrying the enchantment.
- Quenching restores up to `level` food points only when the proposed food level is lower than the current level, capped at 20; eating food is unchanged.
- Colorama checks `LeatherArmorMeta`, assigns a random RGB `Color`, and writes the updated meta back to the damaged item.
- Leaping returns `min(apiMaximum, originalPower + 0.1f * level)` for `HorseJumpEvent`.
- Feather Hooves marks `MutableDamage` cancelled only for `FALL` damage received by an `AbstractHorse`; other causes and entities are unchanged.
- Prismatic randomizes `Sheep` wool color before shearing and ignores non-sheep entities.
- Overflowing restores `WATER_BUCKET` to the event hand after an empty operation; Vacuum restores `BUCKET` after a fill operation. Use the event's `EquipmentSlot` and update the selected inventory slot without creating a second stack.

**Test steps:**

- [ ] Test drop cancellation, hunger decrease/increase precedence, leather-only color mutation, horse jump cap, fall-only horse cancellation, sheep-only color mutation, and both bucket hand slots.
- [ ] Verify all handlers no-op for incompatible item/entity/material contexts.
- [ ] Run only `ArmorAndUtilityCustomEnchantmentTest`; confirm failure before implementation and pass afterward.
- [ ] Commit these handlers and tests with `feat(enchanting): add armor and utility custom enchantments`.

---

### Task 6: Wire every handler into the registry and dispatcher

**Files:**
- Modify `EnchantmentRegistry.java` to supply each handler in the corresponding definition.
- Modify `CustomEnchantmentDispatcher.java` only where handler-specific dispatch methods need final argument wiring.
- Modify `MerlinPlugin.java` only if the new listener remains unregistered; retain a single `CustomEnchantmentListener` registration.
- Extend `CustomEnchantmentRegistryTest.java` and dispatcher coverage tests.

**Interfaces:**
- `EnchantmentRegistry.defaultRegistry()` returns definitions whose handler key matches the definition key.
- Each definition resolves through `OvercapItemAdapter.readOvercap` and is dispatched by the correct trigger interface.

**Test steps:**

- [ ] Assert every non-Expertise custom definition has a handler whose `key()` equals its registry key.
- [ ] Build representative PDC-enchanted items and assert dispatcher reaches the expected handler for combat, kill, bow, block, food, horse, bucket, shear, and drop paths.
- [ ] Confirm Expertise resolves as metadata-only and never creates an unsupported trigger.
- [ ] Run all custom enchanting tests except the full project suite; confirm all pass.
- [ ] Commit only final wiring and tests with `feat(enchanting): wire custom enchantment handlers`.

---

### Task 7: Final verification and cleanup

**Files:**
- Modify only tests or source defects found by the focused verification. Do not alter the archived spec or plan unless a verified contract changed.

**Steps:**

- [ ] Run `./gradlew :merlin-paper:test --tests 'dev.mintychochip.merlin.paper.enchanting.*'`.
- [ ] Run `./gradlew test`.
- [ ] Run `./gradlew :merlin-paper:jar`.
- [ ] If the development server starts, run `./gradlew :merlin-test:runServer` and exercise one enchanted item through the actual Paper event path; otherwise record the startup blocker and retain unit/integration evidence.
- [ ] Inspect the final registry against all 24 archived names and confirm Array II and metadata-only Expertise.
- [ ] Confirm no custom Rank I is lost by `applyEnchantments`, no new trigger bypasses `CascadeGuard`, and no existing vanilla handler behavior changed.
- [ ] Commit any verified cleanup separately with a narrowly scoped message.
