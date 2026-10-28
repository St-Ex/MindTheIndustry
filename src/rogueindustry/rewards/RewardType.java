package rogueindustry.rewards;

/**
 * Reward types available in rogue-like mode.
 *
 * BUILDING       → unlocks a building for the WHOLE group
 * BUILDING_BONUS → bonus on a building type for a TARGET player (chosen at vote time)
 * SCHEMATIC      → unlocks a schematic for an INDIVIDUAL player
 * UNIT_BONUS     → permanent bonus on a TARGET player's units (chosen at vote time)
 * GLOBAL_MOD     → run bonus for the WHOLE group
 * SYNERGY        → conditional bonus for the whole group (if prerequisites are unlocked)
 */
public enum RewardType{
    BUILDING,
    BUILDING_BONUS,
    SCHEMATIC,
    UNIT_BONUS,
    GLOBAL_MOD,
    SYNERGY
}
