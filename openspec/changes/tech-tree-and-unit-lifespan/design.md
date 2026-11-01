# Design — tech-tree-and-unit-lifespan

## Tech currency

- Name TBD ("research grants"?). Session counter per run; no map presence, no
  conveyor, no storage block. Not tradeable in the boss window (it is
  progression, not loot).
- Sources:
  - Forfeited proposals (timer expiry, explicit decline): primary source — makes
    non-aligned RNG "almost normal" instead of pure loss.
  - Kills: small amount per enemy unit, larger per boss. Exact curve deferred to
    balance pass.
- Spent in the tech tree UI (later) or chat commands initially.

## Tech tree

- Group-wide: a purchase by anyone unlocks the plan for everyone. Rationale:
  identity must come from bonus-malus and entity assignment, not lock-outs.
- Node structure reuses the Mindustry research-tree concept (data-driven);
  initial tree = subset of vanilla buildings/units with price tiers.
- Unit unlock = plan + built producer. The producer itself is a tree node.
- The tree is a **backbone, not the whole economy**: random plan proposals
  remain as wave rewards (variance layer) — sometimes offering a node early,
  at a discount or with a bonus-malus attached. Hybrid by design.

## Unit lifespan

- Default: persistent units, **unit cap** per player (value TBD, config).
- Cap-augmenting rewards exist (typed bonus axis).
- **Run modifiers** (chosen at start, or offered later): units produced 2x fast
  but last N waves; alpha-strike builds (firepower to kill a wave fast because
  the units won't outlast it).
- **Excess units**: factories keep producing above cap; units spawned beyond
  the cap get an expiry (measured in waves). The timer is visible (readable
  bet, readable loss).
- **Waves, not seconds**: an expiring unit survives until the end of wave
  `W + n`. No mid-wave melt during the boss window.
- Vanilla behaviour preserved: a destroyed player builder unit respawns at the
  core, camera follows; assignment reverts to standard. **No rogue-like penalty
  on builder death** (with bonus-malus bundles, losing the unit can even be
  near-positive; do not fight the grain of Mindustry).
- Future archetype (noted, out of scope): battlefield factories producing
  disposable 15-second swarms ("factory-turrets").

## Architecture

- Tech currency: session field + event hooks (proposal forfeited, unit killed,
  boss killed). Headless-testable with seeded waves.
- Tech tree: data-driven nodes (id, cost, prerequisites, unlocked plans);
  unlock applies plan sets to all players.
- Unit lifespan: per-unit spawn record (birth wave, expiry wave) held in the
  session; end-of-wave sweep removes expired units. Deterministic, no UI
  dependency.
- Net: tech purchase packet, unit expiry is server-authoritative.

## Acceptance criteria (headless-testable)

1. Forfeited proposal grants tech currency; explicit decline too.
2. Kills grant currency (per-unit and per-boss amounts, seeded wave).
3. Tech purchase by one player unlocks the plan for every player.
4. Unit cap: production beyond cap still spawns units, marked expiring.
5. Expiring units are removed at the end of their expiry wave (not mid-wave).
6. Persistent units below cap never expire.
7. Destroyed builder unit respawns at core with vanilla behaviour, no penalty.
