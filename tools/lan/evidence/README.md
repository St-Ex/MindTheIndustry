# LAN test evidence

Screenshots captured with `mise lan:proof <p1|p2|root> <name> <note>` while the
LAN stack was up, committed here so PRs/features can embed them. PNGs here are
tracked via **Git LFS** (`.gitattributes`: `tools/lan/evidence/*.png filter=lfs
...`), so install git-lfs (`git lfs install`) before pulling.

In PR bodies embed them with the absolute `blob/...?raw=true` URL (printed by
`lan:proof`) — relative links resolve against the base branch (broken until
merged) and `raw.githubusercontent.com` serves only the LFS pointer:

```md
![proof](https://github.com/St-Ex/MindTheIndustry/blob/lan-validation-stack/tools/lan/evidence/lan-two-clients.png?raw=true)
```

Runtime shots stay gitignored in `tools/lan/shots/`; only curated proof shots
land here.

## lan-two-clients — 2026-11-02 (validated run)

![lan-two-clients](lan-two-clients.png)

Root screenshot with both LAN instances side by side (p1 at x=0, p2 at x=960), both in-game on the MTI run map after p1 hosted :6567 and p2 joined 127.0.0.1.

## p1-ingame-mti-run — 2026-11-02 (validated run)

![p1-ingame-mti-run](p1-ingame-mti-run.png)

p1 (host) in-game on mindtheindustry-run; mod server controller active.

## p2-ingame-mti-run — 2026-11-02 (validated run)

![p2-ingame-mti-run](p2-ingame-mti-run.png)

p2 (joined client) in-game on the same map after receiving world data.

## players-2 — 2026-11-02T20:52Z

![players-2](players-2.png)

player list on p1 (host) after p2 joined 127.0.0.1: shows 2 players

## players-2-client — 2026-11-02T20:52Z

![players-2-client](players-2-client.png)

player list on p2 (joined client): 2 players
