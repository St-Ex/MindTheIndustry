# Capability: roguelike run

## Purpose
A cooperative roguelike mode on top of Mindustry: enemy waves get harder every
round, and after each cleared wave every player picks one of three random
rewards that permanently change the run.

## Requirements

### Requirement: run entry point
The mod SHALL register a **MindTheIndustry planet** in the New Game planet menu.
Launching its start sector SHALL load a map based on the vanilla `labyrinth` map
(SectorPreset) and SHALL auto-start the run. The same run can be started on any
other map with `/mindtheindustry start`.

#### Scenario: launch from the menu
- WHEN a player launches the MindTheIndustry planet's sector THEN the labyrinth-based
  map loads and commander selection opens for each player

### Requirement: run lifecycle
The system SHALL run a session with states inactive → commander selection →
waves → ended, driven by the `/mindtheindustry` commands (registered server-side,
working identically in solo and multiplayer).

#### Scenario: start a run
- WHEN a player runs `/mindtheindustry start` THEN commander selection opens for each
  player (3 choices announced in chat), then wave 1 starts

#### Scenario: stop a run
- WHEN a player runs `/mindtheindustry stop` THEN the run ends and its bonuses are cleared

#### Scenario: skip a wave
- WHEN a player runs `/mindtheindustry skip` THEN all current enemies are killed and the
  next wave is prepared

### Requirement: reward choice after each wave
After each cleared wave, the system SHALL offer every player 3 random reward
choices, shown as clickable icons in a HUD box (hover = description, click =
vote via `RewardVotePacket`; also announced in chat as a text fallback); the
next wave SHALL start only once every player has picked.

#### Scenario: choices are seeded
- WHEN a run is started with a seed THEN the reward choices are identical on
  the server and on all clients (sent S→C via `RewardChoicesPacket`,
  serialized with `Reward.write`/`read`)

#### Scenario: pick a reward from the HUD
- WHEN the choice box is visible and a player clicks icon i THEN a
  `RewardVotePacket(i)` is sent to the server and the server applies choice i
- WHEN a player has voted THEN their choice box SHALL hide immediately
- WHEN all players have voted THEN an empty `RewardChoicesPacket` SHALL clear
  every remaining box

### Requirement: reward categories
The 3 choices SHALL always be exactly one of each category (order shuffled per
wave, seeded):
- **TRADEOFF** — +X% on one stat and −X% on another (two distinct stats from
  `GlobalModifierRegistry`), applied to the whole group
- **PLAN** — unlocks building/unit plans for the whole group
  (`state.rules.researched`)
- **RESOURCE** — a gift of one or more item types delivered to the team cores

### Requirement: reward rarity
Each choice SHALL roll a rarity that multiplies its magnitude:
NORMAL (grey, x1), RARE (blue, x2), EPIC (violet, x4) — trade-off percentage,
number of plans, and resource quantity respectively. Weights (60/30/10) and
base magnitudes (±10% trade, 100 base resources) SHALL be static knobs in
`Rarity` / `RewardGenerator`.

#### Scenario: rarity scales magnitude
- WHEN a RARE trade-off is generated THEN both stats move by ±20%
- WHEN an EPIC plan is generated THEN 4 plans are unlocked
- WHEN a RARE resource gift is generated THEN the amount is 2x the base

### Requirement: commanders
At run start, each player SHALL pick one commander with a unique ability
(`/mindtheindustry vote <index>`); commanders unlock permanently through meta-progression
(wave milestones, run completion).

### Requirement: active player per entity
`/mindtheindustry active <entityId> <isBuilding> <playerName|none>` SHALL assign which
player's bonuses apply to a given building or unit group.

### Requirement: difficulty curve
Enemy stats SHALL scale with the wave number, and the spawn composition SHALL be
rebuilt every wave from the run seed so waves go from easy to very hard.

#### Scenario: scaling waves
- WHEN wave N starts THEN `state.rules.spawns` is rebuilt via
  `WaveDesigner.buildSpawns(N, session.rand)` (vanilla `Waves.generate` scaled by
  `DifficultyTuning.difficultyForWave(N)`) and enemy stat multipliers increase

#### Scenario: boss waves
- WHEN wave N is a multiple of `DifficultyTuning.bossInterval` (20), starting at
  `bossWaveStart` THEN the spawn groups SHALL contain exactly one boss group
  (`StatusEffects.boss`, tiered boss unit from `DifficultyTuning.bossUnits`)
- WHEN wave N is not a boss wave THEN no boss group SHALL spawn

All curve knobs SHALL live in `core/DifficultyTuning` (base/per-wave/cap difficulty,
boss interval, boss units, stat bonuses per wave).
