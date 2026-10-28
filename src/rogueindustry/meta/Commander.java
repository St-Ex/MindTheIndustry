package rogueindustry.meta;

import arc.graphics.*;

/**
 * Represents a commander unlockable through meta-progression.
 * Each commander has:
 * - A name and a description (lore)
 * - A theme color
 * - A unique ability (CommanderAbility) active for the whole run
 * - An unlock requirement (e.g. "Reach wave 10", "Win a run")
 */
public class Commander{

    /** Unique ID (e.g. "commander-vanguard"). */
    public final String id;

    /** Display name (e.g. "Vanguard", "Architect", "Overmind"). */
    public final String displayName;

    /** Description/lore of the commander. */
    public final String description;

    /** Theme color for the UI. */
    public final Color color;

    /** The unique ability of this commander. */
    public final CommanderAbility ability;

    /** Unlock requirement (description for the player). */
    public final String unlockRequirement;

    /** Display order (lower = unlocked earlier). */
    public final int tier;

    public Commander(String id, String displayName, String description, Color color,
                     CommanderAbility ability, String unlockRequirement, int tier){
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.ability = ability;
        this.unlockRequirement = unlockRequirement;
        this.tier = tier;
    }
}
