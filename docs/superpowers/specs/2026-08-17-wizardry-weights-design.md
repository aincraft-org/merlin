# Wizardry Model Weights Repository

Date: 2026-08-17

## Goal

Host the current trained glyph-recognition model in a separate, independently-versioned Git repository so the model release cycle is not tied to the main `aincraft-org/wizardry` project version.

## Context

- The main repository is `aincraft-org/wizardry`.
- The current model artifact is `training/artifacts/dev-basic-v1`.
- Main repository modules: `api`, `common`, `paper`, `training`.
- `training/artifacts/` is in `.gitignore`, so model files are not committed to the main repo.

## Decision: Separate Repository with Calver Tags

Create a sibling repository `aincraft-org/wizardry-weights` that contains only model artifacts, published as Git tags with calendar versioning. This keeps model weights out of the main repo history while giving each published model a clear, independent version.

### Versioning

- Tag format: `YYYY.MM.DD.N`
  - `YYYY.MM.DD` = UTC date of publish.
  - `N` = per-day counter, starting at `0`.
- Example: `2026.08.17.0`, `2026.08.17.1`

### Counter Storage

- A local state file, `scripts/.weights-state`, stores the last used date and counter.
- If the state file is missing, the script falls back to inspecting existing tags in the weights repo.
- The state file is not committed to the main repository.

### Repository Layout per Tag

Each tag’s tree contains the selected artifact files at the repository root:

- `model.onnx`
- `model.pt`
- `manifest.json`
- `history.json`
- `sha256sums.json`

No versioned subdirectories; the tag itself is the version.

## Scripts

### `scripts/install.sh`

One-time setup and first publish.

1. Determine source artifact directory (`ARTIFACT_DIR`, default `training/artifacts/dev-basic-v1`).
2. Determine weights repo directory (`WEIGHTS_DIR`, default `../wizardry-weights`).
3. Determine remote (`WEIGHTS_REMOTE`, default `https://github.com/aincraft-org/wizardry-weights.git`).
4. If the weights repo does not exist:
   - Try `git clone $WEIGHTS_REMOTE $WEIGHTS_DIR`.
   - If the remote is not reachable, `git init` an empty local repo, add the remote, and print instructions for creating/pushing the GitHub repository.
5. Verify the artifact contains `model.onnx` and `model.pt`.
6. Remove any existing files in the weights repo root and copy the artifact files in.
7. Compute the next calver tag.
8. Commit with a message including the source artifact and the new tag.
9. Create the annotated tag.
10. Push the tag to `origin` (skipped if no remote or if `DRY_RUN=1`).
11. Generate `README.md` in the weights repo describing the latest model.

### `scripts/update.sh`

Subsequent publishes.

1. Verify the weights repo exists (fail with a helpful message otherwise).
2. Pull latest tags from `origin`.
3. Copy the current artifact into the weights repo root.
4. Compute the next calver tag.
5. Commit, tag, and push.

### Shared Helper

A small `scripts/weights-lib.sh` will be sourced by both scripts. It contains the calver/counter logic, artifact validation, and remote checks. This avoids duplicating logic while keeping the two scripts the user asked for.

## Configuration

Environment variables accepted by both scripts:

| Variable | Default | Description |
|----------|---------|-------------|
| `ARTIFACT_DIR` | `training/artifacts/dev-basic-v1` | Source model artifact. |
| `WEIGHTS_DIR` | `../wizardry-weights` | Local clone of the weights repo. |
| `WEIGHTS_REMOTE` | `https://github.com/aincraft-org/wizardry-weights.git` | Remote URL. |
| `DRY_RUN` | unset | If `1`, print commands without committing or pushing. |
| `FORCE` | unset | If `1`, allow publishing even if the weights repo has uncommitted changes. |

## Error Handling

- Fail early if the source artifact is missing or does not contain `model.onnx` / `model.pt`.
- Refuse to commit if the weights repo has uncommitted changes unless `FORCE=1`.
- Refuse to run `update.sh` if `WEIGHTS_DIR` does not exist.
- Provide clear, actionable error messages.

## Testing

- Run `DRY_RUN=1 ./scripts/install.sh` to verify paths and computed tag.
- Test against a local bare repository before pushing to `aincraft-org/wizardry-weights`.
- Verify that `git tag -l` in the weights repo shows the expected calver tags.
