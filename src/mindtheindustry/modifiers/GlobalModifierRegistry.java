package mindtheindustry.modifiers;

import arc.math.*;
import arc.struct.*;
import arc.util.*;

/**
 * Registry of available global modifiers.
 * Contains the definitions of all possible GlobalModifiers.
 */
public class GlobalModifierRegistry{

    private static final ObjectMap<String, GlobalModifierDef> defs = new ObjectMap<>();

    static{
        register("prod_speed", "Production speed", 0.10f);
        register("build_speed", "Build speed", 0.15f);
        register("turret_damage", "Turret damage", 0.10f);
        register("turret_reload", "Turret fire rate", 0.10f);
        register("unit_damage", "Unit damage (all players)", 0.10f);
        register("unit_health", "Unit health (all players)", 0.15f);
        register("core_health", "Core health", 0.20f);
        register("resource_yield", "Drill yield", 0.15f);
    }

    private static void register(String id, String name, float baseValue){
        defs.put(id, new GlobalModifierDef(id, name, baseValue));
    }

    /** Creates a GlobalModifier from its ID. Returns null if unknown. */
    public static GlobalModifier create(String id, float multiplier){
        GlobalModifierDef def = defs.get(id);
        if(def == null){
            Log.warn("[MindTheIndustry] Unknown GlobalModifier: " + id);
            return null;
        }
        return new GlobalModifier(id, def.baseValue * multiplier, def.name);
    }

    /** Returns all available modifier IDs. */
    public static Seq<String> allIds(){
        return defs.keys().toSeq();
    }

    /** Returns a random weighted modifier. */
    public static GlobalModifier random(Rand rand){
        String id = defs.keys().toSeq().random(rand);
        GlobalModifierDef def = defs.get(id);
        // Value between 1x and 2x the base depending on chance
        float mult = rand.random(1f, 2f);
        return new GlobalModifier(id, def.baseValue * mult, def.name);
    }

    private static class GlobalModifierDef{
        final String id;
        final String name;
        final float baseValue;

        GlobalModifierDef(String id, String name, float baseValue){
            this.id = id;
            this.name = name;
            this.baseValue = baseValue;
        }
    }
}
