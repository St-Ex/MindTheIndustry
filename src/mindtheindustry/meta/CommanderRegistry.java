package mindtheindustry.meta;

import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.world.*;
import mindtheindustry.core.*;

/**
 * Registry of all available commanders.
 * Contains the definitions of each commander with their unique ability.
 *
 * Commanders are unlocked progressively through meta-progression.
 * At the start of each run, the player chooses a commander among the unlocked ones.
 */
public class CommanderRegistry{

    private static final Seq<Commander> all = new Seq<>();

    static{
        // ===== TIER 1: Starter commanders (unlocked by default) =====

        register(new Commander(
            "commander-vanguard",
            "[red]Vanguard",
            "A veteran of the Serpulo wars. Their units hit harder from the start.",
            Color.valueOf("ff6b6b"),
            new CommanderAbility(){
                public String name(){ return "Vanguard Strike"; }
                public String description(){ return "+10% damage for all units (all players)"; }
                public void apply(MindTheIndustrySession session){
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "unit_damage", 0.10f, "Unit damage (Vanguard)"));
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.10f;
                }
            },
            "Unlocked by default",
            1
        ));

        register(new Commander(
            "commander-architect",
            "[green]Architect",
            "Master builder. Their constructions are faster and sturdier.",
            Color.valueOf("26de81"),
            new CommanderAbility(){
                public String name(){ return "Civil Engineering"; }
                public String description(){ return "+20% build speed and +15% building health (all players)"; }
                public void apply(MindTheIndustrySession session){
                    mindustry.Vars.state.rules.buildSpeedMultiplier *= 1.20f;
                    // +15% building health will be handled via a hook on BlockBuildEndEvent (Phase 3)
                }
            },
            "Unlocked by default",
            1
        ));

        register(new Commander(
            "commander-industrialist",
            "[yellow]Industrialist",
            "Industry magnate. Their factories produce faster.",
            Color.valueOf("f7b731"),
            new CommanderAbility(){
                public String name(){ return "Optimized Chain"; }
                public String description(){ return "+15% production speed for all players"; }
                public void apply(MindTheIndustrySession session){
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.15f, "Production speed (Industrialist)"));
                }
            },
            "Unlocked by default",
            1
        ));

        // ===== TIER 2: Unlocked by reaching wave 10 =====

        register(new Commander(
            "commander-overmind",
            "[purple]Overmind AI",
            "A strategic artificial intelligence. Unlocks advanced options from the start.",
            Color.valueOf("a55eea"),
            new CommanderAbility(){
                public String name(){ return "Advanced Protocol"; }
                public String description(){ return "Starts with [accent]Salvo[] and [accent]Multi-Press[] unlocked for everyone"; }
                public void apply(MindTheIndustrySession session){
                    Block salvo = mindustry.Vars.content.block("salvo");
                    Block multiPress = mindustry.Vars.content.block("multi-press");
                    if(salvo != null) session.unlockForGroup(salvo);
                    if(multiPress != null) session.unlockForGroup(multiPress);
                }
            },
            "Reach wave 10",
            2
        ));

        register(new Commander(
            "commander-fortress",
            "[blue]Fortress",
            "Defensive specialist. Their defenses are overpowered.",
            Color.valueOf("4a90d9"),
            new CommanderAbility(){
                public String name(){ return "Impenetrable Wall"; }
                public String description(){ return "+25% damage and +25% health for all turrets (all players)"; }
                public void apply(MindTheIndustrySession session){
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "turret_damage", 0.25f, "Turret damage (Fortress)"));
                }
            },
            "Reach wave 10",
            2
        ));

        register(new Commander(
            "commander-merchant",
            "[orange]Merchant",
            "Interplanetary businessman. Resources arrive faster.",
            Color.valueOf("ff9f43"),
            new CommanderAbility(){
                public String name(){ return "Express Logistics"; }
                public String description(){ return "+20% drill yield and +100 bonus resources at start"; }
                public void apply(MindTheIndustrySession session){
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "resource_yield", 0.20f, "Drill yield (Merchant)"));
                    // The 100 bonus resources will be handled at run start (Phase 3)
                }
            },
            "Reach wave 10",
            2
        ));

        // ===== TIER 3: Unlocked by reaching wave 20 =====

        register(new Commander(
            "commander-warlord",
            "[scarlet]Warlord",
            "Legendary general. Their armies are fearsome.",
            Color.valueOf("c0392b"),
            new CommanderAbility(){
                public String name(){ return "Iron Army"; }
                public String description(){ return "+20% health, +15% damage, +10% speed for all units (all players)"; }
                public void apply(MindTheIndustrySession session){
                    mindustry.Vars.state.rules.unitHealthMultiplier *= 1.20f;
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.15f;
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "unit_damage", 0.15f, "Unit damage (Warlord)"));
                }
            },
            "Reach wave 20",
            3
        ));

        register(new Commander(
            "commander-overclocker",
            "[cyan]Overclocker",
            "Mad engineer. Their machines exceed their limits.",
            Color.valueOf("00d2d3"),
            new CommanderAbility(){
                public String name(){ return "Overclocking"; }
                public String description(){ return "+30% turret fire rate, +20% production speed (all players)"; }
                public void apply(MindTheIndustrySession session){
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "turret_reload", 0.30f, "Fire rate (Overclocker)"));
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.20f, "Production speed (Overclocker)"));
                }
            },
            "Reach wave 20",
            3
        ));

        // ===== TIER 4: Unlocked by winning a run (wave 30+) =====

        register(new Commander(
            "commander-immortal",
            "[gold]Immortal",
            "A being from another era. Their power defies understanding.",
            Color.valueOf("f9ca24"),
            new CommanderAbility(){
                public String name(){ return "Immortality"; }
                public String description(){ return "+50% to ALL stats (damage, health, speed, production, construction)"; }
                public void apply(MindTheIndustrySession session){
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.50f;
                    mindustry.Vars.state.rules.unitHealthMultiplier *= 1.50f;
                    mindustry.Vars.state.rules.buildSpeedMultiplier *= 1.50f;
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.50f, "Production speed (Immortal)"));
                    session.getGlobalModifiers().add(new mindtheindustry.modifiers.GlobalModifier(
                        "turret_damage", 0.50f, "Turret damage (Immortal)"));
                }
            },
            "Win a run (wave 30+)",
            4
        ));
    }

    private static void register(Commander commander){
        all.add(commander);
    }

    /** Returns all commanders. */
    public static Seq<Commander> all(){
        return all;
    }

    /** Returns a commander by ID. */
    public static Commander get(String id){
        return all.find(c -> c.id.equals(id));
    }

    /** Returns the commanders of a specific tier. */
    public static Seq<Commander> byTier(int tier){
        return all.select(c -> c.tier == tier);
    }

    /** Returns the starter commanders (tier 1, always unlocked). */
    public static Seq<Commander> starters(){
        return byTier(1);
    }
}
