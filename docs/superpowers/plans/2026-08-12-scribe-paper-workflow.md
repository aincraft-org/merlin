# Scribe Paper Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Scribe compiler-to-Paper workflow on the current Paper API using a reliable chat editor fallback and atomic Save & Cast execution.

**Architecture:** Keep the Java compiler dependency-free. Add a Paper-side editor/casting service around `ScribeDialog` and `SpellRuntime`; the listener captures right-click and active editor chat, while `/scribe` owns explicit save/cast/cancel commands. No compile-time Dialog API dependency is added because the current Paper API does not expose those classes.

**Tech Stack:** Java 21, Gradle, Paper API 1.21.8, JUnit 5, Bukkit event/command APIs, PDC-backed ItemStack state.

## Global Constraints

- Preserve the documented Scribe grammar and semantic bounds.
- Save & Cast persists the submitted draft even when compilation rejects it; rejected compilation never invokes runtime or mutates cooldown.
- Source limits are 4,096 Unicode scalars and 16,384 UTF-8 bytes, inclusive; editor input also respects 128 physical lines.
- Sessions are one-use, player-bound, exact-book-bound, and expire after 15 minutes.
- Do not import unavailable Paper Dialog API classes.
- Ordinary books and ordinary chat retain vanilla behavior.

---

### Task 1: Fix compiler UTF-8 limit diagnostics

**Files:**
- Modify: `java-compiler/src/main/java/dev/mintychochip/wizardry/scribe/ScribeCompiler.java`
- Test: `java-compiler/src/test/java/dev/mintychochip/wizardry/scribe/CompilerTest.java`

**Interfaces:** Preserve `ScribeCompiler.compile(String)`; add no public API.

- [ ] **Step 1: Write failing tests**

Add tests compiling `"é".repeat(8193)` and an emoji source whose UTF-8 bytes exceed 16,384 while UTF-16 length is below 16,384. Assert rejection with the source-limit diagnostic, no exception, and a span on a valid UTF-8 boundary.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :java-compiler:test --tests '*CompilerTest*'`; expected failure from the current byte/UTF-16 index mismatch.

- [ ] **Step 3: Implement the boundary fix**

Walk code points, accumulate UTF-8 widths, and return the UTF-16 index of the first code-point boundary at or beyond the byte limit. Feed that index to the existing UTF-8 offset helper.

- [ ] **Step 4: Re-run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/mintychochip/wizardry/scribe/ScribeCompiler.java java-compiler/src/test/java/dev/mintychochip/wizardry/scribe/CompilerTest.java
git commit -m "fix: report multibyte scribe source limits safely"
```

### Task 2: Make dialog submission compile-before-cast and runtime-aware

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialog.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntime.java`
- Test: `paper/src/test/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialogStateTest.java`
- Create: `paper/src/test/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntimeTest.java`

**Interfaces:** `ScribeDialog` receives a cast callback/service; `SpellRuntime.cast(Player, LivingEntity, CompiledSpell, long)` remains the execution entry point.

- [ ] **Step 1: Write failing state tests**

Test invalid Save & Cast returns `persisted=true`, `cast=false`, `reopen=true`, includes diagnostics, leaves the submitted draft persisted, and does not call the cast callback. Test accepted Save & Cast calls the callback exactly once after persistence. Test `now == expiresAtMillis` rejects.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :paper:test --tests '*ScribeDialogStateTest*'`; expected failure on callback and expiry behavior.

- [ ] **Step 3: Implement submission flow**

For Save & Cast, validate, compile, save the draft regardless of acceptance, and invoke the cast callback only for accepted compilation. Save remains non-compiling. Preserve session state for reopen where needed and enforce exact book/player/expiry checks.

- [ ] **Step 4: Implement runtime safety**

Preflight operations and cooldown before effects. Permit targetless self-only spells; require a valid target for target operations. Avoid non-finite zero-vector pushes. Set cooldown only after effects complete.

- [ ] **Step 5: Add runtime tests**

Cover self-only cast without target, cooldown block/expiry, and zero-length push safety using existing test dependencies or small local doubles.

- [ ] **Step 6: Run focused tests**

Run `./gradlew :paper:test --tests '*ScribeDialogStateTest*' --tests '*SpellRuntimeTest*'`; expected PASS.

- [ ] **Step 7: Commit**

```bash
git add paper/src/main/java paper/src/test/java
git commit -m "fix: make scribe save and cast execution atomic"
```

### Task 3: Wire reliable chat editor and command routing

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialog.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/listener/ScribeBookListener.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/command/ScribeCommand.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/listener/ScribeChatListener.java`
- Test: `paper/src/test/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialogStateTest.java`

**Interfaces:** `/scribe save`, `/scribe cast`, `/scribe cancel` submit the active session; chat is consumed only while a player session exists.

- [ ] **Step 1: Write failing routing tests**

Test right-click session creation, active-player chat consumption, Save persistence, Cast callback invocation, Cancel non-mutation, and permission denial for `/scribe book` and `/scribe cast`.

- [ ] **Step 2: Run focused tests**

Run `./gradlew :paper:test --tests '*ScribeDialogStateTest*'`; expected failure because production routing is absent.

- [ ] **Step 3: Implement chat editor protocol**

Right-click sends current source and command instructions. Active chat replaces the pending source and cancels chat. `/scribe begin` starts editing; `/scribe save`, `/scribe cast`, and `/scribe cancel` submit or discard. Enforce scalar, UTF-8, and physical-line limits.

- [ ] **Step 4: Implement command actions**

Add command handling and enforce `sender.hasPermission("wizardry.scribe.book")`. Use the active exact-book session for editor actions; keep direct held-book cast using the same compile/runtime service.

- [ ] **Step 5: Register listeners and callback**

Construct the dialog with a plugin callback that selects a target only when required and delegates to `SpellRuntime`. Register interaction and chat listeners. Do not reference Dialog API classes.

- [ ] **Step 6: Run Paper tests**

Run `./gradlew :paper:test`; expected PASS.

- [ ] **Step 7: Commit**

```bash
git add paper/src/main/java paper/src/test/java
git commit -m "feat: wire scribe chat editor to Paper runtime"
```

### Task 4: Update contract documentation and verify end to end

**Files:** Modify `docs/scribe-language.md`; use existing Gradle tasks for verification.

- [ ] **Step 1: Document the actual fallback**

Describe right-click chat editing, `/scribe begin`, `/scribe save`, `/scribe cast`, `/scribe cancel`, and the rule that rejected Save & Cast persists the draft but does not execute or alter cooldown. State native Dialog API support remains optional for a future Paper target exposing it.

- [ ] **Step 2: Run complete verification**

Run `./gradlew :java-compiler:test :paper:test` and `./gradlew :paper:build`; expected `BUILD SUCCESSFUL` with focused behavioral tests passing.

- [ ] **Step 3: Inspect artifact**

Confirm `paper/build/libs` contains the plugin jar with updated editor/listener classes and `paper-plugin.yml`.

- [ ] **Step 4: Commit**

```bash
git add docs/scribe-language.md
git commit -m "docs: document scribe paper editor workflow"
```
