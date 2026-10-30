# DECISIONS.md — Active Decisions

The decisions currently in force for this repo. Agents: read this before making
any change. Keep this file limited to active decisions — when one is superseded,
replace its entry. The history of past decisions lives in the git log, not here.

## Workflow & repo

- **Work on `main` directly.** Never push without explicit owner approval. Commits in English, one topic per commit.
- **Never force-push.** If a branch diverges from its origin with duplicate commits, rebase onto origin (git drops the duplicates automatically).
- **English-only repo**: docs, comments and user-facing strings.
- **Java 17 via mise**; `mise build` builds `build/libs/mindtheindustryDesktop.jar`. Mindustry/Arc dependencies are `compileOnly` — never bundle the engine inside the mod jar.
- **Intent tracking**: backlog in GitHub Issues; non-trivial active features get an `openspec/changes/<slug>/` proposal (format in `openspec/project.md`). Small fixes go straight to a PR.

## Architecture & branding

- **Everything is named after the mod**: display name, mod id, package `mindtheindustry`, main class `MindTheIndustryMod`, `/mindtheindustry` command. "Rogue-like" survives only as the genre word in prose.
- **Game logic never depends on `arc.scene`.** The `ui/` layer only renders state; every mechanic (reward, modifier, synergy, wave) must exist and be testable without a UI. No purely visual content without a gameplay counterpart.
- **Deterministic, seeded logic**: the run seed drives reward generation and all network-synced randomness, so server and clients stay in sync; tests use fixed seeds (`new Rand(42)`, `session.start(42)`).
- **Run entry point = the "MTI" planet in the New Game menu.** A mod-created `Planet` (display name `MTI`, a dwarf moon of Serpulo: small radius + `Planets.serpulo` parent, Serpulo's tech tree via `techTree = Planets.serpulo.techTree`, `alwaysUnlocked = true`) appears automatically in the planet menu with a single selectable sector (only `startSector` is selectable, so the globe shows one marker). Its start sector is bound to the vanilla **labyrinth** map via a `SectorPreset` (`default/labyrinth`), and launching it auto-starts the run (`PlayEvent`). The old in-game UI (HUD buttons, dialogs) is removed — control is exclusively the server-registered `/mindtheindustry` command + chat messages, which work identically in solo and multiplayer. The planet also carries a procedural generator (`SerpuloPlanetGenerator` subclass, random `seedOffset` per launch) for any sector without a preset.
- **Backlog flag**: build a dedicated run map with the map editor later (instead of reusing labyrinth).

## Validation

- **Headless tests only** — never validate a change by launching the game manually from an agent. Tests live in `tests/` (JUnit 5) and boot Mindustry headless once per JVM via `HeadlessMindTheIndustryTest`.
