package rogueindustry;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import rogueindustry.core.*;
import rogueindustry.meta.*;
import rogueindustry.net.*;

/**
 * Mod RogueIndustry - Mode rogue-like pour Mindustry en coop.
 *
 * À chaque vague réussie, chaque joueur reçoit 3 choix de récompenses aléatoires:
 * - Bâtiments débloqués pour tout le groupe
 * - Plans (schematics) débloqués pour un joueur seul
 * - Bonus d'unités ciblés sur un joueur au choix
 * - Modificateurs globaux pour tous
 * - Synergies conditionnelles
 */
public class RogueIndustryMod extends Mod{

    /** Contrôleur serveur (null côté client pur). */
    public static ServerRogueController serverController;

    /** Contrôleur client. */
    public static ClientRogueController clientController;

    /** Session serveur (alias pour accès rapide). */
    public static RogueSession serverSession;

    /** Session client (alias pour accès rapide). */
    public static RogueSession clientSession;

    /** Si true, le mode rogue démarre automatiquement au lancement d'une partie. */
    public static boolean autoStartRogue = false;

    public RogueIndustryMod(){
        Log.info("[RogueIndustry] Mod chargé.");

        // Enregistrer les packets réseau
        RoguePackets.register();
        Log.info("[RogueIndustry] Packets enregistrés.");

        // Initialiser les contrôleurs
        clientController = new ClientRogueController();
        clientSession = clientController.session;

        // Côté serveur (ou solo), initialiser le contrôleur serveur
        Events.on(ServerLoadEvent.class, e -> {
            serverController = new ServerRogueController();
            serverSession = serverController.session;
            Log.info("[RogueIndustry] Contrôleur serveur initialisé (ServerLoadEvent).");
        });

        // Fallback: créer le contrôleur au PlayEvent si ServerLoadEvent n'a pas eu lieu (solo)
        Events.on(mindustry.game.EventType.PlayEvent.class, e -> {
            if(serverController == null){
                serverController = new ServerRogueController();
                serverSession = serverController.session;
                Log.info("[RogueIndustry] Contrôleur serveur initialisé (PlayEvent fallback).");
            }
        });

        // Côté client, à la connexion
        Events.on(ClientLoadEvent.class, e -> {
            Log.info("[RogueIndustry] Client chargé - prêt pour le mode rogue-like.");
            // Charger la meta-progression
            MetaProgression.load();
            // Initialiser le sélecteur de joueur actif
            rogueindustry.ui.EntityPlayerSelector.init();
            // Initialiser le HUD
            rogueindustry.ui.RogueHUD.init();
        });

        // Commande admin pour démarrer une run (debug)
        Events.on(ServerLoadEvent.class, e -> {
            // Sera remplacé par un vrai bouton UI plus tard
            Log.info("[RogueIndustry] Utilisez la console serveur: 'rogue start' pour démarrer une run");
        });
    }

    @Override
    public void loadContent(){
        Log.info("[RogueIndustry] Chargement du contenu...");
        // Le contenu custom (bâtiments, unités) sera ajouté ici en Phase 3
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("rogue", "[start|stop|skip|status]", "Commande du mode RogueIndustry.", (args, player) -> {
            if(serverController == null){
                player.sendMessage("[scarlet]Le contrôleur serveur n'est pas initialisé.");
                return;
            }

            if(args.length == 0 || args[0].equals("status")){
                player.sendMessage("[accent]RogueIndustry: " +
                    (serverController.session.active ? "[green]ACTIF" : "[red]INACTIF") +
                    "[] | Vague: [yellow]" + serverController.session.wave +
                    "[] | Seed: [gray]" + serverController.session.seed);
                return;
            }

            switch(args[0]){
                case "start" -> {
                    if(serverController.session.active){
                        player.sendMessage("[scarlet]Une run est déjà en cours.");
                        return;
                    }
                    // Lancer le flux de sélection de commandant (avec délai pour laisser le joueur se connecter)
                    player.sendMessage("[accent]Sélection du commandant...");
                    arc.util.Timer.schedule(() -> serverController.startCommanderSelection(), 0.5f);
                }
                case "stop" -> {
                    if(!serverController.session.active){
                        player.sendMessage("[scarlet]Aucune run en cours.");
                        return;
                    }
                    serverController.stopRun();
                    player.sendMessage("[yellow]Run arrêtée.");
                }
                case "skip" -> {
                    if(!serverController.session.active){
                        player.sendMessage("[scarlet]Aucune run en cours.");
                        return;
                    }
                    serverController.skipToNextWave();
                    player.sendMessage("[accent]Vague suivante forcée.");
                }
                case "commanders" -> {
                    player.sendMessage("[accent]=== Commandants débloqués ===");
                    player.sendMessage("[gray]" + MetaProgression.unlockedCount() + "/" + MetaProgression.totalCount() + " débloqués");
                    for(Commander c : CommanderRegistry.all()){
                        String status = MetaProgression.isUnlocked(c) ? "[green]✓" : "[red]✗";
                        player.sendMessage(status + " " + c.displayName + " [gray]- " + c.ability.name());
                    }
                }
                case "unlock-all" -> {
                    for(Commander c : CommanderRegistry.all()){
                        MetaProgression.unlock(c);
                    }
                    player.sendMessage("[green]Tous les commandants débloqués!");
                }
                case "vote" -> {
                    if(args.length < 2){
                        player.sendMessage("[scarlet]Usage: /rogue vote <index>");
                        return;
                    }
                    try{
                        int index = Integer.parseInt(args[1]);
                        serverController.handleCommanderVote(player, index);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Index invalide.");
                    }
                }
                case "reward" -> {
                    if(args.length < 2){
                        player.sendMessage("[scarlet]Usage: /rogue reward <index>");
                        return;
                    }
                    try{
                        int index = Integer.parseInt(args[1]);
                        serverController.handleRewardVote(player, index);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Index invalide.");
                    }
                }
                case "active" -> {
                    // /rogue active <entityId> <isBuilding> <playerName|none>
                    if(args.length < 4){
                        player.sendMessage("[scarlet]Usage: /rogue active <entityId> <isBuilding> <playerName|none>");
                        return;
                    }
                    try{
                        int entityId = Integer.parseInt(args[1]);
                        boolean isBuilding = Boolean.parseBoolean(args[2]);
                        String targetName = args[3];
                        Player target = null;
                        if(!targetName.equalsIgnoreCase("none") && !targetName.equals("-1")){
                            target = Groups.player.find(p -> p.name != null && p.name.toLowerCase().contains(targetName.toLowerCase()));
                            if(target == null){
                                player.sendMessage("[scarlet]Joueur non trouvé: " + targetName);
                                return;
                            }
                        }
                        serverController.setActivePlayer(entityId, isBuilding, target);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]ID invalide.");
                    }
                }
                default -> player.sendMessage("[scarlet]Usage: /rogue [start|stop|skip|status|commanders|unlock-all|vote|reward|active]");
            }
        });
    }
}
