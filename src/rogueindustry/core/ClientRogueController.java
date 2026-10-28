package rogueindustry.core;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import rogueindustry.net.*;
import rogueindustry.rewards.*;import static mindustry.Vars.*;

/**
 * Contrôleur client du mode RogueIndustry.
 * Gère l'état local côté client et la communication avec le serveur.
 */
public class ClientRogueController{

    /** Session client (état local, synchronisé avec le serveur). */
    public final RogueSession session = new RogueSession();

    /** Choix actuellement proposés au joueur local. */
    public Seq<Reward> currentChoices = new Seq<>();

    /** Vrai si le dialogue de choix est actuellement affiché. */
    public boolean choosingReward = false;

    public ClientRogueController(){
    }

    /** Appelé quand le serveur envoie les choix de récompenses. */
    public void onChoicesReceived(int wave, Seq<Reward> choices){
        currentChoices = choices;
        choosingReward = true;
    }

    /**
     * Envoie le vote du joueur local au serveur.
     *
     * @param choiceIndex index du choix (0-2)
     * @param target      joueur cible (pour UnitBonus; null pour les autres types)
     */
    public void sendVote(int choiceIndex, Player target){
        RoguePackets.RewardVotePacket packet = new RoguePackets.RewardVotePacket();
        packet.choiceIndex = choiceIndex;
        packet.targetPlayerId = target != null ? target.id : -1;
        net.send(packet, true);

        choosingReward = false;
        currentChoices.clear();
    }

    /** Retourne les bonus d'unités du joueur local (pour affichage HUD). */
    public Seq<rogueindustry.modifiers.UnitBonus> getLocalUnitBonuses(){
        Player local = player;
        if(local == null) return new Seq<>();
        return session.getUnitBonuses(local);
    }

    /** Appelé quand le joueur actif d'une entité change (reçu du serveur). */
    public void onActivePlayerChanged(int[] entityIds, boolean isBuilding, int playerId){
        Player newActive = playerId >= 0 ? Groups.player.find(p -> p.id == playerId) : null;

        for(int entityId : entityIds){
            if(isBuilding){
                Building building = Groups.build.getByID(entityId);
                if(building != null){
                    session.setActivePlayer(building, newActive);
                }
            }else{
                Unit unit = Groups.unit.getByID(entityId);
                if(unit != null){
                    session.setActivePlayer(unit, newActive);
                }
            }
        }
    }

    /**
     * Envoie une demande de changement de joueur actif au serveur.
     * Utilise le chat comme fallback (les packets C→S custom ne passent pas en solo).
     *
     * @param building le bâtiment concerné (null si unités)
     * @param units    les unités concernées (null ou vide si bâtiment)
     * @param target   le joueur à activer (null pour désactiver)
     */
    public void requestActivePlayer(Building building, Seq<Unit> units, Player target){
        String targetName = target != null ? target.name : "none";

        if(building != null){
            // Bâtiment: /rogue active <id> true <playerName>
            if(player != null){
                player.sendMessage("/rogue active " + building.id + " true " + targetName);
            }
        }else if(units != null && !units.isEmpty()){
            // Unités: une commande par unité
            for(Unit u : units){
                if(player != null){
                    player.sendMessage("/rogue active " + u.id + " false " + targetName);
                }
            }
        }
    }

    /**
     * Envoie une demande de changement de joueur actif pour une seule unité.
     */
    public void requestActivePlayer(Unit unit, Player target){
        requestActivePlayer(null, Seq.with(unit), target);
    }

    /**
     * Envoie une demande de changement de joueur actif pour un seul bâtiment.
     */
    public void requestActivePlayer(Building building, Player target){
        requestActivePlayer(building, null, target);
    }
}
