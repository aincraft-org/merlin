#!/bin/sh
# First-time setup and publish of the Wizardry model-weights repository.

set -eu

. "$(dirname "$0")/weights-lib.sh"

ARTIFACT_DIR=${ARTIFACT_DIR:-$(default_artifact_dir)}
WEIGHTS_DIR=${WEIGHTS_DIR:-$(default_weights_dir)}
WEIGHTS_REMOTE=${WEIGHTS_REMOTE:-$(default_remote)}

log "Publishing $ARTIFACT_DIR to weights repo $WEIGHTS_DIR"

validate_artifact "$ARTIFACT_DIR"

if [ -d "$WEIGHTS_DIR/.git" ]; then
    log "Using existing weights repo at $WEIGHTS_DIR"
elif [ -d "$WEIGHTS_DIR" ]; then
    fail "directory exists but is not a git repo: $WEIGHTS_DIR"
else
    log "Cloning $WEIGHTS_REMOTE into $WEIGHTS_DIR ..."
    if git clone "$WEIGHTS_REMOTE" "$WEIGHTS_DIR" >/dev/null 2>&1; then
        log "Cloned successfully"
    else
        log "Remote not reachable; initializing local repo at $WEIGHTS_DIR"
        mkdir -p "$WEIGHTS_DIR"
        git -C "$WEIGHTS_DIR" init >/dev/null 2>&1
        git -C "$WEIGHTS_DIR" remote add origin "$WEIGHTS_REMOTE"
        NO_PUSH=1
        log "Initialized local repo. Create the remote on GitHub, then push with:"
        log "  git -C \"$WEIGHTS_DIR\" push -u origin HEAD --tags"
    fi
fi

require_git_clean "$WEIGHTS_DIR"

if [ -n "${DRY_RUN:-}" ]; then
    log "[dry-run] would clear old artifact files and copy $ARTIFACT_DIR"
fi

TAG=$(next_tag "$WEIGHTS_DIR")
log "Next tag: $TAG"

if [ -n "${DRY_RUN:-}" ]; then
    log "[dry-run] would generate README and commit as $TAG"
else
    MSG="Publish model weights from $(basename "$ARTIFACT_DIR") as $TAG"
    do_publish "$WEIGHTS_DIR" "$ARTIFACT_DIR" "$TAG" "$MSG"
    log "Published $TAG"
fi
