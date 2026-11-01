package mindtheindustry.core;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;

/**
 * Builds the enemy spawn groups ({@code state.rules.spawns}) for a wave.
 *
 * Pure, seeded logic — no UI, no global state besides loaded content:
 * for a given (wave, rand, config) the result is deterministic.
 *
 * Wave N is spawned from groups matching position N-1 (the engine evaluates
 * {@code group.getSpawned(state.wave - 1)} in WaveSpawner.spawnEnemies, before
 * incrementing state.wave). The boss wave of each chapter (every
 * {@link RunConfig#chapterLength} waves) gets one boss-flagged group.
 */
public class WaveDesigner{

    /**
     * Builds the spawn groups for a wave: the vanilla progression scaled by the
     * difficulty curve and the run's calibration hooks, with vanilla's random
     * boss groups removed and one boss group added on the chapter's boss wave.
     */
    public static Seq<SpawnGroup> buildSpawns(int wave, Rand rand, RunConfig config){
        // calibration hook: extra multiplier on the enemy stat curve — balance pass (deferred)
        float difficulty = Math.min(DifficultyTuning.maxDifficulty,
            DifficultyTuning.difficultyForWave(wave) * config.enemyStatMultiplier);
        Seq<SpawnGroup> groups = Waves.generate(difficulty, rand, false);

        //remove vanilla's randomly-placed boss groups; we add our own on the chapter's boss wave
        groups.removeAll(g -> g.effect == StatusEffects.boss);

        // calibration hook: scales spawn group spacing in waves (higher = sparser) — balance pass (deferred)
        if(config.spawnIntervalMultiplier != 1f){
            for(SpawnGroup g : groups){
                g.spacing = Math.max(1, Math.round(g.spacing * config.spawnIntervalMultiplier));
            }
        }

        if(config.isBossWave(wave)){
            //boss tier = chapter index (chapter 1 boss = first boss unit, etc.)
            int bossIndex = config.currentChapter(wave) - 1;
            UnitType boss = DifficultyTuning.bossUnits[Math.min(bossIndex, DifficultyTuning.bossUnits.length - 1)];

            SpawnGroup group = new SpawnGroup(boss);
            //spawn exactly on this wave (the spawner evaluates position wave - 1)
            group.begin = wave - 1;
            group.end = wave - 1;
            group.spacing = 1;
            group.unitAmount = 1;
            group.max = DifficultyTuning.bossMax;
            group.unitScaling = 1;
            group.shieldScaling = 20f + difficulty * 30f;
            group.effect = StatusEffects.boss;
            groups.add(group);
        }

        return groups;
    }
}
