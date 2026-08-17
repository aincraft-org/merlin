# Glyph MapGUI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional Java 25 MapGUI integration artifact that lets players draw vector-authoritative glyphs on a private 128×128 handheld map screen while preserving the existing Paper 1.21.8/Java 21 plugin.

**Architecture:** Keep `java-compiler` and `paper` unchanged in their current targets. Add `mapgui-integration` with Java 25, `io.github.flog99:mapgui-api:1.0.0` as `compileOnly`, and a hard MapGUI server dependency in its descriptor. The screen converts repeated right-click cursor callbacks into interpolated `GlyphCaptureSession` points; a 300 ms pause lifts the pen. It renders the derived full bitmap and exposes clear/undo/save/close controls.

**Tech Stack:** Java 25, Paper 26.2-compatible MapGUI API 1.0.0, existing Java 21 glyph core, JUnit 5, no ML dependency.

## Global Constraints

- Do not change the existing `paper` module’s Paper 1.21.8/Java 21 target.
- Shared glyph classes remain Java 21-compatible and are consumed by the Java 25 adapter.
- MapGUI is `compileOnly` and must be installed on the server.
- The integration descriptor declares MapGUI required and loads it before the adapter.
- Right-click repeat callbacks approximate held-button drawing; no raw client button-state claim.
- Vector strokes are persisted; bitmaps are regenerated.

---

### Task 1: Add optional MapGUI module and dependency contract

**Files:**
- Modify: `settings.gradle.kts`
- Create: `mapgui-integration/build.gradle.kts`
- Create: `mapgui-integration/src/main/resources/paper-plugin.yml`
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapGuiPlugin.java`

**Interfaces:**
- New module consumes `project(":java-compiler")`.
- Plugin descriptor declares MapGUI as a required server dependency with load order before the adapter.

- [ ] **Step 1: Write module smoke test**

Add a test source set that asserts the module compiles against MapGUI API 1.0.0 and the descriptor contains the required MapGUI dependency block.

- [ ] **Step 2: Run the focused module task**

Run `./gradlew :mapgui-integration:test`; expected failure because the module and dependency configuration do not exist.

- [ ] **Step 3: Implement module configuration**

Add Maven Central, `compileOnly("io.github.flog99:mapgui-api:1.0.0")`, `compileOnly(project(":java-compiler"))`, Java 25 toolchain, JUnit 5, and a minimal `JavaPlugin` entrypoint. Do not modify root or existing Paper Java/toolchain settings.

- [ ] **Step 4: Run module compilation**

Run `./gradlew :mapgui-integration:test`; expected PASS or a concrete API mismatch that must be corrected against the published 1.0.0 artifact.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts mapgui-integration
git commit -m "build: add optional MapGUI integration module"
```

### Task 2: Implement input-to-stroke interpolation

**Files:**
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTracker.java`
- Test: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphStrokeTrackerTest.java`

**Interfaces:**
- `GlyphStrokeTracker.acceptClick(int x, int y, long nowMillis)` appends a point or starts a stroke.
- `GlyphStrokeTracker.pause(long nowMillis)` ends the stroke when the elapsed time is at least 300 ms.
- `GlyphStrokeTracker.snapshot()` returns the current `GlyphDraft`.

- [ ] **Step 1: Write failing tests**

Test first click creates one point, repeated clicks interpolate intermediate points, a gap of 299 ms stays in one stroke, a gap of 300 ms starts a new stroke, invalid coordinates are rejected, and repeated identical samples do not grow unboundedly.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :mapgui-integration:test --tests '*GlyphStrokeTrackerTest*'`; expected failure.

- [ ] **Step 3: Implement tracker**

Use `GlyphCaptureSession`; interpolate integer map coordinates with a bounded step count, pass current time to the session, and call `endStroke` before appending after a pause. Keep timing logic deterministic under test.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add mapgui-integration/src/main/java mapgui-integration/src/test/java
git commit -m "feat: track MapGUI glyph drawing strokes"
```

### Task 3: Implement MapGUI drawing screen

**Files:**
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphScreen.java`
- Test: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphScreenModelTest.java`

**Interfaces:**
- `GlyphScreen extends de.flog99.mapgui.Screen`.
- `Draw(this::paintCanvas).onClick(this::stroke)` is the drawing surface.
- `onOpen`/`onClose` preserve the draft; `activateOn()` enables both buttons for drawing/menu controls.

- [ ] **Step 1: Write failing model tests**

Test stroke callbacks update the tracker, left-click toggles the overlay and lifts the pen, clear empties the draft, undo removes the last stroke, and close returns the last snapshot.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :mapgui-integration:test --tests '*GlyphScreenModelTest*'`; expected failure.

- [ ] **Step 3: Implement screen using verified 1.0.0 API**

Use the published signatures from the MapGUI drawing example: `Screen`, `Click`, `Draw`, `PaintContext`, `Node`, `State`, `Ui.Column/Row/Overlay/Button`, and `MapGui.get().open(player, screen)`. Render `GlyphRasterizer.renderFull(draft)` into the `PaintContext` bounds. Route right-click callbacks through the tracker; use left-click for the overlay. Keep the UI state per screen/player.

- [ ] **Step 4: Run module tests and compile**

Run `./gradlew :mapgui-integration:test`; expected PASS.

- [ ] **Step 5: Commit**

```bash
git add mapgui-integration/src/main/java mapgui-integration/src/test/java
git commit -m "feat: add MapGUI glyph drawing screen"
```

### Task 4: Wire plugin command and draft persistence

**Files:**
- Modify: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphMapGuiPlugin.java`
- Create: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapter.java`
- Test: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/GlyphDraftStoreAdapterTest.java`

**Interfaces:**
- `/glyph draw` opens `MapGui.get().open(player, new GlyphScreen(...))`.
- `GlyphDraftStoreAdapter` persists vector drafts through the shared glyph store seam or a versioned adapter format.

- [ ] **Step 1: Write failing persistence/wiring tests**

Test command registration, permission denial, opening only for players, round-trip vector draft persistence, and save/close behavior.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :mapgui-integration:test --tests '*GlyphDraftStoreAdapterTest*'`; expected failure.

- [ ] **Step 3: Implement command and adapter**

Add `/glyph draw`, enforce `wizardry.glyph.draw`, bind each screen to the player and exact marked glyph item, save vector draft on explicit save and close, and leave failed saves open with an error message.

- [ ] **Step 4: Run module tests**

Expected PASS.

- [ ] **Step 5: Commit**

```bash
git add mapgui-integration/src/main/java mapgui-integration/src/test/java
 git commit -m "feat: wire glyph MapGUI command and persistence"
```

### Task 5: Verify deployment and end-to-end behavior

**Files:**
- Modify: `docs/superpowers/specs/2026-08-12-glyph-mapgui-integration-design.md`
- Modify: `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`

- [ ] **Step 1: Document installation**

Document that operators install both the matching MapGUI plugin jar and the optional Glyph MapGUI jar, that MapGUI must load first, and that the existing Paper plugin remains independently deployable.

- [ ] **Step 2: Run all checks**

Run:
```bash
./gradlew :java-compiler:test :paper:test :paper:build :mapgui-integration:test :mapgui-integration:build
```

- [ ] **Step 3: Inspect artifacts and descriptors**

Confirm the adapter jar contains `GlyphScreen`, `GlyphStrokeTracker`, and `paper-plugin.yml`; confirm the descriptor has a required MapGUI dependency; confirm existing Paper tests/build remain green.

- [ ] **Step 4: Record live-server limitation**

Because this environment has no Paper 26.2/Java 25 server and client session, report compilation/unit-test verification separately from the live drag smoke test. Do not claim the live screen was exercised until that smoke test is run.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-08-12-glyph-mapgui-integration-design.md docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md
git commit -m "docs: document optional MapGUI glyph integration"
```
