# Glyph Machine-Learning Classifier Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Train and integrate a machine-learning classifier that labels one drawn glyph from its raster shape and ordered vector strokes, with explicit safe rejection.

**Architecture:** Keep `GlyphDraft` as the authoritative input. Build a versioned JSONL dataset and deterministic Python training package for a compact vector-first/raster-fused model, export ONNX, and load the validated model through a Java 21 classifier seam. Training is blocked until reviewed canonical seeds exist; production classification never falls back to template matching.

**Tech Stack:** Java 21, Gradle 9.6.1, JUnit 5, Jackson 2.18.3, Python 3.12, PyTorch, ONNX, ONNX Runtime Java CPU `com.microsoft.onnxruntime:onnxruntime:1.29.0`, pytest.

## Global Constraints

- Classify one glyph only; do not interpret layouts or construct enchantment graphs.
- Initial labels are `target-ray`, `damage`, `heal`, `push`, `cooldown`, `self`, `target`, `physical`, `fire`, `frost`, `arcane`, plus `reject`.
- Preserve ordered points and stroke boundaries; timestamps are provenance, not model features.
- Offline Python owns training and export; the server never executes Python or trains online.
- Runtime targets Java 21 and CPU inference.
- Package ONNX Runtime with the `mapgui-integration` Paper plugin through a custom Paper `PluginLoader` and `MavenLibraryResolver`. The loader resolves the exact upstream CPU JAR from `MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR`; ONNX Runtime then performs its own native extraction. Do not unpack, merge, relocate, or shade ONNX classes/native resources into the plugin JAR.
- Missing, corrupt, incompatible, failed, or uncertain inference returns typed rejection; no template fallback.
- Do not claim or run model training until the data gate in Task 3 passes.
- Never collect player drawings or identity implicitly.

---

### Task 1: Define the classifier catalog and dataset schema

**Files:**
- Create: `glyph-training/pyproject.toml`
- Create: `glyph-training/src/wizardry_glyphs/__init__.py`
- Create: `glyph-training/src/wizardry_glyphs/schema.py`
- Create: `glyph-training/tests/test_schema.py`
- Create: `glyph-training/catalog-v1.json`
- Create: `glyph-training/dataset/schema-v1.json`
- Modify: `.gitignore`

**Interfaces:**
- Produces: `GlyphExample`, `GlyphStrokeData`, `GlyphPointData`, `load_examples(path: Path) -> list[GlyphExample]`.
- Produces: catalog version `glyph-catalog-v1` with twelve ordered labels, where `reject` is last.
- Produces: JSONL records with `schema_version`, `example_id`, `label`, `source`, `seed_id`, `author_group`, `session_group`, `split_group`, `consent`, `strokes`, and optional `generation`.

- [ ] **Step 1: Write schema validation tests**

Create tests that accept a valid two-stroke example and reject unknown labels, non-finite/out-of-bounds coordinates, empty positive examples, missing consent for player examples, missing seed IDs for synthetic examples, duplicate example IDs, more than 64 strokes, and more than 256 points per stroke.

```python
def test_player_example_requires_consent(tmp_path):
    record = valid_record(source="player", consent=None)
    path = write_jsonl(tmp_path, record)
    with pytest.raises(ValueError, match="consent"):
        load_examples(path)


def test_synthetic_example_requires_seed_group(tmp_path):
    record = valid_record(source="synthetic", seed_id=None)
    path = write_jsonl(tmp_path, record)
    with pytest.raises(ValueError, match="seed_id"):
        load_examples(path)
```

- [ ] **Step 2: Run the schema tests and observe failure**

Run: `python3.12 -m pytest glyph-training/tests/test_schema.py -q`

Expected: FAIL because `wizardry_glyphs.schema` does not exist.

- [ ] **Step 3: Implement the schema package and catalog**

Use frozen dataclasses, explicit finite/bounds checks matching `GlyphLimits`, `json.loads` per line, and `frozenset` catalog validation. Pin Python build/test dependencies in `pyproject.toml`. The JSON Schema must require the same fields and bounds as Python validation.

- [ ] **Step 4: Ignore generated data and artifacts only**

Append:

```gitignore
glyph-training/.venv/
glyph-training/build/
glyph-training/artifacts/
glyph-training/dataset/generated/
glyph-training/dataset/player-private/
```

Do not ignore reviewed seed fixtures, catalog, schema, configuration, or aggregate metrics.

- [ ] **Step 5: Verify schema behavior**

Run: `python3.12 -m pytest glyph-training/tests/test_schema.py -q`

Expected: all schema tests PASS.

- [ ] **Step 6: Commit**

```bash
git add .gitignore glyph-training/pyproject.toml glyph-training/catalog-v1.json glyph-training/dataset/schema-v1.json glyph-training/src/wizardry_glyphs glyph-training/tests/test_schema.py
git commit -m "feat: define glyph training dataset schema"
```

---

### Task 2: Build deterministic preprocessing and augmentation

**Files:**
- Create: `glyph-training/src/wizardry_glyphs/preprocess.py`
- Create: `glyph-training/src/wizardry_glyphs/augment.py`
- Create: `glyph-training/tests/test_preprocess.py`
- Create: `glyph-training/tests/test_augment.py`
- Create: `glyph-training/preprocessing-v1.json`

**Interfaces:**
- Produces: `preprocess(example: GlyphExample) -> ModelInputs` with `vector`, `point_mask`, `stroke_mask`, and `raster` float32 arrays.
- Produces: `augment(seed: GlyphExample, generation_seed: int, count: int) -> list[GlyphExample]`.
- Fixed shapes: 64 strokes × 32 points × 8 vector features, 64 × 32 point mask, 64 stroke mask, and 1 × 64 × 64 grayscale raster.

- [ ] **Step 1: Write deterministic preprocessing tests**

Cover arc-length resampling, singleton strokes, masks, stroke-order preservation, translation/scale normalization, aspect-preserving raster centering, empty-draft rejection, and byte-identical tensors across repeated calls.

```python
def test_stroke_order_changes_vector_tensor(example_factory):
    first = example_factory(strokes=[horizontal(), vertical()])
    second = example_factory(strokes=[vertical(), horizontal()])
    assert not np.array_equal(preprocess(first).vector, preprocess(second).vector)
```

- [ ] **Step 2: Write augmentation tests**

Require deterministic output for the same seed, different output for different seeds, bounds-preserving transformations, inherited `seed_id` and `split_group`, and recorded generation parameters.

- [ ] **Step 3: Run focused tests and observe failure**

Run: `python3.12 -m pytest glyph-training/tests/test_preprocess.py glyph-training/tests/test_augment.py -q`

Expected: FAIL because preprocessing and augmentation modules do not exist.

- [ ] **Step 4: Implement preprocessing**

Normalize coordinates to `[0,1]`, brush width by 32, and per-stroke progress by arc length. Encode features in this exact order: `x`, `y`, `dx`, `dy`, `progress`, `pen_down`, `stroke_start`, `brush_width`. Use deterministic CPU NumPy float32 operations and document every field in `preprocessing-v1.json`.

- [ ] **Step 5: Implement bounded augmentation**

Support seeded translation, uniform scale, rotation, point jitter, arc-length re-spacing, and brush-width variation. Reject any generated sample that cannot be transformed into valid `[0,128)` coordinates; retry with a deterministic bounded attempt count and fail explicitly if the requested count cannot be generated.

- [ ] **Step 6: Verify preprocessing and augmentation**

Run: `python3.12 -m pytest glyph-training/tests/test_preprocess.py glyph-training/tests/test_augment.py -q`

Expected: all tests PASS.

- [ ] **Step 7: Commit**

```bash
git add glyph-training/preprocessing-v1.json glyph-training/src/wizardry_glyphs/preprocess.py glyph-training/src/wizardry_glyphs/augment.py glyph-training/tests/test_preprocess.py glyph-training/tests/test_augment.py
git commit -m "feat: preprocess and augment glyph vectors"
```

---

### Task 3: Add canonical seed authoring and the hard data gate

**Files:**
- Create: `glyph-training/src/wizardry_glyphs/validate_dataset.py`
- Create: `glyph-training/tests/test_validate_dataset.py`
- Create: `glyph-training/dataset/reviewed-seeds.jsonl`
- Create: `glyph-training/dataset/review-manifest-v1.json`

**Interfaces:**
- Produces CLI: `python -m wizardry_glyphs.validate_dataset --catalog ... --seeds ... --review ... --player-data ...`.
- Exit `0`: every positive label has at least three independently reviewed seeds, review manifest hashes match, at least 100 consented player examples per label exist, reject examples are at least the largest positive class, and grouping constraints hold.
- Exit `2`: collection gate not met; prints exact missing counts by label and source.
- Training commands in later tasks MUST call this validator first and stop on exit `2`.

- [ ] **Step 1: Write gate failure tests**

Cover missing labels, fewer than three seeds, duplicate seed author groups, unreviewed hashes, insufficient player examples, insufficient reject data, and a passing complete fixture.

- [ ] **Step 2: Run tests and observe failure**

Run: `python3.12 -m pytest glyph-training/tests/test_validate_dataset.py -q`

Expected: FAIL because the validator does not exist.

- [ ] **Step 3: Implement the validator and review manifest**

The manifest records `catalog_version`, `reviewed_at`, `reviewers`, and SHA-256 per seed record. Validation computes current hashes and rejects stale reviews. `reviewed-seeds.jsonl` starts empty because canonical shapes require human authorship; never fabricate seed vectors.

- [ ] **Step 4: Exercise the real empty repository gate**

Run:

```bash
python3.12 -m wizardry_glyphs.validate_dataset \
  --catalog glyph-training/catalog-v1.json \
  --seeds glyph-training/dataset/reviewed-seeds.jsonl \
  --review glyph-training/dataset/review-manifest-v1.json \
  --player-data glyph-training/dataset/player-private
```

Expected: exit `2` with all eleven labels reporting missing seed and player examples. This is the correct initial result and proves that training cannot be claimed yet.

- [ ] **Step 5: Verify unit tests**

Run: `python3.12 -m pytest glyph-training/tests/test_validate_dataset.py -q`

Expected: all validator fixture tests PASS.

- [ ] **Step 6: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/validate_dataset.py glyph-training/tests/test_validate_dataset.py glyph-training/dataset/reviewed-seeds.jsonl glyph-training/dataset/review-manifest-v1.json
git commit -m "feat: gate glyph training on reviewed data"
```

**Data-availability checkpoint:** Stop execution here until humans have drawn and reviewed at least three canonical seeds per label and supplied the required consented player/reject examples. Do not weaken this gate, substitute fabricated labels, train on empty data, or claim a trained model.

---

### Task 4: Train, calibrate, evaluate, and export the fused model

**Prerequisite:** Task 3 validator exits `0` on the actual dataset.

**Files:**
- Create: `glyph-training/src/wizardry_glyphs/model.py`
- Create: `glyph-training/src/wizardry_glyphs/split.py`
- Create: `glyph-training/src/wizardry_glyphs/train.py`
- Create: `glyph-training/src/wizardry_glyphs/evaluate.py`
- Create: `glyph-training/src/wizardry_glyphs/export.py`
- Create: `glyph-training/tests/test_split.py`
- Create: `glyph-training/tests/test_model.py`
- Create: `glyph-training/tests/test_export.py`
- Create: `glyph-training/train-v1.json`

**Interfaces:**
- Produces: vector-only, raster-only, and fused PyTorch classifiers.
- Produces CLI: `python -m wizardry_glyphs.train --config glyph-training/train-v1.json`.
- Produces: `glyph-training/artifacts/<model-version>/model.onnx`, `manifest.json`, `metrics.json`, `golden-fixtures.json`, and `sha256sums.json`.
- Manifest declares tensor shapes, ordered labels, catalog/preprocessing hashes, ONNX opset, calibration temperature, top-probability threshold, margin threshold, training-data version, and file checksums.

- [ ] **Step 1: Write grouped split tests**

Assert no `seed_id`, `author_group`, `session_group`, or explicit `split_group` appears across train/calibration/test partitions and every label is represented when group counts allow.

- [ ] **Step 2: Write model and export tests**

Use a tiny fixture dataset to verify finite logits of shape `[batch, 12]`, reproducible initialization/training under fixed seeds, ONNX export/load, label order, manifest hashes, and failure when the Task 3 validator reports insufficient real data.

- [ ] **Step 3: Run focused tests and observe failure**

Run: `python3.12 -m pytest glyph-training/tests/test_split.py glyph-training/tests/test_model.py glyph-training/tests/test_export.py -q`

Expected: FAIL because training modules do not exist.

- [ ] **Step 4: Implement the three models**

Use a small vector 1D-convolution encoder, a small raster 2D-convolution encoder, and a fused MLP head. Keep only ONNX operators supported by ONNX Runtime 1.29.0 CPU. Train all three baselines under the same split/configuration.

- [ ] **Step 5: Implement grouped training and calibration**

Seed Python, NumPy, and PyTorch. Fit on training groups, early-stop on validation loss, fit temperature and acceptance thresholds only on calibration groups, and evaluate once on test groups. Record exact dependency versions, seed, dataset hash, and sample counts.

- [ ] **Step 6: Implement evaluation and release comparison**

Report per-class precision/recall, confusion matrix, accepted precision/coverage, reject false-accept rate with confidence intervals, top-one/top-two accuracy, calibration error, source slices, and stroke perturbation slices. Mark fused export eligible only when it beats both ablations on preregistered held-out metrics; otherwise export the winning simpler branch and record the decision.

- [ ] **Step 7: Implement ONNX bundle export**

Export a fixed-shape model, load it back with Python ONNX Runtime, compare outputs against PyTorch, generate golden fixtures, write checksums, and reject non-finite or mismatched output.

- [ ] **Step 8: Verify training-tool tests**

Run: `python3.12 -m pytest glyph-training/tests -q`

Expected: all Python tests PASS.

- [ ] **Step 9: Run real training only after the data gate passes**

Run:

```bash
python3.12 -m wizardry_glyphs.validate_dataset --catalog glyph-training/catalog-v1.json --seeds glyph-training/dataset/reviewed-seeds.jsonl --review glyph-training/dataset/review-manifest-v1.json --player-data glyph-training/dataset/player-private && \
python3.12 -m wizardry_glyphs.train --config glyph-training/train-v1.json
```

Expected after real data exists: exit `0`, an artifact bundle, and metrics with nonzero player-held-out and reject sample counts. Before then, expected exit is `2` and no model artifact.

- [ ] **Step 10: Commit tooling, not private/generated data**

```bash
git add glyph-training/src/wizardry_glyphs/model.py glyph-training/src/wizardry_glyphs/split.py glyph-training/src/wizardry_glyphs/train.py glyph-training/src/wizardry_glyphs/evaluate.py glyph-training/src/wizardry_glyphs/export.py glyph-training/tests glyph-training/train-v1.json
git commit -m "feat: train and export glyph classifier"
```

---

### Task 5: Add the Java feature and classification contracts

**Files:**
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphLabel.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphRejection.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphCandidate.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphClassification.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphModelInputs.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphPreprocessor.java`
- Test: `java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/GlyphPreprocessorTest.java`

**Interfaces:**
- `GlyphLabel` enumerates the eleven positive labels plus `REJECT` with stable manifest IDs.
- `GlyphRejection` enumerates `EMPTY_INPUT`, `INVALID_INPUT`, `MODEL_UNAVAILABLE`, `MODEL_INCOMPATIBLE`, `INFERENCE_FAILED`, `NON_FINITE_OUTPUT`, `MODEL_REJECT`, and `UNCERTAIN`.
- `GlyphClassification` contains immutable ranked candidates, optional accepted label, optional rejection, model version, and catalog version; exactly one of accepted label/rejection is present.
- `GlyphPreprocessor.preprocess(GlyphDraft) -> GlyphModelInputs` mirrors preprocessing-v1.

- [ ] **Step 1: Write Java golden preprocessing tests**

Load Python-generated golden fixtures and assert exact masks/shapes and float tolerance `1e-6` for vector/raster tensors. Cover empty input and swapped stroke order.

- [ ] **Step 2: Run focused Java test and observe failure**

Run: `./gradlew :java-compiler:test --tests '*GlyphPreprocessorTest*'`

Expected: FAIL because ML contracts do not exist.

- [ ] **Step 3: Implement immutable contracts and preprocessing**

Use records and defensive copies. Allocate each fixed tensor once, write into row-major arrays, check all finite outputs, and return `EMPTY_INPUT` before allocating inference tensors for an empty draft.

- [ ] **Step 4: Verify Java preprocessing parity**

Run: `./gradlew :java-compiler:test --tests '*GlyphPreprocessorTest*'`

Expected: PASS against Python fixtures.

- [ ] **Step 5: Commit**

```bash
git add java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/GlyphPreprocessorTest.java java-compiler/src/test/resources/dev/jlo/wizardry/glyph/ml
git commit -m "feat: preprocess glyphs for ML inference"
```

---

### Task 6: Validate and load the ONNX model bundle

**Files:**
- Modify: `java-compiler/build.gradle.kts`
- Modify: `mapgui-integration/build.gradle.kts`
- Modify: `mapgui-integration/src/main/resources/paper-plugin.yml`
- Create: `mapgui-integration/src/main/java/dev/jlo/wizardry/mapgui/GlyphPluginLoader.java`
- Test: `mapgui-integration/src/test/java/dev/jlo/wizardry/mapgui/OnnxRuntimePackagingTest.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphModelManifest.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/GlyphModelBundle.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml/OnnxGlyphClassifier.java`
- Test: `java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/GlyphModelBundleTest.java`
- Test: `java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/OnnxGlyphClassifierTest.java`

**Interfaces:**
- Add `compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")` to `java-compiler` for classifier compilation and `testRuntimeOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")` for focused core tests; the library module does not claim to package runtime dependencies.
- Add `compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")` and matching test runtime dependency to `mapgui-integration`. Add `loader: dev.jlo.wizardry.mapgui.GlyphPluginLoader` to `paper-plugin.yml`. `GlyphPluginLoader implements io.papermc.paper.plugin.loader.PluginLoader`; in `classloader(PluginClasspathBuilder)`, construct `MavenLibraryResolver`, add `new DefaultArtifact("com.microsoft.onnxruntime:onnxruntime:1.29.0")`, add a `RemoteRepository` using `MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR`, then pass the resolver to `classpathBuilder.addLibrary`. The deployable plugin remains one JAR, Paper resolves the original dependency before plugin loading, and ONNX Runtime owns extraction of its native library.
- `GlyphModelBundle.load(Path) -> GlyphModelBundle` verifies the manifest and every checksum before creating an ONNX session.
- `OnnxGlyphClassifier.classify(GlyphDraft) -> GlyphClassification` never throws for input/model/inference failures; it returns typed rejection.
- `OnnxGlyphClassifier implements AutoCloseable` and owns one immutable `OrtSession`.

- [ ] **Step 1: Write bundle rejection tests**

Cover missing files, altered checksum, wrong catalog/preprocessing version, wrong tensor shape, unsupported label order, non-finite thresholds, and valid golden bundle loading.

- [ ] **Step 2: Write inference tests**

Cover ranked finite probabilities, `reject` top class, low probability, low margin, model exception, non-finite output, and repeated use of one session.

- [ ] **Step 3: Write runtime packaging tests**

Build the plugin JAR; parse `paper-plugin.yml` and assert its exact `loader` class; load `GlyphPluginLoader`, invoke `classloader` against a recording `PluginClasspathBuilder`, and assert one `MavenLibraryResolver` containing exact artifact `com.microsoft.onnxruntime:onnxruntime:1.29.0` and repository URL `MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR`. Resolve that recorded classpath and launch a JVM from it. Assert `ai.onnxruntime.OrtEnvironment.getEnvironment()` loads successfully, ONNX Runtime extracts its native library, and `mapgui-integration.jar` contains no `ai/onnxruntime` classes or merged ONNX native resources.

- [ ] **Step 4: Run focused tests and observe failure**

Run: `./gradlew :java-compiler:test --tests '*GlyphModelBundleTest*' --tests '*OnnxGlyphClassifierTest*'`

Expected: FAIL because loader/classifier do not exist.

- [ ] **Step 5: Implement strict bundle validation**

Parse the manifest, compare exact ordered labels and tensor schemas, hash files with SHA-256, reject path traversal, and create the ONNX session only after all metadata validates.

- [ ] **Step 6: Implement bounded ONNX inference**

Create fixed-shape `OnnxTensor` values from preprocessed arrays, run one shared session, copy twelve logits once, verify finite values, apply manifest temperature softmax, sort candidates deterministically by probability then label ID, and apply frozen top-probability/margin gates.

- [ ] **Step 7: Implement and verify server runtime packaging**

Implement `GlyphPluginLoader` with Paper's documented `MavenLibraryResolver` mechanism and exact ONNX Runtime 1.29.0 coordinate. Keep the upstream JAR intact so its platform-native extraction logic works. Fail plugin enable with `MODEL_UNAVAILABLE` when the class or native initialization is unavailable; never mutate `java.library.path` or extract native files manually.

Run: `./gradlew :mapgui-integration:test --tests '*OnnxRuntimePackagingTest*'`

Expected: PASS for dependency metadata, isolated class loading, native environment initialization, and absence of shaded ONNX contents.

- [ ] **Step 8: Verify Java model loading and inference**

Run: `./gradlew :java-compiler:test --tests '*GlyphModelBundleTest*' --tests '*OnnxGlyphClassifierTest*'`

Expected: PASS using the exported tiny golden ONNX fixture.

- [ ] **Step 9: Commit**

```bash
git add java-compiler/build.gradle.kts java-compiler/src/main/java/dev/jlo/wizardry/glyph/ml java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml java-compiler/src/test/resources/dev/jlo/wizardry/glyph/ml mapgui-integration/build.gradle.kts mapgui-integration/src/main/java/dev/jlo/wizardry/mapgui/GlyphPluginLoader.java mapgui-integration/src/main/resources/paper-plugin.yml mapgui-integration/src/test/java/dev/jlo/wizardry/mapgui/OnnxRuntimePackagingTest.java
git commit -m "feat: classify glyphs with ONNX Runtime"
```

---

### Task 7: Remove template recognition and expose asynchronous classification

**Files:**
- Remove: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/GlyphRecognizer.java`
- Remove: `java-compiler/src/main/java/dev/jlo/wizardry/glyph/GlyphTemplate.java`
- Modify: `java-compiler/src/test/java/dev/jlo/wizardry/glyph/GlyphTest.java`
- Create: `mapgui-integration/src/main/java/dev/jlo/wizardry/mapgui/GlyphClassificationService.java`
- Modify: `mapgui-integration/src/main/java/dev/jlo/wizardry/mapgui/GlyphMapGuiPlugin.java`
- Modify: `mapgui-integration/src/main/java/dev/jlo/wizardry/mapgui/GlyphScreen.java`
- Test: `mapgui-integration/src/test/java/dev/jlo/wizardry/mapgui/GlyphClassificationServiceTest.java`

**Interfaces:**
- `GlyphClassificationService.classify(GlyphDraft, Consumer<GlyphClassification>)` schedules inference on a bounded single-worker executor and schedules the callback on the Bukkit main thread.
- The screen requests classification explicitly and renders the accepted label or rejection reason; drawing never auto-classifies on each point.
- Plugin enable loads one configured model bundle. Load failure logs the reason and keeps drawing available with `MODEL_UNAVAILABLE` classification.

- [ ] **Step 1: Find and migrate every template recognizer call site**

Use LSP references for `GlyphRecognizer` and `GlyphTemplate`. Record all results before removal; no deprecated aliases or fallback wrappers remain.

- [ ] **Step 2: Write asynchronous service tests**

Use fake worker/main-thread executors and classifier. Assert inference does not run on the caller thread, callbacks return on the main executor, only one bounded worker is used, exceptions become `INFERENCE_FAILED`, and close rejects new work.

- [ ] **Step 3: Run focused tests and observe failure**

Run: `./gradlew :mapgui-integration:test --tests '*GlyphClassificationServiceTest*'`

Expected: FAIL because the service does not exist.

- [ ] **Step 4: Implement service and plugin lifecycle**

Load the model once during enable, create the bounded executor, reject cleanly if unavailable, close the classifier/session and executor during disable, and never block the Bukkit tick thread on `OrtSession.run`.

- [ ] **Step 5: Add explicit classify UI action**

Route an explicit screen action through the service, invalidate after the main-thread callback, and show ranked result/rejection without changing spell power or persisting model output into the authoritative draft.

- [ ] **Step 6: Remove heuristic production classes and tests**

Delete `GlyphRecognizer` and `GlyphTemplate`, replace template-ranking tests with classification-contract tests, and migrate every caller found in Step 1.

- [ ] **Step 7: Verify focused integration behavior**

Run: `./gradlew :java-compiler:test :mapgui-integration:test --tests '*GlyphClassification*' --tests '*OnnxGlyphClassifier*'`

Expected: all classifier and asynchronous integration tests PASS.

- [ ] **Step 8: Commit**

```bash
git add java-compiler/src mapgui-integration/src
git commit -m "feat: classify drawn glyphs asynchronously"
```

---

### Task 8: Verify the release contract end to end

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `mapgui-integration/build.gradle.kts`
- Create: `glyph-training/tests/test_java_parity.py`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/GlyphClassifierSmokeTest.java`
- Create: `mapgui-integration/src/test/java/dev/jlo/wizardry/mapgui/OnnxRuntimePackagingTest.java`

**Interfaces:**
- CI validates Python tooling without requiring private player data or training a release model.
- CI verifies the hard data gate exits `2` for the empty checked-in dataset.
- CI runs Java inference against a tiny non-release golden model fixture.

- [ ] **Step 1: Add Python CI environment and cache**

Install Python 3.12 and locked training/test dependencies. Run schema, augmentation, split, export, and parity tests on synthetic fixtures only.

- [ ] **Step 2: Add the negative data-gate check**

Run the Task 3 validator against checked-in empty seeds and assert exit `2`. Fail CI if it exits `0`, because that would imply private/generated training data was committed accidentally.

- [ ] **Step 3: Write cross-runtime parity test**

Invoke a narrow Java test fixture from Python or compare committed golden tensors and logits from both runtimes. Assert preprocessing tolerance `1e-6` and ONNX output tolerance `1e-5`.

- [ ] **Step 4: Write Java smoke test**

Construct a real `GlyphDraft`, load the golden bundle, classify it, and assert the expected accepted label; classify a hard-negative fixture and assert typed rejection.

- [ ] **Step 5: Verify the deployable plugin distribution**

Build the plugin JAR on Linux; assert `paper-plugin.yml` names `GlyphPluginLoader`; exercise the loader against a recording Paper classpath builder; verify the recorded Maven resolver uses exact ONNX Runtime 1.29.0 and Paper's central mirror; resolve and launch that classpath; and assert the plugin JAR has neither `ai/onnxruntime/**` classes nor merged native libraries. CI must exercise ONNX Runtime's own native extraction rather than relying on the developer machine's classpath.

- [ ] **Step 6: Run all verification commands**

Run:

```bash
python3.12 -m pytest glyph-training/tests -q
./gradlew :java-compiler:test
./gradlew :mapgui-integration:test
./gradlew clean test
```

Expected: all Python and Gradle checks PASS. If root Gradle configuration fails because CI lacks the module’s Java 25 toolchain, provision Java 25 in CI for `mapgui-integration` while retaining Java 21 for `java-compiler`; do not lower or silently change module targets.

- [ ] **Step 7: Run the runtime smoke scenario**

Start the test server with the pinned ONNX CPU native artifact available, draw a fixture glyph, request classification, observe the expected label on the MapGUI screen, then remove the model bundle and observe `MODEL_UNAVAILABLE` without server-tick failure.

- [ ] **Step 8: Commit**

```bash
git add .github/workflows/ci.yml mapgui-integration/build.gradle.kts mapgui-integration/src/test/java/dev/jlo/wizardry/mapgui/OnnxRuntimePackagingTest.java glyph-training/tests/test_java_parity.py java-compiler/src/test/java/dev/jlo/wizardry/glyph/ml/GlyphClassifierSmokeTest.java
git commit -m "test: verify glyph classifier release contract"
```

## Completion criteria

Implementation is complete only when:

- reviewed seed and player/reject data meet the Task 3 gate;
- a real model has been trained, calibrated, evaluated, and exported from that data;
- the model bundle passes Python and Java checksum/schema/parity tests;
- the deployable MapGUI plugin declares and exercises `GlyphPluginLoader`, resolves exact upstream ONNX Runtime 1.29.0 through Paper's Maven resolver, initializes its native environment through the resolved classpath, and contains no shaded ONNX classes/native resources;
- Java 21 ONNX inference classifies held-out glyphs and safely rejects uncertainty and model failures;
- MapGUI invokes inference off the server tick thread and displays accepted labels or typed rejection;
- template matching has no production call sites;
- focused tests, module tests, root build, and runtime smoke scenario pass;
- reported metrics include real held-out sample counts and confidence intervals.
