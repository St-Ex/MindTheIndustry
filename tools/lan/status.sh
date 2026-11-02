#!/bin/sh
# Show LAN stack state: processes, windows, last log lines.
# Usage: mise lan:status
set -eu
. "$(dirname "$0")/common.sh"

echo "live game instances (agent-launched, shared registry): $(live_game_count)/$MAX_LIVE_GAMES"
echo "-- registry:"
registry_list | sed 's/^/   /'
for p in p1 p2; do
    pidfile="$RUN_DIR/$p.pid"
    if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
        echo "== $p: running (pid $(cat "$pidfile"), window $(win_id "$p" 2>/dev/null || echo '?'))"
    else
        echo "== $p: not running"
    fi
done

echo "-- windows on $LAN_DISPLAY:"
list_game_windows | sed 's/^/   /' || true
[ -z "$(list_game_windows)" ] && echo "(none)"

for p in p1 p2; do
    log="$RUN_DIR/$p/game.log"
    if [ -f "$log" ]; then
        echo "-- $p game.log (tail):"
        tail -n 5 "$log" | sed 's/^/   /'
    fi
done
