# MindTheIndustry

A Mindustry mod that adds a cooperative roguelike mode. After each successful wave, every player picks one of three random rewards. Designed for co-op multiplayer, playable in solo.

- Mod name: `mindtheindustry` (displayed as **MindTheIndustry**)
- Requires Mindustry v154+ (built against v160)
- Java mod, Java 17

## Features

- **Reward choices after each wave** — each player gets 3 random options:
  - **Building**: unlocks a building for the whole group
  - **Schematic**: unlocks a generated schematic for you only
  - **Unit / building bonus**: permanent bonus on a player of your choice
  - **Global modifier**: run-wide bonus (production speed, turret damage, ...)
  - **Synergy**: conditional bonus if a prerequisite is unlocked
- **Commanders** — at the start of a run, each player picks a commander with a unique ability (Vanguard, Architect, Industrialist, ...). Commanders unlock permanently through meta-progression (reach wave 10/20, win a run).
- **Active player per entity** — assign a player to a building or a group of units; that player's bonuses apply to it.
- **Difficulty curve** — enemies get stronger every wave.

## Installation

1. Build the jar: `mise exec -- ./gradlew jar` (or download it from the Releases page).
2. Copy `build/libs/mindtheindustryDesktop.jar` into Mindustry's `mods/` folder:
   - Linux (Steam): `~/.local/share/Steam/steamapps/compatdata/451480/pfx/drive/c/users/steamuser/AppData/Roaming/Mindustry/mods/`
   - Alternatively, launch Mindustry with the jar as an argument.
3. Enable the mod in the Mods menu, then host or join a game.

## Playing

Type `/rogue start` in the chat to begin a run. After each wave, a dialog shows your 3 reward choices — pick one, and the next wave starts once everyone has voted.

| Command | Description |
| --- | --- |
| `/rogue start` | Start a run (commander selection first) |
| `/rogue stop` | Stop the current run |
| `/rogue skip` | Force the next wave |
| `/rogue status` | Show run state (wave, seed, active/inactive) |
| `/rogue commanders` | List unlocked commanders |
| `/rogue unlock-all` | Unlock all commanders (debug) |
| `/rogue vote <index>` | Vote for a commander |
| `/rogue reward <index>` | Pick a reward |
| `/rogue active <entityId> <isBuilding> <playerName\|none>` | Set the active player for an entity |

## Development

Java 17, managed by [mise](https://mise.jdx.dev). See `AGENTS.md` for the rules agents must follow and `DEVELOPMENT.md` for the architecture and roadmap.

```bash
mise build                     # build the mod (build/libs/mindtheindustryDesktop.jar)
mise exec -- ./gradlew test   # headless test suite (no game launch needed)
```

Tests boot Mindustry headless (JUnit 5, `tests/` sourceSet). Game logic is UI-independent by design: never validate a change by launching the game manually — write a headless test instead.

## Links

- `AGENTS.md` — rules for agents working on this repo
- `DECISIONS.md` — decision log (architecture & workflow choices to follow)
- `DEVELOPMENT.md` — architecture, game mechanics, roadmap
- Engine sources for reference: `../Mindustry` (clone of tag `v160`, read-only)
