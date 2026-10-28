package rogueindustry.net;

import arc.struct.*;
import arc.util.io.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import rogueindustry.RogueIndustryMod;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

/**
 * All custom network packets for RogueIndustry mode.
 *
 * Direction S→C (server → client):
 * - RogueStartPacket: starts a run with a seed
 * - RewardChoicesPacket: sends a player's 3 reward choices
 * - RewardAppliedPacket: informs that a reward was applied (log/HUD)
 * - RogueStatePacket: full state sync (for joining mid-game)
 * - SchematicUnlockPacket: sends a schematic to unlock for a player
 *
 * Direction C→S (client → server):
 * - RewardVotePacket: the player sends their choice (+ target for UnitBonus)
 */
public class RoguePackets{

    /** Registers all the mod's packets. Call at mod load. */
    public static void register(){
        Net.registerPacket(RogueStartPacket::new);
        Net.registerPacket(RewardChoicesPacket::new);
        Net.registerPacket(RewardAppliedPacket::new);
        Net.registerPacket(RogueStatePacket::new);
        Net.registerPacket(SchematicUnlockPacket::new);
        Net.registerPacket(RewardVotePacket::new);
        Net.registerPacket(CommanderChoicesPacket::new);
        Net.registerPacket(CommanderVotePacket::new);
        Net.registerPacket(CountdownPacket::new);
        Net.registerPacket(SetActivePlayerPacket::new);
        Net.registerPacket(RogueStartRequestPacket::new);
        Net.registerPacket(RogueStopRequestPacket::new);
    }

    // ==================== S→C ====================

    /** Informs the client that a rogue run starts with a given seed. */
    public static class RogueStartPacket extends Packet{
        public long seed;
        public int wave;

        @Override
        public void write(Writes write){
            write.l(seed);
            write.i(wave);
        }

        @Override
        public void read(Reads read){
            seed = read.l();
            wave = read.i();
        }

        @Override
        public void handleClient(){
            arc.util.Log.info("[RogueIndustry] RogueStartPacket received client-side: seed=" + seed + ", wave=" + wave);
            RogueIndustryMod.clientSession.start(seed);
            RogueIndustryMod.clientSession.wave = wave;
        }
    }

    /** Sends the 3 reward choices for the local player. */
    public static class RewardChoicesPacket extends Packet{
        public int wave;
        public Seq<Reward> choices = new Seq<>(3);

        @Override
        public void write(Writes write){
            write.i(wave);
            write.i(choices.size);
            for(Reward r : choices){
                // r.write() already includes the specific fields (each subclass serializes them)
                r.write(write);
            }
        }

        @Override
        public void read(Reads read){
            wave = read.i();
            int count = read.i();
            choices.clear();
            for(int i = 0; i < count; i++){
                choices.add(Reward.read(read));
            }
        }

        @Override
        public void handleClient(){
            arc.util.Log.info("[RogueIndustry] RewardChoicesPacket received client-side: wave=" + wave + ", " + choices.size + " choices");
            // Show the choice dialog
            rogueindustry.ui.RewardDialog.showChoices(wave, choices);
        }
    }

    /** Informs the client that a reward was applied (for the HUD/log). */
    public static class RewardAppliedPacket extends Packet{
        public String playerName;
        public String rewardName;
        public String rewardType;
        public String targetName; // for UnitBonus, null otherwise

        @Override
        public void write(Writes write){
            write.str(playerName);
            write.str(rewardName);
            write.str(rewardType);
            write.str(targetName == null ? "" : targetName);
        }

        @Override
        public void read(Reads read){
            playerName = read.str();
            rewardName = read.str();
            rewardType = read.str();
            String t = read.str();
            targetName = t.isEmpty() ? null : t;
        }

        @Override
        public void handleClient(){
            String msg = "[Rogue] " + playerName + " chose: " + rewardName;
            if(targetName != null){
                msg += " (target: " + targetName + ")";
            }
            if(mindustry.Vars.player != null){
                mindustry.Vars.player.sendMessage(msg);
            }
        }
    }

    /** Synchronizes the full run state (for a player joining mid-game). */
    public static class RogueStatePacket extends Packet{
        public long seed;
        public int wave;
        public Seq<String> groupUnlocks = new Seq<>();
        public Seq<String> globalModifiers = new Seq<>();

        @Override
        public void write(Writes write){
            write.l(seed);
            write.i(wave);
            write.i(groupUnlocks.size);
            for(String s : groupUnlocks) write.str(s);
            write.i(globalModifiers.size);
            for(String s : globalModifiers) write.str(s);
        }

        @Override
        public void read(Reads read){
            seed = read.l();
            wave = read.i();
            int n = read.i();
            groupUnlocks.clear();
            for(int i = 0; i < n; i++) groupUnlocks.add(read.str());
            int m = read.i();
            globalModifiers.clear();
            for(int i = 0; i < m; i++) globalModifiers.add(read.str());
        }

        @Override
        public void handleClient(){
            // Rebuild the state client-side
            // (simplified for Phase 1)
        }
    }

    /** Sends a schematic to unlock for the local player. */
    public static class SchematicUnlockPacket extends Packet{
        public String schematicName;
        public String schematicData;

        @Override
        public void write(Writes write){
            write.str(schematicName);
            write.str(schematicData);
        }

        @Override
        public void read(Reads read){
            schematicName = read.str();
            schematicData = read.str();
        }

        @Override
        public void handleClient(){
            // Decode the schematic and add it to the player's collection
            try{
                mindustry.game.Schematic schematic = rogueindustry.rewards.SchematicGenerator.fromBase64(schematicData);
                if(schematic != null){
                    // Add to the local collection and save
                    mindustry.Vars.schematics.all().add(schematic);
                    mindustry.Vars.schematics.saveChanges(schematic);

                    if(mindustry.Vars.player != null){
                        mindustry.Vars.player.sendMessage("[purple]Schematic unlocked: [accent]" + schematicName +
                            "[] [gray]- Find it in your schematics!");
                    }
                    arc.util.Log.info("[RogueIndustry] Schematic added: " + schematicName);
                }
            }catch(Throwable t){
                arc.util.Log.err("[RogueIndustry] Error decoding schematic: " + t.getMessage());
            }
        }
    }

    // ==================== C→S ====================

    /** The player sends their reward choice. */
    public static class RewardVotePacket extends Packet{
        public int choiceIndex;
        public int targetPlayerId; // -1 = no target (for UnitBonus: target player ID)

        @Override
        public void write(Writes write){
            write.i(choiceIndex);
            write.i(targetPlayerId);
        }

        @Override
        public void read(Reads read){
            choiceIndex = read.i();
            targetPlayerId = read.i();
        }

        @Override
        public void handleServer(NetConnection con){
            Player player = con.player;
            if(player == null) return;

            Player target = null;
            if(targetPlayerId >= 0){
                target = mindustry.gen.Groups.player.find(p -> p.id == targetPlayerId);
            }
            if(RogueIndustryMod.serverController != null){
                RogueIndustryMod.serverController.handleVote(player, choiceIndex, target);
            }
        }
    }

    // ==================== S→C: Commander selection ====================

    /** Sends the 3 commander choices for the local player. */
    public static class CommanderChoicesPacket extends Packet{
        /** IDs of the 3 proposed commanders (comma-separated or array). */
        public String[] commanderIds;

        @Override
        public void write(Writes write){
            write.i(commanderIds.length);
            for(String id : commanderIds){
                write.str(id);
            }
        }

        @Override
        public void read(Reads read){
            int n = read.i();
            commanderIds = new String[n];
            for(int i = 0; i < n; i++){
                commanderIds[i] = read.str();
            }
        }

        @Override
        public void handleClient(){
            arc.util.Log.info("[RogueIndustry] CommanderChoicesPacket received client-side: " + commanderIds.length + " choices");
            // Convert the IDs to Commanders and show the dialog
            arc.struct.Seq<rogueindustry.meta.Commander> commanders = new arc.struct.Seq<>();
            for(String id : commanderIds){
                rogueindustry.meta.Commander c = rogueindustry.meta.CommanderRegistry.get(id);
                if(c != null) commanders.add(c);
            }
            arc.util.Log.info("[RogueIndustry] Showing commander selection dialog with " + commanders.size + " commanders");
            rogueindustry.meta.CommanderSelectionDialog.showChoices(commanders);
        }
    }

    // ==================== C→S: Commander vote ====================

    /** The player sends their commander choice. */
    public static class CommanderVotePacket extends Packet{
        public int choiceIndex;

        @Override
        public void write(Writes write){
            write.i(choiceIndex);
        }

        @Override
        public void read(Reads read){
            choiceIndex = read.i();
        }

        @Override
        public void handleServer(NetConnection con){
            Player player = con.player;
            if(player == null) return;
            if(RogueIndustryMod.serverController != null){
                RogueIndustryMod.serverController.handleCommanderVote(player, choiceIndex);
            }
        }
    }

    // ==================== S→C: Countdown ====================

    /** Sends the countdown time before the first wave. */
    public static class CountdownPacket extends Packet{
        /** Seconds remaining. */
        public int seconds;

        @Override
        public void write(Writes write){
            write.i(seconds);
        }

        @Override
        public void read(Reads read){
            seconds = read.i();
        }

        @Override
        public void handleClient(){
            arc.util.Log.info("[RogueIndustry] CountdownPacket received client-side: " + seconds + "s");
            rogueindustry.ui.CountdownHUD.show(seconds);
        }
    }

    // ==================== Active player per entity (bidirectional) ====================

    /**
     * Sets the active player for a building or a unit (or a group).
     *
     * C→S: a client requests to change the active player.
     * S→C: the server broadcasts the change to all clients.
     *
     * Supports groups: several entities can be changed at once.
     */
    public static class SetActivePlayerPacket extends Packet{
        /** Entity IDs (buildings or units). */
        public int[] entityIds;

        /** true = buildings, false = units. */
        public boolean isBuilding;

        /** Active player ID (-1 = none). */
        public int playerId;

        @Override
        public void write(Writes write){
            write.i(entityIds.length);
            for(int id : entityIds){
                write.i(id);
            }
            write.bool(isBuilding);
            write.i(playerId);
        }

        @Override
        public void read(Reads read){
            int n = read.i();
            entityIds = new int[n];
            for(int i = 0; i < n; i++){
                entityIds[i] = read.i();
            }
            isBuilding = read.bool();
            playerId = read.i();
        }

        @Override
        public void handleServer(NetConnection con){
            // The server applies the change and broadcasts to everyone
            if(RogueIndustryMod.serverController != null){
                RogueIndustryMod.serverController.handleSetActivePlayer(entityIds, isBuilding, playerId);
            }

            // Broadcast to all clients (including the sender for confirmation)
            for(Player p : mindustry.gen.Groups.player){
                if(p.con != null){
                    p.con.send(this, true);
                }
            }
        }

        @Override
        public void handleClient(){
            // Apply the change client-side (for the UI and visual stats)
            if(RogueIndustryMod.clientController != null){
                RogueIndustryMod.clientController.onActivePlayerChanged(entityIds, isBuilding, playerId);
            }
        }
    }

    // ==================== C→S: Start/stop request ====================

    /** The client asks the server to start a rogue run. */
    public static class RogueStartRequestPacket extends Packet{
        @Override
        public void write(Writes write){}

        @Override
        public void read(Reads read){}

        @Override
        public void handleServer(NetConnection con){
            arc.util.Log.info("[RogueIndustry] RogueStartRequestPacket received!");
            Player player = con.player;
            if(player == null){
                arc.util.Log.warn("[RogueIndustry] Player null in RogueStartRequestPacket!");
                return;
            }
            if(RogueIndustryMod.serverController == null){
                arc.util.Log.warn("[RogueIndustry] serverController null! ServerLoadEvent not fired yet?");
                player.sendMessage("[scarlet]The server is not ready. Try again in a few seconds.");
                return;
            }

            if(RogueIndustryMod.serverController.session.active){
                player.sendMessage("[scarlet]A run is already in progress.");
                return;
            }

            player.sendMessage("[accent]Commander selection...");
            arc.util.Log.info("[RogueIndustry] Starting commander selection for " + player.name);
            RogueIndustryMod.serverController.startCommanderSelection();
        }
    }

    /** The client asks the server to stop the rogue run. */
    public static class RogueStopRequestPacket extends Packet{
        @Override
        public void write(Writes write){}

        @Override
        public void read(Reads read){}

        @Override
        public void handleServer(NetConnection con){
            Player player = con.player;
            if(player == null) return;
            if(RogueIndustryMod.serverController == null) return;

            if(!RogueIndustryMod.serverController.session.active){
                player.sendMessage("[scarlet]No run in progress.");
                return;
            }

            RogueIndustryMod.serverController.stopRun();
            player.sendMessage("[yellow]Run stopped.");
        }
    }
}
