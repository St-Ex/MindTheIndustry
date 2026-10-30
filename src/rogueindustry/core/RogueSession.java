package rogueindustry.core;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import rogueindustry.modifiers.*;

import static mindustry.Vars.*;
/**
 * State of a rogue-like run, managed SERVER-side.
 *
 * Responsibilities:
 * - Track group unlocks (buildings unlocked for everyone)
 * - Track individual unlocks (schematics per player)
 * - Track unit bonuses per player
 * - Track active global modifiers
 * - Apply the rewards chosen by players
 * - Manage wave progression and difficulty
 */
public class RogueSession{

    /** Random seed of the run (shared with all clients). */
    public long seed;

    /** Number of the current wave. */
    public int wave = 0;

    /** Run in progress? */
    public boolean active = false;

    /** Deterministic random based on the seed (for reward generation). */
    public final Rand rand = new Rand();

    // --- Unlocks ---

    /** Buildings/units unlocked for the WHOLE group (via BuildingReward). */
    private final ObjectSet<UnlockableContent> groupUnlocks = new ObjectSet<>();

    /** Schematics unlocked per individual player (via SchematicReward). */
    private final ObjectMap<Player, ObjectSet<String>> playerSchematics = new ObjectMap<>();

    // --- Bonuses ---

    /** Unit bonuses per player (via UnitBonusReward). */
    private final ObjectMap<Player, Seq<UnitBonus>> playerUnitBonuses = new ObjectMap<>();

    /** Building bonuses per player (via BuildingBonusReward). */
    private final ObjectMap<Player, Seq<BuildingBonus>> playerBuildingBonuses = new ObjectMap<>();

    /** Active global modifiers (via GlobalModifierReward). */
    private final Seq<GlobalModifier> globalModifiers = new Seq<>();

    /** Registered synergies (prerequisite → bonus). */
    private final ObjectMap<String, String> synergies = new ObjectMap<>();

    /** Commander selected for this run (meta-progression). */
    public rogueindustry.meta.Commander selectedCommander;

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
        "conveyor", "junction", "router", "bridge", "sorter", "overflow-gate",
        "distributor", "titanium-conveyor", "plastanium-conveyor",
        "power-node", "power-node-large", "battery", "battery-large",
        "combustion-generator", "steam-generator", "solar-panel", "differential-generator",
        "drill", "pneumatic-drill", "laser-drill", "blast-drill",
        "pump", "mechanical-pump",
        "wall", "large-wall", "titanium-wall", "thorium-wall",
        "door", "large-door",
        "mender", "mend-projector", "overdrive-projector",
        "container", "vault",
        "duo", "scatter", "scorch", "hail",
        "copper-wall", "copper-wall-large",
        "graphite-press", "silicon-smelter", "coal-centrifuge", "multi-press",
        "cultivator", "oil-extractor", "pyratite-mixer", "blast-mixer", "spore-press",
        "vent-condenser", "atmospheric-concentrator",
        "phase-conveyor", "phase-bridge",
        "mass-driver",
        "mechanical-drill", "eruption-drill",
        "core-silo",
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
        "pulse-conduit", "plated-conduit", "surge-conduit", "phase-conduit",
        "liquid-router", "liquid-junction", "liquid-bridge", "liquid-container", "liquid-tank",
        "arc", "ws", // Erekir base turrets
        "beryllium-wall", "beryllium-wall-large", "tungsten-wall", "tungsten-wall-large",
        "shielded-wall",
        "thruster", "small-thruster",
        "heavy-power-node",
        "large-battery",
        "fuse", "ripples", "cyclone", "foreshadow", "breach", "sublimate", "disrupt", "tsunami",
        "afflict", "distraction", "aegis", "navanax",
        "conveyor", "titanium-conveyor",
        "duct", "duct-router", "duct-bridge", "armored-duct",
        "heat-conduit", "heat-router", "heat-redirector",
        "surge-conveyor", "surge-router",
        "unit-cargo-loader", "unit-cargo-unload-point",
        "tank", "ship",
        "command-center",
        "payload-conveyor", "payload-router", "payload-mass-driver", "payload-void", "payload-source",
        "large-payload-mass-driver",
        "deconstruct",
        "core-bastion", "core-citadel", "core-core",
        "reinforced-conveyor", "reinforced-junction", "reinforced-router", "reinforced-bridge",
        "reinforced-conduit", "reinforced-liquid-junction", "reinforced-liquid-router", "reinforced-liquid-bridge", "reinforced-liquid-container", "reinforced-liquid-tank",
        "reinforced-pump",
        "reinforced-power-node",
        "large-shield-projector",
        "force-projector",
    };

    public RogueSession(){
    }

    /** Starts a new run with a random seed. */
    public void start(){
        this.seed = new Rand().nextLong();
        this.rand.setSeed(seed);
        this.wave = 0;
        this.active = true;

        groupUnlocks.clear();
        playerSchematics.clear();
        playerUnitBonuses.clear();
        playerBuildingBonuses.clear();
        globalModifiers.clear();
        synergies.clear();
        clearAllActivePlayers();

        // Unlock the base buildings for the group
        for(String blockName : STARTER_BLOCKS){
            Block block = content.block(blockName);
            if(block != null){
                unlockForGroup(block);
            }
        }

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
        playerSchematics.clear();
        playerUnitBonuses.clear();
        playerBuildingBonuses.clear();
        globalModifiers.clear();
        synergies.clear();
        clearAllActivePlayers();

        for(String blockName : STARTER_BLOCKS){
            Block block = content.block(blockName);
            if(block != null){
                unlockForGroup(block);
            }
        }

        Log.info("[MindTheIndustry] Run joined. Seed=" + seed);
    }

    /** Stops the run. */
    public void stop(){
        active = false;
        Log.info("[MindTheIndustry] Run ended at wave " + wave);
    }

    // --- Unlock management ---

    /** Unlocks content for the WHOLE group. */
    public void unlockForGroup(UnlockableContent content){
        if(groupUnlocks.add(content)){
            state.rules.researched.add(content);
            Log.info("[MindTheIndustry] Unlocked for the group: " + content.name);
        }
    }

    /** Checks whether content is unlocked for the group. */
    public boolean isUnlockedForGroup(UnlockableContent content){
        return groupUnlocks.contains(content);
    }

    /** Returns all group unlocks. */
    public ObjectSet<UnlockableContent> getGroupUnlocks(){
        return groupUnlocks;
    }

    /** Unlocks a schematic for an individual player. */
    public void unlockSchematicForPlayer(Player player, String schematicName){
        playerSchematics.get(player, ObjectSet::new).add(schematicName);
        Log.info("[MindTheIndustry] Schematic '" + schematicName + "' unlocked for " + player.name);
    }

    /** Checks whether a player has unlocked a schematic. */
    public boolean hasSchematic(Player player, String schematicName){
        return playerSchematics.get(player, ObjectSet::new).contains(schematicName);
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
        wave++;
        applyDifficulty();
        Log.info("[MindTheIndustry] Wave " + wave + " - Difficulty applied");
    }

    /** Applies the difficulty curve based on the wave. */
    private void applyDifficulty(){
        // Increases enemy stats each wave
        float healthBonus = wave * 0.05f;  // +5% enemy health per wave
        float damageBonus = wave * 0.03f;  // +3% enemy damage per wave

        state.rules.unitHealthMultiplier = 1f + healthBonus;
        state.rules.unitDamageMultiplier = Math.max(state.rules.unitDamageMultiplier, 1f + damageBonus);
    }

    /** Returns the damage multiplier for a player's units (includes bonuses + synergies). */
    public float getDamageMultiplier(Player player){
        float mult = 1f;
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses != null){
            for(UnitBonus b : bonuses){
                if(b.type == rogueindustry.rewards.rewards.UnitBonusReward.BonusType.DAMAGE){
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
                if(b.type == rogueindustry.rewards.rewards.UnitBonusReward.BonusType.RELOAD){
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
