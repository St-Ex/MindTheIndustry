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
 * État d'une run rogue-like, géré côté SERVEUR.
 *
 * Responsabilités:
 * - Tracker les unlocks de groupe (bâtiments débloqués pour tous)
 * - Tracker les unlocks individuels (schematics par joueur)
 * - Tracker les bonus d'unités par joueur
 * - Tracker les modificateurs globaux actifs
 * - Appliquer les récompenses choisies par les joueurs
 * - Gérer la progression des vagues et la difficulté
 */
public class RogueSession{

    /** Seed aléatoire de la run (partagé avec tous les clients). */
    public long seed;

    /** Numéro de la vague actuelle. */
    public int wave = 0;

    /** Run en cours? */
    public boolean active = false;

    /** Random déterministe basé sur le seed (pour la génération de récompenses). */
    public final Rand rand = new Rand();

    // --- Unlocks ---

    /** Bâtiments/unités débloqués pour TOUT le groupe (via BuildingReward). */
    private final ObjectSet<UnlockableContent> groupUnlocks = new ObjectSet<>();

    /** Schematics débloqués par joueur individuel (via SchematicReward). */
    private final ObjectMap<Player, ObjectSet<String>> playerSchematics = new ObjectMap<>();

    // --- Bonus ---

    /** Bonus d'unités par joueur (via UnitBonusReward). */
    private final ObjectMap<Player, Seq<UnitBonus>> playerUnitBonuses = new ObjectMap<>();

    /** Bonus de bâtiments par joueur (via BuildingBonusReward). */
    private final ObjectMap<Player, Seq<BuildingBonus>> playerBuildingBonuses = new ObjectMap<>();

    /** Modificateurs globaux actifs (via GlobalModifierReward). */
    private final Seq<GlobalModifier> globalModifiers = new Seq<>();

    /** Synergies enregistrées (prérequis → bonus). */
    private final ObjectMap<String, String> synergies = new ObjectMap<>();

    /** Commandant sélectionné pour cette run (meta-progression). */
    public rogueindustry.meta.Commander selectedCommander;

    // --- Joueur actif par entité ---
    // Quel joueur est "actif" sur quel bâtiment/unité.
    // Les bonus/malus de ce joueur s'appliquent à cette entité.

    /** Joueur actif par bâtiment. */
    private final ObjectMap<Building, Player> buildingActivePlayer = new ObjectMap<>();

    /** Joueur actif par unité. */
    private final ObjectMap<Unit, Player> unitActivePlayer = new ObjectMap<>();

    // --- Configuration de départ ---

    /** Bâtiments de base toujours débloqués au démarrage d'une run. */
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
        "arc", "ws", // Erekir turrets de base
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

    /** Démarre une nouvelle run avec un seed aléatoire. */
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

        // Débloquer les bâtiments de base pour le groupe
        for(String blockName : STARTER_BLOCKS){
            Block block = content.block(blockName);
            if(block != null){
                unlockForGroup(block);
            }
        }

        Log.info("[RogueIndustry] Run démarrée. Seed=" + seed);

        // Appliquer le pouvoir du commandant si sélectionné
        if(selectedCommander != null){
            Log.info("[RogueIndustry] Application du pouvoir de " + selectedCommander.displayName);
            selectedCommander.ability.apply(this);
        }
    }

    /** Démarre une run avec un seed spécifique (pour rejoindre une partie). */
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

        Log.info("[RogueIndustry] Run rejointe. Seed=" + seed);
    }

    /** Arrête la run. */
    public void stop(){
        active = false;
        Log.info("[RogueIndustry] Run terminée à la vague " + wave);
    }

    // --- Gestion des unlocks ---

    /** Débloque un contenu pour TOUT le groupe. */
    public void unlockForGroup(UnlockableContent content){
        if(groupUnlocks.add(content)){
            state.rules.researched.add(content);
            Log.info("[RogueIndustry] Débloqué pour le groupe: " + content.name);
        }
    }

    /** Vérifie si un contenu est débloqué pour le groupe. */
    public boolean isUnlockedForGroup(UnlockableContent content){
        return groupUnlocks.contains(content);
    }

    /** Retourne tous les unlocks de groupe. */
    public ObjectSet<UnlockableContent> getGroupUnlocks(){
        return groupUnlocks;
    }

    /** Débloque un schematic pour un joueur individuel. */
    public void unlockSchematicForPlayer(Player player, String schematicName){
        playerSchematics.get(player, ObjectSet::new).add(schematicName);
        Log.info("[RogueIndustry] Schematic '" + schematicName + "' débloqué pour " + player.name);
    }

    /** Vérifie si un joueur a débloqué un schematic. */
    public boolean hasSchematic(Player player, String schematicName){
        return playerSchematics.get(player, ObjectSet::new).contains(schematicName);
    }

    // --- Gestion des bonus d'unités ---

    /** Ajoute un bonus d'unité pour un joueur spécifique. */
    public void addUnitBonus(Player player, UnitBonus bonus){
        playerUnitBonuses.get(player, Seq::new).add(bonus);
        // Appliquer immédiatement sur les unités existantes du joueur
        applyBonusToPlayerUnits(player, bonus);
        Log.info("[RogueIndustry] Bonus " + bonus.describe() + " appliqué à " + player.name);
    }

    /** Retourne tous les bonus d'unités d'un joueur. */
    public Seq<UnitBonus> getUnitBonuses(Player player){
        return playerUnitBonuses.get(player, Seq::new);
    }

    /** Applique un bonus sur toutes les unités existantes d'un joueur. */
    private void applyBonusToPlayerUnits(Player player, UnitBonus bonus){
        if(player.unit() == null || !player.unit().isValid()) return;
        applyBonusToUnit(player.unit(), bonus);
    }

    /** Applique un bonus sur une unité spécifique. */
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
            // DAMAGE et RELOAD sont gérés via événements (BulletCreateEvent, etc.)
        }
    }

    /** Applique tous les bonus d'un joueur sur une nouvelle unité. */
    public void applyAllBonusesToUnit(Player player, Unit unit){
        Seq<UnitBonus> bonuses = playerUnitBonuses.get(player);
        if(bonuses != null){
            for(UnitBonus bonus : bonuses){
                applyBonusToUnit(unit, bonus);
            }
        }
    }

    // --- Gestion des bonus de bâtiments ---

    /** Ajoute un bonus de bâtiment pour un joueur spécifique. */
    public void addBuildingBonus(Player player, BuildingBonus bonus){
        playerBuildingBonuses.get(player, Seq::new).add(bonus);
        Log.info("[RogueIndustry] Bonus bâtiment " + bonus.describe() + " appliqué à " + player.name);
    }

    /** Retourne tous les bonus de bâtiments d'un joueur. */
    public Seq<BuildingBonus> getBuildingBonuses(Player player){
        return playerBuildingBonuses.get(player, Seq::new);
    }

    /**
     * Retourne le multiplicateur de dégâts pour un bâtiment spécifique d'un joueur.
     * Prend en compte les bonus ciblés (BuildingBonus) + modificateurs globaux.
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
     * Retourne le multiplicateur de vitesse de tir pour un bâtiment spécifique d'un joueur.
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
     * Retourne le multiplicateur de portée pour un bâtiment spécifique d'un joueur.
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
     * Retourne le multiplicateur de production pour un bâtiment spécifique d'un joueur.
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

    // --- Gestion des modificateurs globaux ---

    /** Ajoute un modificateur global. */
    public void addGlobalModifier(GlobalModifier modifier){
        globalModifiers.add(modifier);
        applyGlobalModifier(modifier);
        Log.info("[RogueIndustry] Modificateur global: " + modifier.describe());
    }

    /** Retourne tous les modificateurs globaux actifs. */
    public Seq<GlobalModifier> getGlobalModifiers(){
        return globalModifiers;
    }

    /** Applique un modificateur global aux règles de jeu. */
    private void applyGlobalModifier(GlobalModifier mod){
        switch(mod.id){
            case "prod_speed" -> {
                // Augmente la vitesse de production de tous les bâtiments
                // (sera implémenté via un multiplier global dans Phase 3)
            }
            case "build_speed" -> {
                state.rules.buildSpeedMultiplier *= (1f + mod.value);
            }
            case "turret_damage" -> {
                // Sera implémenté via événement BulletCreateEvent
            }
            case "unit_damage" -> {
                state.rules.unitDamageMultiplier *= (1f + mod.value);
            }
            case "unit_health" -> {
                state.rules.unitHealthMultiplier *= (1f + mod.value);
            }
            case "core_health" -> {
                // Multiplicateur de vie des cores
            }
        }
    }

    // --- Gestion des synergies ---

    public void addSynergy(String requiredUnlockId, String bonusDescription){
        synergies.put(requiredUnlockId, bonusDescription);
        Log.info("[RogueIndustry] Synergie enregistrée: " + requiredUnlockId + " → " + bonusDescription);
    }

    // --- Progression des vagues ---

    /** Incrémente le numéro de vague et applique la difficulté. */
    public void nextWave(){
        wave++;
        applyDifficulty();
        Log.info("[RogueIndustry] Vague " + wave + " - Difficulté appliquée");
    }

    /** Applique la courbe de difficulté en fonction de la vague. */
    private void applyDifficulty(){
        // Augmente les stats ennemies à chaque vague
        float healthBonus = wave * 0.05f;  // +5% vie ennemie par vague
        float damageBonus = wave * 0.03f;  // +3% dégâts ennemis par vague

        state.rules.unitHealthMultiplier = 1f + healthBonus;
        state.rules.unitDamageMultiplier = Math.max(state.rules.unitDamageMultiplier, 1f + damageBonus);
    }

    /** Retourne le multiplicateur de dégâts pour les unités d'un joueur (inclut bonus + synergies). */
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

    /** Retourne le multiplicateur de vitesse de tir pour les unités d'un joueur. */
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
    // JOUEUR ACTIF PAR ENTITÉ
    // ============================================================
    // Chaque bâtiment/unité peut avoir un "joueur actif" assigné.
    // Les bonus/malus de ce joueur s'appliquent à cette entité.
    // 3 bâtiments identiques peuvent avoir 3 joueurs actifs différents.

    // --- Bâtiments ---

    /** Définit le joueur actif pour un bâtiment. null = aucun joueur actif. */
    public void setActivePlayer(Building building, Player player){
        if(player == null){
            buildingActivePlayer.remove(building);
        }else{
            buildingActivePlayer.put(building, player);
        }
        Log.info("[RogueIndustry] Joueur actif sur bâtiment #" + building.tile.pos() + ": " +
                 (player != null ? player.name : "aucun"));
    }

    /** Retourne le joueur actif pour un bâtiment, ou null. */
    public Player getActivePlayer(Building building){
        return buildingActivePlayer.get(building);
    }

    /** Retourne les bonus de bâtiment d'un joueur applicables à un type de bâtiment. */
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

    /** Nettoie le joueur actif quand un bâtiment est détruit. */
    public void clearActivePlayer(Building building){
        buildingActivePlayer.remove(building);
    }

    // --- Unités ---

    /** Définit le joueur actif pour une unité. null = aucun joueur actif. */
    public void setActivePlayer(Unit unit, Player player){
        if(player == null){
            unitActivePlayer.remove(unit);
        }else{
            unitActivePlayer.put(unit, player);
            // Appliquer immédiatement les bonus du joueur sur l'unité
            applyAllBonusesToUnit(player, unit);
        }
        Log.info("[RogueIndustry] Joueur actif sur unité #" + unit.id + ": " +
                 (player != null ? player.name : "aucun"));
    }

    /** Retourne le joueur actif pour une unité, ou null. */
    public Player getActivePlayer(Unit unit){
        return unitActivePlayer.get(unit);
    }

    /** Retourne les bonus d'unités d'un joueur (pour tooltip). */
    public Seq<UnitBonus> getApplicableUnitBonuses(Player player){
        return playerUnitBonuses.get(player, Seq::new);
    }

    /** Nettoie le joueur actif quand une unité est détruite. */
    public void clearActivePlayer(Unit unit){
        unitActivePlayer.remove(unit);
    }

    // --- Multiplicateurs basés sur le joueur actif ---

    /** Multiplicateur de dégâts pour un bâtiment, basé sur son joueur actif. */
    public float getBuildingDamageMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingDamageMultiplier(active, building.block.name);
    }

    /** Multiplicateur de vitesse de tir pour un bâtiment, basé sur son joueur actif. */
    public float getBuildingReloadMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingReloadMultiplier(active, building.block.name);
    }

    /** Multiplicateur de portée pour un bâtiment, basé sur son joueur actif. */
    public float getBuildingRangeMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingRangeMultiplier(active, building.block.name);
    }

    /** Multiplicateur de production pour un bâtiment, basé sur son joueur actif. */
    public float getBuildingProductionMultiplier(Building building){
        Player active = getActivePlayer(building);
        if(active == null) return 1f;
        return getBuildingProductionMultiplier(active, building.block.name);
    }

    /** Multiplicateur de dégâts pour une unité, basé sur son joueur actif. */
    public float getUnitDamageMultiplier(Unit unit){
        Player active = getActivePlayer(unit);
        if(active == null) return 1f;
        return getDamageMultiplier(active);
    }

    /** Multiplicateur de vitesse de tir pour une unité, basé sur son joueur actif. */
    public float getUnitReloadMultiplier(Unit unit){
        Player active = getActivePlayer(unit);
        if(active == null) return 1f;
        return getReloadMultiplier(active);
    }

    /** Nettoie tous les joueurs actifs (fin de run). */
    public void clearAllActivePlayers(){
        buildingActivePlayer.clear();
        unitActivePlayer.clear();
    }
}
