package mindtheindustry.core;

/**
 * Phases of a run — the session state machine:
 * WAVE → CHOICE(timer) → … → BOSS → BOSS_WINDOW → WAVE.
 *
 * Tick-driven by {@link MindTheIndustrySession#tick(float)}; fully
 * headless-testable (no arc.scene dependency).
 */
public enum RunPhase{
    /** A normal wave is upcoming or in progress; no pending proposals. */
    WAVE,
    /**
     * Wave proposals are pending and the choice timer runs. Choices do NOT
     * block the game: the wave countdown keeps running underneath.
     */
    CHOICE,
    /** A boss wave is in progress (native Waves system, boss-flagged unit). */
    BOSS,
    /**
     * Ceasefire after the boss was killed: wave countdown frozen, no spawns,
     * world keeps simulating. Relic picks, one trade, assignment switches.
     */
    BOSS_WINDOW
}
