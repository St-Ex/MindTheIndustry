package mindtheindustry.core;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindtheindustry.net.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Server controller for MindTheIndustry mode.
 *
 * Responsibilities:
 * - Start/stop a run
 * - Detect the end of a wave (all enemies killed)
 * - Generate and send reward choices to each player
 * - Receive player votes and apply the rewards
 * - Manage wave progression
 *
 * This controller only exists server-side (or in solo, where the local player is also the server).
 */
public class ServerMindTheIndustryController{

    /** Server session (run state). */
    public final MindTheIndustrySession session = new MindTheIndustrySession();

    /** Reward generator. */
    private RewardGenerator generator;

    /** Current choices per player (awaiting vote). */
    private final ObjectMap<Player, Seq<Reward>> pendingChoices = new ObjectMap<>();

    /** Votes received for the current wave. */
    private final ObjectMap<Player, Integer> votes = new ObjectMap<>();

    /** True if waiting for players to vote. */
    private boolean waitingForVotes = false;

    /** Wave being processed (to avoid double triggers). */
    private int lastProcessedWave = 0;

    /** Timer to regularly check whether enemies remain. */
    private float checkTimer = 0f;
    private static final float CHECK_INTERVAL = 1f; // seconds

    // --- Commander selection (run start) ---

    /** The 3 commander choices per player (awaiting vote). */
    private final ObjectMap<Player, Seq<mindtheindustry.meta.Commander>> pendingCommanderChoices = new ObjectMap<>();

    /** The commanders chosen per player. */
    private final ObjectMap<Player, mindtheindustry.meta.Commander> commanderVotes = new ObjectMap<>();

    /** True if waiting for commander votes. */
    private boolean waitingForCommanderVotes = false;

    /** Applies active-player bonuses to game stats. */
    private BonusApplier bonusApplier;

    public ServerMindTheIndustryController(){
        // Listen for game start
        Events.on(PlayEvent.class, e -> {
            if(!state.rules.waves) return;
            // MindTheIndustry mode is activated manually via a command or a button
        });

        // Clear active players when an entity is destroyed
        setupEntityCleanup();

        // Assign the default owner on creation
        setupDefaultOwner();

        // Apply active-player bonuses to game stats
        bonusApplier = new BonusApplier(session);

        // Auto-start rogue mode if requested (main menu button)
        Events.on(PlayEvent.class, e -> {
            if(mindtheindustry.MindTheIndustryMod.autoStartMindTheIndustry && !session.active){
                mindtheindustry.MindTheIndustryMod.autoStartMindTheIndustry = false;
                Log.info("[MindTheIndustry] Auto-starting rogue mode (menu flag).");
                // Short delay to let the game initialize
                Timer.schedule(() -> startCommanderSelection(), 1f);
            }
        });

        // Regularly check whether enemies remain
        Events.run(Trigger.update, () -> {
            if(!session.active || waitingForVotes || waitingForCommanderVotes) return;

            checkTimer += Time.delta;
            if(checkTimer < CHECK_INTERVAL) return;
            checkTimer = 0f;

            checkWaveComplete();
        });

        // During the voting phase: disable all buildings (stop production)
        Events.run(Trigger.update, () -> {
            if(!session.active || !waitingForVotes) return;
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = false;
                }
            }
        });

        // When a new wave starts, increment the counter
        Events.on(WaveEvent.class, e -> {
            if(!session.active) return;
            // The wave just started, nothing to do here
            // End detection is done via checkWaveComplete
        });

        // Reset at end of game
        Events.on(ResetEvent.class, e -> {
            if(session.active){
                session.stop();
            }
            pendingChoices.clear();
            votes.clear();
            waitingForVotes = false;
            waitingForCommanderVotes = false;
            pendingCommanderChoices.clear();
            commanderVotes.clear();
            lastProcessedWave = 0;
            // Re-enable all buildings
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = true;
                }
            }
        });
    }

    /**
     * Starts the commander selection flow.
     * Sends 3 random choices per player among the unlocked commanders.
     * Once all players have voted, sends the countdown then starts the run.
     */
    public void startCommanderSelection(){
        session.start(); // initializes the base state (active=true, seed, etc.)
        generator = new RewardGenerator(session.rand); // create the reward generator
        waitingForCommanderVotes = true;
        pendingCommanderChoices.clear();
        commanderVotes.clear();

        Log.info("[MindTheIndustry] startCommanderSelection: players=" + Groups.player.size());

        Seq<mindtheindustry.meta.Commander> unlocked = mindtheindustry.meta.MetaProgression.getUnlocked();
        if(unlocked.isEmpty()){
            // Fallback: use the starters
            unlocked = mindtheindustry.meta.CommanderRegistry.starters();
        }

        for(Player p : Groups.player){
            // Generate 3 distinct random choices for this player
            Seq<mindtheindustry.meta.Commander> choices = new Seq<>(3);
            Seq<mindtheindustry.meta.Commander> pool = unlocked.copy();
            pool.shuffle();
            for(int i = 0; i < Math.min(3, pool.size); i++){
                choices.add(pool.get(i));
            }
            pendingCommanderChoices.put(p, choices);

            // Send the choices to the client
            MindTheIndustryPackets.CommanderChoicesPacket packet = new MindTheIndustryPackets.CommanderChoicesPacket();
            packet.commanderIds = new String[choices.size];
            for(int i = 0; i < choices.size; i++){
                packet.commanderIds[i] = choices.get(i).id;
            }
            if(p.con != null){
                p.con.send(packet, true);
            }else{
                // In solo, run on the client thread (UI thread)
                Log.info("[MindTheIndustry] Solo: Core.app.post for CommanderChoicesPacket");
                Core.app.post(() -> packet.handleClient());
            }
        }

        Log.info("[MindTheIndustry] Commander selection started. " + pendingCommanderChoices.size + " players.");
        // Note: no auto-vote in solo, the player chooses via the dialog
    }

    /** Handles a player's vote for their commander. */
    public void handleCommanderVote(Player player, int choiceIndex){
        if(!waitingForCommanderVotes) return;
        if(!pendingCommanderChoices.containsKey(player)) return;

        Seq<mindtheindustry.meta.Commander> choices = pendingCommanderChoices.get(player);
        if(choiceIndex < 0 || choiceIndex >= choices.size) return;

        mindtheindustry.meta.Commander chosen = choices.get(choiceIndex);
        commanderVotes.put(player, chosen);
        pendingCommanderChoices.remove(player);

        Log.info("[MindTheIndustry] " + player.name + " chose the commander: " + chosen.displayName);
        player.sendMessage("[accent]Commander chosen: " + chosen.displayName);
        player.sendMessage("[gray]Ability: " + chosen.ability.name() + " - " + chosen.ability.description());

        checkAllCommanderVotesReceived();
    }

    /** Checks whether all players have voted for their commander. */
    private void checkAllCommanderVotesReceived(){
        int totalPlayers = Groups.player.size();

        if(commanderVotes.size >= totalPlayers){
            waitingForCommanderVotes = false;
            finalizeCommanderSelection();
        }
    }

    /** Applies the chosen commanders and starts the countdown. */
    private void finalizeCommanderSelection(){
        // Apply each player's commander
        for(var entry : commanderVotes.entries()){
            Player p = entry.key;
            mindtheindustry.meta.Commander cmdr = entry.value;
            // Store the commander in the session (for the HUD and effects)
            // Note: in co-op, each player has their own commander
            // We store it in a map or apply it directly
            Log.info("[MindTheIndustry] Commander of " + p.name + ": " + cmdr.displayName);
            cmdr.ability.apply(session);
        }

        // Notify all clients that the run starts
        MindTheIndustryPackets.StartPacket packet = new MindTheIndustryPackets.StartPacket();
        packet.seed = session.seed;
        packet.wave = session.wave;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            // In solo, run on the client thread
            Log.info("[MindTheIndustry] Solo: Core.app.post for StartPacket");
            Core.app.post(() -> packet.handleClient());
        }

        // Send the countdown (10 seconds)
        MindTheIndustryPackets.CountdownPacket countdown = new MindTheIndustryPackets.CountdownPacket();
        countdown.seconds = 10;
        sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(countdown, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[MindTheIndustry] Solo: Core.app.post for CountdownPacket");
            Core.app.post(() -> countdown.handleClient());
        }

        Log.info("[MindTheIndustry] All commanders chosen. 10s countdown before wave 1.");

        // Start the first wave after the countdown
        Timer.schedule(() -> {
            if(!session.active) return;
            session.nextWave();
            state.wave = session.wave;
            state.rules.waveTimer = true;
            state.rules.waves = true;
            Log.info("[MindTheIndustry] Wave 1 started!");
        }, 10f);
    }

    /** Starts a new rogue run (after commander selection). */
    public void startRun(){
        session.start();
        generator = new RewardGenerator(session.rand);
        lastProcessedWave = 0;
        waitingForVotes = false;

        // Notify all clients
        MindTheIndustryPackets.StartPacket packet = new MindTheIndustryPackets.StartPacket();
        packet.seed = session.seed;
        packet.wave = session.wave;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[MindTheIndustry] Solo: Core.app.post for StartPacket (startRun)");
            Core.app.post(() -> packet.handleClient());
        }

        Log.info("[MindTheIndustry] Run started server-side. Seed=" + session.seed);

        // Start the first wave
        session.nextWave();
        state.wave = session.wave;
        state.rules.waveTimer = true;
        state.rules.waves = true;
    }

    /** Checks whether the current wave is complete (no enemies left). */
    private void checkWaveComplete(){
        // Do not trigger if this wave was already processed
        if(session.wave <= lastProcessedWave) return;

        // Count living enemies
        int enemies = Groups.unit.count(u -> u.team == state.rules.waveTeam && u.isValid());

        // Also count enemy buildings (enemy cores)
        int enemyCores = Groups.build.count(b -> b.team == state.rules.waveTeam && b instanceof mindustry.world.blocks.storage.CoreBlock.CoreBuild);

        if(enemies == 0 && enemyCores == 0){
            lastProcessedWave = session.wave;
            onWaveComplete();
        }
    }

    /** Called when a wave is complete (all enemies killed). */
    private void onWaveComplete(){
        Log.info("[MindTheIndustry] Wave " + session.wave + " complete!");

        waitingForVotes = true;
        votes.clear();
        pendingChoices.clear();

        // Strategy phase: no new wave, players vote at their own pace
        state.rules.waveTimer = false;
        // Generate and send the choices to each player
        for(Player player : Groups.player){
            if(player.dead()) continue;

            Seq<Reward> choices = generator.generateChoices(session, player, session.wave);
            pendingChoices.put(player, choices);

            // Send the choices to the client
            MindTheIndustryPackets.RewardChoicesPacket packet = new MindTheIndustryPackets.RewardChoicesPacket();
            packet.wave = session.wave;
            packet.choices = choices;
            if(player.con != null){
                player.con.send(packet, true);
            }else{
                // In solo, run on the client thread
                Log.info("[MindTheIndustry] Solo: Core.app.post for RewardChoicesPacket");
                Core.app.post(() -> packet.handleClient());
            }
        }
    }

    /**
     * Processes a player's reward vote (called via /mindtheindustry reward <index>).
     * In solo, the target defaults to the player themselves.
     */
    public void handleRewardVote(Player player, int choiceIndex){
        // In solo, the target is the player themselves
        handleVote(player, choiceIndex, player);
    }

    /**
     * Sets the active player for an entity (called via /mindtheindustry active).
     */
    public void setActivePlayer(int entityId, boolean isBuilding, Player target){
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

    /**
     * Processes a player's vote.
     *
     * @param player      the voting player
     * @param choiceIndex the choice index (0-2)
     * @param target      the target player (for UnitBonus; null otherwise)
     */
    public void handleVote(Player player, int choiceIndex, Player target){
        if(!waitingForVotes) return;
        if(!pendingChoices.containsKey(player)) return;

        Seq<Reward> choices = pendingChoices.get(player);
        if(choiceIndex < 0 || choiceIndex >= choices.size) return;

        Reward reward = choices.get(choiceIndex);

        // Remove the pending choices for this player
        pendingChoices.remove(player);
        votes.put(player, choiceIndex);

        // Apply the reward
        reward.apply(session, player, target);

        // Notify all clients
        MindTheIndustryPackets.RewardAppliedPacket packet = new MindTheIndustryPackets.RewardAppliedPacket();
        packet.playerName = player.name;
        packet.rewardName = reward.displayName;
        packet.rewardType = reward.type.name();
        packet.targetName = target != null ? target.name : null;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[MindTheIndustry] Solo: Core.app.post for RewardAppliedPacket");
            Core.app.post(() -> packet.handleClient());
        }

        Log.info("[MindTheIndustry] " + player.name + " chose: " + reward.displayName +
                 (target != null ? " (target: " + target.name + ")" : ""));

        // If all players have voted, move to the next wave
        checkAllVotesReceived();
    }

    /** Checks whether all players have voted. If so, moves to the next wave. */
    private void checkAllVotesReceived(){
        int alivePlayers = 0;
        for(Player p : Groups.player){
            if(!p.dead()) alivePlayers++;
        }

        if(votes.size >= alivePlayers){
            // Everyone voted, resume the game and start the next wave
            waitingForVotes = false;
            session.nextWave();
            state.wave = session.wave;

            // Re-enable the wave timer
            state.rules.waveTimer = true;

            // Re-enable all buildings
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = true;
                }
            }

            Log.info("[MindTheIndustry] All votes received. Wave " + session.wave + " started.");
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
        onWaveComplete();
    }

    /** Stops the current run. */
    public void stopRun(){
        session.stop();
        waitingForVotes = false;
        waitingForCommanderVotes = false;
        pendingChoices.clear();
        votes.clear();
        pendingCommanderChoices.clear();
        commanderVotes.clear();
        state.rules.waveTimer = true;
        // Re-enable all buildings
        for(Building b : Groups.build){
            if(b.team == state.rules.defaultTeam){
                b.enabled = true;
            }
        }
    }

    // --- Active player per entity ---

    /** Applies an active-player change on one or more entities. */
    public void handleSetActivePlayer(int[] entityIds, boolean isBuilding, int playerId){
        Player player = playerId >= 0 ? Groups.player.find(p -> p.id == playerId) : null;

        for(int entityId : entityIds){
            if(isBuilding){
                Building building = Groups.build.getByID(entityId);
                if(building != null){
                    session.setActivePlayer(building, player);
                }
            }else{
                Unit unit = Groups.unit.getByID(entityId);
                if(unit != null){
                    session.setActivePlayer(unit, player);
                }
            }
        }
    }

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

    // --- Commander selection (run start) ---
    // The handleCommanderVote, startCommanderSelection, etc. methods are defined above.

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
