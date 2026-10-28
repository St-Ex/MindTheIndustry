package rogueindustry.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;

/**
 * Abstract class representing a reward offered to a player.
 *
 * Each reward has:
 * - a type (determines the scope: group, individual, or targeted)
 * - a unique identifier (for network serialization)
 * - a name and a description displayed in the choice dialog
 * - an apply() method that applies the reward in the session
 */
public abstract class Reward{

    /** Unique identifier of this reward (e.g. "building:salvo", "unit_bonus:damage:20"). */
    public final String id;

    /** Reward type (determines the application scope). */
    public final RewardType type;

    /** Name displayed in the dialog (e.g. "Salvo", "+20% unit damage"). */
    public final String displayName;

    /** Detailed description (e.g. "Unlocks the Salvo turret for all players"). */
    public final String description;

    protected Reward(String id, RewardType type, String displayName, String description){
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Applies the reward in the session.
     *
     * @param session the current run session
     * @param chooser the player who chose this reward
     * @param target  the target player (for UNIT_BONUS; null for other types)
     */
    public abstract void apply(RogueSession session, Player chooser, Player target);

    /** Serializes the reward for network sending. */
    public void write(Writes write){
        write.str(id);
        write.str(type.name());
        write.str(displayName);
        write.str(description);
    }

    /** Deserializes the reward (factory called client-side). */
    public static Reward read(Reads read){
        String id = read.str();
        RewardType type = RewardType.valueOf(read.str());
        String name = read.str();
        String desc = read.str();
        // Full deserialization is done by RewardSerializer
        // which knows the type-specific fields
        return RewardSerializer.readFull(id, type, name, desc, read);
    }

    @Override
    public String toString(){
        return displayName + " [" + type + "]";
    }
}
