#!/bin/sh
# Stop the two LAN instances started by up.sh. Logs and screenshots are kept.
# Usage: mise lan:down
set -eu
. "$(dirname "$0")/common.sh"

for p in p1 p2; do
    pidfile="$RUN_DIR/$p.pid"
    if [ -f "$pidfile" ]; then
        pid="$(cat "$pidfile")"
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            echo "$p (pid $pid) stopped"
        fi
        unregister_instance "$p" "$pid"
        rm -f "$pidfile" "$RUN_DIR/$p.win"
    fi
done

# stop the private Xvfb too, if up.sh started one; wait until it is really
# dead so the next lan:up does not race the released X socket
if [ -f "$RUN_DIR/xvfb.pid" ]; then
    xpid="$(cat "$RUN_DIR/xvfb.pid")"
    if kill -0 "$xpid" 2>/dev/null; then
        kill "$xpid" 2>/dev/null || true
        i=0
        while kill -0 "$xpid" 2>/dev/null && [ "$i" -lt 20 ]; do
            i=$((i + 1))
            sleep 0.25
        done
        echo "private Xvfb (pid $xpid) stopped"
    fi
    rm -f "$RUN_DIR/xvfb.pid"
fi
