# Repository Reorganization Design

## Goal

Reorganize the repository into a clear Gradle multi-module structure with three Java modules (`api`, `common`, `paper`) and a few supporting structural cleanups.

## Scope

- Gradle modules: rename/split `java-compiler` into `api` and `common`, merge `mapgui-integration` into `paper`.
- Rename `glyph-training/` to `training/`.
- Move `conformance/` fixtures into `common/src/test/resources/conformance/`.
- Add a root `README.md`.
- Update CI, `settings.gradle.kts`, and `build.gradle.kts` to match.

## Out of scope

- Rewriting docs under `docs/superpowers/` (other than this spec and a README).
- Changing behavior of any compiler, glyph, ML, or MapGUI logic.
- Touching git worktrees under `.worktrees/`.

## 1. Top-level layout

```
wizardry/
├── api/                      # public, platform-agnostic API
├── common/                   # platform-agnostic implementations
├── paper/                    # Paper plugin (includes current mapgui-integration)
├── training/                 # renamed from glyph-training/
├── docs/
│   ├── scribe-language.md
│   └── superpowers/
├── settings.gradle.kts
├── build.gradle.kts
├── .gitignore
└── README.md                 # new
```

`conformance/` is **moved**, not deleted: its contents become `common/src/test/resources/conformance/`.

## 2. Gradle modules

`settings.gradle.kts`:

```kotlin
rootProject.name = "wizardry"
includeBuild("../MapGUI") {
    dependencySubstitution {
        substitute(module("io.github.flog99:mapgui-api")).using(project(":mapgui-api"))
        substitute(module("io.github.flog99:mapgui-layout")).using(project(":mapgui-layout"))
    }
}
include(":api")
include(":common")
include(":paper")
```

### `api` — `java-library`, Java 21

Public types and the entry-point interfaces. No Bukkit/MapGUI/Paper dependencies.

**Provisional class list** — actual placement is decided during implementation by whether a class is a public contract or an implementation helper:

- `dev.mintychochip.wizardry.api.dsl.*`
  - `Compiler` interface
  - `CompileResult`, `CompiledSpell`, `Diagnostic`, `Operation`, `Span`, `Statement`, `Program`
  - `CompilerConstants`
- `dev.mintychochip.wizardry.api.glyph.*`
  - `GlyphDraft`, `GlyphPoint`, `GlyphBitmap`, `GlyphStroke`, `GlyphLimits`, `GlyphCaptureSession`
- `dev.mintychochip.wizardry.api.ml.*`
  - `Classification`, `ClassificationCandidate`, `Label`, `GlyphClassifier` (interface), `ModelBundle`

`api` may depend on Jackson for `ModelBundle` and JUnit for tests.

### `common` — `java-library`, Java 21

Platform-agnostic implementations of the `api` contracts and support code.

- `dev.mintychochip.wizardry.common.dsl.*`
  - `ScribeCompiler` (implements `api.dsl.Compiler`)
  - `dev.mintychochip.wizardry.common.dsl.lexer.Lexer`
  - `dev.mintychochip.wizardry.common.dsl.parser.Parser`
- `dev.mintychochip.wizardry.common.glyph.*`
  - `GlyphRasterizer`
- `dev.mintychochip.wizardry.common.ml.*`
  - `GlyphPreprocessor`, `OnnxGlyphClassifier`, `PreprocessedGlyph`

`common` depends on `api` (`implementation(project(":api"))`), Jackson, ONNX Runtime, and JUnit.

### `paper` — `java`, Java 25

Current `paper/` and current `mapgui-integration/` merged into one module.

- Java toolchain 25.
- `paper-api:26.2.build.84-stable` (the target the MapGUI integration already uses).
- `dev.mintychochip.wizardry.paper.*`
  - existing `command`, `dialog`, `book`, `runtime`, `listener`, `WizardryPlugin`
- `dev.mintychochip.wizardry.paper.mapgui.*`
  - current mapgui classes (`GlyphScreen`, `GlyphStrokeTracker`, etc.)

`paper` depends on `api` and `common` as `implementation`, plus `mapgui-api/layout`, Paper API, ONNX, Mockito, and JUnit.

## 3. Package and dependency rules

- Packages reflect module name: `api.*`, `common.*`, `paper.*`.
- Inside `paper`, mapgui classes move to `...paper.mapgui.*` (not a separate module).
- Inside `common`, domain split is `common.dsl`, `common.glyph`, `common.ml`.
- `common` depends on `api`; `api` does not depend on `common`. This avoids a circular dependency between the `Compiler` interface, the data types it returns, and the `ScribeCompiler`/lexer/parser implementation.
- `paper` depends on both `api` and `common`.

## 4. Conformance corpus

Move `conformance/schema-v1.json` and `conformance/fixtures/*.json` to `common/src/test/resources/conformance/`. Update `ConformanceTest` to load fixtures from the `common` module's test resources (e.g., `getClass().getResourceAsStream(...)` or a path relative to the module's test resource root).

## 5. Build and CI updates

### `build.gradle.kts`

- Keep the `allprojects` group/version block.
- In `runServer`, remove `:mapgui-integration:jar` from dependencies and keep only `:paper:jar` and the MapGUI shadow jar.

### `.github/workflows/ci.yml`

Keep the current Java-only steps:

- `./gradlew clean test`
- `./gradlew :paper:jar`

If the build graph changes, the workflow is updated to reference the new module names (`:api:test`, `:common:test`, `:paper:test`, etc.).

## 6. Other structural changes

- Rename `glyph-training/` to `training/`.
- Add root `README.md` describing the module layout and build commands.
- Remove `mapgui-integration/` as a separate module (merged into `paper`).
- Update `.gitignore` to remove any stale `mapgui-integration` or Rust-specific entries.

## 7. Verification plan

- `./gradlew clean test` passes for `api`, `common`, and `paper`.
- `./gradlew :paper:jar` builds a valid plugin jar.
- No `mapgui-integration/` or `java-compiler/` directory remains at the top level.
- All Java source packages are under the new module-root packages (`api.*`, `common.*`, `paper.*`).
- Conformance fixtures are reachable from `common` tests.
- Root `README.md` is created.

## 8. Open questions for implementation

- Exact final package names for any class that straddles public/implementation boundaries (e.g., `ModelBundle`, `CompilerConstants`, `GlyphCaptureSession`). The rule is: public contracts go to `api`, implementation helpers go to `common`.
- Whether `paper.mapgui` should be renamed to `paper.glyphmap` or similar. Default is `paper.mapgui` to keep the domain name.
