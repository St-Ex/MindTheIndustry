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
choices (announced in chat with indices); the next wave SHALL start only once
every player has picked (`/mindtheindustry reward <index> [targetPlayer|none]`).

#### Scenario: choices are seeded
- WHEN a run is started with a seed THEN the reward choices are identical on
  the server and on all clients

### Requirement: reward types
The system SHALL support: building unlock (whole group), schematic unlock
(single player), unit bonus (target player), global modifier (whole group),
synergy (conditional on prerequisites).

### Requirement: commanders
At run start, each player SHALL pick one commander with a unique ability
(`/mindtheindustry vote <index>`); commanders unlock permanently through meta-progression
(wave milestones, run completion).

### Requirement: active player per entity
`/mindtheindustry active <entityId> <isBuilding> <playerName|none>` SHALL assign which
player's bonuses apply to a given building or unit group.

### Requirement: difficulty curve
Enemy stats SHALL scale with the wave number.
