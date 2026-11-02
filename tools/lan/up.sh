#!/bin/sh
# Boot two local instances of the vanilla desktop game (official non-Steam
# jar cached in tools/lan/.cache) with the mod jar installed, side by side on
# LAN_DISPLAY.
# Usage: mise lan:up
set -eu
. "$(dirname "$0")/common.sh"

# Build the mod jar if missing or stale. MTI_MOD_JAR pins an external jar and
# skips the build entirely (useful when this checkout's src is mid-refactor).
if [ -n "${MTI_MOD_JAR:-}" ]; then
    MOD_JAR="$MTI_MOD_JAR"
else
    MOD_JAR="$(mod_jar)"
    if [ -z "$MOD_JAR" ] || [ -n "$(find "$REPO_ROOT/src" -newer "$MOD_JAR" -print -quit 2>/dev/null)" ]; then
        echo "building mod jar..."
        (cd "$REPO_ROOT" && ./gradlew jar -q)
        MOD_JAR="$(mod_jar)"
    fi
    if [ -z "$MOD_JAR" ]; then
        echo "mod jar not found under build/libs after build" >&2
        exit 1
    fi
fi
echo "mod jar: $MOD_JAR"

GJ="$(game_jar)"
if [ -z "$GJ" ] || [ ! -f "$GJ" ]; then
    echo "no desktop game jar cached in tools/lan/.cache. One-time setup:" >&2
    echo "  mkdir -p tools/lan/.cache" >&2
    echo "  curl -sL --fail -o tools/lan/.cache/Mindustry-$GAME_VERSION-desktop.jar \\" >&2
    echo "    https://github.com/Anuken/Mindustry/releases/download/$GAME_VERSION/Mindustry.jar" >&2
    echo "(official non-Steam release; or point MTI_GAME_JAR at any desktop jar)" >&2
    exit 1
fi
echo "game jar: $GJ"

mkdir -p "$RUN_DIR" "$SHOTS_DIR"

# If LAN_DISPLAY is a private display (e.g. :99) with nothing listening, start
# our user-local Xvfb there so the agent gets its own screen+pointer and the
# session display is never touched.
if ! DISPLAY="$LAN_DISPLAY" xdpyinfo >/dev/null 2>&1; then
    # nothing answers on this display -> any leftover X socket is stale
    sock="/tmp/.X11-unix/X${LAN_DISPLAY#:}"
    [ -S "$sock" ] && rm -f "$sock"
    XTOOLS="${MTI_XTOOLS:-$HOME/.local/mti-x11}"
    if [ -x "$XTOOLS/usr/bin/Xvfb" ]; then
        echo "starting private Xvfb on $LAN_DISPLAY (user-local, ~/.local/mti-x11)"
        LD_LIBRARY_PATH="$XTOOLS/usr/lib/x86_64-linux-gnu:$XTOOLS/usr/lib" \
            nohup "$XTOOLS/usr/bin/Xvfb" "$LAN_DISPLAY" -screen 0 1920x1200x24 -nolisten tcp \
            >"$RUN_DIR/xvfb.log" 2>&1 &
        echo $! > "$RUN_DIR/xvfb.pid"
        # wait until the server actually accepts connections (boot race:
        # a just-killed Xvfb may still hold the socket for a moment)
        i=0
        while ! DISPLAY="$LAN_DISPLAY" xdpyinfo >/dev/null 2>&1; do
            i=$((i + 1))
            if [ "$i" -gt 40 ]; then
                # stale X socket from a killed server: remove and retry once
                sock="/tmp/.X11-unix/X${LAN_DISPLAY#:}"
                if [ -S "$sock" ] && ! kill -0 "$(cat "$RUN_DIR/xvfb.pid")" 2>/dev/null; then
                    echo "removing stale X socket $sock and retrying"
                    rm -f "$sock"
                    LD_LIBRARY_PATH="$XTOOLS/usr/lib/x86_64-linux-gnu:$XTOOLS/usr/lib" \
                        nohup "$XTOOLS/usr/bin/Xvfb" "$LAN_DISPLAY" -screen 0 1920x1200x24 -nolisten tcp \
                        >"$RUN_DIR/xvfb.log" 2>&1 &
                    echo $! > "$RUN_DIR/xvfb.pid"
                    i=0
                    while ! DISPLAY="$LAN_DISPLAY" xdpyinfo >/dev/null 2>&1; do
                        i=$((i + 1))
                        [ "$i" -gt 40 ] && { echo "Xvfb on $LAN_DISPLAY did not come up; see $RUN_DIR/xvfb.log" >&2; exit 1; }
                        sleep 0.5
                    done
                else
                    echo "Xvfb on $LAN_DISPLAY did not come up; see $RUN_DIR/xvfb.log" >&2
                    exit 1
                fi
            fi
            sleep 0.5
        done
    else
        echo "nothing on $LAN_DISPLAY and no user-local Xvfb at $XTOOLS" >&2
    fi
fi

live="$(live_game_count)"
if [ "$live" -ge "$MAX_LIVE_GAMES" ]; then
    echo "cap reached: $live/$MAX_LIVE_GAMES live game instances already running;" >&2
    echo "refusing to start more. Ask the user to approve going above $MAX_LIVE_GAMES." >&2
    exit 1
fi

DEVJAR="$(devtools_jar)"
echo "devtools jar: $DEVJAR"

for p in p1 p2; do
    d="$RUN_DIR/$p"
    mkdir -p "$d/mods"
    cp "$MOD_JAR" "$d/mods/"
    cp "$DEVJAR" "$d/mods/"
    if [ -f "$RUN_DIR/$p.pid" ] && kill -0 "$(cat "$RUN_DIR/$p.pid")" 2>/dev/null; then
        echo "$p already running (pid $(cat "$RUN_DIR/$p.pid"))"
        continue
    fi
    # Sanitize the env for the game process: agent shells often export an
    # LD_LIBRARY_PATH (editor/runtime libs) whose libstdc++ breaks libGL —
    # the game then dies with "Couldn't find matching GLX visual" even though
    # GLX works. LIBGL_ALWAYS_SOFTWARE makes mesa use llvmpipe on a private
    # Xvfb (no GPU there).
    env -u LD_LIBRARY_PATH LIBGL_ALWAYS_SOFTWARE=1 \
        MINDUSTRY_DATA_DIR="$d" DISPLAY="$LAN_DISPLAY" \
        nohup java -cp "$GJ" mindustry.desktop.DesktopLauncher \
        -width "$LAN_WIDTH" -height "$LAN_HEIGHT" -maximized false \
        >"$d/game.log" 2>&1 &
    echo $! > "$RUN_DIR/$p.pid"
    register_instance "$p" "$!" "$LAN_DISPLAY"
    echo "$p started (pid $!)"
done

# Wait for both windows to appear, then pin them side by side and record ids.
echo "waiting for game windows on $LAN_DISPLAY (boot can take a while)..."
i=0
while [ "$i" -lt 150 ]; do
    n=$(list_game_windows | grep -c . || true)
    [ "$n" -ge 2 ] && break
    i=$((i + 1))
    sleep 2
done

wins=$(list_game_windows | sort)
if [ "$(echo "$wins" | grep -c .)" -lt 2 ]; then
    echo "game windows did not appear; check $RUN_DIR/p*/game.log" >&2
    exit 1
fi

first=1
for id in $wins; do
    if [ "$first" -eq 1 ]; then
        move_window "$id" 0 0 "$LAN_WIDTH" "$LAN_HEIGHT"
        echo "$id" > "$RUN_DIR/p1.win"
        register_instance p1 "$(cat "$RUN_DIR/p1.pid")" "$LAN_DISPLAY" "$id"
        first=0
    else
        move_window "$id" "$LAN_WIDTH" 0 "$LAN_WIDTH" "$LAN_HEIGHT"
        echo "$id" > "$RUN_DIR/p2.win"
        register_instance p2 "$(cat "$RUN_DIR/p2.pid")" "$LAN_DISPLAY" "$id"
    fi
done

echo "p1 window: $(win_id p1) at x=0    p2 window: $(win_id p2) at x=$LAN_WIDTH"
echo "next: mise lan:status, mise lan:shot p1 menu, mise lan:ascii menu"
