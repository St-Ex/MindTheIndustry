#!/bin/sh
# Print live agent-launched game instances as N/<cap> (shared registry, any
# worktree/agent on this host). Usage: mise lan:count
set -eu
. "$(dirname "$0")/common.sh"
echo "$(live_game_count)/$MAX_LIVE_GAMES"
echo "registry ($REGISTRY):"
registry_list | sed 's/^/  /'
