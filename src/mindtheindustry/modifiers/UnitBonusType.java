package mindtheindustry.modifiers;

/**
 * Kinds of permanent bonuses applicable to a player's units.
 * (Decoupled from the reward classes: the reward pool no longer offers
 * unit bonuses directly, but the per-player bonus machinery stays.)
 */
public enum UnitBonusType{
    DAMAGE, HEALTH, SPEED, RELOAD, ARMOR
}
