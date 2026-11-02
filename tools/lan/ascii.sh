#!/bin/sh
# Render a screenshot as an ASCII luminance grid so the agent can "see" it
# through plain text (read_file cannot decode PNGs).
# Usage: mise lan:ascii <name>     (reads tools/lan/shots/<name>.png, 120 cols)
#        LAN_COLS=160 mise lan:ascii <name>
set -eu
. "$(dirname "$0")/common.sh"

name="$1"
img="$SHOTS_DIR/$name.png"
[ -f "$img" ] || img="$name" # allow passing a full path too
cols="${LAN_COLS:-120}"
# optional crop before rendering, to read small text: LAN_CROP=WxH+X+Y
tmp=""
if [ -n "${LAN_CROP:-}" ]; then
    tmp="$(mktemp --suffix=.png)"
    convert "$img" -crop "$LAN_CROP" +repage "$tmp"
    img="$tmp"
fi

read w h <<EOF
$(identify -format '%w %h' "$img")
EOF
# character cells are ~twice as tall as wide; keep the aspect ratio
rows=$(( cols * h / w / 2 ))
[ "$rows" -ge 1 ] || rows=1

convert "$img" -resize "${cols}x${rows}!" -colorspace Gray -depth 8 txt:- | awk -v cols="$cols" '
BEGIN { ramp = " .:-=+*#%@"; n = length(ramp); x = 0 }
{
    i = index($0, "(")
    if (i == 0) next
    s = substr($0, i + 1)
    j = index(s, ",")
    v = substr(s, 1, j - 1) + 0
    printf "%s", substr(ramp, int(v / 256 * n) + 1, 1)
    if (++x >= cols) { print ""; x = 0 }
}
END { if (x > 0) print "" }
'
[ -n "$tmp" ] && rm -f "$tmp"
