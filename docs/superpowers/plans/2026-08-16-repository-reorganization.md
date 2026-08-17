# Repository Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Reorganize the repository into three Gradle modules (`api`, `common`, `paper`), merge `mapgui-integration` into `paper`, rename `glyph-training` to `training`, and clean up build/CI accordingly.

**Architecture:** Split the current `java-compiler` into a public surface (`api`) and an implementation module (`common`), keep `paper` as the only Paper/MapGUI front-end plugin, and move non-source directories (`training/`, `conformance/` test resources) to clearer locations.

**Tech Stack:** Gradle Kotlin DSL, Java 21 (`api`/`common`), Java 25 (`paper`), Paper API `26.2.build.84-stable`, MapGUI API `1.0.0`, ONNX Runtime, Jackson, JUnit 5.

## Global Constraints

- `api` must contain only public, platform-agnostic types and interfaces; no Bukkit/MapGUI/Paper dependencies.
- `common` depends on `api` and contains all platform-agnostic implementations; `api` must not depend on `common` (avoids circular dependency).
- `paper` depends on both `api` and `common` and contains all platform-specific (Paper/MapGUI) code in a single module.
- Java packages must reflect the module root: `dev.mintychochip.wizardry.api.*`, `dev.mintychochip.wizardry.common.*`, `dev.mintychochip.wizardry.paper.*`.
- `paper` uses Java 25 and `paper-api:26.2.build.84-stable` to satisfy both the existing Paper and MapGUI code.
- `conformance/` fixtures move to `common/src/test/resources/conformance/`.
- `glyph-training/` renames to `training/`.
- Behavior of every existing class is preserved; only package and module locations change.

---

## Pre-Flight: Source Inventory

Before moving files, produce a definitive `api` vs `common` class list by walking `java-compiler/src/main/java/dev/mintychochip/wizardry/`.

Rule of thumb:
- **Public contract** → `api`: records, enums, public constants, public interfaces, and classes imported by `paper` or `mapgui` for their public API.
- **Implementation helper** → `common`: lexer, parser, static utilities, ONNX runtime code, rasterizer, preprocessor.

Expected provisional split (confirm during implementation):

| Package | `api` public types | `common` implementation |
|---|---|---|
| `dsl` | `Compiler`, `CompileResult`, `CompiledSpell`, `Diagnostic`, `Operation`, `Span`, `Statement`, `Program`, `CompilerConstants` | `ScribeCompiler`, `lexer.Lexer`, `parser.Parser` |
| `glyph` | `GlyphDraft`, `GlyphPoint`, `GlyphBitmap`, `GlyphStroke`, `GlyphLimits`, `GlyphCaptureSession` | `GlyphRasterizer` |
| `ml` | `Classification`, `ClassificationCandidate`, `Label`, `GlyphClassifier` (interface), `ModelBundle` | `GlyphPreprocessor`, `OnnxGlyphClassifier`, `PreprocessedGlyph` |

**Files:**
- Read: `java-compiler/src/main/java/dev/mintychochip/wizardry/**/*.java`
- Update: this plan's class list above if any class straddles the boundary.

---

### Task 1: Scaffold `api/` and `common/` and Move Conformance

**Files:**
- Create: `api/build.gradle.kts`
- Create: `common/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `common/src/test/resources/conformance/schema-v1.json`
- Create: `common/src/test/resources/conformance/fixtures/*.json` (from `conformance/fixtures/`)
- Modify: `.gitignore`
- Delete: `conformance/` (after copy)

**Interfaces:**
- Consumes: existing `java-compiler` module.
- Produces: empty `api/` and `common/` Gradle modules with correct dependencies; `common` test resource root populated with fixtures.

- [x] **Step 1: Create `api/build.gradle.kts`**

```kotlin
plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

- [x] **Step 2: Create `common/build.gradle.kts`**

```kotlin
plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    implementation(project(":api"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

- [x] **Step 3: Update `settings.gradle.kts`**

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

- [x] **Step 4: Copy conformance fixtures into `common` test resources**

Run:

```bash
mkdir -p common/src/test/resources/conformance/fixtures
cp conformance/schema-v1.json common/src/test/resources/conformance/
cp conformance/fixtures/*.json common/src/test/resources/conformance/fixtures/
```

- [x] **Step 5: Delete the old `conformance/` directory and update `.gitignore` if needed**

```bash
rm -rf conformance
```

- [x] **Step 6: Verify the build still configures**

Run: `./gradlew projects`
Expected: `api`, `common`, `paper` are listed; `java-compiler` and `mapgui-integration` are not.

---

### Task 2: Split `java-compiler` into `api` and `common`

**Files:**
- Move: all current `java-compiler/src/main/java/dev/mintychochip/wizardry/<domain>/*.java`
- Move: all current `java-compiler/src/test/java/dev/mintychochip/wizardry/<domain>/*.java`
- Delete: `java-compiler/` after move

**Interfaces:**
- Consumes: the source inventory from the pre-flight step.
- Produces: `api/` and `common/` modules with the correct package roots.

- [x] **Step 1: Move public types to `api` packages**

For each public class, move it from `java-compiler/src/main/java/dev/mintychochip/wizardry/<domain>/Class.java` to `api/src/main/java/dev/mintychochip/wizardry/api/<domain>/Class.java` and change the package declaration:

```java
package dev.mintychochip.wizardry.api.<domain>;
```

Where `<domain>` is `dsl`, `glyph`, or `ml`.

- [x] **Step 2: Move implementation classes to `common` packages**

For each implementation class, move it to `common/src/main/java/dev/mintychochip/wizardry/common/<domain>/Class.java` and change the package declaration:

```java
package dev.mintychochip.wizardry.common.<domain>;
```

- [x] **Step 3: Move tests to `common`**

Move `java-compiler/src/test/java/dev/mintychochip/wizardry/**/*Test.java` to `common/src/test/java/dev/mintychochip/wizardry/common/<domain>/` and update package declarations and imports.

- [x] **Step 4: Update imports across `api` and `common`**

Change all `import dev.mintychochip.wizardry.<domain>` references to `import dev.mintychochip.wizardry.api.<domain>` or `import dev.mintychochip.wizardry.common.<domain>` as appropriate.

A ready-to-run Python script performs the mechanical package renames and directory moves using an explicit class→package mapping:

```bash
python3 scripts/reorg-api-common.py
```

The script is `scripts/reorg-api-common.py`. It:
1. Moves every `java-compiler` main source file to `api/...` or `common/...` according to the source-inventory table.
2. Moves every `java-compiler` test file to `common/src/test/java/dev/mintychochip/wizardry/common/...`.
3. Rewrites each file's `package` declaration from the new file path.
4. Rewrites each import by looking up the target class in an explicit table, and expands old wildcard imports into the appropriate `api`/`common` wildcard imports without causing name clashes.

**Caution:** After running the script the result must be audited:
- Every class must end up in the package declared at the top of its file.
- Every import must point to an existing class in `api` or `common`.
- `common` source must import public types from `api` (`dev.mintychochip.wizardry.api.*`) and internal types from `common` (`dev.mintychochip.wizardry.common.*`).
- `api` source must not import anything from `common`.

Do **not** use a blanket `s/scribe/api.dsl/` or `s/api/common/` text replacement; imports in `common` may reference both modules.

Run `./gradlew :api:compileJava :common:compileJava` after the audit and fix any import/package mismatches.

- [x] **Step 5: Delete `java-compiler/`**

```bash
rm -rf java-compiler
```

- [x] **Step 6: Verify `api` and `common` compile and tests pass**

Run:

```bash
./gradlew :api:compileJava :common:compileJava :common:test
```

Expected: BUILD SUCCESSFUL.

---

### Task 3: Create `Compiler` Interface and Wire `ScribeCompiler`

**Files:**
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Compiler.java`
- Modify: `common/src/main/java/dev/mintychochip/wizardry/common/dsl/ScribeCompiler.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java` (after it is created/merged)

**Interfaces:**
- Consumes: `api.dsl.CompileResult`
- Produces: a public `Compiler` interface and a `ScribeCompiler` implementation that `paper` can use.

- [x] **Step 1: Define `Compiler` interface in `api`**

```java
package dev.mintychochip.wizardry.api.dsl;

public interface Compiler {
    CompileResult compile(String source);
}
```

- [x] **Step 2: Make `ScribeCompiler` a singleton implementing `Compiler`**

In `common/src/main/java/dev/mintychochip/wizardry/common/dsl/ScribeCompiler.java`:

```java
package dev.mintychochip.wizardry.common.dsl;

import dev.mintychochip.wizardry.api.dsl.Compiler;
import dev.mintychochip.wizardry.api.dsl.CompilerConstants;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Operation;
import dev.mintychochip.wizardry.api.dsl.Program;
import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.api.dsl.Statement;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ScribeCompiler implements Compiler {
    public static final ScribeCompiler INSTANCE = new ScribeCompiler();

    private ScribeCompiler() {}

    @Override
    public CompileResult compile(String source) {
        var limitDiagnostics = sourceLimitDiagnostics(source);
        if (!limitDiagnostics.isEmpty()) return CompileResult.rejected(limitDiagnostics);
        var lexed = Lexer.lex(source);
        if (!lexed.diagnostics().isEmpty()) return CompileResult.rejected(cap(lexed.diagnostics()));
        var parsed = Parser.parse(lexed.tokens(), source);
        if (parsed.program().isEmpty()) return CompileResult.rejected(cap(parsed.diagnostics()));
        var validation = validate(parsed.program().orElseThrow(), source);
        if (!validation.isEmpty()) return CompileResult.rejected(cap(validation));
        var program = parsed.program().orElseThrow();
        byte[] canonical = canonicalize(program);
        return CompileResult.accepted(new CompiledSpell(
                CompilerConstants.COMPILER_VERSION, program.name(), sha256(canonical), canonical, operations(program)));
    }

    // Keep every remaining method from the original file exactly as-is, with their
    // existing `private static` modifiers; do not convert them to instance methods.
}
```

Remove the old `public static CompileResult compile(String source)` method and move its body into the instance `compile` method above. The class is stateless, so a single `INSTANCE` is sufficient for all callers.

- [x] **Step 3: Run compiler tests**

```bash
./gradlew :common:test
```

Expected: BUILD SUCCESSFUL.

---

### Task 4: Merge `mapgui-integration` into `paper`

**Files:**
- Move: `mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/*.java` → `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/*.java`
- Move: `mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/*.java` → `paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/*.java`
- Move: `mapgui-integration/src/main/resources/*` → `paper/src/main/resources/`
- Modify: `paper/build.gradle.kts`
- Modify: `paper/src/main/resources/paper-plugin.yml`
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`
- Delete: `mapgui-integration/` after move

**Interfaces:**
- Consumes: `api.glyph.*`, `api.ml.*`, `common.ml.*`, `common.glyph.*`.
- Produces: a single `paper` module that produces one plugin jar with one `paper-plugin.yml`.

- [x] **Step 1: Move mapgui Java sources into `paper.mapgui` package**

```bash
mkdir -p paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui
mv mapgui-integration/src/main/java/dev/mintychochip/wizardry/mapgui/*.java paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/
```

Update package declarations:

```java
package dev.mintychochip.wizardry.paper.mapgui;
```

- [x] **Step 2: Move mapgui test sources**

```bash
mkdir -p paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui
mv mapgui-integration/src/test/java/dev/mintychochip/wizardry/mapgui/*.java paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/
```

- [x] **Step 3: Extract `GlyphCommand` from `GlyphMapGuiPlugin` and delete `GlyphMapGuiPlugin`**

Create `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java` containing the command logic from `GlyphMapGuiPlugin.GlyphCommand`. Update references to use the new package names (`api.glyph.GlyphDraft`, `common.ml.*`, etc.).

Delete `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphMapGuiPlugin.java`.

- [x] **Step 4: Update `WizardryPlugin` to register glyph map features**

In `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`:

- Add fields: `GlyphDraftStoreAdapter store`, `GlyphMapSaveAction mapSaveAction`, `GlyphClassificationService classificationService`.
- In `onEnable`, initialize them, register `GlyphMapRehydrationListener`, and register the `glyph` command using `GlyphCommand`.
- In `onDisable`, close `classificationService`.

- [x] **Step 5: Unify `paper-plugin.yml`**

Write `paper/src/main/resources/paper-plugin.yml`:

```yaml
name: Wizardry
version: 1.0.0-SNAPSHOT
main: dev.mintychochip.wizardry.paper.WizardryPlugin
loader: dev.mintychochip.wizardry.paper.mapgui.GlyphPluginLoader
api-version: '1.21'
author: jlo
dependencies:
  server:
    MapGUI:
      load: BEFORE
      required: true
      join-classpath: true
permissions:
  wizardry.scribe.book:
    description: Allows creating and using Scribe books.
    default: true
  wizardry.glyph.draw:
    description: Allows drawing and saving glyph maps.
    default: true
```

Delete `mapgui-integration/src/main/resources/paper-plugin.yml`.

- [x] **Step 6: Update `paper/build.gradle.kts`**

```kotlin
plugins {
    java
}

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    compileOnly("io.github.flog99:mapgui-api:1.0.0")
    compileOnly("io.github.flog99:mapgui-layout:1.0.0")
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("io.github.flog99:mapgui-api:1.0.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.jar)
}

tasks.jar {
    from(project(":api").sourceSets.main.get().output)
    from(project(":common").sourceSets.main.get().output)
}
```

- [x] **Step 7: Delete `mapgui-integration/`**

```bash
rm -rf mapgui-integration
```

- [x] **Step 8: Run paper tests and jar build**

```bash
./gradlew :paper:test :paper:jar
```

Expected: BUILD SUCCESSFUL.

---

### Task 5: Update `paper` Sources for New Package Names

**Files:**
- Modify: all `paper/src/main/java/dev/mintychochip/wizardry/paper/**/*.java`
- Modify: all `paper/src/test/java/dev/mintychochip/wizardry/paper/**/*.java`

**Interfaces:**
- Consumes: new `api.*` and `common.*` packages.
- Produces: `paper` code that imports the new public and implementation packages.

- [x] **Step 1: Update Scribe imports in `paper`**

Replace:
- `dev.mintychochip.wizardry.scribe.ScribeCompiler` → `dev.mintychochip.wizardry.common.dsl.ScribeCompiler` and change call sites from `ScribeCompiler.compile(source)` to `ScribeCompiler.INSTANCE.compile(source)` (or use `dev.mintychochip.wizardry.api.dsl.Compiler compiler = ScribeCompiler.INSTANCE; compiler.compile(source)`)
- `dev.mintychochip.wizardry.scribe.model.*` → `dev.mintychochip.wizardry.api.dsl.*`
- `dev.mintychochip.wizardry.scribe.*` → `dev.mintychochip.wizardry.common.dsl.*` or `dev.mintychochip.wizardry.api.dsl.*`

- [x] **Step 2: Update glyph and ML imports in `paper`**

Replace:
- `dev.mintychochip.wizardry.glyph.*` → `dev.mintychochip.wizardry.api.glyph.*`
- `dev.mintychochip.wizardry.ml.*` → `dev.mintychochip.wizardry.api.ml.*` (for public types) or `dev.mintychochip.wizardry.common.ml.*` (for `OnnxGlyphClassifier`, `GlyphPreprocessor`)

- [x] **Step 3: Update MapGUI package and imports**

All former `dev.mintychochip.wizardry.mapgui.*` classes are now `dev.mintychochip.wizardry.paper.mapgui.*` and update their imports accordingly.

- [x] **Step 4: Update `OnnxRuntimePackagingTest`**

In `paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/OnnxRuntimePackagingTest.java`, update the loader assertion:

```java
assertTrue(descriptor.lines().anyMatch(line -> line.equals("loader: dev.mintychochip.wizardry.paper.mapgui.GlyphPluginLoader")));
```

- [x] **Step 5: Run paper tests**

```bash
./gradlew :paper:test
```

Expected: BUILD SUCCESSFUL.

---

### Task 6: Update Root Build, CI, and Settings

**Files:**
- Modify: `build.gradle.kts`
- Modify: `.github/workflows/ci.yml`
- Verify: `settings.gradle.kts`

**Interfaces:**
- Consumes: new module names.
- Produces: a working root build and CI that runs the reorganized modules.

- [x] **Step 1: Update `build.gradle.kts`**

Change `runServer` to depend only on `:paper:jar`:

```kotlin
tasks {
    runServer {
        minecraftVersion("26.2")
        dependsOn(":paper:jar", gradle.includedBuild("MapGUI").task(":mapgui-plugin:shadowJar"))
        doFirst {
            delete(fileTree("run/plugins") {
                include("MapGUI-*.jar")
            })
        }
        pluginJars.from(
            project(":paper").tasks.named("jar"),
            layout.projectDirectory.file("../MapGUI/mapgui-plugin/build/libs/MapGUI-1.0.0-SNAPSHOT.jar")
        )
    }
}
```

- [x] **Step 2: Update `.github/workflows/ci.yml`**

Ensure the workflow still runs:

```yaml
      - name: Java and Paper tests
        run: ./gradlew clean test
      - name: Paper jar
        run: ./gradlew :paper:jar
```

- [x] **Step 3: Verify `settings.gradle.kts` does not include `java-compiler` or `mapgui-integration`**

- [x] **Step 4: Run the full build**

```bash
./gradlew clean test :paper:jar
```

Expected: BUILD SUCCESSFUL.

---

### Task 7: Rename `glyph-training` to `training`

**Files:**
- Move: `glyph-training/` → `training/`
- Modify: `.gitignore`
- Modify: CI if needed (currently not exercised)

**Interfaces:**
- Consumes: existing `glyph-training/` directory.
- Produces: `training/` directory with the same Python project contents.

- [x] **Step 1: Rename directory**

```bash
git mv glyph-training training
```

(If `git mv` is not possible because of unstaged changes, use `mv` and then `git add -A`.)

- [x] **Step 2: Update `.gitignore` references**

Replace `glyph-training/.venv/`, `glyph-training/build/`, etc. with `training/.venv/`, `training/build/`, etc.

- [x] **Step 3: Update any internal paths in training scripts**

Search for `glyph-training` in `training/` and replace with `training`.

Run:

```bash
grep -R "glyph-training" training/ || echo "no stale references"
```

---

### Task 8: Add `README.md` and Final Verification

**Files:**
- Create: `README.md`
- Modify: `docs/scribe-language.md` if any stale Rust or module references remain

**Interfaces:**
- Consumes: final module layout.
- Produces: user-facing documentation and verified build.

- [x] **Step 1: Create `README.md`**

```markdown
# Wizardry

A Paper plugin for the Wizardry glyph/spell system.

## Modules

- `api/` — public, platform-agnostic API (`dev.mintychochip.wizardry.api.*`)
- `common/` — platform-agnostic implementations (`dev.mintychochip.wizardry.common.*`)
- `paper/` — Paper plugin, including the MapGUI glyph drawing integration (`dev.mintychochip.wizardry.paper.*`)
- `training/` — Python glyph-training and model pipeline
- `docs/` — user docs and design specs

## Build

```bash
./gradlew clean test
./gradlew :paper:jar
```

## Requirements

- JDK 21 and JDK 25 (Gradle resolves the correct toolchain per module)
- MapGUI included build at `../MapGUI`
```

- [x] **Step 2: Final verification checklist**

Run and confirm each:

```bash
./gradlew clean test
./gradlew :paper:jar
ls api/ common/ paper/ training/
test ! -d java-compiler
test ! -d mapgui-integration
grep -R "dev\.mintychochip\.wizardry\.mapgui" . --include="*.java" || echo "no stale mapgui package references"
```

Expected: all commands succeed; no stale `mapgui` package references remain.

---

## Self-Review Checklist

- [x] Every class from the pre-flight inventory is in `api` or `common`.
- [x] `api` has zero `common` or platform dependencies.
- [x] `paper` has one `paper-plugin.yml` with merged `MapGUI` dependency and permissions.
- [x] `WizardryPlugin` registers both Scribe and glyph map features.
- [x] `conformance` fixtures are in `common/src/test/resources/conformance/`.
- [x] `glyph-training` is renamed to `training`.
- [x] `./gradlew clean test :paper:jar` passes.
