# MindTheIndustry

A Mindustry mod that adds a cooperative roguelike mode. After each successful wave, every player picks one of three random rewards. Designed for co-op multiplayer, playable in solo.

- Mod name: `mindtheindustry` (displayed as **MindTheIndustry**)
- Requires Mindustry v154+ (built against v160)
- Java mod, Java 17

## Features

- **Reward choices after each wave** — each player gets exactly one of each category, in a shuffled order, each with a rarity (normal grey / rare blue / epic violet, x1 / x2 / x4 magnitude):
  - **Trade-off**: +X% on one stat, −X% on another (always a trade, X = 10% × rarity)
  - **Plan**: unlocks 1/2/4 building or unit plans for the whole group
  - **Resource gift**: a quantity of one or more resources delivered to the team cores (quantity × rarity)
- **Basic plans from the start** — the run begins with the basic blocks and units unlocked for everyone (duo, drill, conveyor, ducts, conduits, power nodes + basic generators, vault, logic blocks, dagger, flare, ...); batteries and advanced generators excluded — everything else comes from PLAN rewards.
- **Commanders** — at the start of a run, each player picks a commander with a unique ability (Vanguard, Architect, Industrialist, ...). Commanders unlock permanently through meta-progression (reach wave 10/20, win a run).
- **Active player per entity** — assign a player to a building or a group of units; that player's bonuses apply to it.
- **Difficulty curve** — waves scale from easy to very hard (spawn composition rebuilt every wave from the run seed, enemy stats +5% health / +3% damage per wave), with a **boss every 20 waves**. All knobs live in `src/mindtheindustry/core/DifficultyTuning.java`.

## Installation

1. Build the jar: `mise exec -- ./gradlew jar` (or download it from the Releases page).
2. Copy `build/libs/mindtheindustryDesktop.jar` into Mindustry's `mods/` folder:
   - Linux (Steam): `~/.local/share/Steam/steamapps/compatdata/451480/pfx/drive/c/users/steamuser/AppData/Roaming/Mindustry/mods/`
   - Alternatively, launch Mindustry with the jar as an argument.
3. Enable the mod in the Mods menu, then host or join a game.

## Playing

1. Open **New Game** and pick the **MTI** planet (a small moon of Serpulo, next to Serpulo/Erekir). It has a single sector and uses Serpulo's technology tree.
2. Launch its sector: the run map is based on the vanilla **labyrinth** map, and the roguelike run **auto-starts** (commander selection first).
3. Each player gets 3 commander choices in chat — vote with `/mindtheindustry vote <index>`.
4. After each cleared wave, a box appears at the top of the HUD with 3 reward icons (trade-off / plan / resource gift, rarity shown as a colored bar). Hover an icon to read what it does, click it to pick it. The choices are also listed in chat. The next wave starts once everyone has picked.

You can also start a run on any other map with `/mindtheindustry start`.

| Command | Description |
| --- | --- |
| `/mindtheindustry start` | Start a run (commander selection first) |
| `/mindtheindustry stop` | Stop the current run |
| `/mindtheindustry skip` | Force the next wave |
| `/mindtheindustry status` | Show run state (wave, seed, active/inactive) |
| `/mindtheindustry commanders` | List unlocked commanders |
| `/mindtheindustry unlock-all` | Unlock all commanders (debug) |
| `/mindtheindustry vote <index>` | Vote for a commander |
| `/mindtheindustry active <entityId> <isBuilding> <playerName\|none>` | Set the active player for an entity |

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
- `openspec/` — intent tracking: active feature proposals in `changes/`, current specs in `specs/`
- `DEVELOPMENT.md` — architecture, game mechanics, roadmap
- Engine sources for reference: `../Mindustry` (clone of tag `v160`, read-only)
