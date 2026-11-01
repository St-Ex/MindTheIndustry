package mindtheindustry.core;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindtheindustry.client.*;
import mindtheindustry.net.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Server controller for MindTheIndustry mode.
 *
 * M1 model (reward pacing & boss window): nothing blocks the game. Waves plug
 * onto the native Mindustry wave system (the engine spawns them on its own
 * countdown; this controller only installs each wave's spawn groups). When a
 * wave ends (last enemy of the set dies), each player gets a choice of 3
 * proposals that stays open through the countdown AND the next wave's combat,
 * until the next choice appears — the previous unchosen one then converts to
 * a fallback resource grant. The chapter's boss wave opens a boss window
 * (ceasefire: relic choices, one trade, assignment switches, ready signal);
 * the final boss death goes straight to victory.
 *
 * This controller only exists server-side (in solo, the local player is also
 * the server, so ServerLoadEvent fires there too). Run control goes through
 * the /mindtheindustry server command; reward picking goes through the HUD box.
 */
public class ServerMindTheIndustryController{

    /** Server session (run state). */
    public final MindTheIndustrySession session = new MindTheIndustrySession();

    /** Reward generator. Package-private for tests. */
    RewardGenerator generator;

    /** Wave whose completion was already processed (avoids double triggers). */
    int lastProcessedWave = 0;

    /** Timer to regularly check wave completion / tick the boss window. */
    private float checkTimer = 0f;
    private static final float CHECK_INTERVAL = 1f; // seconds

    // --- Run start ---

    /**
     * Starts a run: initializes the session (starter floor unlocked), applies
     * the run pacing rules and schedules wave 1 after the initial break.
     * No vote gate — the choice overlay never blocks wave progression.
     */
    public void startRun(){
        if(session.active) return;
        session.start();
        generator = new RewardGenerator(session.rand);
        applyRunRules();
        state.wave = 1; //the engine spawns wave N from position N-1 = state.wave
        session.syncWave(0);
        installSpawns();
        state.wavetime = session.config.initialWaveSpacing;
        broadcast(msg("mti.run.started"));
        Log.info("[MindTheIndustry] Run started, wave 1 scheduled.");
    }

    /** Applies the run pacing rules (M1): 30s inter-wave countdown, 5 min initial break, mod-owned victory. */
    private void applyRunRules(){
        state.rules.waveSpacing = session.config.runWaveSpacing;
        state.rules.initialWaveSpacing = session.config.initialWaveSpacing;
        state.rules.winWave = 0; //the mod owns victory (final boss death) — vanilla must not sectorCapture
        state.rules.waves = true;
        state.rules.waveTimer = true;
        state.rules.waitEnemies = true; //countdown pauses while enemies remain: no wave stacking mid-fight
    }

    /** Installs the spawn groups of the upcoming wave (engine: wave N spawns from position state.wave - 1). */
    private void installSpawns(){
        state.rules.spawns = WaveDesigner.buildSpawns(state.wave, session.rand, session.config);
    }

    /** Applies active-player bonuses to game stats. */
    private BonusApplier bonusApplier;

    public ServerMindTheIndustryController(){
        // Clear active players when an entity is destroyed
        setupEntityCleanup();

        // Assign the default owner on creation
        setupDefaultOwner();

        // Apply active-player bonuses to game stats
        bonusApplier = new BonusApplier(session);

        // Native wave progression: when the engine spawned a wave (WaveEvent
        // fires right after state.wave++), sync the session and install the
        // next wave's spawn groups.
        Events.on(WaveEvent.class, e -> {
            if(!session.active || net.client()) return;
            session.syncWave(state.wave - 1);
            installSpawns();
        });

        // Regularly check whether the current wave is over + tick the boss window
        Events.run(Trigger.update, () -> {
            if(!session.active) return;

            checkTimer += Time.delta;
            if(checkTimer < CHECK_INTERVAL) return;
            checkTimer = 0f;

            if(session.tick(CHECK_INTERVAL)){
                closeBossWindow(msg("mti.bosswindow.cap"));
            }
            checkWaveComplete();
        });

        // Disconnect → pending choice converts
        Events.on(PlayerLeave.class, e -> {
            if(session.active) session.convertChoice(e.player);
        });

        // Reset at end of game
        Events.on(ResetEvent.class, e -> {
            if(session.active){
                session.stop();
            }
            lastProcessedWave = 0;
            state.rules.waveTimer = true;
            sendBossWindow(false, 0f);
            clearChoices();
        });
    }

    /** Bundle-backed message (no hardcoded user-facing literals). */
    static String msg(String key, Object... args){
        return Core.bundle.format(key, args);
    }

    /** Sends a chat message to a player (logs instead when there is no connection, e.g. headless tests). */
    static void notify(Player player, String message){
        if(player != null && player.con != null){
            player.sendMessage(message);
        }else{
            Log.info("[MindTheIndustry] -> " + (player == null ? "?" : player.name) + ": " + message);
        }
    }

    /** Broadcasts a chat message to all players. */
    static void broadcast(String message){
        for(Player p : Groups.player){
            notify(p, message);
        }
    }

    /** Checks whether the current wave is complete (last enemy of the wave set died). */
    private void checkWaveComplete(){
        if(session.wave <= lastProcessedWave) return;
        if(session.isBossWindowOpen()) return; //the window drives its own transition

        // Count living enemies
        int enemies = Groups.unit.count(u -> u.team == state.rules.waveTeam && u.isValid());

        // Also count enemy buildings (enemy cores)
        int enemyCores = Groups.build.count(b -> b.team == state.rules.waveTeam && b instanceof mindustry.world.blocks.storage.CoreBlock.CoreBuild);

        if(enemies == 0 && enemyCores == 0){
            lastProcessedWave = session.wave;
            onWaveComplete(session.wave);
        }
    }

    /**
     * Called when a wave is complete (last enemy of the wave set died).
     * Regular wave: reveal a 3-proposal choice per player (non-blocking overlay).
     * Boss wave: open the boss window, or victory if it was the final boss.
     */
    void onWaveComplete(int wave){
        Log.info("[MindTheIndustry] Wave " + wave + " complete!");

        if(session.config.isFinalBoss(wave)){
            victory();
            return;
        }

        if(session.config.isBossWave(wave)){
            openBossWindow();
            return;
        }

        // Regular wave end: the choice overlays the countdown AND the next
        // wave's combat; the countdown keeps running (nothing blocks).
        for(Player player : Groups.player){
            if(player.dead()) continue;

            Seq<Reward> proposals = generator.generateChoices(session, player, wave);
            session.openChoice(player, wave, proposals); //converts the previous unchosen choice
            sendChoices(player, proposals, false);

            StringBuilder sb = new StringBuilder("[accent]Wave " + wave + " complete! Choose a reward:");
            for(int i = 0; i < proposals.size; i++){
                Reward r = proposals.get(i);
                sb.append("\n  [yellow][").append(i).append("] [white]").append(r.displayName)
                  .append(" [gray]- ").append(r.description);
            }
            notify(player, sb.toString());
        }
    }

    /** Opens the boss window (ceasefire) after a boss kill: relic choices, one trade, assignment switches. */
    void openBossWindow(){
        session.openBossWindow(); //converts all pending wave choices
        // ceasefire: freeze the wave countdown (the world keeps simulating), no spawns until the window closes
        state.rules.waveTimer = false;
        state.wavetime = state.rules.waveSpacing;
        installSpawns(); //the next chapter's first wave is ready when the window closes

        for(Player player : Groups.player){
            if(player.dead()) continue;
            Seq<Reward> relics = generator.generateRelicChoices(session, player, session.wave);
            session.openRelicChoice(player, session.wave, relics);
            sendChoices(player, relics, true);
        }
        sendBossWindow(true, session.bossWindow.timeLeft);
        broadcast(msg("mti.bosswindow.open"));
        Log.info("[MindTheIndustry] Boss window opened.");
    }

    /** Closes the boss window (all ready, or duration cap); unchosen relic choices convert. */
    void closeBossWindow(String reason){
        if(!session.isBossWindowOpen()) return;
        session.closeBossWindow();
        state.rules.waveTimer = true; //resume the countdown to the next chapter's first wave
        sendBossWindow(false, 0f);
        broadcast(reason + " " + msg("mti.bosswindow.closed"));
        Log.info("[MindTheIndustry] Boss window closed: " + reason);
    }

    /** Victory: the final chapter's boss death goes straight to the victory screen — no break, no next chapter. */
    void victory(){
        session.markVictory();
        state.rules.waves = false;
        state.rules.waveTimer = false;
        clearChoices();
        sendBossWindow(false, 0f);
        state.gameOver = true;
        Events.fire(new GameOverEvent(state.rules.defaultTeam));
        broadcast(msg("mti.victory.chat"));
        Log.info("[MindTheIndustry] Victory at wave " + session.wave);
    }

    /** Sends a player their choices for the HUD box. In solo/host-local (no connection) the local state is set directly. */
    private void sendChoices(Player player, Seq<Reward> choices, boolean relic){
        if(player.con != null){
            player.con.send(new MindTheIndustryPackets.RewardChoicesPacket(session.wave, choices, relic), true);
        }else if(!net.client()){
            //solo or headless: no connection, deliver to the local client state
            ClientRewardState.instance.set(session.wave, choices, relic);
        }
    }

    /** Clears the reward choice box on every client. */
    private void clearChoices(){
        for(Player p : Groups.player){
            sendChoices(p, new Seq<>(), false);
        }
    }

    /** Sends the boss window state to every client. */
    private void sendBossWindow(boolean open, float timeLeft){
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(new MindTheIndustryPackets.BossWindowPacket(open, timeLeft), true);
            }else if(!net.client()){
                ClientRewardState.instance.setBossWindow(open, timeLeft);
            }
        }
    }

    /**
     * Processes a player's reward pick (called via RewardVotePacket from the HUD choice box).
     * {@code index == -1} is the explicit "take none" decision (conversion).
     */
    public void handleRewardVote(Player player, int choiceIndex, Player target){
        if(!session.active) return;
        ChoiceState choice = session.getOpenChoice(player);
        if(choice == null) return;

        if(choiceIndex == -1){
            session.takeNone(player);
            sendChoices(player, new Seq<>(), false);
            notify(player, msg("mti.choice.converted", session.config.fallbackItem, session.config.fallbackAmount));
            return;
        }

        Reward reward = session.pickChoice(player, choiceIndex);
        if(reward == null) return; //taking 2 of 3 is rejected

        reward.apply(session, player, target);
        sendChoices(player, new Seq<>(), false); //hide the picker's choice box

        broadcast("[MindTheIndustry] " + player.name + " chose: " + reward.displayName);
        Log.info("[MindTheIndustry] " + player.name + " chose: " + reward.displayName);
    }

    /** Ready signal: closes the boss window early when every connected alive player is ready. */
    public void handleReady(Player player){
        if(!session.isBossWindowOpen()) return;
        session.markReady(player);
        Seq<Player> alive = new Seq<>();
        for(Player p : Groups.player){
            if(!p.dead()) alive.add(p);
        }
        if(session.allReady(alive)){
            closeBossWindow(player.name);
        }
    }

    /** Offers a trade of an already-taken reward during the boss window. */
    public boolean offerTrade(Player from, Player to, String rewardId){
        TradeRequest req = session.offerTrade(from, to, rewardId);
        if(req == null){
            notify(from, msg("mti.trade.rejected"));
            return false;
        }
        notify(from, msg("mti.trade.offered", to.name, rewardId));
        notify(to, msg("mti.trade.incoming", from.name, rewardId));
        return true;
    }

    /** Confirms the pending trade (mutual confirmation executes it). */
    public void confirmTrade(Player player){
        if(!session.isBossWindowOpen() || session.bossWindow.pendingTrade == null){
            notify(player, msg("mti.trade.none"));
            return;
        }
        TradeRequest req = session.bossWindow.pendingTrade;
        session.confirmTrade(player);
        if(req.bothConfirmed()){
            broadcast(msg("mti.trade.done", req.from.name + " → " + req.rewardId, req.to.name));
        }
    }

    /**
     * Switches the active player of an entity. Allowed only during the boss
     * window (called via /mindtheindustry active or the switch packet).
     */
    public void setActivePlayer(int entityId, boolean isBuilding, Player target, Player requester){
        if(session.active && !session.isSwitchAllowed()){
            notify(requester, msg("mti.switch.denied"));
            return;
        }
        if(isBuilding){
            Building building = Groups.build.getByID(entityId);
            if(building != null){
                session.setActivePlayer(building, target);
                Log.info("[MindTheIndustry] Active player set for building #" + entityId + ": " +
                    (target != null ? target.name : "none"));
            }
        }else{
            Unit unit = Groups.unit.getByID(entityId);
            if(unit != null){
                session.setActivePlayer(unit, target);
                Log.info("[MindTheIndustry] Active player set for unit #" + entityId + ": " +
                    (target != null ? target.name : "none"));
            }
        }
    }

    /** Forces the next wave (admin command / debug). */
    public void skipToNextWave(){
        if(!session.active) return;

        // Kill all remaining enemies
        Groups.unit.each(u -> {
            if(u.team == state.rules.waveTeam){
                u.kill();
            }
        });

        lastProcessedWave = session.wave;
        onWaveComplete(session.wave);
    }

    /** Stops the current run. */
    public void stopRun(){
        session.stop();
        lastProcessedWave = 0;
        clearChoices();
        sendBossWindow(false, 0f);
        state.rules.waveTimer = true;
    }

    // --- Active player per entity ---

    /** Assigns the default owner when an entity is created. */
    public void setupDefaultOwner(){
        // Building built → owner = builder
        Events.on(BlockBuildEndEvent.class, e -> {
            if(!session.active) return;
            if(e.unit != null && e.unit.isPlayer()){
                Player builder = e.unit.getPlayer();
                if(builder != null && e.tile.build != null){
                    // Do not overwrite if an active player is already assigned
                    if(session.getActivePlayer(e.tile.build) == null){
                        session.setActivePlayer(e.tile.build, builder);
                    }
                }
            }
        });

        // Unit created → owner = the player controlling it
        Events.on(UnitSpawnEvent.class, e -> {
            if(!session.active) return;
            if(e.unit.isPlayer()){
                Player owner = e.unit.getPlayer();
                if(owner != null && session.getActivePlayer(e.unit) == null){
                    session.setActivePlayer(e.unit, owner);
                }
            }
        });
    }

    /** Clears active players when an entity is destroyed. */
    public void setupEntityCleanup(){
        Events.on(mindustry.game.EventType.BlockDestroyEvent.class, e -> {
            if(session.active && e.tile.build != null){
                session.clearActivePlayer(e.tile.build);
            }
        });
        Events.on(mindustry.game.EventType.UnitDestroyEvent.class, e -> {
            if(session.active){
                session.clearActivePlayer(e.unit);
            }
        });
    }
}
