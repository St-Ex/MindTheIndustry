#!/bin/sh
# Screenshot one instance window (or the whole screen) into tools/lan/shots/.
# For p1/p2 the capture is composited with overlapping popup/dialog windows
# (this X server does not paint child windows into the root pixmap).
# Usage: mise lan:shot <p1|p2|root> <name>
set -eu
. "$(dirname "$0")/common.sh"

target="$1"
name="$2"
mkdir -p "$SHOTS_DIR"

case "$target" in
    p1|p2)
        id="$(win_id "$target")"
        if [ -z "$id" ]; then
            echo "no window recorded for $target; run mise lan:up first" >&2
            exit 1
        fi
        python3 "$LAN_DIR/capture.py" "$LAN_DIR" "$target" "$SHOTS_DIR/$name.png" \
            || xcmd import -window "$id" "$SHOTS_DIR/$name.png"
        ;;
    root)
        xcmd import -window root "$SHOTS_DIR/$name.png"
        ;;
    *)
        echo "target must be p1, p2 or root" >&2
        exit 1
        ;;
esac

echo "saved $SHOTS_DIR/$name.png ($(identify -format '%wx%h' "$SHOTS_DIR/$name.png"))"
