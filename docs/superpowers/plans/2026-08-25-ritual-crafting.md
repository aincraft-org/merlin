# Ritual Crafting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a multi-pedestal ritual crafting system that consumes a frozen glyph map + vanilla material and produces magical crafting intermediates, with pips scaling output yield.

**Architecture:** Two placeable tile-entity blocks (`DROPPER`) — a `Ritual Anchor` and `Ritual Pedestal` — are identified by PDC markers. Players place a glyph map and material in the anchor, an ink bottle in a pedestal, and right-click the anchor with a Mortar & Pestle. A pure `RitualCircle` validator checks the 3×3 layout, a static `RitualRecipe` table maps `(word, material)` to outputs, and a thin `RitualListener` consumes inputs, drops results, and plays feedback.

**Tech Stack:** Java 25, Paper API 1.21, `DROPPER` tile state + PDC, JUnit 5, Mockito.

## Global Constraints

- All ritual code lives in `merlin-paper`; no Bukkit types in `merlin-api`.
- Use the same PDC marker pattern as `MortarPestle`/`InkStore`.
- Anchor and pedestals are craftable items that place marked `DROPPER` blocks.
- Activation requires the player to hold a marked Mortar & Pestle and right-click the anchor.
- Inputs are consumed only when validation passes; partial inputs never destroy each other.
- Output is a marked intermediate item with max stack size 1 for v1.
- Do not build custom block models, tile entity rendering, GUIs beyond the vanilla dropper UI, or recipe unlocking.

---

### Task 1: Define the recipe data model and lookup table

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualRecipe.java`
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualRecipeTable.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ritual/RitualRecipeTableTest.java`

**Interfaces:**
- `RitualRecipe(Label word, Material material, GlyphElement school, Material catalyst, ItemStack result, int baseYield, int maxYield)`
- `RitualRecipeTable.lookup(Label word, Material material) -> Optional<RitualRecipe>`
- `RitualRecipeTable.yield(RitualRecipe recipe, int pips) -> int`

- [ ] **Step 1: Write the failing tests**

```java
package dev.mintychochip.merlin.paper.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class RitualRecipeTableTest {
    @Test
    void findsKnownRecipeAndComputesYield() {
        RitualRecipeTable table = new RitualRecipeTable();
        Optional<RitualRecipe> found = table.lookup(Label.fromId("damage"), Material.IRON_INGOT);
        assertTrue(found.isPresent());
        assertEquals(GlyphElement.PHYSICAL, found.get().school());
        assertEquals(1, table.yield(found.get(), 1));
        assertEquals(3, table.yield(found.get(), 5));
    }

    @Test
    void unknownWordOrMaterialReturnsEmpty() {
        RitualRecipeTable table = new RitualRecipeTable();
        assertTrue(table.lookup(Label.fromId("nope"), Material.IRON_INGOT).isEmpty());
        assertTrue(table.lookup(Label.fromId("damage"), Material.DIRT).isEmpty());
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ritual.RitualRecipeTableTest`

Expected: compilation/test failure because `RitualRecipe` and `RitualRecipeTable` do not exist.

- [ ] **Step 3: Implement the recipe table**

```java
package dev.mintychochip.merlin.paper.ritual;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class RitualRecipe {
    private final Label word;
    private final Material material;
    private final GlyphElement school;
    private final Material catalyst;
    private final ItemStack result;
    private final int baseYield;
    private final int maxYield;

    public RitualRecipe(Label word, Material material, GlyphElement school, Material catalyst,
                        ItemStack result, int baseYield, int maxYield) {
        this.word = word;
        this.material = material;
        this.school = school;
        this.catalyst = catalyst;
        this.result = result;
        this.baseYield = baseYield;
        this.maxYield = maxYield;
    }

    public Label word() { return word; }
    public Material material() { return material; }
    public GlyphElement school() { return school; }
    public Material catalyst() { return catalyst; }
    public ItemStack result() { return result; }
    public int baseYield() { return baseYield; }
    public int maxYield() { return maxYield; }
}

public final class RitualRecipeTable {
    private static final Map<RitualKey, RitualRecipe> RECIPES = Map.ofEntries(
            Map.entry(new RitualKey(Label.fromId("damage"), Material.IRON_INGOT),
                    new RitualRecipe(Label.fromId("damage"), Material.IRON_INGOT, GlyphElement.PHYSICAL,
                            Material.STONE, new ItemStack(Material.IRON_INGOT), 1, 3)),
            Map.entry(new RitualKey(Label.fromId("heal"), Material.GOLD_INGOT),
                    new RitualRecipe(Label.fromId("heal"), Material.GOLD_INGOT, GlyphElement.PHYSICAL,
                            Material.GOLD_BLOCK, new ItemStack(Material.GOLD_INGOT), 1, 3)),
            Map.entry(new RitualKey(Label.fromId("push"), Material.COPPER_INGOT),
                    new RitualRecipe(Label.fromId("push"), Material.COPPER_INGOT, GlyphElement.PHYSICAL,
                            Material.PISTON, new ItemStack(Material.COPPER_INGOT), 1, 3)),
            Map.entry(new RitualKey(Label.fromId("flame"), Material.REDSTONE),
                    new RitualRecipe(Label.fromId("flame"), Material.REDSTONE, GlyphElement.FLAME,
                            Material.COAL_BLOCK, new ItemStack(Material.REDSTONE), 1, 3)),
            Map.entry(new RitualKey(Label.fromId("frost"), Material.AMETHYST_SHARD),
                    new RitualRecipe(Label.fromId("frost"), Material.AMETHYST_SHARD, GlyphElement.FROST,
                            Material.SNOW_BLOCK, new ItemStack(Material.AMETHYST_SHARD), 1, 3))
    );

    public Optional<RitualRecipe> lookup(Label word, Material material) {
        return Optional.ofNullable(RECIPES.get(new RitualKey(word, material)));
    }

    public int yield(RitualRecipe recipe, int pips) {
        return Math.min(recipe.maxYield(), recipe.baseYield() + (pips - 1) / 2);
    }

    private record RitualKey(Label word, Material material) {}
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ritual.RitualRecipeTableTest`

Expected: `BUILD SUCCESSFUL` and both tests pass.

---

### Task 2: Create the marked anchor and pedestal items

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualAnchor.java`
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualPedestal.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ritual/RitualAnchorTest.java`
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java` to register recipes and `BlockPlaceEvent` listener.

**Interfaces:**
- `RitualAnchor(Plugin plugin)` — item factory, `isAnchor(Block)`/`isAnchor(TileState)`, `registerRecipe()`, `applyTo(Block)`.
- `RitualPedestal(Plugin plugin)` — same shape for pedestals.

- [ ] **Step 1: Write the failing tests**

```java
package dev.mintychochip.merlin.paper.ritual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class RitualAnchorTest {
    @Test
    void recognizesMarkedAnchor() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");
        RitualAnchor anchor = new RitualAnchor(plugin);

        TileState state = mock(TileState.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(state.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(new NamespacedKey("merlin", "ritual_anchor"), PersistentDataType.BYTE)).thenReturn(true);

        assertTrue(anchor.isAnchor(state));

        TileState plain = mock(TileState.class);
        PersistentDataContainer plainPdc = mock(PersistentDataContainer.class);
        when(plain.getPersistentDataContainer()).thenReturn(plainPdc);
        assertFalse(anchor.isAnchor(plain));
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ritual.RitualAnchorTest`

Expected: compilation failure because `RitualAnchor` does not exist.

- [ ] **Step 3: Implement the anchor and pedestal items**

```java
package dev.mintychochip.merlin.paper.ritual;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class RitualAnchor {
    private final NamespacedKey markerKey;
    private final NamespacedKey recipeKey;

    public RitualAnchor(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "ritual_anchor");
        recipeKey = new NamespacedKey(plugin, "ritual_anchor");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DROPPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Ritual Anchor").decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    public boolean isAnchor(TileState state) {
        if (state == null) return false;
        return state.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void mark(TileState state) {
        state.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        state.update();
    }

    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
        recipe.addIngredient(Material.OBSIDIAN);
        recipe.addIngredient(Material.DROPPER);
        Bukkit.addRecipe(recipe);
    }
}
```

`RitualPedestal` is the same class with `ritual_pedestal` keys and `Ritual Pedestal` display name. Extract the common marker logic or keep two small classes.

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ritual.RitualAnchorTest`

Expected: `BUILD SUCCESSFUL` and the marker test passes.

- [ ] **Step 5: Wire BlockPlaceEvent in the plugin**

In `MerlinPlugin.onEnable`:

```java
var ritualAnchor = new RitualAnchor(this);
var ritualPedestal = new RitualPedestal(this);
ritualAnchor.registerRecipe();
ritualPedestal.registerRecipe();
getServer().getPluginManager().registerEvents(new RitualBlockListener(ritualAnchor, ritualPedestal), this);
```

Create `RitualBlockListener.java`:

```java
package dev.mintychochip.merlin.paper.ritual;

import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public final class RitualBlockListener implements Listener {
    private final RitualAnchor anchor;
    private final RitualPedestal pedestal;

    public RitualBlockListener(RitualAnchor anchor, RitualPedestal pedestal) {
        this.anchor = anchor;
        this.pedestal = pedestal;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.DROPPER) return;
        if (!(event.getBlock().getState() instanceof TileState state)) return;
        if (anchor.isAnchorItem(item)) {
            anchor.mark(state);
        } else if (pedestal.isPedestalItem(item)) {
            pedestal.mark(state);
        }
    }
}
```

- [ ] **Step 6: Verify compile**

Run: `./gradlew :merlin-paper:compileJava`

Expected: `BUILD SUCCESSFUL`.

---

### Task 3: Validate the ritual circle layout

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualCircle.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ritual/RitualCircleTest.java`

**Interfaces:**
- `RitualCircle.inspect(TileState anchor, RitualAnchor marker, RitualPedestal pedestalMarker) -> RitualLayout`
- `RitualLayout` exposes the anchor inventory, the list of pedestal inventories, and whether the layout is valid.

- [ ] **Step 1: Write the failing layout tests**

Mock `TileState`, `Inventory`, and `Block` to test `RitualCircle`.

- [ ] **Step 2: Run the test and verify it fails**

- [ ] **Step 3: Implement layout scanning**

`RitualCircle` checks the 8 blocks around the anchor for marked droppers. It returns the first pedestal inventory found (v1 needs only one ink bottle) and the anchor inventory.

- [ ] **Step 4: Run the test and verify it passes**

---

### Task 4: Create the product items and the listener

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualProducts.java`
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualListener.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ritual/RitualListenerTest.java`

**Interfaces:**
- `RitualProducts.create(RitualRecipe recipe, int pips) -> List<ItemStack>`
- `RitualListener` responds to `PlayerInteractEvent` on an anchor block when the player holds a Mortar & Pestle.

- [ ] **Step 1: Write the failing listener and product tests**

- [ ] **Step 2: Run the tests and verify they fail**

- [ ] **Step 3: Implement product factory and listener**

`RitualProducts` clones the recipe's result and sets amount based on `RitualRecipeTable.yield`.

`RitualListener`:
1. Verify `event.getHand() == EquipmentSlot.HAND` and `Action.RIGHT_CLICK_BLOCK`.
2. Verify the player holds a Mortar & Pestle.
3. Verify the clicked block is an anchor.
4. Use `RitualCircle` to find the anchor and pedestal inventories.
5. Find the glyph map and material in the anchor, and the matching ink in a pedestal.
6. Look up the recipe; if valid, consume inputs and drop outputs.
7. Play success or failure feedback.

- [ ] **Step 4: Run the tests and verify they pass**

---

### Task 5: Wire everything and document

**Files:**
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java` to register the `RitualListener`.
- Modify: `docs/glyphcraft-language.md` to add a "Ritual Crafting" section.

- [ ] **Step 1: Add listener to plugin bootstrap**

In `MerlinPlugin.onEnable`:

```java
var recipes = new RitualRecipeTable();
getServer().getPluginManager().registerEvents(
        new RitualListener(recipes, ritualAnchor, ritualPedestal, inkStore, mortar), this);
```

- [ ] **Step 2: Add player-facing docs**

Insert after the "Grinding Ink" section:

```markdown
### Ritual Crafting

Build a 3×3 ritual circle: one **Ritual Anchor** in the center and **Ritual Pedestals** around it. Place the glyph map and a vanilla material in the anchor, and a matching ink bottle in a pedestal. Right-click the anchor with a Mortar & Pestle to create magical intermediates. Pips on the glyph determine how many outputs you receive.

| Glyph | Material | Output |
|---|---|---|
| `damage` | iron ingot | Aether Ingot |
| `heal` | gold ingot | Sun Drop |
| `push` | copper ingot | Kinetic Coil |
| `flame` | redstone | Cinder Salt |
| `frost` | amethyst shard | Rime Crystal |
```

- [ ] **Step 3: Run module tests**

Run: `./gradlew :merlin-paper:test`

Expected: `BUILD SUCCESSFUL`.

---

### Task 6: Verify the complete feature

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build the Paper jar**

Run: `./gradlew :merlin-paper:jar`

Expected: `BUILD SUCCESSFUL`; jar is written under `merlin-paper/build/libs/`.

- [ ] **Step 3: Inspect the source contract**

Verify by inspection that the listener:

- only activates on right-click with a Mortar & Pestle;
- requires a marked anchor;
- consumes exactly one glyph map, one material, one ink bottle, and one catalyst;
- drops output at the anchor location;
- leaves invalid layouts untouched.

If a live dev-server is available, place the anchor and pedestals, perform a ritual, and confirm the intermediate item drops. If not, report unit/build evidence.
