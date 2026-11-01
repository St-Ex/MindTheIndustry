package mindtheindustry;

import arc.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindtheindustry.client.*;
import mindtheindustry.content.*;
import mindtheindustry.core.*;
import mindtheindustry.meta.*;
import mindtheindustry.net.*;
import mindtheindustry.ui.*;

import static mindustry.Vars.*;

/**
 * MindTheIndustry mod - Rogue-like mode for Mindustry co-op.
 *
 * Entry point: the "MindTheIndustry" planet in the New Game menu. Launching it
 * procedurally generates a fresh map (native campaign generator, new seed per
 * run) and auto-starts the run.
 *
 * In-game control: the server-side /mindtheindustry command for run control
 * (start/stop/skip/status — works identically in solo and multiplayer, since
 * solo runs an integrated server). After each completed wave, each player gets
 * 3 random reward choices shown as clickable icons in a HUD box; clicking one
 * sends the vote to the server.
 */
public class MindTheIndustryMod extends Mod{

    /** Server controller (null on a pure client). */
    public static ServerMindTheIndustryController serverController;

    /** Server session (alias for quick access). */
    public static MindTheIndustrySession serverSession;

    public MindTheIndustryMod(){
        Log.info("[MindTheIndustry] Mod loaded.");

        // Register network packets (reward choices S→C, votes C→S)
        MindTheIndustryPackets.register();
        Log.info("[MindTheIndustry] Packets registered.");

        // Server-side controller. ServerLoadEvent fires in solo too (Mindustry
        // solo runs an integrated server) and on hosted/dedicated servers.
        Events.on(ServerLoadEvent.class, e -> {
            serverController = new ServerMindTheIndustryController();
            serverSession = serverController.session;
            Log.info("[MindTheIndustry] Server controller initialized (ServerLoadEvent).");
        });

        // Client-side meta-progression (unlocked commanders are stored locally)
        // + the HUD reward choice box
        Events.on(ClientLoadEvent.class, e -> {
            MetaProgression.load();
            ClientRewardState.instance.clear();
            ui.hudGroup.addChild(RewardChoicePanel.createContainer());

            // Victory screen flavor (corporate satire): GG title + subtitle from
            // the mod bundle — never hardcoded. Only on MindTheIndustry runs.
            Events.on(GameOverEvent.class, gameOver -> {
                if(!MindTheIndustryPlanet.isMindTheIndustryMap()) return;
                if(gameOver.winner != state.rules.defaultTeam) return;
                Dialog dialog = new Dialog(Core.bundle.get("mti.victory.title"));
                dialog.cont.add(Core.bundle.get("mti.victory.flavor")).wrap().width(520f).pad(10f);
                dialog.addCloseButton();
                dialog.show();
            });
        });

        // When a MindTheIndustry map loads, force run-friendly rules
        Events.on(WorldLoadEvent.class, e -> {
            // Safety net: make sure the server controller exists on any map load
            // (solo = integrated server, multi = host/dedicated)
            if(serverController == null){
                serverController = new ServerMindTheIndustryController();
                serverSession = serverController.session;
                Log.info("[MindTheIndustry] Server controller initialized (WorldLoadEvent fallback).");
            }
            if(MindTheIndustryPlanet.isMindTheIndustryMap()){
                state.rules.waitEnemies = true; //wave timer pauses while enemies remain, so waves don't stack mid-fight
                state.rules.winWave = 0; //the mod owns victory (final boss death) — vanilla must not sectorCapture
                //hide everything not unlocked for the run group (in-memory only, so
                //campaign-unlocked content does not leak into the MTI build menu)
                serverController.session.isolateUnlocks();
                Log.info("[MindTheIndustry] MindTheIndustry map detected, run rules applied.");
            }else if(serverController != null){
                //leaving an MTI map: restore the campaign's unlocked flags
                serverController.session.restoreUnlocks();
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
                serverController.startRun();
            }
        });

        Events.on(ServerLoadEvent.class, e -> {
            Log.info("[MindTheIndustry] In-game command: /mindtheindustry [start|stop|skip|status|vote|active]");
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
        handler.<Player>register("mindtheindustry", "[start|stop|skip|status|ready|trade|confirm|commanders|unlock-all|active]", "MindTheIndustry run control.", (args, player) -> {
            if(serverController == null){
                player.sendMessage("[scarlet]Server controller is not initialized.");
                return;
            }

            if(args.length == 0 || args[0].equals("status")){
                player.sendMessage("[accent]MindTheIndustry: " +
                    (serverController.session.active ? "[green]ACTIVE" : "[red]INACTIVE") +
                    "[] | Wave: [yellow]" + serverController.session.wave +
                    "[] | Chapter: [yellow]" + serverController.session.config.currentChapter(Math.max(1, serverController.session.wave)) +
                    "[] | Seed: [gray]" + serverController.session.seed);
                return;
            }

            switch(args[0]){
                case "start" -> {
                    if(serverController.session.active){
                        player.sendMessage("[scarlet]A run is already in progress.");
                        return;
                    }
                    serverController.startRun();
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
                case "ready" -> serverController.handleReady(player);
                case "trade" -> {
                    // /mindtheindustry trade <playerName> <rewardId> — one trade per boss window
                    if(args.length < 3){
                        player.sendMessage("[scarlet]Usage: /mindtheindustry trade <playerName> <rewardId>");
                        return;
                    }
                    Player target = Groups.player.find(p -> p.name != null && p.name.toLowerCase().contains(args[1].toLowerCase()));
                    if(target == null){
                        player.sendMessage("[scarlet]Player not found: " + args[1]);
                        return;
                    }
                    serverController.offerTrade(player, target, args[2]);
                }
                case "confirm" -> serverController.confirmTrade(player);
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
                case "active" -> {
                    // /mindtheindustry active <entityId> <isBuilding> <playerName|none> — boss window only
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
                        serverController.setActivePlayer(entityId, isBuilding, target, player);
                    }catch(NumberFormatException e){
                        player.sendMessage("[scarlet]Invalid ID.");
                    }
                }
                default -> player.sendMessage("[scarlet]Usage: /mindtheindustry [start|stop|skip|status|ready|trade|confirm|commanders|unlock-all|active]");
            }
        });
    }
}
