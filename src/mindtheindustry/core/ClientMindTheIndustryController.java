package mindtheindustry.core;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindtheindustry.net.*;
import mindtheindustry.rewards.*;import static mindustry.Vars.*;

/**
 * Client controller for MindTheIndustry mode.
 * Manages local state client-side and communication with the server.
 */
public class ClientMindTheIndustryController{

    /** Client session (local state, synchronized with the server). */
    public final MindTheIndustrySession session = new MindTheIndustrySession();

    /** Choices currently offered to the local player. */
    public Seq<Reward> currentChoices = new Seq<>();

    /** True if the choice dialog is currently displayed. */
    public boolean choosingReward = false;

    public ClientMindTheIndustryController(){
    }

    /** Called when the server sends reward choices. */
    public void onChoicesReceived(int wave, Seq<Reward> choices){
        currentChoices = choices;
        choosingReward = true;
    }

    /**
     * Sends the local player's vote to the server.
     *
     * @param choiceIndex choice index (0-2)
     * @param target      target player (for UnitBonus; null for other types)
     */
    public void sendVote(int choiceIndex, Player target){
        MindTheIndustryPackets.RewardVotePacket packet = new MindTheIndustryPackets.RewardVotePacket();
        packet.choiceIndex = choiceIndex;
        packet.targetPlayerId = target != null ? target.id : -1;
        net.send(packet, true);

        choosingReward = false;
        currentChoices.clear();
    }

    /** Returns the local player's unit bonuses (for HUD display). */
    public Seq<mindtheindustry.modifiers.UnitBonus> getLocalUnitBonuses(){
        Player local = player;
        if(local == null) return new Seq<>();
        return session.getUnitBonuses(local);
    }

    /** Called when an entity's active player changes (received from the server). */
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
     * Sends an active-player change request to the server.
     * Uses chat as a fallback (custom C→S packets don't work in solo).
     *
     * @param building the building concerned (null if units)
     * @param units    the units concerned (null or empty if building)
     * @param target   the player to activate (null to deactivate)
     */
    public void requestActivePlayer(Building building, Seq<Unit> units, Player target){
        String targetName = target != null ? target.name : "none";

        if(building != null){
            // Building: /mindtheindustry active <id> true <playerName>
            if(player != null){
                player.sendMessage("/mindtheindustry active " + building.id + " true " + targetName);
            }
        }else if(units != null && !units.isEmpty()){
            // Units: one command per unit
            for(Unit u : units){
                if(player != null){
                    player.sendMessage("/mindtheindustry active " + u.id + " false " + targetName);
                }
            }
        }
    }

    /**
     * Sends an active-player change request for a single unit.
     */
    public void requestActivePlayer(Unit unit, Player target){
        requestActivePlayer(null, Seq.with(unit), target);
    }

    /**
     * Sends an active-player change request for a single building.
     */
    public void requestActivePlayer(Building building, Player target){
        requestActivePlayer(building, null, target);
    }
}
