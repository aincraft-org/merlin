# Glyph Machine-Learning Classifier Design

## Goal

Replace template-distance glyph recognition with a trained classifier that identifies a single drawn glyph from both its rendered shape and its ordered vector strokes.

The first delivery is classification only. It does not interpret multi-glyph diagrams, construct enchantment graphs, assign spell power, or add gameplay effects.

## Scope

A player draws one glyph on the existing private 128×128 MapGUI canvas. The system returns ranked label probabilities and either accepts the top label or rejects the drawing as unknown or uncertain.

The initial closed label catalog contains concepts already implemented by the Java compiler:

- `target-ray`
- `damage`
- `heal`
- `push`
- `cooldown`
- `self`
- `target`
- `physical`
- `fire`
- `frost`
- `arcane`

The model also learns an explicit `reject` class from unrelated, incomplete, malformed, and ambiguous drawings.

## Canonical glyph source

The repository has no canonical visual glyph definitions. The training workflow therefore begins with a catalog-authoring dataset rather than inventing shapes in classifier code.

Each label receives at least three independently drawn and reviewed `GlyphDraft` seed examples. Seeds define the intended appearance, permitted stroke orders, and materially distinct valid variants. They are stored in a versioned dataset using the same vector representation as runtime capture. A seed is accepted only after human review confirms that it is visually distinct from every other initial label.

Synthetic examples are deterministic augmentations of these reviewed seeds. Every derivative records its seed ID and generation parameters. All derivatives from one seed remain in one data split so augmentation cannot leak into evaluation.

## Inputs and preprocessing

`GlyphDraft` remains authoritative. Training and Java inference apply the same versioned preprocessing contract.

### Vector input

Each stroke is resampled by arc length to a fixed number of points. The ordered sequence contains normalized coordinates, local deltas, normalized path progress, pen-down and stroke-boundary markers, normalized brush width, and masks for absent points and strokes.

Stroke and point order are preserved. Timestamps are retained only as dataset provenance and are not model features.

### Raster input

The existing full-canvas raster is cropped to its ink bounds, padded, aspect-ratio preserving, centered, and resized to a fixed grayscale tensor. Empty drawings are rejected before inference. The preprocessing version fixes raster dimensions, interpolation, padding, normalization, tensor order, and floating-point representation.

Python and Java golden-vector fixtures must produce equivalent tensors before a model can be released.

## Model

Use a compact dual-input neural classifier:

- a small one-dimensional convolutional encoder processes ordered vector features and stroke boundaries;
- a small convolutional encoder processes the normalized raster shape;
- the two embeddings are concatenated;
- a compact multilayer perceptron produces logits for the eleven catalog labels plus `reject`.

The vector encoder is primary because `GlyphDraft` order and geometry are authoritative. The raster encoder supplies overall-shape evidence. Exact layer widths are selected through validation and ablation, not fixed as a product contract. A release must demonstrate that the fused model materially outperforms both a raster-only and vector-only baseline on player-held-out data; otherwise the unnecessary branch is removed.

## Training workflow

Training runs offline in Python. The Minecraft server never starts Python or trains a model.

The JSON Lines dataset contains schema version, raw `GlyphDraft` vectors, label, seed ID, source type, pseudonymous author and session grouping, split group, consent or provenance metadata, and generation parameters when synthetic. Derived rasters remain reproducible and are not authoritative data.

Training data combines reviewed canonical seed drawings, deterministic synthetic variations, explicitly labeled player drawings, and explicit reject examples including unrelated shapes, partial glyphs, extra or missing strokes, reversed strokes, and near-neighbor ambiguities.

Player drawings are labeled by the requested glyph or explicit reject choice, never by the current model prediction. Splits are grouped by canonical seed, player, and session. The initial target is a minimum of three canonical seeds and 100 player drawings per label, plus a reject set at least as large as the largest positive class. These are collection floors, not claims of statistical sufficiency. Training uses 70% of groups, calibration uses 15%, and test uses 15%; a separately collected frozen challenge set remains untouched until release evaluation. No derivatives of one seed, player, or session cross split boundaries.

Training is reproducible from a fixed dataset version, preprocessing version, configuration, random seed, and dependency lock.

## Runtime and model artifact

The first runtime uses ONNX Runtime Java CPU inference with a pinned version compatible with Java 21. The trained model exports to a fixed ONNX opset whose operators are exercised by an integration test on the actual Java runtime. No Python dependency is packaged with the server.

A model bundle contains `model.onnx`, a manifest with model/catalog/preprocessing versions, ordered label IDs and tensor schemas, calibration parameters and thresholds, dataset and training identifiers, checksums, golden fixtures, and evaluation metrics with sample counts.

Loading validates checksums, catalog identity, preprocessing schema, tensor dimensions, ONNX compatibility, and finite calibration values. A missing, corrupt, unsupported, or incompatible model leaves classification available only as a typed rejection. The old template heuristic is removed from the production path and is not a fallback.

Inference runs outside the Paper server tick thread. One immutable model session is shared, and requests are bounded so malformed or oversized inputs cannot allocate unbounded tensors.

## Classification contract

Recognition returns ranked candidates, model and catalog versions, and either an accepted label or a typed rejection reason.

Acceptance requires validation-derived conditions for calibrated top probability, top-to-runner-up margin, and input/model quality. Exact thresholds are selected on a held-out calibration set and frozen in the model manifest; they are not guessed in source code.

The system rejects an empty or invalid draft, missing or incompatible model, preprocessing or inference failure, non-finite outputs, a top-ranked `reject`, or insufficient calibrated confidence or margin.

Confidence only controls acceptance. It never changes enchantment strength, cost, or runtime behavior.

## Evaluation and release gates

Every candidate reports per-class precision and recall, confusion matrix, accepted precision and coverage, reject false-accept rate, top-one and top-two accuracy, calibration error, grouped held-out metrics, synthetic-versus-player slices, stroke perturbation results, Java/Python parity, and Java latency and memory.

Rates include sample counts and confidence intervals. Thresholds and release gates are fixed only after a pilot dataset establishes feasible operating characteristics. A model that cannot meet the frozen gates is not released; runtime remains safe-rejecting.

## Collection and rollout

1. Author and review canonical vector seeds for all eleven labels.
2. Generate deterministic synthetic data and a balanced reject corpus.
3. Train vector-only, raster-only, and fused baselines.
4. Add explicit player labeling and correction export.
5. Retrain with grouped splits and calibrate acceptance.
6. Verify Python/Java parity and Java 21 ONNX inference.
7. Run shadow evaluation without affecting gameplay.
8. Release to a limited cohort and monitor rejection and confusion slices.
9. Retrain only from reviewed labels; roll back to a previous valid model or safe rejection, never template heuristics.

## Testing

Permanent tests cover dataset validation, deterministic augmentation, split isolation, preprocessing parity, model manifest validation, corrupt artifacts, calibration, ambiguity rejection, stroke-order perturbations, and Java inference over golden fixtures.

A smoke scenario captures a real `GlyphDraft`, preprocesses both branches, invokes the bundled model through Java, and observes the expected accepted label or typed rejection.

## Non-goals

- General handwriting or OCR.
- Multi-glyph layout interpretation.
- Automatic enchantment graph construction.
- Online or per-server training.
- Implicit player-data collection.
- Confidence-based spell power.
- Template matching as a production fallback.
