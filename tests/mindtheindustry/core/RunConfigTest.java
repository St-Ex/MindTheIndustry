package mindtheindustry.core;

import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the run configuration (chapter/boss-wave math). */
class RunConfigTest extends HeadlessMindTheIndustryTest{

    @Test
    void defaultsMatchTheDesign(){
        RunConfig config = new RunConfig();
        assertEquals(10, config.chapterLength);
        assertEquals(3, config.runChapters);
        assertEquals(30, config.winWave(), "winWave = runChapters x chapterLength");
        assertEquals(30f, config.runWaveSpacing, 0.001f, "inter-wave countdown overridden to 30s");
        assertEquals(300f, config.initialWaveSpacing, 0.001f, "5 min break before wave 1");
        assertEquals(300f, config.bossWindowTimeout, 0.001f, "boss window cap = 5 min");
    }

    @Test
    void durationsAreSecondsAndConvertToEngineTicks(){
        RunConfig config = new RunConfig();
        //engine time is in ticks (60/s): vanilla waveSpacing default = 2 * Time.toMinutes = 7200 ticks = 120s
        assertEquals(1800f, config.ticks(config.runWaveSpacing), 0.001f, "30s = 1800 ticks");
        assertEquals(18000f, config.ticks(config.initialWaveSpacing), 0.001f, "300s = 18000 ticks");
        assertEquals(60f, RunConfig.TICKS_PER_SECOND, 0.001f);
    }

    @Test
    void bossWaveIsTheLastWaveOfEachChapter(){
        RunConfig config = new RunConfig();
        for(int w = 1; w <= 30; w++){
            assertEquals(w % 10 == 0, config.isBossWave(w), "boss wave check wrong on wave " + w);
        }
        assertFalse(config.isBossWave(0));
    }

    @Test
    void finalBossIsTheWinWave(){
        RunConfig config = new RunConfig();
        assertTrue(config.isFinalBoss(30));
        assertFalse(config.isFinalBoss(10));
        assertFalse(config.isFinalBoss(20));
    }

    @Test
    void chapterOfWave(){
        RunConfig config = new RunConfig();
        assertEquals(1, config.currentChapter(0));
        assertEquals(1, config.currentChapter(1));
        assertEquals(1, config.currentChapter(10));
        assertEquals(2, config.currentChapter(11));
        assertEquals(3, config.currentChapter(30));
    }

    @Test
    void configurableChapterCountChangesWinWave(){
        RunConfig config = new RunConfig();
        config.runChapters = 5;
        assertEquals(50, config.winWave());
        assertTrue(config.isFinalBoss(50));
        assertFalse(config.isFinalBoss(30));
    }
}
