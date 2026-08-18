#!/bin/sh
# Shared helpers for publishing Wizardry model weights.
# Sourced by install.sh and update.sh.

set -eu

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

script_repo_root() {
    cd "$(cd "$(dirname "$0")" && pwd)/.." && pwd -P
}

main_repo_root() {
    _script_dir=$(cd "$(dirname "$0")" && pwd)
    _git_dir=$(git -C "$_script_dir" rev-parse --git-common-dir)
    cd "$_script_dir/$_git_dir/.." && pwd -P
}

default_artifact_dir() {
    printf '%s/training/artifacts/dev-basic-v1\n' "$(main_repo_root)"
}

default_weights_dir() {
    printf '%s/wizardry-weights\n' "$(dirname "$(main_repo_root)")"
}

default_remote() {
    printf 'https://github.com/aincraft-org/wizardry-weights.git\n'
}

state_file() {
    printf '%s/scripts/.weights-state\n' "$(script_repo_root)"
}

today_ymd() {
    date -u +%Y.%m.%d
}

# Compute the next calver tag (YYYY.MM.DD.N) for today.
# Uses the local state file if present, otherwise falls back to existing tags.
next_tag() {
    _weights_dir=$1
    _date=$(today_ymd)
    _state=$(state_file)
    _counter=-1

    if [ -f "$_state" ]; then
        _state_date=$(sed -n 's/^DATE=//p' "$_state" | head -n1)
        _state_counter=$(sed -n 's/^COUNTER=//p' "$_state" | head -n1)
        if [ "$_state_date" = "$_date" ] && [ -n "$_state_counter" ]; then
            _counter=$_state_counter
        fi
    fi

    if [ -d "$_weights_dir/.git" ]; then
        _tag_counter=$(git -C "$_weights_dir" tag -l "${_date}.*" 2>/dev/null \
            | awk -F. '{print $4}' \
            | sed '/^$/d' \
            | sort -n \
            | tail -n1)
        if [ -n "$_tag_counter" ]; then
            if [ "$_tag_counter" -gt "$_counter" ]; then
                _counter=$_tag_counter
            fi
        fi
    fi

    _next=$((_counter + 1))
    printf '%s.%s\n' "$_date" "$_next"
}

write_state() {
    _state=$(state_file)
    _date=$(today_ymd)
    mkdir -p "$(dirname "$_state")"
    printf 'DATE=%s\nCOUNTER=%s\n' "$_date" "$1" > "$_state"
}

validate_artifact() {
    _dir=$1
    if [ ! -d "$_dir" ]; then
        fail "artifact directory not found: $_dir"
    fi
    for _f in model.onnx model.pt manifest.json history.json sha256sums.json; do
        if [ ! -f "$_dir/$_f" ]; then
            fail "missing artifact file: $_dir/$_f"
        fi
    done
}

clean_weights_root() {
    _dir=$1
    for _f in model.onnx model.pt manifest.json history.json sha256sums.json README.md; do
        rm -f "$_dir/$_f"
    done
}

copy_artifact() {
    _src=$1
    _dst=$2
    clean_weights_root "$_dst"
    for _f in model.onnx model.pt manifest.json history.json sha256sums.json; do
        cp -p "$_src/$_f" "$_dst/$_f"
    done
}

generate_readme() {
    _dst=$1
    _tag=$2
    _artifact=$3
    _date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    {
        printf '# Wizardry Model Weights\n\n'
        printf '%s\n' "- Version: $_tag"
        printf '%s\n' "- Source artifact: $_artifact"
        printf '%s\n\n' "- Published: $_date"
        printf 'This repository contains trained model weights for the Wizardry glyph-recognition plugin.\n'
        printf 'Each Git tag is an independently-versioned model release.\n\n'
        printf '## Files\n\n'
        cat <<'EOF'
- `model.onnx` — ONNX runtime model
- `model.pt` — PyTorch checkpoint
- `manifest.json` — model metadata
- `history.json` — training history
- `sha256sums.json` — file checksums
EOF
    } > "$_dst/README.md"
}

require_git_clean() {
    _dir=$1
    if [ -z "${FORCE:-}" ]; then
        if [ -n "$(git -C "$_dir" status --porcelain 2>/dev/null)" ]; then
            fail 'weights repo has uncommitted changes; commit/stash them or set FORCE=1'
        fi
    fi
}

commit_and_tag() {
    _dir=$1
    _tag=$2
    _msg=$3
    git -C "$_dir" add -A
    git -C "$_dir" commit -m "$_msg" >/dev/null
    git -C "$_dir" tag -a "$_tag" -m "Publish model weights $_tag" >/dev/null
}

push_release() {
    _dir=$1
    _tag=$2
    if [ -n "${NO_PUSH:-}" ]; then
        log "Skipping push; remote not yet configured"
        return
    fi
    if [ -n "${DRY_RUN:-}" ]; then
        log "[dry-run] would push branch and tag $_tag to origin"
        return
    fi
    if [ -n "$(git -C "$_dir" remote 2>/dev/null)" ]; then
        git -C "$_dir" push origin HEAD --tags >/dev/null || fail "push to origin failed (commit and tag $_tag are local)"
    fi
}

do_publish() {
    _dir=$1
    _artifact=$2
    _tag=$3
    _msg=$4
    copy_artifact "$_artifact" "$_dir"
    generate_readme "$_dir" "$_tag" "$_artifact"
    commit_and_tag "$_dir" "$_tag" "$_msg"
    _counter=${_tag##*.}
    write_state "$_counter"
    push_release "$_dir" "$_tag"
}
