package rogueindustry.meta;

import arc.graphics.*;

/**
 * Représente un commandant déblocable en meta-progression.
 * Chaque commandant a:
 * - Un nom et une description (lore)
 * - Une couleur thématique
 * - Un pouvoir unique (CommanderAbility) actif pour toute la run
 * - Un critère de déblocage (ex: "Atteindre vague 10", "Gagner une run")
 */
public class Commander{

    /** ID unique (ex: "commander-vanguard"). */
    public final String id;

    /** Nom affiché (ex: "Vanguard", "Architecte", "Overmind"). */
    public final String displayName;

    /** Description/lore du commandant. */
    public final String description;

    /** Couleur thématique pour l'UI. */
    public final Color color;

    /** Le pouvoir unique de ce commandant. */
    public final CommanderAbility ability;

    /** Critère de déblocage (description pour le joueur). */
    public final String unlockRequirement;

    /** Ordre d'affichage (plus petit = débloqué plus tôt). */
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
