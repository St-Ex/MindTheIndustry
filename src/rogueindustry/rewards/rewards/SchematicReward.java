package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

/**
 * Récompense: débloque un plan (schematic) pour un joueur INDIVIDUEL.
 * Le schematic est généré par le serveur et envoyé au joueur via un packet.
 * Les autres joueurs ne l'ont pas — ils devront le lui demander.
 *
 * Note: les schematics Mindustry sont stockés localement par joueur.
 * Le serveur envoie les données du schematic via SchematicUnlockPacket.
 */
public class SchematicReward extends Reward{

    /**
     * Données sérialisées du schematic (base64 ou format custom).
     * Le serveur génère le schematic et leSerialize ici.
     */
    public final String schematicData;

    /** Nom du schematic (pour l'affichage dans la liste des schematics du joueur). */
    public final String schematicName;

    public SchematicReward(String schematicData, String schematicName, String displayName, String description){
        super("schematic:" + schematicName, RewardType.SCHEMATIC, displayName, description);
        this.schematicData = schematicData;
        this.schematicName = schematicName;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        // Enregistrer le déblocage côté serveur
        session.unlockSchematicForPlayer(chooser, schematicName);

        // Envoyer le schematic au client du joueur (pour qu'il l'ajoute à sa collection)
        if(chooser.con != null && !schematicData.isEmpty()){
            rogueindustry.net.RoguePackets.SchematicUnlockPacket packet = new rogueindustry.net.RoguePackets.SchematicUnlockPacket();
            packet.schematicName = schematicName;
            packet.schematicData = schematicData;
            chooser.con.send(packet, true);
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(schematicData);
        write.str(schematicName);
    }

    public static SchematicReward readSpecific(String id, String name, String desc, Reads read){
        String data = read.str();
        String schemName = read.str();
        return new SchematicReward(data, schemName, name, desc);
    }
}
