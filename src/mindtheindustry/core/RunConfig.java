package mindtheindustry.core;

/**
 * Per-run configuration knobs (M1: reward pacing & boss window).
 *
 * Ubiquitous language: a chapter is {@link #chapterLength} waves and its last
 * wave is the boss wave; a run is {@link #runChapters} chapters.
 *
 * Numeric balance values (fallback amounts, scaling curves) are deliberately
 * frozen: fields marked "balance pass" carry placeholder values and must be
 * tuned in the balance pass (issue tracker), not invented while implementing.
 */
public class RunConfig{

    /**
     * Engine time is in TICKS (60 ticks = 1 second — vanilla waveSpacing default
     * is 2 * Time.toMinutes = 7200 ticks = 120s). All RunConfig durations are in
     * SECONDS (design language); convert with {@link #ticks(float)} when applying
     * them to engine fields (state.rules.waveSpacing, state.wavetime, ...).
     */
    public static final float TICKS_PER_SECOND = 60f;

    /** Waves per chapter; the last wave of each chapter is the boss wave. */
    public int chapterLength = 10;

    /** Chapters per run; the run's winWave = runChapters * chapterLength. */
    public int runChapters = 3;

    /** Inter-wave countdown override in seconds (vanilla survival default is 120s). */
    public float runWaveSpacing = 30f;

    /** Break before wave 1 in seconds (chapter 1's initial build time). */
    public float initialWaveSpacing = 300f;

    /** Boss window / chapter break cap in seconds; all-ready closes it early. */
    public float bossWindowTimeout = 300f;

    /** Converts a duration in seconds to engine ticks. */
    public float ticks(float seconds){
        return seconds * TICKS_PER_SECOND;
    }

    /** Item granted by the fallback conversion of an untaken choice. */
    public String fallbackItem = "copper";

    /** Amount granted by the fallback conversion — balance pass (deferred). */
    public int fallbackAmount = 50; // balance pass

    /** Relic pool weights (rare/boosted pool, ~70/30 target). */
    public float relicRareWeight = 0.70f;
    public float relicEpicWeight = 0.30f;

    /** Calibration hook: scales spawn group spacing (lower = faster) — balance pass (deferred). */
    public float spawnIntervalMultiplier = 1f; // balance pass

    /** Calibration hook: extra multiplier on the enemy stat curve — balance pass (deferred). */
    public float enemyStatMultiplier = 1f; // balance pass

    /** @return true if the given wave number is the boss wave of its chapter. */
    public boolean isBossWave(int wave){
        return wave > 0 && wave % chapterLength == 0;
    }

    /** @return the wave number at which the run is won (final boss wave). */
    public int winWave(){
        return runChapters * chapterLength;
    }

    /** @return true if the given wave number is the final boss (victory, no window). */
    public boolean isFinalBoss(int wave){
        return wave == winWave();
    }

    /** @return the 1-based chapter containing the given wave number. */
    public int currentChapter(int wave){
        return wave <= 0 ? 1 : (wave - 1) / chapterLength + 1;
    }
}
