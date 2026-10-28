package rogueindustry.core;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;
import rogueindustry.rewards.rewards.*;

/**
 * Génère les choix de récompenses aléatoires pour un joueur.
 *
 * Stratégie de génération:
 * - 3 choix aléatoires pondérés selon la vague et l'historique
 * - Éviter les doublons (un bâtiment déjà débloqué pour le groupe n'est plus proposé)
 * - Pondération: plus la vague est haute, plus les bonus sont puissants
 * - Chaque joueur a ses propres choix (asymétrie coop)
 */
public class RewardGenerator{

    /** Pool de bâtiments proposables (tourelles, usines, défenses, etc.). */
    private static final String[] BUILDING_POOL = {
        // Tourelles
        "duo", "scatter", "scorch", "hail", "wave", "lancer", "arc", "parallax", "segment",
        "tsunami", "swarm", "salvo", "fuse", "ripple", "cyclone", "foreshadow", "spectre", "meltdown",
        // Erekir
        "breach", "sublimate", "disrupt", "afflict", "distraction", "aegis", "navanax",
        // Défenses
        "mender", "mend-projector", "overdrive-projector", "force-projector", "large-shield-projector",
        "repair-point", "repair-tower", "regen-projector", "shockwave-tower",
        // Production
        "graphite-press", "silicon-smelter", "coal-centrifuge", "multi-press",
        "pyratite-mixer", "blast-mixer", "spore-press",
        "cultivator", "oil-extractor", "vent-condenser", "atmospheric-concentrator",
        "phase-weaver", "surge-smelter", "cryofluid-mixer", "neoplasia-centrifuge", "pyratite-mixer",
        "silicon-arc-furnace", "slag-incinerator", "phase-heater", "heat-turbine", "thermal-generator",
        "carbide-crucible", "slag-heater", "neoplasia-heater",
        // Forage
        "laser-drill", "blast-drill", "plasma-bore", "impact-drill", "eruption-drill",
        // Power
        "steam-generator", "solar-panel", "differential-generator", "thorium-reactor",
        "impact-reactor", "beam-node", "beam-tower", "turbine-condenser",
        // Distribution
        "plastanium-conveyor", "phase-conveyor", "phase-bridge", "surge-conveyor", "surge-router",
        "armored-duct", "duct-bridge", "duct-router",
        "pulse-conduit", "plated-conduit", "surge-conduit", "phase-conduit",
        "liquid-router", "liquid-junction", "liquid-bridge", "liquid-container", "liquid-tank",
        "reinforced-conduit", "reinforced-liquid-junction", "reinforced-liquid-bridge",
        // Stockage
        "vault", "container", "unloader",
        // Murs
        "titanium-wall", "thorium-wall", "phase-wall", "surge-wall",
        "titanium-wall-large", "thorium-wall-large", "phase-wall-large", "surge-wall-large",
        "beryllium-wall", "beryllium-wall-large", "tungsten-wall", "tungsten-wall-large",
        "shielded-wall", "carbide-wall", "carbide-wall-large",
        // Unités factories
        "ground-factory", "air-factory", "naval-factory",
        "additive-reconstructor", "multiplicative-reconstructor",
        "exponential-reconstructor", "tetrative-reconstructor",
        "tank-fabricator", "tank-refabricator", "tank-assembler",
        "ship-fabricator", "ship-refabricator", "ship-assembler",
        "mech-fabricator", "mech-refabricator", "mech-assembler",
        "prime-refabricator", "basic-assembler-module",
        // Liquid/Payload
        "mechanical-pump", "rotary-pump", "impulse-pump", "pulsar-pump",
        "reinforced-pump", "graphite-press",
        "payload-conveyor", "payload-router", "payload-mass-driver", "payload-void", "payload-source",
        "large-payload-mass-driver", "payload-deconstructor",
        "unit-cargo-loader", "unit-cargo-unload-point",
        // Cores
        "core-bastion", "core-citadel", "core-silo",
        // Special
        "launch-pad", "interplanetary-accelerator",
        "command-center", "reinforced-message-block",
        "world-processor", "neural-processor", "quantum-processor", "rune-processor",
        "overdrive-dome",
        "mass-driver",
        "heat-redirector", "heat-router",
        "slag-incinerator", "vent-condenser",
        "neoplasia-centrifuge", "neoplasia-heater",
        "pump", "rotary-pump",
        "large-liquid-container",
        "illuminator",
        "world-cell",
        "ancim-membrane-concentrator",
        "slag-heater", "thermal-generator",
        "beam-link", "beam-tower",
        "heavy-power-node", "large-battery",
        "large-shield-projector", "large-mend-projector",
        "overdrive-dome",
        "tetrative-reconstructor",
    };

    /** Pool d'unités proposables (pour déblocage dans les factories). */
    private static final String[] UNIT_POOL = {
        // Serpulo ground
        "dagger", "mace", "fortress", "scepter", "reign",
        "nova", "pulsar", "quasar", "vela", "corvus",
        "crawler", "atrax", "spiroct", "arkyid", "toxopid",
        // Serpulo air
        "flare", "horizon", "zenith", "antumbra", "eclipse",
        // Serpulo naval
        "risso", "minke", "bryde", "sei", "omura",
        "retusa", "oxynoe", "cyerce", "aegires", "navanax",
        // Erekir ground
        "stell", "locus", "precept", "vanquish", "conquer",
        "merui", "cleris", "anthicus", "tecta", "collaris",
        "elude", "avert", "obviate", "quell", "disrupt",
        // Erekir air
        "antumbra", "eclipse", "aegires",
        "navanax",
        "alpha", "beta", "gamma",
        // Erekir naval
        "risso", "minke", "bryde", "sei", "omura",
        "retusa", "oxynoe", "cyerce", "aegires", "navanax",
    };

    /** Tourelles avec bonus de dégâts/reload/portée. */
    private static final String[] TURRET_POOL = {
        "duo", "scatter", "scorch", "hail", "wave", "lancer", "arc", "parallax", "segment",
        "tsunami", "swarm", "salvo", "fuse", "ripple", "cyclone", "foreshadow", "spectre", "meltdown",
        "breach", "sublimate", "disrupt", "afflict", "distraction", "aegis", "navanax",
        "fuse", "ripples", "cyclone", "foreshadow", "breach", "sublimate", "disrupt", "tsunami",
        "afflict", "distraction", "aegis", "navanax",
    };

    /** Usines avec bonus de production. */
    private static final String[] FACTORY_POOL = {
        "graphite-press", "silicon-smelter", "coal-centrifuge", "multi-press",
        "pyratite-mixer", "blast-mixer", "spore-press",
        "cultivator", "oil-extractor", "vent-condenser", "atmospheric-concentrator",
        "phase-weaver", "surge-smelter", "cryofluid-mixer", "neoplasia-centrifuge",
        "silicon-arc-furnace", "slag-incinerator", "phase-heater", "heat-turbine", "thermal-generator",
        "carbide-crucible", "slag-heater", "neoplasia-heater",
        "pump", "rotary-pump", "impulse-pump",
        "laser-drill", "blast-drill", "plasma-bore", "impact-drill", "eruption-drill",
    };

    /** Générateurs avec bonus de puissance. */
    private static final String[] POWER_POOL = {
        "combustion-generator", "steam-generator", "solar-panel", "differential-generator",
        "thorium-reactor", "impact-reactor", "thermal-generator", "turbine-condenser",
        "slag-heater", "neoplasia-heater", "heat-turbine",
        "beam-node", "beam-tower",
    };

    private final Rand rand;

    public RewardGenerator(Rand rand){
        this.rand = rand;
    }

    /**
     * Génère 3 choix de récompenses pour un joueur.
     *
     * @param session la session serveur (pour connaître les unlocks existants)
     * @param player  le joueur pour qui générer les choix
     * @param wave    le numéro de vague actuel (influence la puissance des récompenses)
     * @return une séquence de 3 récompenses
     */
    public Seq<Reward> generateChoices(RogueSession session, Player player, int wave){
        Seq<Reward> choices = new Seq<>(3);

        // Pondérations: plus la vague est haute, plus on favorise les bonus puissants
        float buildingChance = 0.25f;
        float buildingBonusChance = 0.20f;
        float unitBonusChance = 0.20f;
        float globalModChance = 0.20f;
        float schematicChance = 0.10f;
        float synergyChance = 0.05f;

        // Ajustement selon la vague
        if(wave > 10){
            unitBonusChance += 0.05f;
            buildingBonusChance += 0.05f;
            globalModChance += 0.05f;
            buildingChance -= 0.10f;
            schematicChance -= 0.05f;
        }

        for(int i = 0; i < 3; i++){
            RewardType type = pickType(buildingChance, buildingBonusChance, unitBonusChance,
                                       globalModChance, schematicChance, synergyChance);
            Reward reward = generateReward(session, player, type, wave);
            if(reward != null){
                choices.add(reward);
            }else{
                // Fallback: un modificateur global aléatoire
                choices.add(generateGlobalMod(wave));
            }
        }

        return choices;
    }

    private RewardType pickType(float building, float buildingBonus, float unitBonus,
                                float globalMod, float schematic, float synergy){
        float total = building + buildingBonus + unitBonus + globalMod + schematic + synergy;
        float roll = rand.random(total);

        if(roll < building) return RewardType.BUILDING;
        roll -= building;
        if(roll < buildingBonus) return RewardType.BUILDING_BONUS;
        roll -= buildingBonus;
        if(roll < unitBonus) return RewardType.UNIT_BONUS;
        roll -= unitBonus;
        if(roll < globalMod) return RewardType.GLOBAL_MOD;
        roll -= globalMod;
        if(roll < schematic) return RewardType.SCHEMATIC;
        return RewardType.SYNERGY;
    }

    private Reward generateReward(RogueSession session, Player player, RewardType type, int wave){
        return switch(type){
            case BUILDING -> generateBuilding(session, wave);
            case BUILDING_BONUS -> generateBuildingBonus(session, wave);
            case UNIT_BONUS -> generateUnitBonus(wave);
            case GLOBAL_MOD -> generateGlobalMod(wave);
            case SCHEMATIC -> generateSchematic(wave);
            case SYNERGY -> generateSynergy(session, wave);
        };
    }

    /** Génère une récompense de bâtiment (débloqué pour tout le groupe). */
    private BuildingReward generateBuilding(RogueSession session, int wave){
        // Filtrer les bâtiments déjà débloqués
        Seq<String> available = new Seq<>();
        for(String name : BUILDING_POOL){
            Block block = mindustry.Vars.content.block(name);
            if(block != null && !session.isUnlockedForGroup(block)){
                available.add(name);
            }
        }

        if(available.isEmpty()){
            // Tous les bâtiments sont débloqués, proposer un bonus à la place
            return null;
        }

        String blockName = available.random(rand);
        Block block = mindustry.Vars.content.block(blockName);
        String displayName = block.localizedName != null ? block.localizedName : blockName;

        return new BuildingReward(
            blockName,
            displayName,
            "Débloque [accent]" + displayName + "[] pour [green]tous les joueurs"
        );
    }

    /** Génère un bonus de bâtiment (appliqué au joueur cible choisi au moment du vote). */
    private BuildingBonusReward generateBuildingBonus(RogueSession session, int wave){
        // Choisir le type de bonus et le bâtiment concerné
        BuildingBonus.BonusType bonusType;
        String blockName;
        String blockDisplay;

        // 50% tourelles (dégâts/reload/portée), 30% usines (production), 20% générateurs (power)
        float roll = rand.random(1f);
        if(roll < 0.5f){
            // Tourelle: DAMAGE, RELOAD ou RANGE
            BuildingBonus.BonusType[] turretTypes = {
                BuildingBonus.BonusType.DAMAGE,
                BuildingBonus.BonusType.RELOAD,
                BuildingBonus.BonusType.RANGE
            };
            bonusType = turretTypes[rand.random(turretTypes.length - 1)];
            blockName = TURRET_POOL[rand.random(TURRET_POOL.length - 1)];
        }else if(roll < 0.8f){
            // Usine: PRODUCTION
            bonusType = BuildingBonus.BonusType.PRODUCTION;
            blockName = FACTORY_POOL[rand.random(FACTORY_POOL.length - 1)];
        }else{
            // Générateur: POWER
            bonusType = BuildingBonus.BonusType.POWER;
            blockName = POWER_POOL[rand.random(POWER_POOL.length - 1)];
        }

        Block block = mindustry.Vars.content.block(blockName);
        blockDisplay = block != null && block.localizedName != null ? block.localizedName : blockName;

        // La valeur augmente avec la vague
        float baseValue = switch(bonusType){
            case DAMAGE -> 0.10f;
            case RELOAD -> 0.10f;
            case RANGE -> 0.10f;
            case PRODUCTION -> 0.15f;
            case POWER -> 0.15f;
            case EFFICIENCY -> 0.10f;
        };
        float waveBonus = wave * 0.01f; // +1% par vague
        float value = baseValue + rand.random(baseValue * 0.5f) + waveBonus;

        String bonusDesc = switch(bonusType){
            case DAMAGE -> "[red]+" + (int)(value * 100) + "% dégâts";
            case RELOAD -> "[cyan]+" + (int)(value * 100) + "% vitesse de tir";
            case RANGE -> "[yellow]+" + (int)(value * 100) + "% portée";
            case PRODUCTION -> "[green]+" + (int)(value * 100) + "% production";
            case POWER -> "[orange]+" + (int)(value * 100) + "% énergie";
            case EFFICIENCY -> "[blue]+" + (int)(value * 100) + "% efficacité";
        };

        return new BuildingBonusReward(
            bonusType,
            value,
            blockName,
            bonusDesc + " [white]" + blockDisplay,
            bonusDesc + " sur [accent]" + blockDisplay + "[] d'un joueur [accent]au choix[] (soi ou allié)"
        );
    }
    private UnitBonusReward generateUnitBonus(int wave){
        UnitBonusReward.BonusType[] types = UnitBonusReward.BonusType.values();
        UnitBonusReward.BonusType type = types[rand.random(types.length - 1)];

        // La valeur augmente avec la vague
        float baseValue = switch(type){
            case DAMAGE -> 0.10f;
            case HEALTH -> 0.15f;
            case SPEED -> 0.10f;
            case RELOAD -> 0.10f;
            case ARMOR -> 1f;
        };
        float waveBonus = wave * 0.01f; // +1% par vague
        float value = baseValue + rand.random(baseValue * 0.5f) + waveBonus;

        String bonusDesc = switch(type){
            case DAMAGE -> "[red]+" + (int)(value * 100) + "% dégâts";
            case HEALTH -> "[green]+" + (int)(value * 100) + "% vie max";
            case SPEED -> "[yellow]+" + (int)(value * 100) + "% vitesse";
            case RELOAD -> "[cyan]+" + (int)(value * 100) + "% vitesse de tir";
            case ARMOR -> "[orange]+" + (int)value + " armure";
        };

        return new UnitBonusReward(
            type,
            value,
            bonusDesc + " [white]unités",
            bonusDesc + " sur les unités d'un joueur [accent]au choix[] (soi ou allié)"
        );
    }

    /** Génère un modificateur global (pour tous les joueurs). */
    private GlobalModifierReward generateGlobalMod(int wave){
        GlobalModifier mod = GlobalModifierRegistry.random(rand);
        // Augmenter la valeur avec la vague
        float scaledValue = mod.value * (1f + wave * 0.03f);
        GlobalModifier scaled = new GlobalModifier(mod.id, scaledValue, mod.displayName);

        String sign = scaledValue >= 0 ? "+" : "";
        return new GlobalModifierReward(
            scaled.id,
            scaledValue,
            "[blue]" + scaled.displayName + " " + sign + (int)(scaledValue * 100) + "%",
            "[blue]" + scaled.displayName + " " + sign + (int)(scaledValue * 100) + "% pour [green]tous les joueurs"
        );
    }

    /** Génère un schematic (plan) débloqué pour un joueur individuel. */
    private SchematicReward generateSchematic(int wave){
        mindustry.game.Schematic schematic = rogueindustry.rewards.SchematicGenerator.generateRandom(rand);
        if(schematic == null){
            // Fallback si la génération échoue
            String name = "Plan Rogue #" + (wave + 1);
            return new SchematicReward(
                "",
                name,
                "[purple]Plan: " + name,
                "Débloque le plan [accent]" + name + "[] pour [yellow]vous seul[] - les autres joueurs ne l'auront pas"
            );
        }

        String base64 = rogueindustry.rewards.SchematicGenerator.toBase64(schematic);
        return new SchematicReward(
            base64,
            schematic.name(),
            "[purple]Plan: " + schematic.name(),
            "Débloque le plan [accent]" + schematic.name() + "[] pour [yellow]vous seul[] - les autres joueurs ne l'auront pas.\n[gray]" + schematic.description()
        );
    }

    /** Génère une synergie (bonus conditionnel). */
    private SynergyReward generateSynergy(RogueSession session, int wave){
        // Choisir un prérequis parmi les bâtiments déjà débloqués
        Seq<String> unlocked = new Seq<>();
        for(var content : session.getGroupUnlocks()){
            if(content instanceof Block){
                unlocked.add(content.name);
            }
        }

        String required = unlocked.isEmpty() ? "duo" : unlocked.random(rand);
        Block reqBlock = mindustry.Vars.content.block(required);
        String reqName = reqBlock != null ? reqBlock.localizedName : required;

        float bonusValue = 0.10f + wave * 0.01f;
        return new SynergyReward(
            required,
            "+" + (int)(bonusValue * 100) + "% dégâts " + reqName,
            "[orange]Synergie: " + reqName,
            "Si [accent]" + reqName + "[] est débloqué, +" + (int)(bonusValue * 100) + "% dégâts pour " + reqName + " (tous joueurs)"
        );
    }
}
