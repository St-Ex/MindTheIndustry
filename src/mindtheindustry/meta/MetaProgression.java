package mindtheindustry.meta;

import arc.*;
import arc.struct.*;
import arc.util.*;

/**
 * Manages meta-progression between runs.
 * Stores which commanders are unlocked (persisted locally via Preferences).
 *
 * Tier 1 commanders are always unlocked.
 * The others unlock by reaching certain waves or by winning runs.
 */
public class MetaProgression{

    private static final String PREF_KEY = "mindtheindustry-unlocked-commanders";

    /** IDs of unlocked commanders (excluding tier 1 which are always available). */
    private static final ObjectSet<String> unlocked = new ObjectSet<>();

    /** Loads unlocked commanders from preferences. */
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
        Log.info("[MindTheIndustry] Meta-progression loaded: " + unlocked.size + " commanders unlocked.");
    }

    /** Saves unlocked commanders. */
    public static void save(){
        StringBuilder sb = new StringBuilder();
        for(String id : unlocked){
            if(sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        Core.settings.put(PREF_KEY, sb.toString());
        Core.settings.saveValues();
    }

    /** Checks whether a commander is unlocked (tier 1 = always unlocked). */
    public static boolean isUnlocked(Commander commander){
        if(commander.tier <= 1) return true;
        return unlocked.contains(commander.id);
    }

    /** Unlocks a commander. */
    public static void unlock(Commander commander){
        if(commander.tier <= 1) return; // already unlocked
        if(unlocked.add(commander.id)){
            save();
            Log.info("[MindTheIndustry] Commander unlocked: " + commander.displayName);
        }
    }

    /** Returns all unlocked commanders. */
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
     * Called at the end of a run to unlock commanders based on performance.
     *
     * @param maxWave the max wave reached in the run
     * @param won     the run was won (wave 30 reached)
     */
    public static void onRunComplete(int maxWave, boolean won){
        // Tier 2: wave 10
        if(maxWave >= 10){
            for(Commander c : CommanderRegistry.byTier(2)){
                unlock(c);
            }
        }
        // Tier 3: wave 20
        if(maxWave >= 20){
            for(Commander c : CommanderRegistry.byTier(3)){
                unlock(c);
            }
        }
        // Tier 4: victory (wave 30+)
        if(won || maxWave >= 30){
            for(Commander c : CommanderRegistry.byTier(4)){
                unlock(c);
            }
        }
    }

    /** Returns the total number of unlocked commanders (for the UI). */
    public static int unlockedCount(){
        return getUnlocked().size;
    }

    /** Returns the total number of commanders. */
    public static int totalCount(){
        return CommanderRegistry.all().size;
    }
}
