package mindtheindustry.net;

import arc.util.io.*;
import mindustry.net.*;

/**
 * Custom network packets for MindTheIndustry.
 *
 * Only one packet remains: {@link SchematicUnlockPacket} (S→C). Everything else
 * (reward choices, votes, commander selection, run state) travels through chat
 * messages and server commands (/mindtheindustry ...), which work identically in
 * solo and multiplayer. Schematic data cannot go through chat, so it keeps a packet.
 */
public class MindTheIndustryPackets{

    /** Registers all the mod's packets. Call at mod load. */
    public static void register(){
        Net.registerPacket(SchematicUnlockPacket::new);
    }

    /** Sends a generated schematic to a player's client so it lands in their schematic collection. */
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
                mindustry.game.Schematic schematic = mindtheindustry.rewards.SchematicGenerator.fromBase64(schematicData);
                if(schematic != null){
                    // Add to the local collection and save
                    mindustry.Vars.schematics.all().add(schematic);
                    mindustry.Vars.schematics.saveChanges(schematic);

                    if(mindustry.Vars.player != null){
                        mindustry.Vars.player.sendMessage("[purple]Schematic unlocked: [accent]" + schematicName +
                            "[] [gray]- Find it in your schematics!");
                    }
                    arc.util.Log.info("[MindTheIndustry] Schematic added: " + schematicName);
                }
            }catch(Throwable t){
                arc.util.Log.err("[MindTheIndustry] Error decoding schematic: " + t.getMessage());
            }
        }
    }
}
