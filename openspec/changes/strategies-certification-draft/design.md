# Design — strategies-certification-draft

## Strategy identity

Each Strategy maps a real industrial philosophy to a reward-pool weighting axis:

| Strategy | Theme | Pool bias |
| --- | --- | --- |
| Kanban | flow & inventory | conveyors, storage, transport |
| Lean | waste reduction | building costs, efficiency, low-waste chains |
| Stock zéro / JIT | just-in-time | fast unit production, low reserves, high risk |
| Six Sigma | quality | rare, expensive, high-power units and buildings |
| Agile / Scrum | iteration & rotations | re-spec, boss-window switching, cycle bonuses |

- Bias = **weights, not filters**: target ~70/30 toward the Strategy axis vs the
  generic pool (value to calibrate). Hard exclusions are forbidden by design.
- The bias is **displayed on the Strategy card** at draft time: the player sees
  exactly how their RNG will shift. No hidden guidance after the run starts —
  once the draft is done, the pool weights are fixed and visible.
- Existing commanders (Vanguard, Architect, Industrialist) are remapped onto the
  initial Strategy set; their current active abilities are kept or folded into
  the pool bias (content decision, listed in tasks).

## Draft

- Order: random per run, announced to all before picks.
- **Lock**: once a player confirms a Strategy, it is unavailable to the others.
  The draft is the first cooperative strategic decision of the run.
- Solo run: free pick, no lock contention.
- AFK: safety timeout (default `60s` per turn) auto-picks a random remaining
  Strategy.
- Meta-progression gates (reach wave 10/20, win a run) unlock additional
  Strategies over time; the draft pool for a run is the intersection of all
  players' unlocked sets.

## Lobby state

- Until every connected player has a Strategy: **no builder units spawned, no
  wave countdown**. The world exists (map generated) but the run has not started.
- This is a lobby, not a pause: no freeze logic to get wrong, trivially
  testable ("wave timer stays at initial value until all picked").
- Late joiners mid-run: assigned a random remaining Strategy automatically
  (no re-draft).

## Architecture

- Strategy = data (id, display name, pool weights, optional ability), not code
  paths; registered like existing commanders so meta-progression code is reused.
- Pool weighting implemented in the reward generator: strategy weights compose
  with the typed-bonus weights (see tech-tree-and-unit-lifespan change). Fully
  seeded and headless-testable.
- Net: draft-order broadcast, pick/lock confirmation, lobby-ready packet.

## Acceptance criteria (headless-testable)

1. Seeded run: draft order deterministic; announced order matches generation.
2. Lock: second player picking the same Strategy is rejected.
3. Lobby: no builder units and no wave countdown while any player unpicked.
4. AFK timeout: auto-pick happens and is announced.
5. Pool bias: over N seeded generations, a Strategy's axis dominates (~70/30,
   tolerance band) without ever excluding other axes.
6. Meta-progression: locked Strategies do not appear in the draft pool.
