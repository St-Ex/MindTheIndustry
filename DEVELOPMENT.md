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
`tests/mindtheindustryindustry/HeadlessMindTheIndustryTest.java`) and cover the game logic:
session, reward generation (seed determinism), network serialization,
schematics. Rules and conventions: see `AGENTS.md`.

**Manual — UI debugging:**

1. Build the JAR: `mise build`
2. Copy `build/libs/mindtheindustryDesktop.jar` into Mindustry's `mods/` folder
   - Linux Steam: `~/.local/share/Steam/steamapps/compatdata/451480/pfx/drive/c/users/steamuser/AppData/Roaming/Mindustry/mods/`
   - Or launch Mindustry with the JAR as an argument
3. Launch Mindustry, enable the mod in the Mods menu
4. Host a solo or multiplayer game
5. In the chat, type `/mindtheindustry start` to start a run

### In-game commands

| Command | Description |
|----------|-------------|
| `/mindtheindustry start` | Starts a rogue-like run |
| `/mindtheindustry stop` | Stops the current run |
| `/mindtheindustry skip` | Forces the next wave (kills all enemies) |
| `/mindtheindustry status` | Shows the run state (wave, seed, active/inactive) |

## Architecture

```
src/mindtheindustryindustry/
├── MindTheIndustryMod.java          # Entry point, commands, init
├── core/
│   ├── MindTheIndustrySession.java          # Server state: unlocks, bonuses, modifiers
│   ├── ServerMindTheIndustryController.java # Server orchestration (waves, votes)
│   ├── ClientMindTheIndustryController.java # Client orchestration (choices, votes)
│   └── RewardGenerator.java       # Generates the 3 choices per player
├── rewards/
│   ├── Reward.java                # Abstract class
│   ├── RewardType.java            # BUILDING, SCHEMATIC, UNIT_BONUS, GLOBAL_MOD, SYNERGY
│   ├── RewardSerializer.java      # Network serialization
│   └── rewards/
│       ├── BuildingReward.java    # Unlocks a building for EVERYONE
│       ├── SchematicReward.java   # Unlocks a schematic for ONE player
│       ├── UnitBonusReward.java   # Unit bonus for a TARGET player
│       ├── GlobalModifierReward.java # Bonus for EVERYONE
│       └── SynergyReward.java     # Conditional bonus
├── modifiers/
│   ├── UnitBonus.java             # Unit bonus (damage, health, speed...)
│   ├── GlobalModifier.java        # Global modifier
│   └── GlobalModifierRegistry.java # Modifier registry
├── net/
│   └── MindTheIndustryPackets.java          # All network packets
└── ui/
    └── RewardDialog.java          # Choice dialog (3 cards)
```

## Reward mechanics

| Type | Scope | Description |
|------|--------|-------------|
| **Building** | Whole group | Unlocks a building for all players |
| **Schematic** | Individual player | Unlocks a schematic usable by a single player |
| **Unit Bonus** | Target player | Permanent bonus on a player's units (chosen) |
| **Global Mod** | Whole group | Run bonus (production speed, damage, etc.) |
| **Synergy** | Whole group | Conditional bonus if prerequisites are unlocked |

## Next steps (TODO)

- [ ] Phase 2: Implement SchematicReward (random schematic generation)
- [ ] Phase 2: Implement UnitBonusReward (apply to existing + future units)
- [ ] Phase 3: MindTheIndustryHUD (wave display, modifiers, bonuses)
- [ ] Phase 3: Meta-progression between runs
- [ ] Phase 3: Difficulty curve balancing
- [ ] Phase 3: Custom content (mode-specific buildings/units)

## Notes & Resources

- **Server UI Builder System** (arc.scene.ui) — rich possibilities for building custom interfaces (Table, Button, Tex, Styles). To explore for improving the mod's dialogs (commander selection, rewards, HUD).
- **Modding wiki**: https://mindustrygame.github.io/wiki/modding/1-modding/
- **Mindustry v160 sources** — clone next to the project: `../Mindustry` (tag `v160`, read-only). Headless boot pattern: `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- **Mindustry v160.7** — build 160, Java 17
- **NewHorizonMod** — full custom campaign mod (planet / sectors / tech tree), good reference if we add campaign content: https://github.com/Yuria-Shikibe/NewHorizonMod
