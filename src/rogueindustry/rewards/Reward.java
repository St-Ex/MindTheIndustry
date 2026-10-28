package rogueindustry.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;

/**
 * Classe abstraite représentant une récompense proposée à un joueur.
 *
 * Chaque récompense a:
 * - un type (détermine la portée: groupe, individuel, ou ciblé)
 * - un identifiant unique (pour la sérialisation réseau)
 * - un nom et une description affichés dans le dialogue de choix
 * - une méthode apply() qui applique la récompense dans la session
 */
public abstract class Reward{

    /** Identifiant unique de cette récompense (ex: "building:salvo", "unit_bonus:damage:20"). */
    public final String id;

    /** Type de récompense (détermine la portée d'application). */
    public final RewardType type;

    /** Nom affiché dans le dialogue (ex: "Salvo", "+20% dégâts unités"). */
    public final String displayName;

    /** Description détaillée (ex: "Débloque la tourelle Salvo pour tous les joueurs"). */
    public final String description;

    protected Reward(String id, RewardType type, String displayName, String description){
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Applique la récompense dans la session.
     *
     * @param session la session de run en cours
     * @param chooser le joueur qui a choisi cette récompense
     * @param target  le joueur cible (pour UNIT_BONUS; null pour les autres types)
     */
    public abstract void apply(RogueSession session, Player chooser, Player target);

    /** Sérialise la récompense pour envoi réseau. */
    public void write(Writes write){
        write.str(id);
        write.str(type.name());
        write.str(displayName);
        write.str(description);
    }

    /** Désérialise la récompense (usine appelée côté client). */
    public static Reward read(Reads read){
        String id = read.str();
        RewardType type = RewardType.valueOf(read.str());
        String name = read.str();
        String desc = read.str();
        // La désérialisation complète est faite par RewardSerializer
        // qui connaît les champs spécifiques à chaque type
        return RewardSerializer.readFull(id, type, name, desc, read);
    }

    @Override
    public String toString(){
        return displayName + " [" + type + "]";
    }
}
