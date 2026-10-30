package mindtheindustry.core;

import mindustry.content.*;
import mindustry.type.*;

/**
 * All difficulty knobs for a run, in one place — tune the curve here.
 * Static fields, so they can also be tweaked at runtime (debug/REPL).
 */
public class DifficultyTuning{

    /** Difficulty (0..1, fed to Waves.generate) on wave 1. */
    public static float baseDifficulty = 0.15f;
    /** Extra difficulty per wave: 0.15 + 0.01 * (wave-1) reaches ~1.0 around wave 85. */
    public static float difficultyPerWave = 0.01f;
    /** Difficulty cap (Waves.generate saturates above 1). */
    public static float maxDifficulty = 1f;

    /** A boss wave happens every this many waves. */
    public static int bossInterval = 20;
    /** First boss wave. */
    public static int bossWaveStart = 20;
    /** Boss units, cycled by boss tier ((wave - bossWaveStart) / bossInterval). Serpulo-flavored. */
    public static UnitType[] bossUnits = {UnitTypes.reign, UnitTypes.toxopid, UnitTypes.eclipse};
    /** Max bosses spawned on a boss wave. */
    public static int bossMax = 4;

    /** Enemy health bonus per wave (multiplier = 1 + wave * healthPerWave). */
    public static float healthPerWave = 0.05f;
    /** Enemy damage bonus per wave. */
    public static float damagePerWave = 0.03f;

    /** Difficulty value (0..1) for a given wave, fed to Waves.generate. */
    public static float difficultyForWave(int wave){
        return Math.min(maxDifficulty, baseDifficulty + difficultyPerWave * (wave - 1));
    }
}
