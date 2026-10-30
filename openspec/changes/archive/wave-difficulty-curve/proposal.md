# Proposal: scaling wave difficulty with periodic bosses

## Why

The run currently reuses whatever `state.rules.spawns` the map shipped with, so
waves don't scale with the run's wave counter. A roguelike needs waves that go
from easy to very hard, with boss waves every 20 waves (campaign-style), and a
single obvious place to tune the curve.

## What

1. `core/DifficultyTuning` — one class holding every difficulty knob:
   base difficulty, per-wave growth, cap, boss interval (20), boss unit list,
   enemy stat bonuses per wave.
2. `core/WaveDesigner.buildSpawns(wave, rand)` — pure, seeded logic that builds
   `state.rules.spawns` for a wave: vanilla `Waves.generate(difficulty, rand, false)`
   scaled by the curve, vanilla's random boss groups removed, and one boss
   `SpawnGroup` (`StatusEffects.boss`, tiered Serpulo boss unit) added every
   `bossInterval` waves starting at `bossWaveStart`.
3. `ServerMindTheIndustryController.startWave()` — advances the wave, rebuilds
   `state.rules.spawns` via `WaveDesigner`, sets `state.wave` + wave timer.
   `MindTheIndustrySession.applyDifficulty()` uses the tuning constants for the
   global enemy stat multipliers.
4. Headless tests: wave 1 easy/no boss, more enemies on later waves, boss
   exactly every 20 waves with scaling boss unit, determinism per seed.
