# CI Coverage Hardening — Design Spec

## Goal

Close the verified CI blind spots so every tested subsystem is guarded in `ci.yml`. Currently CI runs Rust and the full Gradle suite (which already covers java-compiler, paper, and the MapGUI integration module via per-module toolchains), but **not** the Python glyph-training pipeline, and it does not pin test counts so a silently-dropped test class goes unnoticed. This spec adds the Python gate, makes the mixed-toolchain Java topology explicit, asserts pinned baseline counts, and documents how both JDKs are provisioned.

Links: living spec `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md` (verified baselines table).

## Scope

- Add the Python glyph-training suite to CI.
- Make the Java toolchain topology explicit: `java-compiler` and `paper` target Java 21, `mapgui-integration` targets Java 25; all are already exercised by the root `./gradlew clean test` because Gradle resolves each module's toolchain independently.
- Provision **both JDK 21 and JDK 25** so the toolchain resolver finds them, without splitting into per-JDK jobs.
- Assert pinned baseline test counts per subsystem so a dropped test suite/class fails CI.
- Keep the existing Rust gates green.

## Current CI (verified `ci.yml`)

Jobs (single `verify`): checkout, rust toolchain, setup-java **21**, then:
- `cargo fmt --all --check`
- `cargo test -- --nocapture`
- `cargo clippy --all-targets --all-features -- -D warnings`
- `cargo run --bin generate_conformance -- --check`
- `./gradlew clean test`
- `./gradlew :paper:jar`

### Toolchain topology (verified build.gradle.kts)

- `java-compiler` and `paper`: Java 21 toolchain, `options.release=21` (java-compiler); `paper-api:1.21.8`.
- `mapgui-integration`: Java **25** toolchain, `compileOnly mapgui-api/layout`, `compileOnly paper-api:26.2`, `implementation(project(":java-compiler"))`, ONNX runtime.
- Gradle resolves **each module's toolchain independently**, so `./gradlew clean test` already compiles and tests all three modules — including MapGUI — **provided both JDK 21 and JDK 25 are installed**. It does not need a Java-25-exclusive job.

**Gaps:** Python pipeline absent from CI; CI installs only JDK 21 (so the Java 25 toolchain would not resolve MapGUI on a fresh runner); no pinned test-count assertions.

## Additions

### 1. Python glyph-training gate (new job or step)

```text
steps:
  - uses: actions/setup-python@v5
    with: python-version: '3.12'
  - run: cd glyph-training && python -m pip install --upgrade pip uv
  - run: cd glyph-training && uv sync --extra test --python 3.12
  - name: Python suite
    run: cd glyph-training && uv run --python 3.12 --extra test python -m pytest -q
```

Expected baseline: **73 tests pass**. Assert the collected count ≥ the pinned baseline via a `pytest -q` summary check so a dropped test file is caught.

### 2. Java toolchain provisioning + explicit MapGUI gate

`mapgui-integration` is already exercised by the root `./gradlew clean test` (verified `ci.yml`), but only if the Java 25 toolchain is installed. The current CI installs only JDK 21, so on a fresh runner the Java 25 toolchain would not resolve and MapGUI is silently untested. Two changes:

**A. Keep the root Gradle job whole; provision both JDKs in it.** Do **not** split into separate Java-21 and Java-25 jobs — Gradle resolves each module's toolchain independently, so one job with both JDKs installed and `setup-java` caching both is simpler and matches the existing `./gradlew clean test`:

```text
- uses: actions/setup-java@v4
  with: distribution: temurin, java-version: '21'
- uses: actions/setup-java@v4
  with: distribution: temurin, java-version: '25'
- name: Full Gradle suite (java-compiler 21, paper 21, mapgui 25)
  run: ./gradlew clean test
- name: Explicit MapGUI integration tests
  run: ./gradlew :mapgui-integration:test --rerun-tasks
```

The explicit `:mapgui-integration:test --rerun-tasks` step is **not redundant**: it runs the MapGUI suite on its own so its report/junit count is a discrete gate that does not depend on the aggregate root run, giving the pinned-count assertion a stable, isolated input.

**Expected baseline:** 12 tests pass (screen model, stroke tracker, draft-store adapter, classification service, ONNX packaging). Note MapGUI tests exercise the drawing model and ONNX packaging without a live server; the live Paper/MapGUI smoke test remains manual (environment has no server/client session — see design `docs/superpowers/specs/2026-08-12-glyph-mapgui-integration-design.md`).

**B. Fallback if only one JDK is available.** If the runner cannot host both JDKs, split the Gradle job into two matrixed jobs: `java-version: 21` running `:java-compiler:test :paper:test`, and `java-version: 25` running `:mapgui-integration:test`. This split is acceptable only where dual-JDK provisioning is impossible; the single-job dual-JDK topology is preferred and matches the verified per-module toolchains.

### 3. Pinned baseline assertions

Add a lightweight parked-assertion so a regression that silently drops tests breaks CI:

- Rust: expect ≥ 16 lib+conformance tests (15 lib + 1 conformance).
- Java: ≥ 25 tests across `java-compiler`.
- MapGUI: ≥ 12 tests.
- Paper: ≥ 2 tests.
- Python: ≥ 73 tests.

Implement as CI steps that parse `*.xml` `tests=`/`pytest -q` counts and fail below the floor. This converts the living spec's pinned baselines into automatic gate.

## Verification

- Full `ci.yml` + new gates lint (`actionlint` if available locally) and the exact commands pass:
  - `cargo fmt --all --check`, `cargo test`, `cargo clippy -D warnings`, `generate_conformance --check` (existing, unchanged).
  - `./gradlew clean test` with **both JDK 21 and JDK 25** installed (dual-JDK single job), or the fallback matrix: `:java-compiler:test :paper:test` under JDK 21 and `:mapgui-integration:test` under JDK 25 if dual-JDK provisioning is impossible.
  - `./gradlew :mapgui-integration:test --rerun-tasks` (isolated MapGUI count gate) → 12 tests.
  - `cd glyph-training && uv run --python 3.12 --extra test python -m pytest -q` → 73 passed.
- Assert dropping a test class/file triggers the pinned-count failure.
- Local proof mirrors CI commands exactly; job-level yaml is validated via a local `actionlint` run if present.

## Interfaces produced (for later specs)

- Extended `ci.yml` with a Python gate, dual-JDK provisioning for the single Gradle job (or fallback matrix), and pinned baseline assertions.

Consumed by the equivalence spec (which adds an end-to-end cross-front-end test that CI must include).