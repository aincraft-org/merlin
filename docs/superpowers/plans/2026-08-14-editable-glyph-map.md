# Editable Glyph Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert a saved editable glyph canvas into a filled Minecraft map that displays the glyph and can be reopened for editing.

**Architecture:** `GlyphDraftStoreAdapter` validates identity, persists editable draft metadata on paper and filled maps, and prepares map replacements without mutating inventory. `GlyphMapRenderer` renders one immutable 128×128 grayscale glyph image. `GlyphMapGuiPlugin` performs the guarded main-hand replacement only after preparation succeeds.

**Tech Stack:** Java 25, Paper API, Bukkit map API, MapGUI, Gradle, JUnit 5, MockBukkit if already available in the module.

## Global Constraints

- A successful first save converts exactly one paper canvas into exactly one `FILLED_MAP`.
- Preserve the original glyph UUID and complete editable draft.
- Repeat saves reuse the existing map view and do not duplicate items or renderers.
- Accept only plugin-marked paper or filled-map items.
- Never overwrite a changed or invalid held item.
- Failed conversion leaves the original item unchanged.
- Render every glyph coordinate deterministically onto the corresponding 128×128 map pixel.
- Lock saved glyph maps against normal terrain updates.
- Existing paper drafts remain readable and convert on their next save.

---

### Task 1: Render immutable glyph pixels on a map

**Files:**
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapRenderer.java`
- Create: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphMapRendererTest.java`

**Interfaces:**
- Consumes: `GlyphBitmap.pixels()` containing exactly 16,384 unsigned intensity bytes.
- Produces: `GlyphMapRenderer(GlyphBitmap bitmap)` extending Bukkit `MapRenderer`, with `render(MapView, MapCanvas, Player)` painting a stable 128×128 image.

- [ ] **Step 1: Write failing renderer tests**

Create tests using a real or module-standard test canvas. Verify pixel conversion through a package-private pure helper so no mock renderer plumbing is needed:

```java
@Test void mapsGlyphIntensityToDarkBackgroundAndWhiteInk() {
    assertEquals(MapPalette.matchColor(0, 0, 0), GlyphMapRenderer.mapColor((byte) 0));
    assertEquals(MapPalette.matchColor(255, 255, 255), GlyphMapRenderer.mapColor((byte) 255));
}

@Test void preservesPartialIntensityOrdering() {
    int dark = Byte.toUnsignedInt(GlyphMapRenderer.mapColor((byte) 32));
    int light = Byte.toUnsignedInt(GlyphMapRenderer.mapColor((byte) 192));
    assertNotEquals(dark, light);
}
```

Also construct a bitmap with nonzero pixels at `(0,0)`, `(64,32)`, and `(127,127)` and verify the renderer writes those exact coordinates through the project’s available `MapCanvas` test implementation.

- [ ] **Step 2: Run the renderer test and confirm red**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphMapRendererTest
```

Expected: compilation failure because `GlyphMapRenderer` does not exist.

- [ ] **Step 3: Implement the renderer**

Implement one immutable defensive copy of the 16,384 map colors in the constructor. Convert each glyph intensity to equal RGB channels using `MapPalette.matchColor(int, int, int)`. In `render`, call `canvas.setPixel(x, y, colors[y * 128 + x])` for every coordinate. Do not retain the draft, map view, player, or canvas.

- [ ] **Step 4: Run renderer tests**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphMapRendererTest
```

Expected: PASS.

- [ ] **Step 5: Commit renderer behavior**

```bash
git add mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapRenderer.java mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphMapRendererTest.java
git diff --cached --check
git commit -m "feat: render glyphs onto Minecraft maps"
```

### Task 2: Convert persistent glyph canvases into editable maps

**Files:**
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapter.java`
- Modify: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapterTest.java`

**Interfaces:**
- Consumes: `Plugin`, `Server`, held `ItemStack`, expected `UUID`, and `GlyphDraft`.
- Produces: `Optional<ItemStack> prepareSavedMap(ItemStack source, UUID expectedId, GlyphDraft draft)`, returning a replacement only after all metadata and renderer work succeeds.
- Preserves: `UUID itemId(ItemStack)`, `Optional<GlyphDraft> load(ItemStack, UUID)`, and blank paper creation.

- [ ] **Step 1: Write failing persistence tests**

Using the module’s Paper test fixture, assert:

```java
var paper = store.createGlyphItem();
var id = store.itemId(paper);
var draft = draftWithVisibleStroke();
var saved = store.prepareSavedMap(paper, id, draft).orElseThrow();
assertEquals(Material.FILLED_MAP, saved.getType());
assertEquals(id, store.itemId(saved));
assertEquals(draft, store.load(saved, id).orElseThrow());
assertNotNull(((MapMeta) saved.getItemMeta()).getMapView());
```

Add separate tests proving an existing saved map reuses its `MapView`, its renderer list contains exactly one `GlyphMapRenderer` after repeat saves, a wrong UUID returns empty, an ordinary filled map returns empty, and an oversized encoded draft returns empty without modifying the source item.

- [ ] **Step 2: Run persistence tests and confirm red**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphDraftStoreAdapterTest
```

Expected: compilation failure because `prepareSavedMap` does not exist.

- [ ] **Step 3: Generalize glyph-item recognition**

Change item recognition from `Material.PAPER` only to `(Material.PAPER || Material.FILLED_MAP)` while retaining marker, UUID, and metadata validation. Keep loading old marked paper items unchanged.

- [ ] **Step 4: Implement transactional map preparation**

Encode and validate the draft before allocating or modifying map state. Clone the source item, converting paper to `FILLED_MAP`. For paper, create a map through `server.createMap(world)` using the player world supplied by the plugin boundary; therefore use the final signature:

```java
Optional<ItemStack> prepareSavedMap(
        ItemStack source, UUID expectedId, GlyphDraft draft, World world)
```

For existing filled maps, require a non-null `MapMeta.getMapView()`. Set `MapView#setLocked(true)`, remove all existing renderers, add one new `GlyphMapRenderer(GlyphRasterizer.renderFull(draft))`, and write marker, unchanged UUID, and encoded draft into cloned item metadata. Return empty on validation or runtime failure. Never mutate `source`.

- [ ] **Step 5: Run persistence tests**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphDraftStoreAdapterTest
```

Expected: PASS.

- [ ] **Step 6: Commit editable map persistence**

```bash
git add mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapter.java mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapterTest.java
git diff --cached --check
git commit -m "feat: persist editable glyphs on filled maps"
```

### Task 3: Replace only the validated held canvas on save

**Files:**
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapGuiPlugin.java`
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapSaveAction.java`
- Create: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphMapSaveActionTest.java`

**Interfaces:**
- Consumes: player main-hand access, expected glyph UUID, draft supplier, and `GlyphDraftStoreAdapter.prepareSavedMap(ItemStack, UUID, GlyphDraft, World)`.
- Produces: `boolean GlyphMapSaveAction.save(Player player, UUID expectedId, GlyphDraft draft)`; true only when the exact validated item is replaced.

- [ ] **Step 1: Write failing guarded replacement tests**

Test with the module’s server/player fixture:

```java
var paper = store.createGlyphItem();
player.getInventory().setItemInMainHand(paper);
var id = store.itemId(paper);
assertTrue(action.save(player, id, draftWithVisibleStroke()));
assertEquals(Material.FILLED_MAP, player.getInventory().getItemInMainHand().getType());
```

Add tests proving a changed main-hand item returns false and remains byte-for-byte/item-meta equivalent, a wrong UUID returns false, and saving an existing glyph map retains its map view ID.

- [ ] **Step 2: Run save-action tests and confirm red**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphMapSaveActionTest
```

Expected: compilation failure because `GlyphMapSaveAction` does not exist.

- [ ] **Step 3: Implement guarded inventory replacement**

`save` reads the main-hand item once, validates `store.itemId(held)` equals `expectedId`, calls `prepareSavedMap(held, expectedId, draft, player.getWorld())`, rechecks that the currently held item still has the expected ID immediately before mutation, then calls `player.getInventory().setItemInMainHand(replacement)`. Return false at any failed step.

- [ ] **Step 4: Route plugin saves through the action**

Construct the save action after the store in `onEnable`. Replace the existing metadata-only save callback with `saveAction.save(player, itemId, tracker.snapshot())`. Send `Glyph saved to map.` on success and retain the existing changed-item failure message. Since `itemId` and `load` now recognize plugin-marked filled maps, no separate reopen path is required.

- [ ] **Step 5: Run save-action and screen tests**

```bash
./gradlew :mapgui-integration:test --tests dev.mintychochip.wizardry.mapgui.GlyphMapSaveActionTest --tests dev.mintychochip.wizardry.mapgui.GlyphScreenModelTest
```

Expected: PASS.

- [ ] **Step 6: Commit plugin integration**

```bash
git add mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapGuiPlugin.java mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapSaveAction.java mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphMapSaveActionTest.java
git diff --cached --check
git commit -m "feat: replace saved glyph canvases with maps"
```

### Task 4: Verify the editable map workflow end to end

**Files:**
- Modify only if a verified defect is found in Tasks 1–3.

**Interfaces:**
- Consumes: `/glyph book`, `/glyph`, the editor save action, and a running Paper server.
- Produces: observed paper→filled-map conversion and successful reopening of stored strokes.

- [ ] **Step 1: Run the complete affected test suites**

```bash
./gradlew :java-compiler:test :mapgui-integration:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build the deployed plugin artifact**

```bash
./gradlew :mapgui-integration:jar
```

Expected: `BUILD SUCCESSFUL`, with the current plugin jar produced under `mapgui-integration/build/libs/`.

- [ ] **Step 3: Restart the managed Paper server**

Stop and restart the existing `wizardry-paper` managed process. Wait for the `Done (...)! For help` readiness line and verify `WizardryGlyphMapGUI` enables without an exception from map persistence.

- [ ] **Step 4: Exercise the actual player workflow**

From an available test player/session: run `/glyph book`, hold the paper, open `/glyph`, draw a visible stroke, save, and observe the main-hand item become `FILLED_MAP`. View the map and confirm the stroke is visible. Run `/glyph` while holding the map and confirm the editor reloads the saved stroke. Save again and confirm the same map ID remains.

- [ ] **Step 5: Re-run tests after any smoke-fix**

```bash
./gradlew :java-compiler:test :mapgui-integration:test
```

Expected: `BUILD SUCCESSFUL`.
