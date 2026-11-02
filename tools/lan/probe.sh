#!/bin/sh
# Send a probe command to a LAN instance's devtools mod (devtools/).
# Commands: dump | highlight <text> | clear
# The mod answers by writing run/<target>/mti-scene.json (see mise lan:scene).
# Usage: mise lan:probe <p1|p2> <command...>
set -eu
. "$(dirname "$0")/common.sh"

target="$1"
shift
# mise shell-quotes variadic args ('highlight Play' arrives with literal
# quotes) — strip them and rejoin
cmd=""
for a in "$@"; do
    case "$a" in
        \'*\') a="${a#?}"; a="${a%?}" ;;
    esac
    cmd="$cmd $a"
done
cmd="${cmd# }"
mkdir -p "$RUN_DIR/$target"
printf '%s\n' "$cmd" >> "$RUN_DIR/$target/mti-probe.log"
printf '%s\n' "$cmd" > "$RUN_DIR/$target/mti-probe"
echo "probe -> $target: $cmd"
echo "answer (after ~1s): run/$target/mti-scene.json  (mise lan:scene $target)"
