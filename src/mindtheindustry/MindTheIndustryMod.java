package mindtheindustry;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindtheindustry.content.*;
import mindtheindustry.core.*;
import mindtheindustry.meta.*;
import mindtheindustry.net.*;

import static mindustry.Vars.*;

/**
 * MindTheIndustry mod - Rogue-like mode for Mindustry co-op.
 *
 * Entry point: the "MindTheIndustry" planet in the New Game menu. Launching it
 * procedurally generates a fresh map (native campaign generator, new seed per
 * run) and auto-starts the run.
 *
 * In-game control: the server-side /mindtheindustry command (works identically
 * in solo and multiplayer). After each completed wave, each player gets 3
 * random reward choices in chat and picks with /mindtheindustry reward <index>.
 */
public class MindTheIndustryMod extends Mod{

    /** Server controller (null on a pure client). */
    public static ServerMindTheIndustryController serverController;

    /** Server session (alias for quick access). */
    public static MindTheIndustrySession serverSession;

    public MindTheIndustryMod(){
        Log.info("[MindTheIndustry] Mod loaded.");

        // Register network packets (only SchematicUnlockPacket remains: S→C data
        // that cannot travel through chat)
        MindTheIndustryPackets.register();
        Log.info("[MindTheIndustry] Packets registered.");

        // Server-side controller
        Events.on(ServerLoadEvent.class, e -> {
            serverController = new ServerMindTheIndustryController();
            serverSession = serverController.session;
            Log.info("[MindTheIndustry] Server controller initialized (ServerLoadEvent).");
        });

        // Client-side meta-progression (unlocked commanders are stored locally)
        Events.on(ClientLoadEvent.class, e -> MetaProgression.load());

        // When a MindTheIndustry map loads, force run-friendly rules
        Events.on(WorldLoadEvent.class, e -> {
            if(MindTheIndustryPlanet.isMindTheIndustryMap()){
                state.rules.waves = true;
                state.rules.waveTimer = true;
                state.rules.winWave = 0; //endless: the run ends when players stop
                Log.info("[MindTheIndustry] MindTheIndustry map detected, run rules applied.");
            }
        });

        // Auto-start the run when a MindTheIndustry map is played (solo + multi)
        Events.on(PlayEvent.class, e -> {
            if(serverController == null){
                serverController = new ServerMindTheIndustryController();
                serverSession = serverController.session;
                Log.info("[MindTheIndustry] Server controller initialized (PlayEvent fallback).");
            }
            if(MindTheIndustryPlanet.isMindTheIndustryMap() && !serverController.session.active){
                Log.info("[MindTheIndustry] Auto-starting run on MindTheIndustry map.");
                serverController.startCommanderSelection();
            }
        });

        Events.on(ServerLoadEvent.class, e -> {
            Log.info("[MindTheIndustry] In-game command: /mindtheindustry [start|stop|skip|status|vote|reward|active]");
        });
    }

    @Override
    public void loadContent(){
        // Registers the MindTheIndustry planet in the New Game menu
        new MindTheIndustryPlanet();
        Log.info("[MindTheIndustry] MindTheIndustry planet registered (New Game menu).");
    }

    /**
     * Server-side commands. Registered on the server so they work identically
     * in solo (integrated server) and in multiplayer (host/dedicated server).
     */
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.<Player>register("mindtheindustry", "[start|stop|skip|status|commanders|unlock-all|vote|reward|active]", "MindTheIndustry run control.", (args, player) -> {
            if(serverController == null){
                player.sendMessage("[scarlet]Server controller is not initialized.");
                return;
            }

            if(args.length == 0 || args[0].equals("status")){
                player.sendMessage("[accent]MindTheIndustry: " +
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
                    serverController.startCommanderSelection();
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
                        player.sendMessage("[scarlet]Usage: /mindtheindustry vote <index>");
                        return;
                    }
                    try{
                        serverController.handleCommanderVote(player, Integer.parseInt(args[1]));
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Invalid index.");
                    }
                }
                case "reward" -> {
                    // /mindtheindustry reward <index> [targetPlayer|none]
                    if(args.length < 2){
                        player.sendMessage("[scarlet]Usage: /mindtheindustry reward <index> [targetPlayer|none]");
                        return;
                    }
                    try{
                        int index = Integer.parseInt(args[1]);
                        Player target = player; //default: the voter themselves
                        if(args.length >= 3 && !args[2].equalsIgnoreCase("none")){
                            target = Groups.player.find(p -> p.name != null && p.name.toLowerCase().contains(args[2].toLowerCase()));
                            if(target == null){
                                player.sendMessage("[scarlet]Player not found: " + args[2]);
                                return;
                            }
                        }
                        serverController.handleRewardVote(player, index, target);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Invalid index.");
                    }
                }
                case "active" -> {
                    // /mindtheindustry active <entityId> <isBuilding> <playerName|none>
                    if(args.length < 4){
                        player.sendMessage("[scarlet]Usage: /mindtheindustry active <entityId> <isBuilding> <playerName|none>");
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
                default -> player.sendMessage("[scarlet]Usage: /mindtheindustry [start|stop|skip|status|commanders|unlock-all|vote|reward|active]");
            }
        });
    }
}
