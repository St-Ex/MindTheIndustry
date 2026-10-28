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
 * MindTheIndustry mod - Rogue-like mode for Mindustry co-op.
 *
 * After each completed wave, each player receives 3 random reward choices:
 * - Buildings unlocked for the whole group
 * - Schematics unlocked for a single player
 * - Unit bonuses targeted at a player of your choice
 * - Global modifiers for everyone
 * - Conditional synergies
 */
public class RogueIndustryMod extends Mod{

    /** Server controller (null on pure client). */
    public static ServerRogueController serverController;

    /** Client controller. */
    public static ClientRogueController clientController;

    /** Server session (alias for quick access). */
    public static RogueSession serverSession;

    /** Client session (alias for quick access). */
    public static RogueSession clientSession;

    /** If true, rogue mode starts automatically when a game starts. */
    public static boolean autoStartRogue = false;

    public RogueIndustryMod(){
        Log.info("[MindTheIndustry] Mod loaded.");

        // Register network packets
        RoguePackets.register();
        Log.info("[MindTheIndustry] Packets registered.");

        // Initialize the controllers
        clientController = new ClientRogueController();
        clientSession = clientController.session;

        // Server-side (or solo), initialize the server controller
        Events.on(ServerLoadEvent.class, e -> {
            serverController = new ServerRogueController();
            serverSession = serverController.session;
            Log.info("[MindTheIndustry] Server controller initialized (ServerLoadEvent).");
        });

        // Fallback: create the controller at PlayEvent if ServerLoadEvent didn't fire (solo)
        Events.on(mindustry.game.EventType.PlayEvent.class, e -> {
            if(serverController == null){
                serverController = new ServerRogueController();
                serverSession = serverController.session;
                Log.info("[MindTheIndustry] Server controller initialized (PlayEvent fallback).");
            }
        });

        // Client-side, on connect
        Events.on(ClientLoadEvent.class, e -> {
            Log.info("[MindTheIndustry] Client loaded - ready for rogue-like mode.");
            // Load meta-progression
            MetaProgression.load();
            // Initialize the active player selector
            rogueindustry.ui.EntityPlayerSelector.init();
            // Initialize the HUD
            rogueindustry.ui.RogueHUD.init();
        });

        // Admin command to start a run (debug)
        Events.on(ServerLoadEvent.class, e -> {
            // Will be replaced by a real UI button later
            Log.info("[MindTheIndustry] Use the server console: 'rogue start' to start a run");
        });
    }

    @Override
    public void loadContent(){
        Log.info("[MindTheIndustry] Loading content...");
        // Custom content (buildings, units) will be added here in Phase 3
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("rogue", "[start|stop|skip|status]", "Command for MindTheIndustry mode.", (args, player) -> {
            if(serverController == null){
                player.sendMessage("[scarlet]Server controller is not initialized.");
                return;
            }

            if(args.length == 0 || args[0].equals("status")){
                player.sendMessage("[accent]RogueIndustry: " +
                    (serverController.session.active ? "[green]ACTIVE" : "[red]INACTIVE") +
                    "[] | Wave: [yellow]" + serverController.session.wave +
                    "[] | Seed: [gray]" + serverController.session.seed);
                return;
            }

            switch(args[0]){
                case "start" -> {
                    if(serverController.session.active){
                        player.sendMessage("[scarlet]A run is already in progress.");
                        return;
                    }
                    // Start the commander selection flow (with a delay to let the player connect)
                    player.sendMessage("[accent]Commander selection...");
                    arc.util.Timer.schedule(() -> serverController.startCommanderSelection(), 0.5f);
                }
                case "stop" -> {
                    if(!serverController.session.active){
                        player.sendMessage("[scarlet]No run in progress.");
                        return;
                    }
                    serverController.stopRun();
                    player.sendMessage("[yellow]Run stopped.");
                }
                case "skip" -> {
                    if(!serverController.session.active){
                        player.sendMessage("[scarlet]No run in progress.");
                        return;
                    }
                    serverController.skipToNextWave();
                    player.sendMessage("[accent]Next wave forced.");
                }
                case "commanders" -> {
                    player.sendMessage("[accent]=== Unlocked commanders ===");
                    player.sendMessage("[gray]" + MetaProgression.unlockedCount() + "/" + MetaProgression.totalCount() + " unlocked");
                    for(Commander c : CommanderRegistry.all()){
                        String status = MetaProgression.isUnlocked(c) ? "[green]✓" : "[red]✗";
                        player.sendMessage(status + " " + c.displayName + " [gray]- " + c.ability.name());
                    }
                }
                case "unlock-all" -> {
                    for(Commander c : CommanderRegistry.all()){
                        MetaProgression.unlock(c);
                    }
                    player.sendMessage("[green]All commanders unlocked!");
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
                        player.sendMessage("[scarlet]Invalid index.");
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
                        player.sendMessage("[scarlet]Invalid index.");
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
                                player.sendMessage("[scarlet]Player not found: " + targetName);
                                return;
                            }
                        }
                        serverController.setActivePlayer(entityId, isBuilding, target);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Invalid ID.");
                    }
                }
                default -> player.sendMessage("[scarlet]Usage: /rogue [start|stop|skip|status|commanders|unlock-all|vote|reward|active]");
            }
        });
    }
}
