package mindtheindustry.core;

import arc.math.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static mindustry.Vars.*;

/**
 * Generates the 3 reward choices offered to a player after each wave:
 * always exactly one TRADEOFF, one PLAN and one RESOURCE (order shuffled).
 *
 * - TRADEOFF: +X% on one stat, -X% on another (two distinct stats)
 * - PLAN: unlocks 1 building/unit plan(s) for the whole group
 * - RESOURCE: a gift of resources delivered to the team cores
 *
 * Each choice rolls a rarity (NORMAL x1 / RARE x2 / EPIC x4, weighted) that
 * multiplies its magnitude: trade-off percentage, number of plans, resource
 * quantity.
 *
 * Generation is fully driven by the run seed (session.rand) → deterministic.
 */
public class RewardGenerator{

    // --- Tuning knobs ---

    /** Base trade-off magnitude at NORMAL rarity (+10% / -10%). Rarer = x2 / x4. */
    public static float tradeoffBase = 0.10f;

    /** Base resource amount per item type at NORMAL rarity. Rarer = x2 / x4. */
    public static int resourceBaseAmount = 100;

    /** Maximum number of distinct item types in a single resource gift. */
    public static int resourceMaxTypes = 3;

    /** Pool of offerable building plans. */
    private static final String[] BUILDING_POOL = {
        // Turrets
        "duo", "scatter", "scorch", "hail", "wave", "lancer", "arc", "parallax", "segment",
        "tsunami", "swarm", "salvo", "fuse", "ripple", "cyclone", "foreshadow", "spectre", "meltdown",
        // Erekir
        "breach", "sublimate", "disrupt", "afflict", "distraction", "aegis", "navanax",
        // Defenses
        "mender", "mend-projector", "overdrive-projector", "force-projector", "large-shield-projector",
        "repair-point", "repair-tower", "regen-projector", "shockwave-tower",
        // Production
        "graphite-press", "silicon-smelter", "coal-centrifuge", "multi-press",
        "pyratite-mixer", "blast-mixer", "spore-press",
        "cultivator", "oil-extractor", "vent-condenser", "atmospheric-concentrator",
        "phase-weaver", "surge-smelter", "cryofluid-mixer", "neoplasia-centrifuge",
        "silicon-arc-furnace", "slag-incinerator", "phase-heater", "heat-turbine", "thermal-generator",
        "carbide-crucible", "slag-heater", "neoplasia-heater",
        // Drilling
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
        // Storage
        "vault", "container", "unloader",
        // Walls
        "titanium-wall", "thorium-wall", "phase-wall", "surge-wall",
        "titanium-wall-large", "thorium-wall-large", "phase-wall-large", "surge-wall-large",
        "beryllium-wall", "beryllium-wall-large", "tungsten-wall", "tungsten-wall-large",
        "shielded-wall", "carbide-wall", "carbide-wall-large",
        // Unit factories
        "ground-factory", "air-factory", "naval-factory",
        "additive-reconstructor", "multiplicative-reconstructor",
        "exponential-reconstructor", "tetrative-reconstructor",
        "tank-fabricator", "tank-refabricator", "tank-assembler",
        "ship-fabricator", "ship-refabricator", "ship-assembler",
        "mech-fabricator", "mech-refabricator", "mech-assembler",
        "prime-refabricator", "basic-assembler-module",
        // Liquid/Payload
        "mechanical-pump", "rotary-pump", "impulse-pump", "pulsar-pump",
        "reinforced-pump",
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
        "pump",
        "large-liquid-container",
        "illuminator",
        "world-cell",
        "ancim-membrane-concentrator",
        "beam-link",
        "heavy-power-node", "large-battery",
        "large-mend-projector",
    };

    /** Pool of offerable unit plans. */
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
        "alpha", "beta", "gamma",
    };

    private final Rand rand;

    public RewardGenerator(Rand rand){
        this.rand = rand;
    }

    /**
     * Generates 3 reward choices for a player.
     *
     * @param session the server session (to know existing unlocks)
     * @param player  the player to generate choices for (unused: choices are group-wide)
     * @param wave    the current wave number
     * @return a sequence of 3 rewards
     */
    public Seq<Reward> generateChoices(MindTheIndustrySession session, Player player, int wave){
        Seq<Reward> choices = new Seq<>(3);
        // content already offered in this round must not appear twice
        ObjectSet<String> offered = new ObjectSet<>();

        // Always exactly one of each category; rarity rolled per choice.
        // If the plan pool is exhausted, fall back to a second resource gift.
        Reward plan = generatePlan(session, Rarity.roll(rand), offered);
        choices.add(generateTradeoff(Rarity.roll(rand)));
        choices.add(plan != null ? plan : generateResource(Rarity.roll(rand)));
        choices.add(generateResource(Rarity.roll(rand)));

        // Shuffle so the category order varies per wave (seeded → deterministic)
        for(int i = choices.size - 1; i > 0; i--){
            int j = rand.random(i);
            choices.swap(i, j);
        }
        return choices;
    }

    /** Trade-off: +X% on one stat, -X% on another (X = tradeoffBase x rarity). */
    private TradeoffReward generateTradeoff(Rarity rarity){
        float magnitude = tradeoffBase * rarity.multiplier;
        Seq<String> ids = GlobalModifierRegistry.allIds();

        String bonusId = ids.random(rand);
        String malusId = ids.random(rand);
        while(malusId.equals(bonusId) && ids.size > 1){
            malusId = ids.random(rand);
        }

        String bonusName = GlobalModifierRegistry.nameOf(bonusId);
        String malusName = GlobalModifierRegistry.nameOf(malusId);
        int pct = (int)(magnitude * 100);
        String color = rarity.color;

        String displayName = color + "+" + pct + "% " + bonusName + " [gray]/ [red]-" + pct + "% " + malusName;
        String description = color + "Trade-off: [green]+" + pct + "% " + bonusName +
            "[] but [red]-" + pct + "% " + malusName + "[] (all players)";

        return new TradeoffReward(bonusId, magnitude, malusId, -magnitude, rarity, displayName, description);
    }

    /** Plan: unlocks rarity.multiplier (1/2/4) distinct not-yet-unlocked plans for the group. */
    private PlanReward generatePlan(MindTheIndustrySession session, Rarity rarity, ObjectSet<String> offered){
        Seq<String> available = new Seq<>();
        for(String name : BUILDING_POOL){
            Block block = content.block(name);
            if(block != null && !session.isUnlockedForGroup(block) && !offered.contains("block:" + name)){
                available.add("block:" + name);
            }
        }
        for(String name : UNIT_POOL){
            UnitType unit = content.unit(name);
            if(unit != null && !session.isUnlockedForGroup(unit) && !offered.contains("unit:" + name)){
                available.add("unit:" + name);
            }
        }

        if(available.isEmpty()) return null;

        int count = Math.min(rarity.multiplier, available.size);
        Seq<String> picked = new Seq<>(count);
        while(picked.size < count){
            String candidate = available.random(rand);
            if(!picked.contains(candidate)){
                picked.add(candidate);
            }
        }
        for(String name : picked){
            offered.add(name);
        }

        StringBuilder names = new StringBuilder();
        for(int i = 0; i < picked.size; i++){
            UnlockableContent c = PlanReward.resolve(picked.get(i));
            if(i > 0) names.append(", ");
            names.append(c != null ? c.localizedName : picked.get(i));
        }

        String color = rarity.color;
        String displayName = color + "Plan: " + names;
        String description = color + "Unlocks [accent]" + names + "[] for [green]all players";

        return new PlanReward(picked, rarity, displayName, description);
    }

    /** Resource gift: 1-3 item types (count grows with rarity), amount = base x rarity. */
    private ResourceReward generateResource(Rarity rarity){
        int types = Math.min(resourceMaxTypes, 1 + rarity.ordinal());
        Seq<Item> pool = content.items();

        Seq<String> itemNames = new Seq<>(types);
        Seq<Integer> amounts = new Seq<>(types);
        int amount = resourceBaseAmount * rarity.multiplier;

        while(itemNames.size < types){
            Item item = pool.random(rand);
            if(item == null || itemNames.contains(item.name)) continue;
            itemNames.add(item.name);
            amounts.add(amount);
        }

        StringBuilder names = new StringBuilder();
        for(int i = 0; i < itemNames.size; i++){
            Item item = content.item(itemNames.get(i));
            if(i > 0) names.append(", ");
            names.append(amounts.get(i)).append(" ").append(item != null ? item.localizedName : itemNames.get(i));
        }

        String color = rarity.color;
        String displayName = color + "Gift: " + names;
        String description = color + "Resource gift: [white]" + names + "[] delivered to the team cores";

        return new ResourceReward(itemNames, amounts, rarity, displayName, description);
    }
}
