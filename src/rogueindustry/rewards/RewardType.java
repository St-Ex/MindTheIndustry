package rogueindustry.rewards;

/**
 * Types de récompenses disponibles dans le mode rogue-like.
 *
 * BUILDING       → débloque un bâtiment pour TOUT le groupe
 * BUILDING_BONUS → bonus sur un type de bâtiment pour un joueur CIBLE (choisi au moment du vote)
 * SCHEMATIC      → débloque un plan (schematic) pour un joueur INDIVIDUEL
 * UNIT_BONUS     → bonus permanent sur les unités d'un joueur CIBLE (choisi au moment du vote)
 * GLOBAL_MOD     → bonus de run pour TOUT le groupe
 * SYNERGY        → bonus conditionnel pour tout le groupe (si prérequis débloqués)
 */
public enum RewardType{
    BUILDING,
    BUILDING_BONUS,
    SCHEMATIC,
    UNIT_BONUS,
    GLOBAL_MOD,
    SYNERGY
}
