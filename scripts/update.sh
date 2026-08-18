#!/bin/sh
# Publish a new version of the Wizardry model-weights repository.

set -eu

. "$(dirname "$0")/weights-lib.sh"

ARTIFACT_DIR=${ARTIFACT_DIR:-$(default_artifact_dir)}
WEIGHTS_DIR=${WEIGHTS_DIR:-$(default_weights_dir)}

if [ ! -d "$WEIGHTS_DIR/.git" ]; then
    fail "weights repo not found at $WEIGHTS_DIR; run install.sh first"
fi

log "Updating $WEIGHTS_DIR from $ARTIFACT_DIR"

if [ -n "$(git -C "$WEIGHTS_DIR" remote 2>/dev/null)" ]; then
    git -C "$WEIGHTS_DIR" fetch origin --tags >/dev/null
    git -C "$WEIGHTS_DIR" pull --ff-only >/dev/null
fi

validate_artifact "$ARTIFACT_DIR"
require_git_clean "$WEIGHTS_DIR"

if [ -n "${DRY_RUN:-}" ]; then
    log "[dry-run] would clear old artifact files and copy $ARTIFACT_DIR"
fi

TAG=$(next_tag "$WEIGHTS_DIR")
log "Next tag: $TAG"

if [ -n "${DRY_RUN:-}" ]; then
    log "[dry-run] would generate README and commit as $TAG"
else
    MSG="Update model weights from $(basename "$ARTIFACT_DIR") to $TAG"
    do_publish "$WEIGHTS_DIR" "$ARTIFACT_DIR" "$TAG" "$MSG"
    log "Published $TAG"
fi
