# Enchantment Port — Tools, Harvesting & Durability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the first 12 Phase-1 enchants from the AdvancedEnchantments roster into Merlin's existing Custom Enchantment Framework.

**Architecture:** Each enchant is a concrete `OvercapEffectHandler` implementing an existing trigger interface (`BlockBreakTrigger`, `BlockDropTrigger`, `ActiveInteractTrigger`, `ItemDamageTrigger`). Handlers are registered in `EnchantmentRegistry.defaultRegistry` and covered by JUnit/Mockito unit tests.

**Tech Stack:** Java 21, PaperMC 1.21.4, JUnit 5, Mockito, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-29-advanced-enchantments-port-design.md`

## Global Constraints

- Java 21 records, sealed types, and pattern matching.
- Scoped cascade limit set to `MAX_CASCADE_DEPTH = 3`.
- Triggers and contexts interface directly with `EnchantmentRegistry` and `OvercapItemAdapter`.
- All new enchants must be compatible with `enchantments.disabled` in `config.yml`.
- Never create a second item-storage model; reuse `OvercapItemAdapter`.
- Use `CascadeScope.breakBlockSafely` for any simulated block breaks.

---

### Task 1: `Telepathy` — mined drops go to inventory

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TelepathyHandler.java`
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TelepathyHandlerTest.java`

**Interfaces:**
- Consumes: `BlockDropTrigger` (`onBlockDrop(Player, BlockState, List<Item>, int)`)
- Produces: `TelepathyHandler` registered under `merlin:telepathy`

- [ ] **Step 1: Write failing test**

```java
package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class TelepathyHandlerTest {
    @Test
    void teleportsMinedDropsToPlayerInventory() {
        TelepathyHandler handler = new TelepathyHandler();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        World world = mock(World.class);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        Item item = mock(Item.class);
        ItemStack stack = new ItemStack(org.bukkit.Material.DIAMOND);
        when(item.getItemStack()).thenReturn(stack);
        when(item.getLocation()).thenReturn(location);
        List<Item> drops = new ArrayList<>(List.of(item));
        BlockState state = mock(BlockState.class);

        handler.onBlockDrop(player, state, drops, 1);

        assertEquals(true, drops.isEmpty(), "Drop entities should be removed");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :merlin-paper:test --tests 'dev.mintychochip.merlin.paper.enchanting.custom.handler.TelepathyHandlerTest'`

Expected: FAIL, class not found.

- [ ] **Step 3: Implement `TelepathyHandler`**

```java
package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockDropTrigger;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TelepathyHandler implements OvercapEffectHandler, BlockDropTrigger {
    private static final NamespacedKey KEY = new NamespacedKey("merlin", "telepathy");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockDrop(Player player, BlockState blockState, List<Item> items, int level) {
        if (player == null || items == null || items.isEmpty() || level <= 0) return;

        for (Item item : items) {
            if (item == null) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) continue;
            player.getInventory().addItem(stack);
            item.remove();
        }
        items.clear();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :merlin-paper:test --tests 'dev.mintychochip.merlin.paper.enchanting.custom.handler.TelepathyHandlerTest'`

Expected: PASS.

- [ ] **Step 5: Register in `EnchantmentRegistry`**

Do not register yet; Task 11 registers all 12 enchants together.

- [ ] **Step 6: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TelepathyHandler.java \
        merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TelepathyHandlerTest.java
git commit -m "feat(enchanting): add Telepathy handler and test"
```

---

### Task 2: `Timber` — fell whole trees

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TimberHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TimberHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger` (`onBlockBreak(Player, Block, int, CascadeScope)`)
- Produces: `TimberHandler` registered under `merlin:timber`

- [ ] **Step 1: Write failing test**

Test that breaking a log with `Timber` causes `CascadeScope.breakBlockSafely` to be called on adjacent logs up to the limit. Use Mockito to verify `scope.breakBlockSafely` is called.

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `TimberHandler`**

Flood-fill connected logs (material `LOG` or `WOOD` variants, including stripped/deep-slate variants is not needed; use `Tag.LOGS` or a set of `Material` log types) up to `4L * level` blocks, breaking via `scope.breakBlockSafely(neighbor, true)`.

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Commit**

---

### Task 3: `Trench` — 3×3 mining

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TrenchHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/TrenchHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger`
- Produces: `TrenchHandler` registered under `merlin:trench`

- [ ] **Step 1-4:** Same TDD cycle.

Implementation: on break, break the 3×3 area centered on the broken block in the plane orthogonal to the player's facing direction (or the block face). Use `scope.breakBlockSafely`.

- [ ] **Step 5: Commit**

---

### Task 4: `Replanter` — replant crops when harvested

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReplanterHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReplanterHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger`
- Produces: `ReplanterHandler` registered under `merlin:replanter`

- [ ] **Step 1-4:** TDD cycle.

Implementation: detect mature crop blocks (wheat, carrots, potatoes, beetroots, nether wart). On break, drop crop and immediately set the block back to its initial age-0 state instead of breaking the soil.

- [ ] **Step 5: Commit**

---

### Task 5: `Planter`, `Carrot Planter`, `Potato Planter` — 3×3 planting

**Files:**
- Create:
  - `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/PlanterHandler.java`
  - `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/CarrotPlanterHandler.java`
  - `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/PotatoPlanterHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/PlanterHandlerTest.java` (covers all three via helper)

**Interfaces:**
- Consumes: `ActiveInteractTrigger` (`onActiveInteract(Player, Action, Block, ItemStack, int)`)
- Produces: three handlers under `merlin:planter`, `merlin:carrot_planter`, `merlin:potato_planter`

- [ ] **Step 1: Write failing test**

Test that shift-right-clicking farmland plants seeds in a 3×3 area and consumes one seed per block from inventory.

- [ ] **Step 2: Implement handlers**

Each handler checks `action == Action.RIGHT_CLICK_BLOCK` and the clicked block is farmland. It then iterates the 3×3 farmland grid, plants the matching seed/crop if the farmland is empty, and consumes the seed from the player's inventory.

- [ ] **Step 3: Run tests and commit**

---

### Task 6: `Experience` — bonus XP from ores

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ExperienceHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ExperienceHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger`
- Produces: `ExperienceHandler` registered under `merlin:experience`

- [ ] **Step 1-4:** TDD cycle.

Implementation: if the broken block is in `ORES` (matching `DrillHandler.ORES` plus deepslate variants), spawn an experience orb or directly call `player.giveExp` proportional to `level`. Do not double-dip with `drill`.

- [ ] **Step 5: Commit**

---

### Task 7: `Rebreather` — regain air underwater mining

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/RebreatherHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/RebreatherHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger`
- Produces: `RebreatherHandler` registered under `merlin:rebreather`

- [ ] **Step 1-4:** TDD cycle.

Implementation: on any block break, if `player.getRemainingAir() < player.getMaximumAir()`, restore `level * 20` ticks of air, capped at max.

- [ ] **Step 5: Commit**

---

### Task 8: `Replenish` — restore food while mining

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReplenishHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReplenishHandlerTest.java`

**Interfaces:**
- Consumes: `BlockBreakTrigger`
- Produces: `ReplenishHandler` registered under `merlin:replenish`

- [ ] **Step 1-4:** TDD cycle.

Implementation: on block break, if the block is in `ORES`, restore `level` food and `level * 0.5` saturation, capped at 20.

- [ ] **Step 5: Commit**

---

### Task 9: `Unbreakable` — tools never take durability

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/UnbreakableHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/UnbreakableHandlerTest.java`

**Interfaces:**
- Consumes: `ItemDamageTrigger` (`onItemDamage(Player, ItemStack, int, int)`)
- Produces: `UnbreakableHandler` registered under `merlin:unbreakable`

- [ ] **Step 1: Write failing test**

```java
@Test
void unbreakableReturnsZeroDamage() {
    UnbreakableHandler handler = new UnbreakableHandler();
    assertEquals(0, handler.onItemDamage(null, null, 5, 1));
}
```

- [ ] **Step 2: Implement**

```java
public final class UnbreakableHandler implements OvercapEffectHandler, ItemDamageTrigger {
    private static final NamespacedKey KEY = new NamespacedKey("merlin", "unbreakable");

    @Override
    public NamespacedKey key() { return KEY; }

    @Override
    public int onItemDamage(Player player, ItemStack item, int originalDamageAmount, int level) {
        return level > 0 ? 0 : originalDamageAmount;
    }
}
```

- [ ] **Step 3: Run tests and commit**

---

### Task 10: `Reforged` — tools take durability slower

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReforgedHandler.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ReforgedHandlerTest.java`

**Interfaces:**
- Consumes: `ItemDamageTrigger`
- Produces: `ReforgedHandler` registered under `merlin:reforged`

- [ ] **Step 1-4:** TDD cycle.

Implementation: `return level > 0 ? Math.max(0, originalDamageAmount - level) : originalDamageAmount;` or scale by `1.0 / (1.0 + 0.5 * level)`.

- [ ] **Step 5: Commit**

---

### Task 11: Register all 12 enchants in `EnchantmentRegistry`

**Files:**
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java`
- Modify: `merlin-paper/src/main/resources/config.yml` (add to default `disabled` example list if desired)

**Interfaces:**
- Consumes: all 12 handlers
- Produces: definitions in `defaultRegistry`

- [ ] **Step 1: Add imports and `registerCustom` calls**

Add one `registerCustom(...)` call per enchant in `EnchantmentRegistry.defaultRegistry()` with appropriate target `Set<Material>`:

- `telepathy` → `weaponsAndTools` (or `tools`)
- `timber` → axes that can mine logs
- `trench` → pickaxes and shovels
- `replanter` → hoes
- `planter`/`carrot_planter`/`potato_planter` → hoes
- `experience` → pickaxes
- `rebreather` → pickaxes
- `replenish` → pickaxes
- `unbreakable` → all tools/weapons/bows
- `reforged` → all tools/weapons/bows

- [ ] **Step 2: Set rarity/levels/eterna/weight**

Use conservative tunings:
- Max level 1–3 for utility, 3–5 for combat/durability
- Weight 5–10, baseEterna 5–15, eternaPerLevel 5

- [ ] **Step 3: Add to `CustomEnchantmentRegistryTest` if needed**

Ensure `defaultRegistry()` still returns the new definitions; existing test covers count of expected enchants.

- [ ] **Step 4: Run full `:merlin-paper:test` and commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java \
        merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ \
        merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/
git commit -m "feat(enchanting): register tools/harvesting/durability enchants"
```

---

### Task 12: Smoke test dispatcher integration

**Files:**
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcherTest.java`

**Interfaces:**
- Consumes: `EnchantmentRegistry`, `OvercapItemAdapter`, `CustomEnchantmentDispatcher`
- Produces: passing integration test

- [ ] **Step 1: Add dispatcher test**

Add one test per trigger type covered: `BlockBreakTrigger`, `BlockDropTrigger`, `ActiveInteractTrigger`, `ItemDamageTrigger`. For each, mock an item with the enchant and verify the handler is invoked through the dispatcher.

- [ ] **Step 2: Run full module tests**

Run: `./gradlew :merlin-paper:test`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcherTest.java
git commit -m "test(enchanting): dispatcher integration for tool enchants"
```

---

## Self-Review

**Spec coverage:**
- §2.2 Tools / harvesting: covered by Tasks 1–8
- §2.2 Tool durability: covered by Tasks 9–10
- §4.2 (handler + registry pattern): covered by Task 11
- §8 (testing): covered by every task and Task 12

**Placeholder scan:**
- No `TBD` or `TODO`.
- No "Similar to Task N" references; each handler code/behavior is specified.
- Code blocks provided for the simplest cases (`Telepathy`, `Unbreakable`); complex cases include algorithmic specs.

**Type consistency:**
- All handlers implement `OvercapEffectHandler` and one of `BlockBreakTrigger`, `BlockDropTrigger`, `ActiveInteractTrigger`, `ItemDamageTrigger`.
- `NamespacedKey` keys use `merlin:<name>`.
