package mindtheindustry.core;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the wave designer: chapter boss cadence, calibration hooks, determinism. */
class WaveDesignerTest extends HeadlessMindTheIndustryTest{

    private final RunConfig config = new RunConfig();

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
        return bossOf(groups, wave) != null;
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
        Seq<SpawnGroup> early = WaveDesigner.buildSpawns(1, new Rand(42), config);
        Seq<SpawnGroup> late = WaveDesigner.buildSpawns(30, new Rand(42), config);

        assertFalse(early.isEmpty(), "wave 1 must have spawn groups");
        assertFalse(hasBoss(early, 1), "no boss on wave 1");
        assertTrue(totalSpawned(early, 1) < totalSpawned(late, 30),
            "later waves must spawn more enemies than wave 1 (" + totalSpawned(early, 1) + " vs " + totalSpawned(late, 30) + ")");
    }

    @Test
    void bossOnTheLastWaveOfEachChapter(){
        for(int w = 1; w <= 61; w++){
            assertEquals(config.isBossWave(w), hasBoss(WaveDesigner.buildSpawns(w, new Rand(42), config), w),
                "boss presence wrong on wave " + w);
        }
    }

    @Test
    void bossUnitScalesWithChapterTier(){
        SpawnGroup g10 = bossOf(WaveDesigner.buildSpawns(10, new Rand(42), config), 10);
        SpawnGroup g20 = bossOf(WaveDesigner.buildSpawns(20, new Rand(42), config), 20);
        SpawnGroup g30 = bossOf(WaveDesigner.buildSpawns(30, new Rand(42), config), 30);

        assertNotNull(g10);
        assertNotNull(g20);
        assertNotNull(g30);
        assertEquals(DifficultyTuning.bossUnits[0], g10.type, "chapter 1 boss uses the first boss unit");
        assertEquals(DifficultyTuning.bossUnits[1], g20.type, "chapter 2 boss uses the next boss unit");
        assertEquals(DifficultyTuning.bossUnits[2], g30.type, "chapter 3 boss uses the third boss unit");
    }

    @Test
    void deterministicPerSeed(){
        Seq<SpawnGroup> a = WaveDesigner.buildSpawns(25, new Rand(42), config);
        Seq<SpawnGroup> b = WaveDesigner.buildSpawns(25, new Rand(42), config);
        Seq<SpawnGroup> c = WaveDesigner.buildSpawns(25, new Rand(43), config);

        assertEquals(signature(a), signature(b), "same seed must give the same spawn groups");
        assertNotEquals(signature(a), signature(c), "different seed should give a different composition");
    }

    @Test
    void calibrationHooksScaleSpawns(){
        RunConfig tuned = new RunConfig();
        tuned.spawnIntervalMultiplier = 3f; //sparser spawn groups
        Seq<SpawnGroup> base = WaveDesigner.buildSpawns(5, new Rand(42), config);
        Seq<SpawnGroup> tunedGroups = WaveDesigner.buildSpawns(5, new Rand(42), tuned);

        int baseSpacing = 0, tunedSpacing = 0;
        for(SpawnGroup g : base) baseSpacing += g.spacing;
        for(SpawnGroup g : tunedGroups) tunedSpacing += g.spacing;
        assertTrue(tunedSpacing > baseSpacing, "spawn interval multiplier must scale group spacing");
        assertTrue(hasBoss(WaveDesigner.buildSpawns(10, new Rand(42), tuned), 10), "calibration must not affect boss cadence");
    }

    @Test
    void difficultyCurveIsMonotonicAndCapped(){
        assertTrue(DifficultyTuning.difficultyForWave(1) < DifficultyTuning.difficultyForWave(50));
        assertTrue(DifficultyTuning.difficultyForWave(50) < DifficultyTuning.difficultyForWave(500));
        assertEquals(DifficultyTuning.maxDifficulty, DifficultyTuning.difficultyForWave(10000), 0.0001f, "difficulty must be capped");
    }
}
