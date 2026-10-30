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
 * for a given (wave, rand) the result is deterministic.
 *
 * Wave N is spawned from groups matching position N-1 (the engine evaluates
 * {@code group.getSpawned(state.wave - 1)} in WaveSpawner.spawnEnemies).
 */
public class WaveDesigner{

    /**
     * Builds the spawn groups for a wave: the vanilla progression scaled by the
     * difficulty curve, with vanilla's random boss groups removed and one boss
     * group added on boss waves (every DifficultyTuning.bossInterval waves).
     */
    public static Seq<SpawnGroup> buildSpawns(int wave, Rand rand){
        float difficulty = DifficultyTuning.difficultyForWave(wave);
        Seq<SpawnGroup> groups = Waves.generate(difficulty, rand, false);

        //remove vanilla's randomly-placed boss groups; we add our own on a fixed interval
        groups.removeAll(g -> g.effect == StatusEffects.boss);

        if(wave >= DifficultyTuning.bossWaveStart && (wave - DifficultyTuning.bossWaveStart) % DifficultyTuning.bossInterval == 0){
            int bossIndex = (wave - DifficultyTuning.bossWaveStart) / DifficultyTuning.bossInterval;
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
