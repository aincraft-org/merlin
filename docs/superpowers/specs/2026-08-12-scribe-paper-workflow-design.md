# Scribe Paper Workflow Design

## Goal

Make Scribe usable end-to-end on the current Paper target: a marked book opens a real editor workflow, source reaches persistence and compilation, Save & Cast executes the spell, and invalid input remains atomic.

## Findings

- The Java compiler implements the documented grammar and semantic bounds, but its UTF-8 source-limit diagnostic passes a byte limit as a UTF-16 index and can throw for multibyte input.
- `ScribeDialog` is only an in-memory state machine. The listener cancels right-click and discards the session; no Paper Dialog API classes are present in the declared Paper API jar, so no native dialog can be compiled against the current target.
- `Save & Cast` persists before compilation and never invokes `SpellRuntime`.
- The command path omits the declared permission.
- Runtime behavior lacks focused coverage and has unsafe edge cases around self-only spells, zero-length push vectors, and cooldown mutation.

## Decision

Use a reliable chat editor as the complete current Paper path. Keep the UI boundary in `ScribeDialog`/a small editor service so an optional runtime capability can later provide a native Dialog API implementation without importing unavailable classes or risking plugin startup. Do not upgrade the Paper API in this change.

## Architecture and data flow

1. `/scribe book` creates a writable, PDC-marked book containing a UUID and starter source.
2. Right-clicking a marked main-hand book cancels vanilla behavior and opens a player-bound editor session. The session announces the current source and editor commands.
3. Chat input while editing is captured by a listener and appended/replaced according to the editor protocol. `/scribe save`, `/scribe cast`, and `/scribe cancel` submit the session. A source submission is bounded by scalar, UTF-8, and line limits.
4. `SAVE` validates input and persists source. `SAVE_AND_CAST` validates and compiles first; rejected compilation still persists the draft but returns diagnostics and does not invoke `SpellRuntime`; accepted compilation persists and invokes `SpellRuntime`.
5. The command cast path uses the same compile/runtime service and enforces `wizardry.scribe.book`.
6. Runtime validates the operation set before mutations, permits self-only casts without a target, avoids zero-vector pushes, and applies cooldown only after successful execution.

## Error and security behavior

- Sessions are one-use, player-bound, exact-book-bound, and expire at or after the expiry instant.
- Invalid Save & Cast persists the submitted draft but never mutates cooldown state or invokes runtime effects.
- Chat events are cancelled only for an active editor session; ordinary chat and ordinary books retain behavior.
- No compile-time reference to Paper Dialog API classes is introduced while the target dependency lacks them.
- All source limits use Unicode scalar count, UTF-8 byte count, and physical line count consistently.

## Verification

Add tests for:

- multibyte source over the UTF-8 limit returning a diagnostic instead of throwing;
- invalid Save & Cast persists the submitted draft but does not invoke runtime or mutate cooldown;
- editor callback routing from interaction/chat/submit to persistence/runtime;
- command permission enforcement;
- self-only runtime casts without a target, cooldown behavior, and zero-vector push safety.

Run `./gradlew :java-compiler:test :paper:test` and a Paper module smoke/build check after implementation.
