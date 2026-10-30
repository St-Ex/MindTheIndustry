package mindtheindustry.core;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindtheindustry.meta.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Server controller for MindTheIndustry mode.
 *
 * Responsibilities:
 * - Start/stop a run
 * - Detect the end of a wave (all enemies killed)
 * - Generate reward choices for each player and announce them in chat
 * - Receive player votes via /mindtheindustry and apply the rewards
 * - Manage wave progression
 *
 * This controller only exists server-side (in solo, the local player is also
 * the server). All player interaction goes through chat + server commands, so
 * no custom UI packets are needed (only SchematicUnlockPacket remains, for
 * schematic data which cannot travel through chat).
 */
public class ServerMindTheIndustryController{

    /** Server session (run state). */
    public final MindTheIndustrySession session = new MindTheIndustrySession();

    /** Reward generator. Package-private for tests. */
    RewardGenerator generator;

    /** Current choices per player (awaiting vote). */
    final ObjectMap<Player, Seq<Reward>> pendingChoices = new ObjectMap<>();

    /** Votes received for the current wave. */
    final ObjectMap<Player, Integer> votes = new ObjectMap<>();

    /** True if waiting for players to vote. */
    boolean waitingForVotes = false;

    /** Wave being processed (to avoid double triggers). */
    int lastProcessedWave = 0;

    /** Timer to regularly check whether enemies remain. */
    private float checkTimer = 0f;
    private static final float CHECK_INTERVAL = 1f; // seconds

    // --- Commander selection (run start) ---

    /** The 3 commander choices per player (awaiting vote). */
    final ObjectMap<Player, Seq<Commander>> pendingCommanderChoices = new ObjectMap<>();

    /** The commanders chosen per player. */
    final ObjectMap<Player, Commander> commanderVotes = new ObjectMap<>();

    /** True if waiting for commander votes. */
    boolean waitingForCommanderVotes = false;

    /** Applies active-player bonuses to game stats. */
    private BonusApplier bonusApplier;

    public ServerMindTheIndustryController(){
        // Clear active players when an entity is destroyed
        setupEntityCleanup();

        // Assign the default owner on creation
        setupDefaultOwner();

        // Apply active-player bonuses to game stats
        bonusApplier = new BonusApplier(session);

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

    /** Sends a chat message to a player (logs instead when there is no connection, e.g. headless tests). */
    static void notify(Player player, String message){
        if(player.con != null){
            player.sendMessage(message);
        }else{
            Log.info("[MindTheIndustry] -> " + player.name + ": " + message);
        }
    }

    /** Broadcasts a chat message to all players. */
    static void broadcast(String message){
        for(Player p : Groups.player){
            notify(p, message);
        }
    }

    /**
     * Starts the commander selection flow.
     * Announces 3 random commander choices per player in chat;
     * players vote with /mindtheindustry vote <index>.
     * Once all players have voted, starts the run countdown.
     */
    public void startCommanderSelection(){
        session.start(); // initializes the base state (active=true, seed, etc.)
        generator = new RewardGenerator(session.rand); // create the reward generator
        waitingForCommanderVotes = true;
        pendingCommanderChoices.clear();
        commanderVotes.clear();

        Log.info("[MindTheIndustry] startCommanderSelection: players=" + Groups.player.size());

        Seq<Commander> unlocked = MetaProgression.getUnlocked();
        if(unlocked.isEmpty()){
            // Fallback: use the starters
            unlocked = CommanderRegistry.starters();
        }

        for(Player p : Groups.player){
            // Generate 3 distinct random choices for this player
            Seq<Commander> choices = new Seq<>(3);
            Seq<Commander> pool = unlocked.copy();
            pool.shuffle();
            for(int i = 0; i < Math.min(3, pool.size); i++){
                choices.add(pool.get(i));
            }
            pendingCommanderChoices.put(p, choices);

            StringBuilder sb = new StringBuilder("[accent]Choose your commander:");
            for(int i = 0; i < choices.size; i++){
                Commander c = choices.get(i);
                sb.append("\n  [yellow][").append(i).append("] [white]").append(c.displayName)
                  .append(" [gray]- ").append(c.ability.name());
            }
            sb.append("\n[gray]Vote with: [white]/mindtheindustry vote <index>");
            notify(p, sb.toString());
        }

        Log.info("[MindTheIndustry] Commander selection started. " + pendingCommanderChoices.size + " players.");
    }

    /** Handles a player's vote for their commander (/mindtheindustry vote <index>). */
    public void handleCommanderVote(Player player, int choiceIndex){
        if(!waitingForCommanderVotes) return;
        if(!pendingCommanderChoices.containsKey(player)) return;

        Seq<Commander> choices = pendingCommanderChoices.get(player);
        if(choiceIndex < 0 || choiceIndex >= choices.size) return;

        Commander chosen = choices.get(choiceIndex);
        commanderVotes.put(player, chosen);
        pendingCommanderChoices.remove(player);

        Log.info("[MindTheIndustry] " + player.name + " chose the commander: " + chosen.displayName);
        notify(player, "[accent]Commander chosen: " + chosen.displayName);
        notify(player, "[gray]Ability: " + chosen.ability.name() + " - " + chosen.ability.description());

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
            Commander cmdr = entry.value;
            Log.info("[MindTheIndustry] Commander of " + p.name + ": " + cmdr.displayName);
            cmdr.ability.apply(session);
        }

        broadcast("[accent]All commanders chosen — wave 1 in 10 seconds!");
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

    /**
     * Called when a wave is complete (all enemies killed).
     * Announces 3 reward choices per player in chat;
     * players vote with /mindtheindustry reward <index> [targetPlayer|none].
     */
    void onWaveComplete(){
        Log.info("[MindTheIndustry] Wave " + session.wave + " complete!");

        waitingForVotes = true;
        votes.clear();
        pendingChoices.clear();

        // Strategy phase: no new wave, players vote at their own pace
        state.rules.waveTimer = false;
        // Generate and announce the choices for each player
        for(Player player : Groups.player){
            if(player.dead()) continue;

            Seq<Reward> choices = generator.generateChoices(session, player, session.wave);
            pendingChoices.put(player, choices);

            StringBuilder sb = new StringBuilder("[accent]Wave " + session.wave + " complete! Choose a reward:");
            for(int i = 0; i < choices.size; i++){
                Reward r = choices.get(i);
                sb.append("\n  [yellow][").append(i).append("] [white]").append(r.displayName)
                  .append(" [gray]- ").append(r.description);
            }
            sb.append("\n[gray]Vote with: [white]/mindtheindustry reward <index> [targetPlayer|none]");
            notify(player, sb.toString());
        }
    }

    /**
     * Processes a player's reward vote (called via /mindtheindustry reward <index> [target]).
     */
    public void handleRewardVote(Player player, int choiceIndex, Player target){
        handleVote(player, choiceIndex, target);
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
     * @param target      the target player (for unit bonuses; may be null)
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

        broadcast("[MindTheIndustry] " + player.name + " chose: " + reward.displayName +
            (target != null ? " (target: " + target.name + ")" : ""));
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
