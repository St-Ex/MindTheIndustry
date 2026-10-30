package mindtheindustry.core;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the wave difficulty curve (WaveDesigner + DifficultyTuning). */
class WaveDesignerTest extends HeadlessMindTheIndustryTest{

    /** Total enemies a wave's spawn groups produce (the spawner evaluates position wave - 1). */
    private static int totalSpawned(Seq<SpawnGroup> groups, int wave){
        int sum = 0;
        for(SpawnGroup g : groups){
            sum += g.getSpawned(wave - 1);
        }
        return sum;
    }

    /** Whether a boss group actually spawns on this wave. */
    private static boolean hasBoss(Seq<SpawnGroup> groups, int wave){
        for(SpawnGroup g : groups){
            if(g.effect == StatusEffects.boss && g.getSpawned(wave - 1) > 0) return true;
        }
        return false;
    }

    private static SpawnGroup bossOf(Seq<SpawnGroup> groups, int wave){
        for(SpawnGroup g : groups){
            if(g.effect == StatusEffects.boss && g.getSpawned(wave - 1) > 0) return g;
        }
        return null;
    }

    private static String signature(Seq<SpawnGroup> groups){
        StringBuilder sb = new StringBuilder();
        for(SpawnGroup g : groups) sb.append(g.type.name).append(g.begin).append(g.spacing).append(';');
        return sb.toString();
    }

    @Test
    void wave1IsEasyAndHasNoBoss(){
        Seq<SpawnGroup> early = WaveDesigner.buildSpawns(1, new Rand(42));
        Seq<SpawnGroup> late = WaveDesigner.buildSpawns(30, new Rand(42));

        assertFalse(early.isEmpty(), "wave 1 must have spawn groups");
        assertFalse(hasBoss(early, 1), "no boss on wave 1");
        assertTrue(totalSpawned(early, 1) < totalSpawned(late, 30),
            "later waves must spawn more enemies than wave 1 (" + totalSpawned(early, 1) + " vs " + totalSpawned(late, 30) + ")");
    }

    @Test
    void bossEveryBossIntervalWaves(){
        for(int w = 1; w <= 61; w++){
            boolean expected = w >= DifficultyTuning.bossWaveStart && (w - DifficultyTuning.bossWaveStart) % DifficultyTuning.bossInterval == 0;
            assertEquals(expected, hasBoss(WaveDesigner.buildSpawns(w, new Rand(42)), w), "boss presence wrong on wave " + w);
        }
    }

    @Test
    void bossUnitScalesWithTier(){
        SpawnGroup g20 = bossOf(WaveDesigner.buildSpawns(20, new Rand(42)), 20);
        SpawnGroup g40 = bossOf(WaveDesigner.buildSpawns(40, new Rand(42)), 40);

        assertNotNull(g20);
        assertNotNull(g40);
        assertEquals(DifficultyTuning.bossUnits[0], g20.type, "first boss wave uses the first boss unit");
        assertEquals(DifficultyTuning.bossUnits[1], g40.type, "second boss wave uses the next boss unit");
    }

    @Test
    void deterministicPerSeed(){
        Seq<SpawnGroup> a = WaveDesigner.buildSpawns(25, new Rand(42));
        Seq<SpawnGroup> b = WaveDesigner.buildSpawns(25, new Rand(42));
        Seq<SpawnGroup> c = WaveDesigner.buildSpawns(25, new Rand(43));

        assertEquals(signature(a), signature(b), "same seed must give the same spawn groups");
        assertNotEquals(signature(a), signature(c), "different seed should give a different composition");
    }

    @Test
    void difficultyCurveIsMonotonicAndCapped(){
        assertTrue(DifficultyTuning.difficultyForWave(1) < DifficultyTuning.difficultyForWave(50));
        assertTrue(DifficultyTuning.difficultyForWave(50) < DifficultyTuning.difficultyForWave(500));
        assertEquals(DifficultyTuning.maxDifficulty, DifficultyTuning.difficultyForWave(10000), 0.0001f, "difficulty must be capped");
    }
}
