# Wizardry Sigil Reference Design

## Purpose

Define an original, drawable visual language for all 24 classifier labels. The language combines public-domain alchemical geometry with economical rune-like strokes without copying named ceremonial, religious, or culturally significant seals.

The symbols are canonical concepts for collecting independent human redraws. They are not synthetic training records and do not make a model release-ready by themselves.

## Visual grammar

Each canonical symbol uses one to four logical strokes and remains identifiable without color or small decoration. It should occupy most of the 128 × 128 canvas and tolerate ordinary variation in scale, rotation, translation, speed, brush width, and imperfect closure.

The vocabulary uses these primitives:

- vertical, horizontal, and diagonal stems;
- forks, chevrons, and arrowheads;
- circles and open arcs;
- triangles and diamonds;
- bars crossing a larger structure;
- dots only when the surrounding structure remains sufficient at low resolution.

No two labels may differ only by rotation, reflection, stroke order, color, or one tiny mark. Labels should differ in at least two coarse features such as enclosure, main axis, endpoint count, branching, or symmetry.

## Canonical concepts

These descriptions define topology and intent, not exact coordinates. The existing glyph studio should capture the final geometry as explicit strokes.

| Label | Canonical concept | Logical strokes | Distinguishing structure |
|---|---|---:|---|
| `target-ray` | Small origin ring feeding a rightward shaft and arrowhead | 3 | Only mark combining an origin enclosure with an outbound arrow |
| `damage` | Downward spear through an open triangular blade | 2 | Strong vertical cut through an upward apex |
| `heal` | Upward stem ending in two raised branches, with a lower restoring fork | 3 | Tree-like upward growth; no enclosure |
| `push` | Vertical backstop feeding a rightward shaft and broad arrowhead | 3 | Arrow starts at a barrier rather than a ring |
| `cooldown` | Broken circular arc around an angled clock hand | 2 | Nearly closed enclosure with one time hand |
| `self` | Small central ring inside a larger open-bottom arc | 2 | Inward focus and personal enclosure |
| `target` | Diamond center with four outward cardinal ticks | 3 | Symmetric crosshair around an angular center |
| `physical` | Upward triangle crossed by a low horizontal grounding bar | 2 | Stable earth form and low bar |
| `fire` | Upward triangle split by a central rising stem | 2 | Rising apex and internal vertical axis |
| `frost` | Downward triangle crossed by a high horizontal bar | 2 | Descending apex and high bar |
| `arcane` | Open pentagonal crystal crossed by a middle channel | 2 | Five-sided asymmetric enclosure |
| `on-hit` | Rightward arrow striking a short vertical impact bar with two outward sparks | 4 | Motion terminates at an impact surface |
| `on-hurt` | Left-pointing notch entering an open diamond around a central slash | 3 | Incoming damage toward an enclosed subject |
| `on-use` | Upward fork rising from a small base ring | 2 | Activation fork anchored to a control-like base |
| `periodic` | Three-quarter circular arrow around two opposing ticks | 3 | Recurrent motion without clock hand |
| `if-health` | Open heart-like chevron crossed by a short pulse zigzag | 3 | Organic enclosure plus pulse |
| `if-undead` | Split lower fork beneath a barred hollow diamond | 3 | Hollow head above skeletal branching |
| `if-outdoors` | Roofless horizon beneath three upward rays | 4 | Horizon and sky rays; no enclosure |
| `shield` | Rounded pointed enclosure with a reinforcing horizontal bar | 2 | Defensive closed boundary |
| `attacker` | Inward-pointing spear entering an open target diamond from the left | 3 | Actor direction enters target rather than leaving origin |
| `area` | Central diamond surrounded by four detached corner arcs | 4 | One center affecting a distributed boundary |
| `repeat` | Two opposed angular arrows forming an open loop | 2 | Bidirectional cycle with no clock features |
| `charges` | Vertical stack of three connected diamonds | 2 | Discrete stored units on one axis |
| `reject` | No canonical positive sigil | — | Negative class consists of malformed, unrelated, partial, and ambiguous drawings |

## Collision review

Potentially confusable groups require explicit drawing review before collection:

- `fire`, `physical`, and `frost`: apex direction and internal bar/stem must remain visibly different.
- `push`, `target-ray`, and `attacker`: distinguish barrier origin, ring origin, and inbound motion.
- `cooldown`, `periodic`, and `repeat`: distinguish clock hand, circular arrow, and paired angular arrows.
- `self`, `target`, and `area`: distinguish personal enclosure, crosshair, and distributed boundary.
- `damage`, `on-hit`, and `on-hurt`: distinguish free spear, terminal impact, and inbound enclosed injury.

A concept fails review if an untrained viewer cannot distinguish every member at 64 × 64 monochrome rendering or if common imperfect redraws collapse two concepts into the same coarse topology.

## Reference sources and licensing

Use historical sources only to understand stroke grammar:

- Kenelm Digby's 1682 alchemical symbol chart: public domain and free of known restrictions.
- Wikimedia's alchemical fire symbol: simple geometry in the public domain.
- The public-domain medieval rune chart by Tasnu Arakun.

Record source URLs and license evidence in the catalog metadata. Final Wizardry geometry must be independently authored from the concepts above rather than traced from an individual source image. Do not use Goetic spirit sigils, Solomonic seals, active religious symbols, or modern fictional emblems as direct templates.

## Collection protocol

The canonical geometry is a design seed. Every human redraw is a separate observation with provenance; redraws must not be treated as independent merely because their point sequences differ.

For each positive label:

1. Approve at least six structurally distinct canonical templates matching the same concept. A mirrored or lightly perturbed copy does not count as distinct.
2. Collect redraws from multiple consenting writers using the actual map drawing surface.
3. Use the existing pseudonymous `author_group` field as the stable writer identifier; do not store real-world identity in the corpus. Assign a `session_group` to each collection sitting.
4. Record the canonical template as `seed_id`; record every redraw derived from it under a shared `lineage_group`.
5. Preserve source provenance in `independent_source`. It must identify a genuinely independent writer/source, not an augmentation seed.
6. Set `split_group` to the writer-level `author_group` for player records so all work by one writer remains together.
7. Keep the canonical seed record distinct from all redraw records; never rewrite its provenance to match a redraw.
8. Store consent and review status according to the existing dataset schema.

Generated augmentation inherits the original redraw's `author_group`, source, lineage, and split group. It never creates a new independent source.

## Partitioning and evaluation

Split by writer and lineage, never by individual drawing or stroke. No `author_group`, `independent_source`, or `lineage_group` may appear in more than one of training, calibration, and sealed test partitions. The current cross-validation implementation groups by `lineage_group`; before collecting production data, it must additionally validate writer/source isolation and honor writer-level `split_group` so separate template lineages from one writer cannot cross partitions.

Use grouped cross-validation inside the non-sealed development data. Select the model, calibrate temperature and rejection thresholds, then evaluate the sealed writer-held-out test once. Synthetic development data remains explicitly non-release-ready.

Production claims require:

- real, consented player drawings;
- writer-held-out sealed evaluation;
- per-class precision, recall, and F1;
- coverage and accepted precision after rejection;
- reject false-accept rate;
- confusion review for every collision group;
- thresholds defined before opening the sealed result.

## Negative class

`reject` must represent realistic non-glyph input rather than a single invented reject mark. Include blank or near-blank attempts, partial positive glyphs, accidental taps, scribbles, malformed closures, ambiguous blends between collision-group members, and ordinary doodles. Reject samples derived from a positive retain that positive seed's lineage and writer provenance.

Do not generate the sealed reject set algorithmically from training positives. Collect it independently from held-out writers.

## Acceptance criteria

The reference set is ready for data collection when:

- all 23 positive labels have approved explicit stroke geometry;
- each positive has at least six genuinely distinct template lineages;
- every symbol is drawable with one to four logical strokes;
- collision groups remain distinguishable in monochrome at 64 × 64;
- no symbol is a direct copy of an excluded cultural or fictional emblem;
- catalog records include reference provenance and license notes;
- collection captures `author_group`, `seed_id`, `lineage_group`, `independent_source`, `session_group`, writer-level `split_group`, and consent;
- split validation rejects writer, source, or lineage overlap across partitions;
- `reject` remains a heterogeneous negative class rather than a drawable word.
