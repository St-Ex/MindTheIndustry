#!/bin/sh
# Print the last scene-graph dump written by the devtools probe.
# Usage: mise lan:scene <p1|p2> [jq-filter]
set -eu
. "$(dirname "$0")/common.sh"

target="$1"
filter="${2:-}"
f="$RUN_DIR/$target/mti-scene.json"
if [ ! -f "$f" ]; then
    echo "no scene dump yet; run: mise lan:probe $target dump" >&2
    exit 1
fi
if [ -n "$filter" ] && command -v jq >/dev/null 2>&1; then
    jq "$filter" "$f"
elif command -v jq >/dev/null 2>&1; then
    jq . "$f"
else
    cat "$f"
fi
