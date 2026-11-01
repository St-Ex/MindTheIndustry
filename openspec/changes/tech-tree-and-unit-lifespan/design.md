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
- Node structure reuses the Mindustry research-tree concept (data-driven).
- **Standard pack (owner, Nov 2026)**: the initial unlocked set is a named,
  curated baseline — the **standard pack** — identical for every run without
  certification mutations. Contents (which nodes are pre-unlocked at run
  start) are data-driven; values owned by the balance pass.
  - **Protected floor**: survival-critical basics never vary, so a mutated
    pack is always playable.
  - **Certifications (M2) mutate the pack** — add or remove techs above the
    floor, group-wide, at the draft. Mutations are a chosen run identity, not
    a per-player lock-out (the rejected lock-outs were individual; the whole
    team shares the mutated pack).
  - Adding techs must be paid inside the certification's trade-off budget;
    removing techs is an accepted restriction. Removed techs are gone for
    the run (not re-purchasable with tech currency) — revisit at balance
    pass if too harsh.
  - **M3 scope: implement the pack as data** (declarable, diffable set) so
    certification mutations hook in without rework; no mutation logic in M3.
- **Constraint (DECISIONS.md, clarified by owner)**: the MTI planet tech tree is **based on Serpulo's tree**, with its own root at the run `SectorPreset` — never registered under the campaign trees. `new TechNode(...)` reassigns `content.techNode`, so any node creation must stay under the MTI root to avoid leaking into the campaign. The run tech tree (currency purchases) must either integrate with this existing Serpulo-based MTI tree or stay a parallel data structure. Supporting a second tech tree (Erekir) is **deferred — out of scope for now**.
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
8. A fresh run starts with exactly the standard pack's nodes unlocked
   (deterministic per config; protected floor present).
