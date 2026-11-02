# Shared config for the LAN UI-validation stack. Sourced by the other scripts.
# See tools/lan/README.md.

# The agent shell exports an LD_LIBRARY_PATH (editor runtime libs) whose
# libstdc++ breaks system libraries (ImageMagick/libicu, libGL) — sanitize it
# for everything this stack runs. Spots that genuinely need the user-local X
# tools (Xvfb, xdotool) set their own LD_LIBRARY_PATH explicitly.
unset LD_LIBRARY_PATH

LAN_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$LAN_DIR/../.." && pwd)"
RUN_DIR="$LAN_DIR/run"
SHOTS_DIR="$LAN_DIR/shots"
# The mod jar name follows the checkout dir name (<project>Desktop.jar), so
# resolve it dynamically. Override with MTI_MOD_JAR if needed.
mod_jar(){
    if [ -n "${MTI_MOD_JAR:-}" ] && [ -f "$MTI_MOD_JAR" ]; then
        echo "$MTI_MOD_JAR"
        return 0
    fi
    find "$REPO_ROOT/build/libs" -name '*Desktop.jar' 2>/dev/null | sort | tail -1
}

# X display the two instances render on. Defaults to the session display;
# set LAN_DISPLAY=:99 etc. to use a private Xvfb instead.
LAN_DISPLAY="${LAN_DISPLAY:-${DISPLAY:-:0}}"

LAN_WIDTH=960
LAN_HEIGHT=600

# Path to the vanilla desktop game jar (non-Steam official release, cached in
# tools/lan/.cache by up.sh; the Gradle cache only holds the core jar, which
# has no DesktopLauncher). Override with MTI_GAME_JAR if needed.
GAME_VERSION="v160" # must match mindustryVersion in build.gradle.kts
game_jar(){
    if [ -n "${MTI_GAME_JAR:-}" ] && [ -f "$MTI_GAME_JAR" ]; then
        echo "$MTI_GAME_JAR"
        return 0
    fi
    local cached
    cached="$(find "$LAN_DIR/.cache" -name 'Mindustry-*-desktop.jar' 2>/dev/null | sort -V | tail -1)"
    if [ -n "$cached" ]; then
        echo "$cached"
        return 0
    fi
    echo ""
}

# Recorded window id of an instance, e.g. win_id p1
win_id(){
    cat "$RUN_DIR/$1.win" 2>/dev/null
}

# Dev-only probe mod jar (built by devtools/build.sh into .cache, gitignored).
# Copied into each instance's mods/ by up.sh; never part of the shipped mod.
devtools_jar(){
    sh "$LAN_DIR/devtools/build.sh"
}

# Cap on agent-launched live game instances (shared, free to use; going above
# requires explicit user approval — enforced in up.sh).
MAX_LIVE_GAMES=2

# Shared live-instance registry, visible to every agent/worktree on this host:
# one JSON file per live instance. Override with MTI_LAN_REGISTRY.
REGISTRY="${MTI_LAN_REGISTRY:-$HOME/.local/share/mindtheindustry-lan/live}"
THIS_HOST="$(hostname)"

register_instance(){
    # register_instance <name> <pid> <display> [winid]
    mkdir -p "$REGISTRY"
    printf '{"host":"%s","name":"%s","pid":%s,"display":"%s","win":"%s","worktree":"%s","started":%s}\n' \
        "$THIS_HOST" "$1" "$2" "$3" "${4:-}" "$REPO_ROOT" "$(date +%s)" \
        > "$REGISTRY/$THIS_HOST-$1-$2.json"
}

unregister_instance(){
    rm -f "$REGISTRY/$THIS_HOST-$1-$2.json"
}

# True if a registry entry's pid is a live game process on this host.
entry_alive(){
    # entry_alive <json-file>
    local f="$1" host pid
    host=$(sed -n 's/.*"host":"\([^"]*\)".*/\1/p' "$f")
    pid=$(sed -n 's/.*"pid":\([0-9]*\).*/\1/p' "$f")
    [ "$host" = "$THIS_HOST" ] || return 1
    [ -d "/proc/$pid" ] && grep -qa 'DesktopLauncher\|Mindustry' "/proc/$pid/cmdline" 2>/dev/null
}

# Number of live agent-launched game instances on this host (shared registry,
# self-cleaning: stale entries whose pid is gone are removed).
live_game_count(){
    local n=0 f
    mkdir -p "$REGISTRY"
    for f in "$REGISTRY"/*.json; do
        [ -f "$f" ] || continue
        if entry_alive "$f"; then
            n=$((n + 1))
        else
            sed -n 's/.*"host":"\([^"]*\)".*/\1/p' "$f" | grep -qx "$THIS_HOST" && rm -f "$f"
        fi
    done
    echo "$n"
}

# List all registry entries (any host), one per line: host name pid display win state
registry_list(){
    local f
    mkdir -p "$REGISTRY"
    for f in "$REGISTRY"/*.json; do
        [ -f "$f" ] || continue
        local host name pid disp win
        host=$(sed -n 's/.*"host":"\([^"]*\)".*/\1/p' "$f")
        name=$(sed -n 's/.*"name":"\([^"]*\)".*/\1/p' "$f")
        pid=$(sed -n 's/.*"pid":\([0-9]*\).*/\1/p' "$f")
        disp=$(sed -n 's/.*"display":"\([^"]*\)".*/\1/p' "$f")
        win=$(sed -n 's/.*"win":"\([^"]*\)".*/\1/p' "$f")
        if entry_alive "$f"; then st=live; elif [ "$host" = "$THIS_HOST" ]; then st=dead; rm -f "$f"; else st=remote; fi
        echo "$host $name pid=$pid display=$disp win=$win $st"
    done
}

xcmd(){
    DISPLAY="$LAN_DISPLAY" "$@"
}

# User-local X tools (Xvfb, xdotool) extracted from Ubuntu debs into
# ~/.local/mti-x11 — used when LAN_DISPLAY is a private Xvfb without a WM.
XTOOLS="$HOME/.local/mti-x11"
xdotool_cmd(){
    DISPLAY="$LAN_DISPLAY" LD_LIBRARY_PATH="$XTOOLS/usr/lib/x86_64-linux-gnu:$XTOOLS/usr/lib" \
        "$XTOOLS/usr/bin/xdotool" "$@"
}

# List game window ids, one per line. Prefers wmctrl (needs a WM); falls back
# to xwininfo -root -tree (works on a bare Xvfb).
list_game_windows(){
    local wins
    wins="$(xcmd wmctrl -lG 2>/dev/null | grep 'Mindustry' | awk '{print $1}')"
    if [ -n "$wins" ]; then
        echo "$wins"
        return 0
    fi
    DISPLAY="$LAN_DISPLAY" xwininfo -root -tree 2>/dev/null \
        | grep '"Mindustry"' | grep -oE '^\s+0x[0-9a-f]+' | tr -d ' '
}

# Move/resize a window (works without a WM via xdotool; wmctrl first).
move_window(){
    local id="$1" x="$2" y="$3" w="$4" h="$5"
    xcmd wmctrl -i -r "$id" -e "0,$x,$y,$w,$h" 2>/dev/null || true
    xdotool_cmd windowsize "$id" "$w" "$h" 2>/dev/null || true
    xdotool_cmd windowmove "$id" "$x" "$y" 2>/dev/null || true
}

# Activate (raise + focus) a window; without a WM, wmctrl may fail, so also do
# a raw X raise via python as a fallback.
activate_window(){
    local id="$1"
    xcmd wmctrl -i -a "$id" 2>/dev/null || true
    DISPLAY="$LAN_DISPLAY" python3 - "$id" <<'EOF' 2>/dev/null || true
import sys
from Xlib import X, display as xdisplay
d = xdisplay.Display()
root = d.screen().root
w = d.create_resource_object("window", int(sys.argv[1], 0))
top = w
cur = w
while cur.id != root.id:
    top = cur
    cur = cur.query_tree().parent
top.configure(stack_mode=X.Above)
w.set_input_focus(X.RevertToParent, X.CurrentTime)
d.sync()
EOF
}
