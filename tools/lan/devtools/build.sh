#!/bin/sh
# Build the dev-only probe mod (tools/lan/devtools) into tools/lan/.cache/
# using plain javac against the desktop game jar. The result is copied into
# each LAN instance's mods/ dir by up.sh; it is never part of the shipped mod.
# Usage: sh tools/lan/devtools/build.sh   (prints the jar path)
set -eu
. "$(dirname "$0")/../common.sh"
# common.sh derives LAN_DIR from $0, which is this script one level deeper
LAN_DIR="$(cd "$(dirname "$0")/.." && pwd)"

GJ="${MTI_GAME_JAR:-$(game_jar)}"
if [ -z "$GJ" ] || [ ! -f "$GJ" ]; then
    echo "no desktop game jar found; set MTI_GAME_JAR or cache one in tools/lan/.cache" >&2
    exit 1
fi

SRC="$LAN_DIR/devtools/src"
OUT="$LAN_DIR/.cache/mti-devtools.jar"
CLASSES="$LAN_DIR/.cache/devtools-classes"

if [ ! -f "$OUT" ] || [ -n "$(find "$SRC" "$LAN_DIR/devtools/mod.hjson" -newer "$OUT" -print -quit 2>/dev/null)" ]; then
    echo "building devtools jar with javac against $GJ"
    rm -rf "$CLASSES"
    mkdir -p "$CLASSES"
    find "$SRC" -name '*.java' > "$LAN_DIR/.cache/devtools-sources.txt"
    javac -cp "$GJ" -d "$CLASSES" @"$LAN_DIR/.cache/devtools-sources.txt"
    cp "$LAN_DIR/devtools/mod.hjson" "$CLASSES/"
    (cd "$CLASSES" && jar cf "$OUT" mod.hjson mti)
fi
echo "$OUT"
