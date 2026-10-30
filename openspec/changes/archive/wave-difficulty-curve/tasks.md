# Tasks: wave difficulty curve

- [x] openspec change created (this directory)
- [x] `core/DifficultyTuning` (all knobs) + `core/WaveDesigner.buildSpawns(wave, rand)`
- [x] `ServerMindTheIndustryController.startWave()` wired into commander countdown + vote completion
- [x] `MindTheIndustrySession.applyDifficulty()` uses DifficultyTuning constants
- [x] Tests: WaveDesignerTest (easy start, scaling, boss every 20, determinism) + controller asserts spawns set
- [x] Docs: README feature line, DECISIONS bullet, spec delta applied + change archived
- [x] `mise exec -- ./gradlew test` green + `mise build` + commit
