package rogueindustry.meta;

import arc.*;
import arc.struct.*;
import arc.util.*;

/**
 * Gère la meta-progression entre les runs.
 * Stocke quels commandants sont débloqués (persisté localement via Preferences).
 *
 * Les commandants de tier 1 sont toujours débloqués.
 * Les autres se débloquent en atteignant certaines vagues ou en gagnant des runs.
 */
public class MetaProgression{

    private static final String PREF_KEY = "rogue-industry-unlocked-commanders";

    /** IDs des commandants débloqués (hors tier 1 qui sont toujours disponibles). */
    private static final ObjectSet<String> unlocked = new ObjectSet<>();

    /** Charge les commandants débloqués depuis les préférences. */
    public static void load(){
        String saved = Core.settings.getString(PREF_KEY, "");
        unlocked.clear();
        if(!saved.isEmpty()){
            for(String id : saved.split(",")){
                if(!id.isEmpty()){
                    unlocked.add(id);
                }
            }
        }
        Log.info("[RogueIndustry] Meta-progression chargée: " + unlocked.size + " commandants débloqués.");
    }

    /** Sauvegarde les commandants débloqués. */
    public static void save(){
        StringBuilder sb = new StringBuilder();
        for(String id : unlocked){
            if(sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        Core.settings.put(PREF_KEY, sb.toString());
        Core.settings.saveValues();
    }

    /** Vérifie si un commandant est débloqué (tier 1 = toujours débloqué). */
    public static boolean isUnlocked(Commander commander){
        if(commander.tier <= 1) return true;
        return unlocked.contains(commander.id);
    }

    /** Débloque un commandant. */
    public static void unlock(Commander commander){
        if(commander.tier <= 1) return; // déjà débloqué
        if(unlocked.add(commander.id)){
            save();
            Log.info("[RogueIndustry] Commandant débloqué: " + commander.displayName);
        }
    }

    /** Retourne tous les commandants débloqués. */
    public static Seq<Commander> getUnlocked(){
        Seq<Commander> result = new Seq<>();
        for(Commander c : CommanderRegistry.all()){
            if(isUnlocked(c)){
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Appelé à la fin d'une run pour débloquer des commandants selon la performance.
     *
     * @param maxWave atteinte la vague max de la run
     * @param won     la run a été gagnée (vague 30 atteinte)
     */
    public static void onRunComplete(int maxWave, boolean won){
        // Tier 2: vague 10
        if(maxWave >= 10){
            for(Commander c : CommanderRegistry.byTier(2)){
                unlock(c);
            }
        }
        // Tier 3: vague 20
        if(maxWave >= 20){
            for(Commander c : CommanderRegistry.byTier(3)){
                unlock(c);
            }
        }
        // Tier 4: victoire (vague 30+)
        if(won || maxWave >= 30){
            for(Commander c : CommanderRegistry.byTier(4)){
                unlock(c);
            }
        }
    }

    /** Retourne le nombre total de commandants débloqués (pour l'UI). */
    public static int unlockedCount(){
        return getUnlocked().size;
    }

    /** Retourne le nombre total de commandants. */
    public static int totalCount(){
        return CommanderRegistry.all().size;
    }
}
