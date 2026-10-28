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
 * Tous les packets réseau custom du mode RogueIndustry.
 *
 * Direction S→C (serveur → client):
 * - RogueStartPacket: démarre une run avec un seed
 * - RewardChoicesPacket: envoie les 3 choix de récompense d'un joueur
 * - RewardAppliedPacket: informe qu'une récompense a été appliquée (log/HUD)
 * - RogueStatePacket: synchro complète de l'état (pour rejoindre en cours)
 * - SchematicUnlockPacket: envoie un schematic à débloquer pour un joueur
 *
 * Direction C→S (client → serveur):
 * - RewardVotePacket: le joueur envoie son choix (+ cible pour UnitBonus)
 */
public class RoguePackets{

    /** Enregistre tous les packets du mod. À appeler au chargement du mod. */
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

    /** Informe le client qu'une run rogue démarre avec un seed donné. */
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
            arc.util.Log.info("[RogueIndustry] RogueStartPacket reçu côté client: seed=" + seed + ", wave=" + wave);
            RogueIndustryMod.clientSession.start(seed);
            RogueIndustryMod.clientSession.wave = wave;
        }
    }

    /** Envoie les 3 choix de récompense pour le joueur local. */
    public static class RewardChoicesPacket extends Packet{
        public int wave;
        public Seq<Reward> choices = new Seq<>(3);

        @Override
        public void write(Writes write){
            write.i(wave);
            write.i(choices.size);
            for(Reward r : choices){
                r.write(write);
                RewardSerializer.writeSpecific(r, write);
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
            arc.util.Log.info("[RogueIndustry] RewardChoicesPacket reçu côté client: vague=" + wave + ", " + choices.size + " choix");
            // Afficher le dialogue de choix
            rogueindustry.ui.RewardDialog.showChoices(wave, choices);
        }
    }

    /** Informe le client qu'une récompense a été appliquée (pour le HUD/log). */
    public static class RewardAppliedPacket extends Packet{
        public String playerName;
        public String rewardName;
        public String rewardType;
        public String targetName; // pour UnitBonus, null sinon

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
            String msg = "[Rogue] " + playerName + " a choisi: " + rewardName;
            if(targetName != null){
                msg += " (cible: " + targetName + ")";
            }
            if(mindustry.Vars.player != null){
                mindustry.Vars.player.sendMessage(msg);
            }
        }
    }

    /** Synchronise l'état complet de la run (pour un joueur qui rejoint en cours). */
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
            // Reconstruire l'état côté client
            // (simplifié pour la Phase 1)
        }
    }

    /** Envoie un schematic à débloquer pour le joueur local. */
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
            // Décoder le schematic et l'ajouter à la collection du joueur
            try{
                mindustry.game.Schematic schematic = rogueindustry.rewards.SchematicGenerator.fromBase64(schematicData);
                if(schematic != null){
                    // Ajouter à la collection locale et sauvegarder
                    mindustry.Vars.schematics.all().add(schematic);
                    mindustry.Vars.schematics.saveChanges(schematic);

                    if(mindustry.Vars.player != null){
                        mindustry.Vars.player.sendMessage("[purple]Plan débloqué: [accent]" + schematicName +
                            "[] [gray]- Retrouvez-le dans vos plans!");
                    }
                    arc.util.Log.info("[RogueIndustry] Schematic ajouté: " + schematicName);
                }
            }catch(Throwable t){
                arc.util.Log.err("[RogueIndustry] Erreur lors du décodage du schematic: " + t.getMessage());
            }
        }
    }

    // ==================== C→S ====================

    /** Le joueur envoie son choix de récompense. */
    public static class RewardVotePacket extends Packet{
        public int choiceIndex;
        public int targetPlayerId; // -1 = pas de cible (pour UnitBonus: ID du joueur cible)

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

    // ==================== S→C: Sélection commandant ====================

    /** Envoie les 3 choix de commandant pour le joueur local. */
    public static class CommanderChoicesPacket extends Packet{
        /** IDs des 3 commandants proposés (séparés par virgule ou tableau). */
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
            arc.util.Log.info("[RogueIndustry] CommanderChoicesPacket reçu côté client: " + commanderIds.length + " choix");
            // Convertir les IDs en Commanders et afficher le dialogue
            arc.struct.Seq<rogueindustry.meta.Commander> commanders = new arc.struct.Seq<>();
            for(String id : commanderIds){
                rogueindustry.meta.Commander c = rogueindustry.meta.CommanderRegistry.get(id);
                if(c != null) commanders.add(c);
            }
            arc.util.Log.info("[RogueIndustry] Affichage dialogue sélection commandant avec " + commanders.size + " commandants");
            rogueindustry.meta.CommanderSelectionDialog.showChoices(commanders);
        }
    }

    // ==================== C→S: Vote commandant ====================

    /** Le joueur envoie son choix de commandant. */
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

    // ==================== S→C: Compte à rebours ====================

    /** Envoie le temps de compte à rebours avant la première vague. */
    public static class CountdownPacket extends Packet{
        /** Secondes restantes. */
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
            arc.util.Log.info("[RogueIndustry] CountdownPacket reçu côté client: " + seconds + "s");
            rogueindustry.ui.CountdownHUD.show(seconds);
        }
    }

    // ==================== Joueur actif par entité (bidirectionnel) ====================

    /**
     * Définit le joueur actif pour un bâtiment ou une unité (ou un groupe).
     *
     * C→S: un client demande à changer le joueur actif.
     * S→C: le serveur broadcast le changement à tous les clients.
     *
     * Supporte les groupes: plusieurs entités peuvent être modifiées en une fois.
     */
    public static class SetActivePlayerPacket extends Packet{
        /** IDs des entités (bâtiments ou unités). */
        public int[] entityIds;

        /** true = bâtiments, false = unités. */
        public boolean isBuilding;

        /** ID du joueur actif (-1 = aucun). */
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
            // Le serveur applique le changement et broadcast à tous
            if(RogueIndustryMod.serverController != null){
                RogueIndustryMod.serverController.handleSetActivePlayer(entityIds, isBuilding, playerId);
            }

            // Broadcast à tous les clients (y compris l'expéditeur pour confirmation)
            for(Player p : mindustry.gen.Groups.player){
                if(p.con != null){
                    p.con.send(this, true);
                }
            }
        }

        @Override
        public void handleClient(){
            // Appliquer le changement côté client (pour l'UI et les stats visuelles)
            if(RogueIndustryMod.clientController != null){
                RogueIndustryMod.clientController.onActivePlayerChanged(entityIds, isBuilding, playerId);
            }
        }
    }

    // ==================== C→S: Demande de démarrage/arrêt ====================

    /** Le client demande au serveur de démarrer une run rogue. */
    public static class RogueStartRequestPacket extends Packet{
        @Override
        public void write(Writes write){}

        @Override
        public void read(Reads read){}

        @Override
        public void handleServer(NetConnection con){
            arc.util.Log.info("[RogueIndustry] RogueStartRequestPacket reçu!");
            Player player = con.player;
            if(player == null){
                arc.util.Log.warn("[RogueIndustry] Player null dans RogueStartRequestPacket!");
                return;
            }
            if(RogueIndustryMod.serverController == null){
                arc.util.Log.warn("[RogueIndustry] serverController null! ServerLoadEvent pas encore déclenché?");
                player.sendMessage("[scarlet]Le serveur n'est pas prêt. Réessayez dans quelques secondes.");
                return;
            }

            if(RogueIndustryMod.serverController.session.active){
                player.sendMessage("[scarlet]Une run est déjà en cours.");
                return;
            }

            player.sendMessage("[accent]Sélection du commandant...");
            arc.util.Log.info("[RogueIndustry] Démarrage de la sélection de commandant pour " + player.name);
            RogueIndustryMod.serverController.startCommanderSelection();
        }
    }

    /** Le client demande au serveur d'arrêter la run rogue. */
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
                player.sendMessage("[scarlet]Aucune run en cours.");
                return;
            }

            RogueIndustryMod.serverController.stopRun();
            player.sendMessage("[yellow]Run arrêtée.");
        }
    }
}
