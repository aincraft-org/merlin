# Glyph Stratified Group Cross-Validation Design

## Goal

Replace the single synthetic 70/15/15 evaluation with a lineage-isolated evaluation protocol that uses a sealed final test set and five-fold stratified group cross-validation for model and hyperparameter selection. The protocol must detect an infeasible corpus before model initialization and must never present synthetic cross-validation scores as evidence of real-player generalization.

## Independent lineage identity

Every training example must carry an explicit, stable `lineage_group` identifying the independently created source from which it descends.

- Synthetic examples use the original template lineage, not a derivative seed, transformation seed, or example ID.
- Player examples use an independently authored capture lineage, represented by the appropriate author/session collection identity.
- Derived and augmented examples inherit their parent's `lineage_group` unchanged.
- Cross-validation has no fallback from `lineage_group` to `split_group`, `seed_id`, or `example_id`. Missing lineage identity is a hard validation error.

A lineage may contain only one label. Mixed-label lineages are rejected because label-stratified assignment would otherwise be ambiguous.

## Corpus feasibility

The default protocol uses five folds and a sealed test partition. Each label therefore needs at least six independent lineages: at least one complete lineage for the sealed test set and at least five complete non-test lineages so every validation fold can contain that label.

The splitter computes actual independent lineage counts per label before any model is constructed. It reports every deficient label and exits without training when the requested fold count is infeasible. Nominal sample count does not satisfy this requirement; transformed siblings count as one lineage.

The synthetic-development generator will provide at least six genuinely independent base templates per label. Creating additional transformations of the existing three templates does not meet this requirement.

## Partition protocol

Partitioning is deterministic for a configured seed:

1. Group all examples by `lineage_group` and validate lineage invariants.
2. For each label, reserve complete lineages for the sealed test set while approximating the configured test ratio and guaranteeing at least five non-test lineages.
3. Assign remaining complete lineages to five folds, balancing per-label example counts.
4. For fold `i`, use fold `i` as validation and all other non-test folds as training.
5. Apply augmentation only to that fold's training partition after assignment. Augmentations retain their source lineage.

No lineage may occur in more than one of the sealed test set or cross-validation folds. Every validation fold must contain every label.

## Model and hyperparameter selection

Each candidate configuration trains independently on all five folds. The selection report records fold macro F1, per-class F1, mean macro F1, sample standard deviation, minimum fold macro F1, parameter count, and runtime.

Candidates are ranked by:

1. Higher mean validation macro F1.
2. Lower macro-F1 standard deviation.
3. Fewer parameters.
4. Shorter runtime.

The sealed test set is inaccessible to the candidate-selection loop. No test logits or test metrics are generated while candidates are being compared.

## Final training, calibration, and test

After selection, retrain the selected configuration using all non-test groups except a dedicated grouped calibration subset. Calibration groups are selected from the non-test pool without crossing lineage boundaries and are used only for temperature and rejection-threshold fitting. Train the final model on the remaining non-test groups, fit calibration parameters once, then evaluate the sealed test set exactly once.

The exported manifest records:

- Fold assignment hashes and the partition seed.
- Cross-validation fold metrics and aggregate statistics.
- Selected configuration and ranking inputs.
- Calibration and sealed-test lineage counts.
- Final sealed-test metrics.
- Dataset profile and the explicit statement that synthetic results do not estimate real-player performance.

## Error handling

Training fails before model construction when:

- `lineage_group` is missing or empty.
- A lineage contains multiple labels.
- Any label has too few independent lineages for the sealed test plus requested folds.
- Any lineage crosses partitions.
- A fold lacks a required label.

Configuration values for fold count, test ratio, and seed are validated before corpus assignment.

## Verification

Automated tests must prove:

- No lineage appears in multiple folds or in both cross-validation and sealed test data.
- Every fold contains every label.
- Fewer than six lineages for any label fails before model construction for the default protocol.
- Derivatives and augmentations retain the parent lineage.
- Fixed seed and input reproduce identical assignments.
- A different seed may change assignments without violating isolation.
- Candidate selection never evaluates or receives the sealed test partition.
- Final sealed-test evaluation occurs only after configuration selection.
- Cross-validation aggregates match independently computed mean, sample standard deviation, minimum, and per-class metrics.

A CUDA smoke run must execute the complete five-fold workflow, export the final checkpoint and ONNX bundle, validate hashes and ONNX inference, and produce a cross-validation report. Any perfect synthetic score is reported as benchmark saturation and triggers a warning; it is never described as proof of generalization.
