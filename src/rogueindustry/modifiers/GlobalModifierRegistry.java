package rogueindustry.modifiers;

import arc.math.*;
import arc.struct.*;
import arc.util.*;

/**
 * Registre des modificateurs globaux disponibles.
 * Contient les définitions de tous les GlobalModifier possibles.
 */
public class GlobalModifierRegistry{

    private static final ObjectMap<String, GlobalModifierDef> defs = new ObjectMap<>();

    static{
        register("prod_speed", "Vitesse de production", 0.10f);
        register("build_speed", "Vitesse de construction", 0.15f);
        register("turret_damage", "Dégâts des tourelles", 0.10f);
        register("turret_reload", "Vitesse de tir des tourelles", 0.10f);
        register("unit_damage", "Dégâts des unités (tous joueurs)", 0.10f);
        register("unit_health", "Vie des unités (tous joueurs)", 0.15f);
        register("core_health", "Vie des cores", 0.20f);
        register("resource_yield", "Rendement des foreuses", 0.15f);
    }

    private static void register(String id, String name, float baseValue){
        defs.put(id, new GlobalModifierDef(id, name, baseValue));
    }

    /** Crée un GlobalModifier à partir de son ID. Retourne null si inconnu. */
    public static GlobalModifier create(String id, float multiplier){
        GlobalModifierDef def = defs.get(id);
        if(def == null){
            Log.warn("[RogueIndustry] GlobalModifier inconnu: " + id);
            return null;
        }
        return new GlobalModifier(id, def.baseValue * multiplier, def.name);
    }

    /** Retourne tous les IDs de modificateurs disponibles. */
    public static Seq<String> allIds(){
        return defs.keys().toSeq();
    }

    /** Retourne un modificateur aléatoire pondéré. */
    public static GlobalModifier random(Rand rand){
        String id = defs.keys().toSeq().random(rand);
        GlobalModifierDef def = defs.get(id);
        // Valeur entre 1x et 2x la base selon le hasard
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
