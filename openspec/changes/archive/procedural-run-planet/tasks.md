# Tasks: procedural run planet

- [x] openspec change created (this directory)
- [x] `content/MindTheIndustryPlanet` + `content/MindTheIndustryPlanetGenerator` (Serpulo gen, random seed per launch); start sector bound to the vanilla labyrinth map via SectorPreset
- [x] `MindTheIndustryMod`: create planet in `loadContent()`; WorldLoadEvent rules for our sector; PlayEvent auto-start; command moved to `registerServerCommands` (`reward <i> [target]`)
- [x] Remove broken UI: `ui/` package, `meta/CommanderSelectionDialog`, `core/ClientMindTheIndustryController`
- [x] `MindTheIndustryPackets`: keep only `SchematicUnlockPacket`
- [x] `ServerMindTheIndustryController`: chat-based notifications, no packets/Core.app.post, drop unused `startRun()`, test-friendly visibility
- [x] Tests: generator determinism/variety, controller command flow, schematic packet round-trip; removed obsolete packet tests
- [x] Docs: README (start via New Game → MindTheIndustry planet), DEVELOPMENT.md, DECISIONS.md entry, openspec spec delta applied + change archived
- [x] `mise exec -- ./gradlew test` green (40 tests) + `mise build` + commit
