# Proposal: post-wave rewards as trade-offs, plans and resource gifts with rarity

## Why

The current reward pool (building unlock, building/unit bonus, schematic, global
modifier, synergy) mixes categories with different scopes and offers no sense of
power scaling within a single wave reward. The roguelike feel we want after each
wave is: 3 choices, each one a gamble with a visible rarity, and every choice
being one of three clear categories.

## What

After every cleared wave, each of the 3 proposed choices SHALL be exactly one of:

1. **Trade-off** — always +X% on one stat and −X% on another stat (two distinct
   stats from the `GlobalModifierRegistry` pool). Base X = 10%.
2. **Plan** — unlocks 1 building or unit "plan" for the whole group (added to
   `state.rules.researched`), taken from the not-yet-unlocked pool.
3. **Resource gift** — a quantity of one or more resource types, delivered to the
   team cores.

Each choice SHALL have a **rarity** that multiplies magnitude:

| Rarity | Color | Multiplier | Trade-off | Plan | Resource |
|--------|-------|-----------|-----------|------|----------|
| NORMAL | grey  | x1 | ±10% | 1 plan | base qty |
| RARE   | blue  | x2 | ±20% | 2 plans | x2 qty |
| EPIC   | violet| x4 | ±40% | 4 plans | x4 qty |

Rarity weights (tunable static fields): NORMAL 60%, RARE 30%, EPIC 10%.

## Out of scope

- The old reward types (BUILDING, BUILDING_BONUS, SCHEMATIC, UNIT_BONUS,
  GLOBAL_MOD, SYNERGY) are removed from the reward pool. The per-player
  unit/building bonus session machinery stays (used elsewhere), only decoupled
  from the deleted reward classes.
- Schematic generation and the `SchematicUnlockPacket` network packet are
  removed entirely (schematics no longer exist as a reward).
- UI stays chat-based (`/mindtheindustry reward <index>`); no dialog.
