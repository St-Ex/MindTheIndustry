# Design: wave difficulty curve

## Engine mechanics (Mindustry v160, `../Mindustry`)

- `state.rules.spawns` (`Seq<SpawnGroup>`) is the single source of wave
  composition. On each wave, `Logic.runWave()` calls `WaveSpawner.spawnEnemies()`,
  which evaluates `group.getSpawned(state.wave - 1)` for every group — i.e. wave N
  is built from groups matching position `N - 1` (`begin <= N-1 <= end`,
  `(N-1-begin) % spacing == 0`) — then increments `state.wave`.
- `Waves.generate(difficulty, rand, attack)` (`game/Waves.java:263`) builds a full
  progression for positions 0..150 from a 0..1 difficulty, including 2 random boss
  groups (`effect = StatusEffects.boss`) at random waves/spacings. We strip those
  and add our own bosses on a fixed interval.
- `Rules.unitHealthMultiplier` / `unitDamageMultiplier` scale all enemy stats
  globally (already used by `MindTheIndustrySession.applyDifficulty`).

## New classes

- `core/DifficultyTuning` — static knobs: `baseDifficulty`, `difficultyPerWave`,
  `maxDifficulty`, `bossInterval` (20), `bossWaveStart` (20), `bossUnits[]`
  (Serpulo-flavored: reign, toxopid, eclipse), `bossMax`, `healthPerWave`,
  `damagePerWave`, plus `difficultyForWave(wave)`.
- `core/WaveDesigner.buildSpawns(int wave, Rand rand)`:
  - `difficulty = DifficultyTuning.difficultyForWave(wave)`
  - `groups = Waves.generate(difficulty, rand, false)`; remove groups with
    `effect == StatusEffects.boss` (vanilla's random bosses)
  - on boss waves (`wave >= bossWaveStart && (wave - bossWaveStart) % bossInterval == 0`):
    add a boss group with `begin = end = wave - 1`, `spacing = 1`, `unitAmount = 1`,
    `max = bossMax`, `shieldScaling` scaled by difficulty, `effect = StatusEffects.boss`,
    unit = `bossUnits[min(bossIndex, len-1)]` where `bossIndex = (wave - bossWaveStart) / bossInterval`

## Wiring

- `ServerMindTheIndustryController.startWave()`: `session.nextWave()` →
  `state.wave = session.wave` → `state.rules.spawns = WaveDesigner.buildSpawns(session.wave, session.rand)`
  → `waves = waveTimer = true`. Called from the commander-selection countdown and
  from `checkAllVotesReceived()`.
- `MindTheIndustrySession.applyDifficulty()` now reads `DifficultyTuning.healthPerWave`
  / `damagePerWave` (same values as before: 0.05 / 0.03).

## Determinism

One `buildSpawns` call per wave with the session's seeded `rand` → the whole run's
spawn composition is replayable from the seed. Tests use `new Rand(42)`.

## Tests

- wave 1: spawns exist, no boss, fewer total units than wave 30
- boss present exactly on waves 20, 40, 60 (and not 19/21), boss unit tier scales
- same seed → identical groups; different seed → different composition
