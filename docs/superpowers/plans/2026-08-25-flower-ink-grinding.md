# Flower Ink Grinding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a handheld Mortar & Pestle survival path that grinds mapped vanilla flowers into full Merlin ink bottles.

**Architecture:** Keep flower classification as a pure paper-side data table. A marked, permanent `BOWL` item is crafted from a bowl and stick; a thin `PlayerInteractEvent` listener recognizes the main-hand tool and off-hand flower, consumes one flower, and delegates bottle creation to the existing `InkStore`. The existing `/glyph ink` command remains unchanged.

**Tech Stack:** Java 25, Paper API 1.21, Bukkit `ShapelessRecipe`, PDC marker keys, JUnit 5, Mockito.

## Global Constraints

- Preserve the existing four `GlyphElement` values and their `InkStore` bottle representation.
- One mapped flower produces one new full bottle (`MagicalInk.DEFAULT_MAX`); do not implement refill or partial-fill behavior.
- Canonical gesture is Mortar & Pestle main hand + mapped flower off hand + right-click air/block.
- Unmapped flowers and non-flower off-hand items are silent no-ops and are not consumed.
- The Mortar & Pestle has no durability and is not stackable.
- `/glyph ink <element>` remains an authoring/admin hatch.
- New Bukkit implementation stays in `merlin-paper`; do not add Bukkit types to `merlin-api`.
- Work on top of the existing dirty tree; do not reset, format, or stage unrelated user changes.

---

### Task 1: Add the flower-to-element mapping

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/FlowerGrind.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ink/FlowerGrindTest.java`

**Interfaces:**
- Produces `public static Optional<GlyphElement> elementFor(Material flower)`.
- Produces `public static Map<Material, GlyphElement> mappings()` returning the immutable mapping used by tests and future callers.

- [ ] **Step 1: Write the failing mapping tests**

```java
package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class FlowerGrindTest {
    @Test
    void mapsGenerousFlowerGroupsToElements() {
        Map<Material, GlyphElement> expected = Map.ofEntries(
                Map.entry(Material.TORCHFLOWER, GlyphElement.FLAME),
                Map.entry(Material.POPPY, GlyphElement.FLAME),
                Map.entry(Material.BLUE_ORCHID, GlyphElement.FROST),
                Map.entry(Material.CORNFLOWER, GlyphElement.FROST),
                Map.entry(Material.ALLIUM, GlyphElement.ARCANE),
                Map.entry(Material.WITHER_ROSE, GlyphElement.ARCANE),
                Map.entry(Material.LILAC, GlyphElement.ARCANE),
                Map.entry(Material.OXEYE_DAISY, GlyphElement.PHYSICAL),
                Map.entry(Material.DANDELION, GlyphElement.PHYSICAL),
                Map.entry(Material.AZURE_BLUET, GlyphElement.PHYSICAL),
                Map.entry(Material.SUNFLOWER, GlyphElement.PHYSICAL));

        assertEquals(expected, FlowerGrind.mappings());
        expected.forEach((flower, element) -> assertEquals(element, FlowerGrind.elementFor(flower).orElseThrow()));
    }

    @Test
    void unmappedMaterialsAreNotConsumedAsFlowers() {
        assertTrue(FlowerGrind.elementFor(Material.ROSE_BUSH).isEmpty());
        assertTrue(FlowerGrind.elementFor(Material.BOWL).isEmpty());
        assertTrue(FlowerGrind.elementFor(null).isEmpty());
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.FlowerGrindTest`

Expected: compilation/test failure because `FlowerGrind` does not exist.

- [ ] **Step 3: Implement the immutable mapping**

```java
package dev.mintychochip.merlin.paper.ink;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class FlowerGrind {
    private static final Map<Material, GlyphElement> MAPPINGS = Map.ofEntries(
            Map.entry(Material.TORCHFLOWER, GlyphElement.FLAME),
            Map.entry(Material.POPPY, GlyphElement.FLAME),
            Map.entry(Material.BLUE_ORCHID, GlyphElement.FROST),
            Map.entry(Material.CORNFLOWER, GlyphElement.FROST),
            Map.entry(Material.ALLIUM, GlyphElement.ARCANE),
            Map.entry(Material.WITHER_ROSE, GlyphElement.ARCANE),
            Map.entry(Material.LILAC, GlyphElement.ARCANE),
            Map.entry(Material.OXEYE_DAISY, GlyphElement.PHYSICAL),
            Map.entry(Material.DANDELION, GlyphElement.PHYSICAL),
            Map.entry(Material.AZURE_BLUET, GlyphElement.PHYSICAL),
            Map.entry(Material.SUNFLOWER, GlyphElement.PHYSICAL));

    private FlowerGrind() {}

    public static Optional<GlyphElement> elementFor(Material flower) {
        if (flower == null) return Optional.empty();
        return Optional.ofNullable(MAPPINGS.get(flower));
    }

    public static Map<Material, GlyphElement> mappings() {
        return MAPPINGS;
    }
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.FlowerGrindTest`

Expected: `BUILD SUCCESSFUL` and both tests pass.

---

### Task 2: Add the marked Mortar & Pestle and its craft recipe

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/MortarPestle.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ink/MortarPestleTest.java`

**Interfaces:**
- Constructor: `MortarPestle(Plugin plugin)`.
- Produces `ItemStack create()`.
- Produces `boolean isMortar(ItemStack item)`.
- Provides `void registerRecipe()`.

- [ ] **Step 1: Write the failing identity tests**

```java
package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class MortarPestleTest {
    @Test
    void createsAndRecognizesMarkedMortar() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        MortarPestle mortar = new MortarPestle(plugin);

        assertTrue(mortar.isMortar(mortar.create()));
        assertFalse(mortar.isMortar(new ItemStack(Material.BOWL)));
        assertFalse(mortar.isMortar(new ItemStack(Material.STICK)));
        assertFalse(mortar.isMortar(null));
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.MortarPestleTest`

Expected: compilation failure because `MortarPestle` does not exist.

- [ ] **Step 3: Implement the marked item and shapeless recipe**

Use a plugin-scoped PDC marker and the same Paper data-component style already used by `InkStore`:

```java
package dev.mintychochip.merlin.paper.ink;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MortarPestle {
    private final NamespacedKey markerKey;
    private final NamespacedKey recipeKey;

    public MortarPestle(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "mortar_pestle");
        recipeKey = new NamespacedKey(plugin, "mortar_pestle");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.BOWL);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(
                DataComponentTypes.ITEM_NAME,
                Component.text("Mortar & Pestle").decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    public boolean isMortar(ItemStack item) {
        if (item == null || item.getType() != Material.BOWL || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
        recipe.addIngredient(Material.BOWL);
        recipe.addIngredient(Material.STICK);
        Bukkit.addRecipe(recipe);
    }
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.MortarPestleTest`

Expected: `BUILD SUCCESSFUL` and the identity test passes.

---

### Task 3: Add the right-click grinding listener

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/GrindListener.java`
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/ink/GrindListenerTest.java`

**Interfaces:**
- Constructor: `GrindListener(InkStore inks, MortarPestle mortar)`.
- Package-visible pure seam: `static Optional<GlyphElement> grindElement(ItemStack tool, ItemStack flower, MortarPestle mortar)`.
- Event behavior consumes exactly one mapped flower, adds `inks.create(element)`, drops any inventory leftovers, cancels the triggering right-click, and plays grind feedback.

- [ ] **Step 1: Write the failing pure routing tests**

```java
package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class GrindListenerTest {
    @Test
    void routesMortarAndMappedFlowerToElement() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        MortarPestle mortar = new MortarPestle(plugin);

        assertEquals(
                Optional.of(GlyphElement.FLAME),
                GrindListener.grindElement(mortar.create(), new ItemStack(Material.TORCHFLOWER), mortar));
    }

    @Test
    void rejectsWrongToolUnmappedFlowerAndNullItems() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        MortarPestle mortar = new MortarPestle(plugin);

        assertTrue(GrindListener.grindElement(new ItemStack(Material.BOWL), new ItemStack(Material.POPPY), mortar).isEmpty());
        assertTrue(GrindListener.grindElement(mortar.create(), new ItemStack(Material.ROSE_BUSH), mortar).isEmpty());
        assertTrue(GrindListener.grindElement(null, null, mortar).isEmpty());
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.GrindListenerTest`

Expected: compilation failure because `GrindListener` does not exist.

- [ ] **Step 3: Implement the thin listener**

```java
package dev.mintychochip.merlin.paper.ink;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Optional;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class GrindListener implements Listener {
    private final InkStore inks;
    private final MortarPestle mortar;

    public GrindListener(InkStore inks, MortarPestle mortar) {
        this.inks = inks;
        this.mortar = mortar;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemStack flower = player.getInventory().getItemInOffHand();
        Optional<GlyphElement> element = grindElement(tool, flower, mortar);
        if (element.isEmpty()) return;

        event.setCancelled(true);
        flower.setAmount(flower.getAmount() - 1);
        player.getInventory().addItem(inks.create(element.get())).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.8f);
        player.getWorld().spawnParticle(
                Particle.COMPOSTER, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
    }

    static Optional<GlyphElement> grindElement(ItemStack tool, ItemStack flower, MortarPestle mortar) {
        if (!mortar.isMortar(tool) || flower == null || flower.getType().isAir()) return Optional.empty();
        return FlowerGrind.elementFor(flower.getType());
    }
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `./gradlew :merlin-paper:test --tests dev.mintychochip.merlin.paper.ink.GrindListenerTest`

Expected: `BUILD SUCCESSFUL` and both routing tests pass.

---

### Task 4: Wire the feature and document the player contract

**Files:**
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java:70-74`
- Modify: `docs/glyphcraft-language.md` after the `/glyph enchant` row and before the `Right-click a frozen map...` paragraph.
- Modify: `docs/living-specs/glyphcraft.md` Current checkboxes and Last updated date after verification.

**Interfaces:**
- Bootstrap creates one `MortarPestle`, registers its recipe, and registers one `GrindListener` using the existing `InkStore`.

- [ ] **Step 1: Add bootstrap wiring**

Immediately after `var inks = new InkStore(this);`, add:

```java
var mortar = new MortarPestle(this);
mortar.registerRecipe();
getServer().getPluginManager().registerEvents(new GrindListener(inks, mortar), this);
```

Add imports for `GrindListener` and `MortarPestle`. Do not alter `/glyph` command construction.

- [ ] **Step 2: Add the player-facing language section**

Insert after the `/glyph enchant` command row and before the `Right-click a frozen map while a tome is in the off hand...` paragraph:

```markdown
### Grinding Ink

Craft a **Mortar & Pestle** from one bowl and one stick. Hold it in your main hand and a mapped flower in your off hand, then right-click air or a block. The flower is consumed and produces one full bottle of the matching ink. The mortar has no durability.

| Element | Flowers |
|---|---|
| Flame Ink | Torchflower, poppy |
| Frost Ink | Blue orchid, cornflower |
| Arcane Ink | Allium, wither rose, lilac |
| Physical Ink | Oxeye daisy, dandelion, azure bluet, sunflower |

Other flowers do nothing and are not consumed. Grinding creates a new full bottle; it does not refill an empty bottle. `/glyph ink <element>` remains the authoring shortcut.
```

- [ ] **Step 3: Run docs and module tests**

Run: `./gradlew :merlin-paper:test`

Expected: `BUILD SUCCESSFUL`; all existing Paper tests and the three new focused test classes pass.

- [ ] **Step 4: Mark the living-spec Current items complete**

Only after the tests pass, change the two unchecked Mortar & Pestle Current entries to checked and set `Last updated` to `2026-08-25`. Keep the decisions log and out-of-scope removal already recorded; do not restore refill as a future item.

---

### Task 5: Verify the complete contract

**Files:**
- No new files; inspect the changed source, tests, language guide, and living spec.

- [ ] **Step 1: Run the full repository test suite**

Run: `./gradlew test`

Expected: `BUILD SUCCESSFUL` with no failures.

- [ ] **Step 2: Build the Paper jar**

Run: `./gradlew :merlin-paper:jar`

Expected: `BUILD SUCCESSFUL`; jar is written under `merlin-paper/build/libs/`.

- [ ] **Step 3: Check the actual source contract**

Verify by inspection that the listener:

- only handles main-hand right-click events;
- requires the marked Mortar & Pestle, not an ordinary bowl;
- consumes one and only one mapped flower;
- creates a full bottle through `InkStore.create`;
- drops inventory overflow;
- leaves unmapped flowers and empty/offhand non-flower items untouched;
- does not alter `/glyph ink` behavior.

If a live dev-server interaction is available, launch `./gradlew :merlin-test:runServer`, craft/use the tool, and observe the bottle. If no interactive client is available, report unit/build evidence without claiming an in-game click was exercised.
