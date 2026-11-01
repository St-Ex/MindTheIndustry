package mindtheindustry.core;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;
/**
 * State of a rogue-like run, managed SERVER-side.
 *
 * Responsibilities:
 * - Track group unlocks (buildings/units unlocked for everyone)
 * - Track unit bonuses per player
 * - Track active global modifiers
 * - Apply the rewards chosen by players
 * - Manage wave progression and difficulty
 */
public class MindTheIndustrySession{

    /** Random seed of the run (shared with all clients). */
    public long seed;

    /** Number of the current wave. */
    public int wave = 0;

    /** Run in progress? */
    public boolean active = false;

    /** Deterministic random based on the seed (for reward generation). */
    public final Rand rand = new Rand();

    // --- Run pacing (M1: reward pacing & boss window) ---

    /** Per-run configuration knobs (chapter length, pacing, fallback grant, relic weights). */
    public final RunConfig config = new RunConfig();

    /** Current phase of the run state machine (WAVE / BOSS / BOSS_WINDOW; choices overlay, never block). */
    public RunPhase phase = RunPhase.WAVE;

    /** True once the final boss died (victory — no break, no next chapter). */
    public boolean victory = false;

    /** Open wave-end choice per player; converted (fallback grant) when replaced or at window open. */
    private final ObjectMap<Player, ChoiceState> choices = new ObjectMap<>();

    /** Current boss window (ceasefire), or null when not in a break. */
    public BossWindow bossWindow = null;

    /** Already-taken rewards per player (tradeable during a boss window). */
    private final ObjectMap<Player, Seq<Reward>> takenRewards = new ObjectMap<>();

    // --- Unlocks ---

    /** Content unlocked for the WHOLE group (via PlanReward). */
    private final ObjectSet<UnlockableContent> groupUnlocks = new ObjectSet<>();

    // --- Bonuses ---

    /** Unit bonuses per player. */
    private final ObjectMap<Player, Seq<UnitBonus>> playerUnitBonuses = new ObjectMap<>();

    /** Building bonuses per player. */
    private final ObjectMap<Player, Seq<BuildingBonus>> playerBuildingBonuses = new ObjectMap<>();

    /** Active global modifiers (applied via TradeoffReward). */
    private final Seq<GlobalModifier> globalModifiers = new Seq<>();

    /** Registered synergies (prerequisite → bonus). */
    private final ObjectMap<String, String> synergies = new ObjectMap<>();

    /** Commander selected for this run (meta-progression). */
    public mindtheindustry.meta.Commander selectedCommander;

    // --- Active player per entity ---
    // Which player is "active" on which building/unit.
    // This player's bonuses/maluses apply to that entity.

    /** Active player per building. */
    private final ObjectMap<Building, Player> buildingActivePlayer = new ObjectMap<>();

    /** Active player per unit. */
    private final ObjectMap<Unit, Player> unitActivePlayer = new ObjectMap<>();

    // --- Starting configuration ---

    /** Base buildings always unlocked when a run starts. */
    private static final String[] STARTER_BLOCKS = {
        "core-shard", "core-foundation", "core-nucleus",
        "conveyor", "junction", "router", "sorter", "overflow-gate",
        "distributor", "titanium-conveyor", "plastanium-conveyor",
        "power-node", "power-node-large",
        "combustion-generator", "steam-generator", "solar-panel", "differential-generator",
        "drill", "pneumatic-drill", "laser-drill", "blast-drill",
        "mechanical-pump", "rotary-pump", "impulse-pump",
        "wall", "large-wall", "titanium-wall", "thorium-wall",
        "door", "large-door",
        "mender", "mend-projector", "overdrive-projector",
        "container", "vault", "launch-pad",
        "switch", "message", "micro-processor", "logic-processor", "memory-cell", "memory-bank",
        "duo", "scatter", "scorch", "hail",
        "copper-wall", "copper-wall-large",
        "graphite-press", "silicon-smelter", "coal-centrifuge", "multi-press",
        "cultivator", "oil-extractor", "pyratite-mixer", "blast-mixer", "spore-press",
        "vent-condenser", "atmospheric-concentrator",
        "phase-conveyor", "phase-bridge",
        "mass-driver",
        "mechanical-drill", "eruption-drill",
        "ground-factory", "air-factory", "naval-factory",
        "additive-reconstructor", "multiplicative-reconstructor",
        "exponential-reconstructor", "tetrative-reconstructor",
        "tank-fabricator", "tank-refabricator", "tank-assembler",
        "ship-fabricator", "ship-refabricator", "ship-assembler",
        "mech-fabricator", "mech-refabricator", "mech-assembler",
        "prime-refabricator", "basic-assembler-module",
        "rally", "repair-point", "repair-tower", "regen-projector",
        "shockwave-tower",
        "luminous-silicon-fabricator", "heat-redirector", "heat-router",
        "slag-incinerator", "phase-heater", "heat-turbine",
        "slag-heater", "thermal-generator",
        "neoplasia-heater", "neoplasia-centrifuge",
        "vent-compressor",
        "plasma-bore", "impact-drill", "eruption-drill",
        "carbide-crucible", "slag-incinerator",
        "pulse-conduit", "plated-conduit", "surge-conduit", "phase-conduit", "bridge-conduit",
        "liquid-router", "liquid-junction", "liquid-container", "liquid-tank",
        "arc", "ws", // Erekir base turrets
        "beryllium-wall", "beryllium-wall-large", "tungsten-wall", "tungsten-wall-large",
        "shielded-wall",
        "thruster", "small-thruster",
        "fuse", "ripples", "cyclone", "foreshadow", "breach", "sublimate", "disrupt", "tsunami",
        "afflict", "distraction", "aegis", "navanax",
        "conveyor", "titanium-conveyor",
        "duct", "duct-router", "duct-bridge", "armored-duct", "duct-unloader",
        "armored-conveyor", "bridge-conveyor", "underflow-gate",
        "heat-conduit", "heat-router", "heat-redirector",
        "surge-conveyor", "surge-router",
        "unit-cargo-loader", "unit-cargo-unload-point",
        "tank", "ship",
        "command-center",
        "payload-conveyor", "payload-router", "payload-mass-driver", "payload-void", "payload-source",
        "large-payload-mass-driver",
        "small-deconstructor",
        "core-bastion", "core-citadel", "core-core",
        "reinforced-conveyor", "reinforced-junction", "reinforced-router", "reinforced-bridge",
        "reinforced-conduit", "reinforced-liquid-junction", "reinforced-liquid-router", "reinforced-bridge-conduit", "reinforced-liquid-container", "reinforced-liquid-tank",
        "reinforced-pump",
        "reinforced-power-node",
        "large-shield-projector",
        "force-projector",
    };

    /** Basic unit plans available from the start of a run. */
    private static final String[] STARTER_UNITS = {
        // Serpulo ground/air/naval basics
        "dagger", "mace", "nova", "flare", "horizon", "crawler", "risso", "minke",
        // Erekir basics (small mechs/fliers)
        "alpha", "beta", "gamma", "stell", "locus", "elude",
    };

    public MindTheIndustrySession(){
    }

    /** Unlocks the basic plans (starter blocks + units) for the whole group. */
    private void unlockStarterPlans(){
        for(String blockName : STARTER_BLOCKS){
            Block block = content.block(blockName);
            if(block != null){
                unlockForGroup(block);
            }
        }
        for(String unitName : STARTER_UNITS){
            UnitType unit = content.unit(unitName);
            if(unit != null){
                unlockForGroup(unit);
            }
        }
    }

    /** Starts a new run with a random seed. */
    public void start(){
        this.seed = new Rand().nextLong();
        this.rand.setSeed(seed);
        this.wave = 0;
        this.active = true;

        groupUnlocks.clear();
        playerUnitBonuses.clear();
        playerBuildingBonuses.clear();
        globalModifiers.clear();
        synergies.clear();
        clearAllActivePlayers();
        choices.clear();
        takenRewards.clear();
        bossWindow = null;
        phase = RunPhase.WAVE;
        victory = false;

        // Unlock the basic plans (blocks + units) for the group
        unlockStarterPlans();

        Log.info("[MindTheIndustry] Run started. Seed=" + seed);

        // Apply the commander's ability if one is selected
        if(selectedCommander != null){
            Log.info("[MindTheIndustry] Applying ability of " + selectedCommander.displayName);
            selectedCommander.ability.apply(this);
        }
    }

    /** Starts a run with a specific seed (to join a game). */
    public void start(long seed){
        this.seed = seed;
        this.rand.setSeed(seed);
        this.wave = 0;
        this.active = true;

        groupUnlocks.clear();
        playerUnitBonuses.clear();
        playerBuildingBonuses.clear();
        globalModifiers.clear();
        synergies.clear();
        clearAllActivePlayers();
        choices.clear();
        takenRewards.clear();
        bossWindow = null;
        phase = RunPhase.WAVE;
        victory = false;

        // Unlock the basic plans (blocks + units) for the group
        unlockStarterPlans();

        Log.info("[MindTheIndustry] Run joined. Seed=" + seed);
    }

    /** Stops the run. */
    public void stop(){
        active = false;
        choices.clear();
        takenRewards.clear();
        bossWindow = null;
        phase = RunPhase.WAVE;
        Log.info("[MindTheIndustry] Run ended at wave " + wave);
    }

    // --- Wave progression (native engine counter) ---

    /** Syncs the session wave with the engine's wave counter and applies the difficulty curve. */
    public void syncWave(int wave){
        this.wave = wave;
        applyDifficulty();
        Log.info("[MindTheIndustry] Wave " + wave + " - difficulty applied");
    }

    // --- Choices (wave-end proposals, non-blocking overlay) ---

    /**
     * Opens a new wave-end choice for a player. An unchosen previous choice
     * converts first (fallback resource grant) — conversion on replacement.
     */
    public ChoiceState openChoice(Player player, int waveNumber, Seq<Reward> proposals){
        convertChoice(player);
        ChoiceState choice = new ChoiceState(player, waveNumber, proposals);
        choices.put(player, choice);
        return choice;
    }

    /** The player's current wave-end choice (may be resolved). */
    public ChoiceState getChoice(Player player){
        return choices.get(player);
    }

    /** The player's currently open choice: the relic choice during a boss window, else the wave choice. Null when none is open. */
    public ChoiceState getOpenChoice(Player player){
        if(isBossWindowOpen()){
            ChoiceState relic = bossWindow.relicChoices.get(player);
            if(relic != null && relic.isOpen()) return relic;
        }
        ChoiceState c = choices.get(player);
        return c != null && c.isOpen() ? c : null;
    }

    /**
     * Takes proposal {@code index} of the player's open choice and records it as taken.
     * At most 1 of the 3 proposals may be taken: a second pick is rejected (null).
     */
    public Reward pickChoice(Player player, int index){
        ChoiceState c = getOpenChoice(player);
        if(c == null) return null;
        Reward r = c.pick(index);
        if(r != null) recordTaken(player, r);
        return r;
    }

    /** Explicit "take none" decision: resolves the open choice and converts it. */
    public void takeNone(Player player){
        if(getOpenChoice(player) == null) return;
        convertChoice(player);
    }

    /**
     * Converts the player's open unchosen choice (wave or relic) into the fallback
     * resource grant delivered to the group cores.
     * @return true if a choice was converted.
     */
    public boolean convertChoice(Player player){
        ChoiceState c = isBossWindowOpen() ? bossWindow.relicChoices.get(player) : null;
        if(c == null) c = choices.get(player);
        if(c == null || c.resolved) return false;
        c.takeNone();
        choices.remove(player);
        if(bossWindow != null) bossWindow.relicChoices.remove(player);
        grantFallback();
        Log.info("[MindTheIndustry] Choice of " + player.name + " (wave " + c.waveNumber + ") converted to fallback grant");
        return true;
    }

    /** Fallback conversion: a small resource grant to the group cores (amount = balance pass). */
    private void grantFallback(){
        Item item = content.item(config.fallbackItem);
        Team team = state.rules.defaultTeam;
        if(item == null || team == null) return;
        for(CoreBuild core : team.cores()){
            if(core.items != null) core.items.add(item, config.fallbackAmount);
        }
    }

    // --- Taken rewards (tradeable during the boss window) ---

    public void recordTaken(Player player, Reward reward){
        takenRewards.get(player, Seq::new).add(reward);
    }

    public Seq<Reward> getTakenRewards(Player player){
        return takenRewards.get(player, Seq::new);
    }

    public boolean hasTaken(Player player, String rewardId){
        return findTaken(player, rewardId) != null;
    }

    public Reward findTaken(Player player, String rewardId){
        for(Reward r : takenRewards.get(player, Seq::new)){
            if(r.id.equals(rewardId)) return r;
        }
        return null;
    }

    public Reward removeTaken(Player player, String rewardId){
        Seq<Reward> taken = takenRewards.get(player);
        if(taken == null) return null;
        for(int i = 0; i < taken.size; i++){
            if(taken.get(i).id.equals(rewardId)) return taken.remove(i);
        }
        return null;
    }

    // --- Boss window (ceasefire between chapters) ---

    public boolean isBossWindowOpen(){
        return phase == RunPhase.BOSS_WINDOW && bossWindow != null;
    }

    /** Opens the boss window after a boss kill; all pending wave choices convert. */
    public void openBossWindow(){
        phase = RunPhase.BOSS_WINDOW;
        bossWindow = new BossWindow(config);
        for(Player p : choices.keys().toSeq()){
            convertChoice(p);
        }
        Log.info("[MindTheIndustry] Boss window opened after wave " + wave);
    }

    /** Opens the relic choice of a player inside the current boss window. */
    public ChoiceState openRelicChoice(Player player, int waveNumber, Seq<Reward> proposals){
        if(bossWindow == null) return null;
        convertChoice(player);
        ChoiceState c = new ChoiceState(player, waveNumber, proposals);
        bossWindow.relicChoices.put(player, c);
        return c;
    }

    /** Closes the boss window; unchosen relic choices convert. */
    public void closeBossWindow(){
        if(bossWindow == null) return;
        for(Player p : bossWindow.relicChoices.keys().toSeq()){
            convertChoice(p);
        }
        bossWindow = null;
        phase = RunPhase.WAVE;
        Log.info("[MindTheIndustry] Boss window closed");
    }

    /**
     * Ticks the boss window countdown (called from the server update loop).
     * @return true when the duration cap was reached and the window must close.
     */
    public boolean tick(float delta){
        if(isBossWindowOpen()){
            bossWindow.timeLeft -= delta;
            return bossWindow.timeLeft <= 0f;
        }
        return false;
    }

    /** Entity assignment switches are allowed only during the boss window. */
    public boolean isSwitchAllowed(){
        return isBossWindowOpen();
    }

    public void markReady(Player player){
        if(bossWindow != null) bossWindow.readyPlayers.add(player);
    }

    /** True when every given (connected, alive) player signalled ready — the window can close early. */
    public boolean allReady(Seq<Player> players){
        if(bossWindow == null || players.isEmpty()) return false;
        for(Player p : players){
            if(!bossWindow.readyPlayers.contains(p)) return false;
        }
        return true;
    }

    /**
     * Offers a trade of an already-taken reward during the boss window.
     * At most one trade per window; both sides must confirm.
     * @return the pending request, or null if rejected.
     */
    public TradeRequest offerTrade(Player from, Player to, String rewardId){
        if(!isBossWindowOpen()) return null;
        if(bossWindow.tradeUsed || bossWindow.pendingTrade != null) return null;
        if(from == to || !hasTaken(from, rewardId)) return null;
        TradeRequest req = new TradeRequest(from, to, rewardId);
        bossWindow.pendingTrade = req;
        return req;
    }

    /**
     * Confirms the pending trade for a player. When both sides confirmed, the
     * reward is transferred (effects move from donor to recipient) and the
     * window's single trade is consumed.
     */
    public void confirmTrade(Player player){
        TradeRequest req = bossWindow == null ? null : bossWindow.pendingTrade;
        if(req == null || !req.involves(player)) return;
        if(player == req.from) req.confirmedFrom = true;
        else req.confirmedTo = true;
        if(req.bothConfirmed()){
            bossWindow.pendingTrade = null;
            bossWindow.tradeUsed = true;
            Reward r = removeTaken(req.from, req.rewardId);
            if(r != null){
                recordTaken(req.to, r);
                r.transfer(this, req.from, req.to);
            }
            Log.info("[MindTheIndustry] Trade executed: " + req.rewardId + " from " + req.from.name + " to " + req.to.name);
        }
    }

    /** Marks the run won (final boss death): victory, no break, no next chapter. */
    public void markVictory(){
        victory = true;
        for(Player p : choices.keys().toSeq()){
            convertChoice(p);
        }
        Log.info("[MindTheIndustry] Victory at wave " + wave);
    }

    // --- Per-player bonus removal (trade transfer) ---

    public boolean removeUnitBonus(Player player, UnitBonusType type, float value){
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses == null) return false;
        for(int i = 0; i < bonuses.size; i++){
            UnitBonus b = bonuses.get(i);
            if(b.type == type && Math.abs(b.value - value) < 0.0001f){
                bonuses.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean removeBuildingBonus(Player player, BuildingBonus.BonusType type, float value, String blockName){
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses == null) return false;
        for(int i = 0; i < bonuses.size; i++){
            BuildingBonus b = bonuses.get(i);
            if(b.type == type && Math.abs(b.value - value) < 0.0001f &&
               (b.blockName == null ? blockName == null : b.blockName.equals(blockName))){
                bonuses.remove(i);
                return true;
            }
        }
        return false;
    }

    // --- Unlock management ---

    /** Snapshot of in-memory unlocked flags taken when entering an MTI map (for restoreUnlocks). */
    private final ObjectMap<UnlockableContent, Boolean> savedUnlockFlags = new ObjectMap<>();

    /**
     * Unlocks content for the WHOLE group. Sets only the in-memory flag (NOT
     * Core.settings, so MTI unlocks never leak into the campaign); the UnlockEvent
     * fired here is broadcast to clients by the engine (Call.researched), and
     * rules.researched carries the unlock for client build menus.
     */
    public void unlockForGroup(UnlockableContent content){
        if(groupUnlocks.add(content)){
            UnlockFlags.set(content, true);
            content.onUnlock();
            arc.Events.fire(new UnlockEvent(content));
            state.rules.researched.add(content);
            Log.info("[MindTheIndustry] Unlocked for the group: " + content.name);
        }
    }

    /**
     * MTI map loaded: hide every content that is not unlocked for the group, even if it
     * was unlocked in the campaign (the host build menu reads the in-memory 'unlocked'
     * flag). In-memory only — Core.settings is never written, so campaign progress on
     * disk is untouched. Call on the server only.
     */
    public void isolateUnlocks(){
        savedUnlockFlags.clear();
        for(ContentType type : ContentType.all){
            for(Content c : content.getBy(type)){
                if(c instanceof UnlockableContent u && !u.alwaysUnlocked){
                    savedUnlockFlags.put(u, UnlockFlags.get(u));
                    if(!groupUnlocks.contains(u)){
                        UnlockFlags.set(u, false);
                    }
                }
            }
        }
    }

    /** Restores the unlocked flags snapshot taken by isolateUnlocks (when leaving an MTI map). */
    public void restoreUnlocks(){
        for(ObjectMap.Entry<UnlockableContent, Boolean> e : savedUnlockFlags.entries()){
            UnlockFlags.set(e.key, e.value);
        }
        savedUnlockFlags.clear();
    }

    /** Checks whether content is unlocked for the group. */
    public boolean isUnlockedForGroup(UnlockableContent content){
        return groupUnlocks.contains(content);
    }

    /** Returns all group unlocks. */
    public ObjectSet<UnlockableContent> getGroupUnlocks(){
        return groupUnlocks;
    }

    // --- Unit bonus management ---

    /** Adds a unit bonus for a specific player. */
    public void addUnitBonus(Player player, UnitBonus bonus){
        playerUnitBonuses.get(player, Seq::new).add(bonus);
        // Apply immediately to the player's existing units
        applyBonusToPlayerUnits(player, bonus);
        Log.info("[MindTheIndustry] Bonus " + bonus.describe() + " applied to " + player.name);
    }

    /** Returns all unit bonuses of a player. */
    public Seq<UnitBonus> getUnitBonuses(Player player){
        return playerUnitBonuses.get(player, Seq::new);
    }

    /** Applies a bonus to all of a player's existing units. */
    private void applyBonusToPlayerUnits(Player player, UnitBonus bonus){
        if(player.unit() == null || !player.unit().isValid()) return;
        applyBonusToUnit(player.unit(), bonus);
    }

    /** Applies a bonus to a specific unit. */
    public void applyBonusToUnit(Unit unit, UnitBonus bonus){
        switch(bonus.type){
            case HEALTH -> {
                float ratio = unit.health / unit.maxHealth;
                unit.maxHealth *= (1f + bonus.value);
                unit.health = unit.maxHealth * ratio;
            }
            case SPEED -> {
                unit.speedMultiplier *= (1f + bonus.value);
            }
            case ARMOR -> {
                unit.armorOverride = unit.armorOverride >= 0 ? unit.armorOverride + bonus.value : unit.armor + bonus.value;
            }
            // DAMAGE and RELOAD are handled via events (BulletCreateEvent, etc.)
        }
    }

    /** Applies all of a player's bonuses to a new unit. */
    public void applyAllBonusesToUnit(Player player, Unit unit){
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses != null){
            for(UnitBonus bonus : bonuses){
                applyBonusToUnit(unit, bonus);
            }
        }
    }

    // --- Building bonus management ---

    /** Adds a building bonus for a specific player. */
    public void addBuildingBonus(Player player, BuildingBonus bonus){
        playerBuildingBonuses.get(player, Seq::new).add(bonus);
        Log.info("[MindTheIndustry] Building bonus " + bonus.describe() + " applied to " + player.name);
    }

    /** Returns all building bonuses of a player. */
    public Seq<BuildingBonus> getBuildingBonuses(Player player){
        return playerBuildingBonuses.get(player, Seq::new);
    }

    /**
     * Returns the damage multiplier for a specific building of a player.
     * Takes targeted bonuses (BuildingBonus) + global modifiers into account.
     */
    public float getBuildingDamageMultiplier(Player player, String blockName){
        float mult = 1f;
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses != null){
            for(BuildingBonus b : bonuses){
                if(b.type == BuildingBonus.BonusType.DAMAGE &&
                   (b.blockName == null || b.blockName.equals(blockName))){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    /**
     * Returns the fire rate multiplier for a specific building of a player.
     */
    public float getBuildingReloadMultiplier(Player player, String blockName){
        float mult = 1f;
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses != null){
            for(BuildingBonus b : bonuses){
                if(b.type == BuildingBonus.BonusType.RELOAD &&
                   (b.blockName == null || b.blockName.equals(blockName))){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    /**
     * Returns the range multiplier for a specific building of a player.
     */
    public float getBuildingRangeMultiplier(Player player, String blockName){
        float mult = 1f;
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses != null){
            for(BuildingBonus b : bonuses){
                if(b.type == BuildingBonus.BonusType.RANGE &&
                   (b.blockName == null || b.blockName.equals(blockName))){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    /**
     * Returns the production multiplier for a specific building of a player.
     */
    public float getBuildingProductionMultiplier(Player player, String blockName){
        float mult = 1f;
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses != null){
            for(BuildingBonus b : bonuses){
                if(b.type == BuildingBonus.BonusType.PRODUCTION &&
                   (b.blockName == null || b.blockName.equals(blockName))){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    // --- Global modifier management ---

    /** Adds a global modifier. */
    public void addGlobalModifier(GlobalModifier modifier){
        globalModifiers.add(modifier);
        applyGlobalModifier(modifier);
        Log.info("[MindTheIndustry] Global modifier: " + modifier.describe());
    }

    /** Returns all active global modifiers. */
    public Seq<GlobalModifier> getGlobalModifiers(){
        return globalModifiers;
    }

    /** Applies a global modifier to the game rules. */
    private void applyGlobalModifier(GlobalModifier mod){
        switch(mod.id){
            case "prod_speed" -> {
                // Increases the production speed of all buildings
                // (will be implemented via a global multiplier in Phase 3)
            }
            case "build_speed" -> {
                state.rules.buildSpeedMultiplier *= (1f + mod.value);
            }
            case "turret_damage" -> {
                // Will be implemented via BulletCreateEvent event
            }
            case "unit_damage" -> {
                state.rules.unitDamageMultiplier *= (1f + mod.value);
            }
            case "unit_health" -> {
                state.rules.unitHealthMultiplier *= (1f + mod.value);
            }
            case "core_health" -> {
                // Core health multiplier
            }
        }
    }

    // --- Synergy management ---

    public void addSynergy(String requiredUnlockId, String bonusDescription){
        synergies.put(requiredUnlockId, bonusDescription);
        Log.info("[MindTheIndustry] Synergy registered: " + requiredUnlockId + " → " + bonusDescription);
    }

    // --- Wave progression ---

    /** Increments the wave number and applies the difficulty. */
    public void nextWave(){
        syncWave(wave + 1);
    }

    /** Applies the difficulty curve based on the wave (knobs in {@link DifficultyTuning}). */
    private void applyDifficulty(){
        // Increases enemy stats each wave
        float healthBonus = wave * DifficultyTuning.healthPerWave;
        float damageBonus = wave * DifficultyTuning.damagePerWave;

        state.rules.unitHealthMultiplier = 1f + healthBonus;
        state.rules.unitDamageMultiplier = Math.max(state.rules.unitDamageMultiplier, 1f + damageBonus);
    }

    /** Returns the damage multiplier for a player's units (includes bonuses + synergies). */
    public float getDamageMultiplier(Player player){
        float mult = 1f;
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses != null){
            for(UnitBonus b : bonuses){
                if(b.type == UnitBonusType.DAMAGE){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    /** Returns the fire rate multiplier for a player's units. */
    public float getReloadMultiplier(Player player){
        float mult = 1f;
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses != null){
            for(UnitBonus b : bonuses){
                if(b.type == UnitBonusType.RELOAD){
                    mult *= (1f + b.value);
                }
            }
        }
        return mult;
    }

    // ============================================================
    // ACTIVE PLAYER PER ENTITY
    // ============================================================
    // Each building/unit can have an "active player" assigned.
    // This player's bonuses/maluses apply to that entity.
    // 3 identical buildings can have 3 different active players.

    // --- Buildings ---

    /** Sets the active player for a building. null = no active player. */
    public void setActivePlayer(Building building, Player player){
        if(player == null){
            buildingActivePlayer.remove(building);
        }else{
            buildingActivePlayer.put(building, player);
        }
        Log.info("[MindTheIndustry] Active player on building #" + building.tile.pos() + ": " +
                 (player != null ? player.name : "none"));
    }

    /** Returns the active player for a building, or null. */
    public Player getActivePlayer(Building building){
        return buildingActivePlayer.get(building);
    }

    /** Returns a player's building bonuses applicable to a building type. */
    public Seq<BuildingBonus> getApplicableBuildingBonuses(Player player, String blockName){
        Seq<BuildingBonus> result = new Seq<>();
        Seq<BuildingBonus> bonuses = playerBuildingBonuses.get(player);
        if(bonuses != null){
            for(BuildingBonus b : bonuses){
                if(b.blockName == null || b.blockName.equals(blockName)){
                    result.add(b);
                }
            }
        }
        return result;
    }

    /** Clears the active player when a building is destroyed. */
    public void clearActivePlayer(Building building){
        buildingActivePlayer.remove(building);
    }

    // --- Units ---

    /** Sets the active player for a unit. null = no active player. */
    public void setActivePlayer(Unit unit, Player player){
        if(player == null){
            unitActivePlayer.remove(unit);
        }else{
            unitActivePlayer.put(unit, player);
            // Immediately apply the player's bonuses to the unit
            applyAllBonusesToUnit(player, unit);
        }
        Log.info("[MindTheIndustry] Active player on unit #" + unit.id + ": " +
                 (player != null ? player.name : "none"));
    }

    /** Returns the active player for a unit, or null. */
    public Player getActivePlayer(Unit unit){
        return unitActivePlayer.get(unit);
    }

    /** Returns a player's unit bonuses (for tooltip). */
    public Seq<UnitBonus> getApplicableUnitBonuses(Player player){
        return playerUnitBonuses.get(player, Seq::new);
    }

    /** Clears the active player when a unit is destroyed. */
    public void clearActivePlayer(Unit unit){
        unitActivePlayer.remove(unit);
    }

    // --- Multipliers based on the active player ---

    /** Damage multiplier for a building, based on its active player. */
    public float getBuildingDamageMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingDamageMultiplier(active, building.block.name);
    }

    /** Fire rate multiplier for a building, based on its active player. */
    public float getBuildingReloadMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingReloadMultiplier(active, building.block.name);
    }

    /** Range multiplier for a building, based on its active player. */
    public float getBuildingRangeMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingRangeMultiplier(active, building.block.name);
    }

    /** Production multiplier for a building, based on its active player. */
    public float getBuildingProductionMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingProductionMultiplier(active, building.block.name);
    }

    /** Damage multiplier for a unit, based on its active player. */
    public float getUnitDamageMultiplier(Unit unit){
        Player active = getActivePlayer(unit);
        if(active == null) return 1f;
        return getDamageMultiplier(active);
    }

    /** Fire rate multiplier for a unit, based on its active player. */
    public float getUnitReloadMultiplier(Unit unit){
        Player active = getActivePlayer(unit);
        if(active == null) return 1f;
        return getReloadMultiplier(active);
    }

    /** Clears all active players (end of run). */
    public void clearAllActivePlayers(){
        buildingActivePlayer.clear();
        unitActivePlayer.clear();
    }
}
