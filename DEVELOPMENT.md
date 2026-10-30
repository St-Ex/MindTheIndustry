# MindTheIndustry — Rogue-like Mode for Mindustry

A mod that turns Mindustry into a cooperative roguelike: after each successful wave,
each player picks from random rewards (buildings, schematics, unit bonuses).

## Development environment

### Prerequisites
- [mise](https://mise.jdx.dev/) installed (`~/.local/bin/mise`)
- Java 17 (managed automatically by mise)

### Commands

```bash
# Go to the project directory
cd /home/stex/projects/mindtheindustry

# Build the mod (generates build/libs/mindtheindustryDesktop.jar)
mise build

# Compile only
mise exec -- ./gradlew compileJava

# Run the test suite (headless harness)
mise exec -- ./gradlew test

# Clean
mise exec -- ./gradlew clean
```

### Testing the mod

**Automated — headless harness (preferred):**

```bash
mise exec -- ./gradlew test
```

The tests in `tests/` boot Mindustry without a UI (headless mode, see
`tests/mindtheindustry/HeadlessMindTheIndustryTest.java`) and cover the game logic:
session, reward generation (seed determinism), network serialization,
schematics. Rules and conventions: see `AGENTS.md`.

**Manual — UI debugging:**

1. Build the JAR: `mise build`
2. Copy `build/libs/mindtheindustryDesktop.jar` into Mindustry's `mods/` folder
   - Linux Steam: `~/.local/share/Steam/steamapps/compatdata/451480/pfx/drive/c/users/steamuser/AppData/Roaming/Mindustry/mods/`
   - Or launch Mindustry with the JAR as an argument
3. Launch Mindustry, enable the mod in the Mods menu
4. Open **New Game**, pick the **MindTheIndustry** planet and launch its sector — the run map is based on the vanilla labyrinth map and the run auto-starts. (On any other map, `/mindtheindustry start` works too.)

### In-game commands (server-side, work in solo and multiplayer)

| Command | Description |
|----------|-------------|
| `/mindtheindustry start` | Starts a rogue-like run (commander selection first) |
| `/mindtheindustry stop` | Stops the current run |
| `/mindtheindustry skip` | Forces the next wave (kills all enemies) |
| `/mindtheindustry status` | Shows the run state (wave, seed, active/inactive) |
| `/mindtheindustry commanders` | Lists unlocked commanders |
| `/mindtheindustry unlock-all` | Unlocks all commanders (debug) |
| `/mindtheindustry vote <index>` | Votes for a commander choice |
| `/mindtheindustry reward <index> [targetPlayer\|none]` | Picks a reward choice |
| `/mindtheindustry active <entityId> <isBuilding> <playerName\|none>` | Sets the active player for an entity |

## Architecture

```
src/mindtheindustry/
├── MindTheIndustryMod.java          # Entry point: planet in loadContent, server commands, auto-start
├── content/
│   ├── MindTheIndustryPlanet.java        # New Game menu planet (alwaysUnlocked); start sector → labyrinth map
│   └── MindTheIndustryPlanetGenerator.java # Procedural Serpulo-style generator (random seed per launch)
├── core/
│   ├── MindTheIndustrySession.java          # Server state: unlocks, bonuses, modifiers
│   ├── ServerMindTheIndustryController.java # Server orchestration (waves, chat votes)
│   ├── RewardGenerator.java       # Generates the 3 choices per player
│   └── BonusApplier.java          # Applies active-player bonuses to game stats
├── meta/                          # Commanders + meta-progression
├── modifiers/
│   ├── UnitBonus.java             # Unit bonus (damage, health, speed...)
│   ├── GlobalModifier.java        # Global modifier
│   └── GlobalModifierRegistry.java # Modifier registry
├── rewards/
│   ├── Reward.java                # Abstract class
│   ├── RewardType.java            # BUILDING, SCHEMATIC, UNIT_BONUS, GLOBAL_MOD, SYNERGY
│   ├── RewardSerializer.java      # Network serialization
│   ├── SchematicGenerator.java    # Random schematic generation
│   └── rewards/                   # BuildingReward, SchematicReward, UnitBonusReward, ...
└── net/
    └── MindTheIndustryPackets.java          # Only SchematicUnlockPacket (S→C); everything else is chat + commands
```

There is intentionally **no `ui/` package**: all player interaction goes through chat
messages and the server-side `/mindtheindustry` command, so no custom dialogs/HUD are needed.

## Reward mechanics

| Type | Scope | Description |
|------|--------|-------------|
| **Building** | Whole group | Unlocks a building for all players |
| **Schematic** | Individual player | Unlocks a schematic usable by a single player |
| **Unit Bonus** | Target player | Permanent bonus on a player's units (chosen) |
| **Global Mod** | Whole group | Run bonus (production speed, damage, etc.) |
| **Synergy** | Whole group | Conditional bonus if prerequisites are unlocked |

## Next steps (TODO)

- [ ] Build a dedicated run map with the map editor (currently reusing the vanilla labyrinth map)
- [ ] Difficulty curve balancing
- [ ] Custom content (mode-specific buildings/units)

## Notes & Resources

- **Server UI Builder System** (arc.scene.ui) — rich possibilities for building custom interfaces (Table, Button, Tex, Styles). To explore for improving the mod's dialogs (commander selection, rewards, HUD).
- **Modding wiki**: https://mindustrygame.github.io/wiki/modding/1-modding/
- **Mindustry v160 sources** — clone next to the project: `../Mindustry` (tag `v160`, read-only). Headless boot pattern: `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- **Mindustry v160.7** — build 160, Java 17
- **NewHorizonMod** — full custom campaign mod (planet / sectors / tech tree), good reference if we add campaign content: https://github.com/Yuria-Shikibe/NewHorizonMod
