package rogueindustry.meta;

import rogueindustry.core.*;

/**
 * Interface pour un pouvoir de commandant.
 * Chaque commandant a un pouvoir unique qui s'applique au démarrage d'une run
 * et reste actif pendant toute la durée de la run.
 */
public interface CommanderAbility{

    /** Nom court du pouvoir (ex: "Démarrage Salvo", "Boost unités"). */
    String name();

    /** Description complète du pouvoir. */
    String description();

    /**
     * Applique le pouvoir au démarrage de la run.
     * Appelé côté serveur quand la run démarre.
     *
     * @param session la session de run
     */
    void apply(RogueSession session);
}
