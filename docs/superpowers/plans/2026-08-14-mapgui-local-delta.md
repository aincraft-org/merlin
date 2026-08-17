# Local MapGUI Fork and Delta Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Wizardry's MapGUI integration against `/home/jlo/dev/MapGUI` and verify efficient delta map updates globally.

**Architecture:** Add `/home/jlo/dev/MapGUI` as a Gradle included build and substitute the published coordinate `io.github.flog99:mapgui-api` with the included build's `:mapgui-api` project. Keep the fork's separate `mapgui-plugin` shadow jar as the runtime server plugin. Keep `MapSurface` as the sole dirty-state owner; strengthen tests and only change implementation where a failing behavioral test proves a gap.

**Tech Stack:** Gradle Kotlin DSL, Java 25, Paper 26.2, JUnit 5, MapGUI `MapSurface`/`Patches` transport.

## Global Constraints

- Use `/home/jlo/dev/MapGUI`, never the remote MapGUI artifact for integration compilation or tests.
- Substitute `io.github.flog99:mapgui-api` through the composite build; do not use an invalid cross-build `project(":mapgui-api")` dependency.
- Keep the runtime `mapgui-plugin` shadow jar separate from the API dependency.
- Preserve full synchronization for new viewers.
- Established viewers receive dirty rectangles only.
- Equal pixel writes do not produce dirty state.
- Do not add a parallel diff layer in Wizardry.

---

### Task 1: Wire the local MapGUI composite build

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `mapgui-integration/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `mapgui-integration/src/main/resources/paper-plugin.yml`

**Interfaces:**
- Produces composite substitution `io.github.flog99:mapgui-api -> /home/jlo/dev/MapGUI:mapgui-api`.
- Produces a root `runServer` dependency on `/home/jlo/dev/MapGUI:mapgui-plugin:shadowJar`.

- [ ] Add `includeBuild("../MapGUI")` with explicit dependency substitution for `io.github.flog99:mapgui-api` and, if required by the integration classpath, `io.github.flog99:mapgui-layout`.
- [ ] Keep the existing dependency coordinates in `mapgui-integration/build.gradle.kts`; the composite substitution must make them resolve locally, avoiding invalid cross-build project references.
- [ ] Make root `runServer` depend on the fork's `:mapgui-plugin:shadowJar` through the included-build task path and add its output to `pluginJars`.
- [ ] Keep the plugin descriptor's `MapGUI` dependency required and load-before ordering intact.
- [ ] Run `./gradlew :mapgui-integration:dependencies --configuration compileClasspath` and confirm the selected component is the included MapGUI project, not a downloaded Maven artifact.

### Task 2: Add regression tests for actual delta semantics

**Files:**
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/test/java/de/flog99/mapgui/MapSurfaceTest.java`
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/test/java/de/flog99/mapgui/PatchesTest.java`
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/test/java/de/flog99/mapgui/WallTilesTest.java`

**Interfaces:**
- Tests observe `MapSurface.isDirty`, `dirtyRegions`, and fake transport sent rectangles.

- [ ] Add a test that changes a pixel, restores it before sending, and assert behavior matches the documented current-pixel dirty contract.
- [ ] Add a test for two changed rows with a clean gap and assert the planner chooses separate regions when cheaper.
- [ ] Add a test proving an established viewer receives changed regions while a newly arrived viewer receives full state.
- [ ] Run only the MapGUI API test classes and capture the failure before implementation changes.

### Task 3: Fix dirty metadata if regression tests expose a gap

**Files:**
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/main/java/de/flog99/mapgui/MapSurface.java`
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/main/java/de/flog99/mapgui/Patches.java`
- Modify: `/home/jlo/dev/MapGUI/mapgui-api/src/main/java/de/flog99/mapgui/WallTiles.java`

**Interfaces:**
- Preserve public signatures of `MapSurface`, `Patches`, and transport interfaces.

- [ ] Implement only the smallest change required by the failing behavioral test.
- [ ] Ensure region bytes are copied from current pixels and dirty state is cleared only after the send pass.
- [ ] Run MapGUI API tests and WallTiles tests.

### Task 4: Verify integration and server path

**Files:**
- No source changes unless build verification identifies a wiring defect.

- [ ] Run `../MapGUI/gradlew :mapgui-plugin:shadowJar :mapgui-api:test`.
- [ ] Run `./gradlew :mapgui-integration:test :mapgui-integration:jar`.
- [ ] Run the root server smoke task with the local MapGUI shadow jar and confirm both plugins load.
- [ ] Inspect dependency output and produced jars to ensure no published MapGUI API jar is used by the integration.
