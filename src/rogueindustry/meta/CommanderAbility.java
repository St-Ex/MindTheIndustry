package rogueindustry.meta;

import rogueindustry.core.*;

/**
 * Interface for a commander ability.
 * Each commander has a unique ability that applies when a run starts
 * and stays active for the whole run.
 */
public interface CommanderAbility{

    /** Short name of the ability (e.g. "Salvo Start", "Unit Boost"). */
    String name();

    /** Full description of the ability. */
    String description();

    /**
     * Applies the ability when the run starts.
     * Called server-side when the run starts.
     *
     * @param session the run session
     */
    void apply(RogueSession session);
}
