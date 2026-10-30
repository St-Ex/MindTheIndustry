package mindtheindustry.core;

import java.lang.reflect.*;
import mindustry.ctype.*;

/**
 * In-memory access to UnlockableContent's 'unlocked' flag, WITHOUT touching
 * Core.settings (unlike unlock()/quietUnlock()/clearUnlock(), which persist to
 * the global settings file and would leak between the MTI mode and the campaign).
 *
 * The build menu on the host reads this flag in campaign (unlockedNowHost());
 * clients read state.rules.researched instead, which is synced via Call.researched.
 */
public class UnlockFlags{

    private static final Field UNLOCKED;

    static{
        try{
            UNLOCKED = UnlockableContent.class.getDeclaredField("unlocked");
            UNLOCKED.setAccessible(true);
        }catch(NoSuchFieldException e){
            throw new RuntimeException("UnlockableContent.unlocked field not found", e);
        }
    }

    /** Sets the in-memory unlocked flag (no settings write). */
    public static void set(UnlockableContent content, boolean value){
        try{
            UNLOCKED.setBoolean(content, value);
        }catch(IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    /** Reads the raw in-memory unlocked flag (ignores alwaysUnlocked and net.client() logic). */
    public static boolean get(UnlockableContent content){
        try{
            return UNLOCKED.getBoolean(content);
        }catch(IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }
}
