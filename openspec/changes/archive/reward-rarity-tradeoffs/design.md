# Design: reward categories x rarity

## Model

```
RewardType: TRADEOFF | PLAN | RESOURCE
Rarity:     NORMAL(x1, grey) | RARE(x2, blue) | EPIC(x4, violet)
Reward (abstract): id, type, rarity, displayName, description + write/read
├── TradeoffReward  bonusId/value, malusId/value   → session.addGlobalModifier(+/-)
├── PlanReward      Seq<String> contentNames        → session.unlockForGroup(each)
└── ResourceReward  Seq<ItemStack-like> items      → default team cores' items
```

- `Rarity` (`rewards/Rarity.java`): enum with `color` (chat markup, e.g.
  `[#9d9d9d]`), `multiplier` (1/2/4), and a weighted `roll(Rand)` using static
  weight fields (`normalWeight=0.60`, `rareWeight=0.30`, `epicWeight=0.10`).
- `Reward` gains a `rarity` field, serialized after the base fields
  (`write.str(rarity.name())`), so clients reconstruct the colored display.
- Rarity color is baked into `displayName`/`description` at generation time by
  the generator (e.g. `[blue]+20% ...`), so the chat announcement needs no
  changes.

## Generation (`core/RewardGenerator`)

`generateChoices(session, player, wave)` still returns exactly 3 rewards,
fully driven by `session.rand` (deterministic per seed):

1. Roll a category uniformly among TRADEOFF / PLAN / RESOURCE.
2. Roll a rarity via `Rarity.roll(rand)`.
3. Build the reward with magnitude = base x rarity.multiplier.

- **Trade-off**: pick two distinct ids from `GlobalModifierRegistry.allIds()`;
  value = `0.10f * rarity.multiplier`; first +, second −. Applied via
  `session.addGlobalModifier(new GlobalModifier(id, ±value, name))`.
  Base trade value is a static knob `TRADEOFF_BASE = 0.10f`.
- **Plan**: pick `1 * rarity.multiplier` distinct content names from the
  not-yet-unlocked entries of `BUILDING_POOL` ∪ `UNIT_POOL` (blocks via
  `content.block(name)`, units via `content.unit(name)`); if the pool is
  exhausted, fall back to a RESOURCE choice. Applied via
  `session.unlockForGroup(content)`.
- **Resource**: pick 1–3 distinct `Item`s (count itself scales mildly with
  rarity), amount = `baseAmount * rarity.multiplier` with
  `RESOURCE_BASE = 100` (static knob). Applied by adding items to every core of
  `state.rules.defaultTeam` (null-guarded for headless tests).

## Serialization

`Reward.write` = id, type, rarity, displayName, description; `RewardSerializer`
switches on type for the specific fields:

- TRADEOFF: bonusId, bonusValue(f), malusId, malusValue(f)
- PLAN: int n, then n strings (content names)
- RESOURCE: int n, then n pairs (item name, amount int)

Content is resolved by name at read time (`content.block` / `content.unit` /
`content.item`), same pattern as the old `BuildingReward`.

## Deletions

- `rewards/rewards/`: BuildingReward, BuildingBonusReward, SchematicReward,
  UnitBonusReward, GlobalModifierReward, SynergyReward.
- `rewards/SchematicGenerator.java`, `net/MindTheIndustryPackets.java` (whole
  `net/` package), the `MindTheIndustryPackets.register()` call in the mod.
- Session schematic machinery (`playerSchematics`, `unlockSchematicForPlayer`,
  `hasSchematic`) — nothing else uses it.
- `UnitBonus` decoupled from the deleted `UnitBonusReward.BonusType`: new
  `modifiers/UnitBonusType` enum (DAMAGE, HEALTH, SPEED, RELOAD, ARMOR), session
  references updated. Per-player bonus machinery itself stays.

## Tests (headless, seeded)

- Generator: exactly 3 choices; deterministic per seed; different seeds differ;
  all 3 categories and all 3 rarities appear over many rolls; plans never offer
  already-unlocked content; trade-off always has 2 distinct stats with opposite
  signs and magnitude = 10% x multiplier; resource amounts scale with rarity.
- Serializer: write→read round-trip for each of the 3 reward types incl. rarity.
- Apply: trade-off moves the two rules multipliers in opposite directions;
  plan unlocks content in `state.rules.researched`; resource gift adds items to
  the default team core.
