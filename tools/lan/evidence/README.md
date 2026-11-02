# LAN test evidence

Screenshots captured with `mise lan:proof <p1|p2|root> <name> <note>` while the
LAN stack was up, committed here so PRs/features can embed them — relative
links like `![proof](tools/lan/evidence/lan-two-clients.png)` render in PR
bodies when the file exists in the PR head branch. Runtime shots stay
gitignored in `tools/lan/shots/`; only curated proof shots land here.

## lan-two-clients — 2026-11-02 (validated run)

![lan-two-clients](lan-two-clients.png)

Root screenshot with both LAN instances side by side (p1 at x=0, p2 at x=960), both in-game on the MTI run map after p1 hosted :6567 and p2 joined 127.0.0.1.

## p1-ingame-mti-run — 2026-11-02 (validated run)

![p1-ingame-mti-run](p1-ingame-mti-run.png)

p1 (host) in-game on mindtheindustry-run; mod server controller active.

## p2-ingame-mti-run — 2026-11-02 (validated run)

![p2-ingame-mti-run](p2-ingame-mti-run.png)

p2 (joined client) in-game on the same map after receiving world data.
