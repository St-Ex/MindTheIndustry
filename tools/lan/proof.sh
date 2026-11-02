#!/bin/sh
# Capture a screenshot as COMMITTED PR evidence: tools/lan/evidence/<name>.png
# plus a markdown entry in tools/lan/evidence/README.md. PR bodies can embed
# these with relative links, e.g. ![proof](tools/lan/evidence/lan-two-clients.png)
# (GitHub renders them when the file exists in the PR head branch).
# Usage: mise lan:proof <p1|p2|root> <name> [note...]
set -eu
. "$(dirname "$0")/common.sh"

target="$1"
name="$2"
shift 2
# mise shell-quotes variadic args — strip the literal quotes
note=""
for a in "$@"; do
    case "$a" in
        \'*\') a="${a#?}"; a="${a%?}" ;;
    esac
    note="$note $a"
done
note="${note# }"

EVID="$LAN_DIR/evidence"
mkdir -p "$EVID"
sh "$LAN_DIR/shot.sh" "$target" "proof-$name" >/dev/null
cp "$SHOTS_DIR/proof-$name.png" "$EVID/$name.png"

if [ ! -f "$EVID/README.md" ]; then
    printf '# LAN test evidence\n\nScreenshots captured with `mise lan:proof <p1|p2|root> <name> <note>` while the stack was up, committed here so PRs/features can embed them (relative links render in PR bodies when the file is in the head branch). Runtime shots stay gitignored in `tools/lan/shots/`.\n' > "$EVID/README.md"
fi
printf '\n## %s — %s\n\n![%s](%s.png)\n\n%s\n' "$name" "$(date -u +%Y-%m-%dT%H:%MZ)" "$name" "$name" "${note:-no note}" >> "$EVID/README.md"
echo "saved $EVID/$name.png (+ evidence/README.md entry)"

# print a ready-to-paste PR embed line. Evidence PNGs are Git LFS objects:
# raw.githubusercontent.com serves only the LFS pointer, so use the blob page
# with ?raw=true, which resolves LFS content. Absolute URL because relative
# links in PR bodies resolve against the base branch and break until merged.
branch="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo HEAD)"
remote="$(git -C "$REPO_ROOT" remote get-url origin 2>/dev/null || echo '')"
case "$remote" in
    *github.com[:/]*)
        gh_path="${remote#*github.com[:/]}"
        gh_path="${gh_path%.git}"
        echo "PR embed line:"
        echo "![${name}](https://github.com/$gh_path/blob/$branch/tools/lan/evidence/$name.png?raw=true)"
        ;;
esac
