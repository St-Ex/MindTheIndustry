package mindtheindustry.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;

/**
 * Abstract class representing a reward offered to a player after a wave.
 *
 * Each reward has:
 * - a type (TRADEOFF, PLAN or RESOURCE)
 * - a rarity (NORMAL/RARE/EPIC) that scales its magnitude
 * - a unique identifier (for network serialization)
 * - a name and a description displayed in the chat choice announcement
 * - an apply() method that applies the reward in the session
 */
public abstract class Reward{

    /** Unique identifier of this reward (e.g. "tradeoff:unit_damage:0.2:-build_speed", "plan:salvo", "resource:copper:400"). */
    public final String id;

    /** Reward category. */
    public final RewardType type;

    /** Rarity (scales the magnitude; also drives the display color). */
    public final Rarity rarity;

    /** Name displayed in the chat announcement (rarity color baked in). */
    public final String displayName;

    /** Detailed description. */
    public final String description;

    protected Reward(String id, RewardType type, Rarity rarity, String displayName, String description){
        this.id = id;
        this.type = type;
        this.rarity = rarity;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Applies the reward in the session.
     *
     * @param session the current run session
     * @param chooser the player who chose this reward
     * @param target  unused by the current reward categories (kept for API stability)
     */
    public abstract void apply(MindTheIndustrySession session, Player chooser, Player target);

    /**
     * Transfers this reward's effects from one player to another (boss-window
     * trade of an already-taken reward). Default: re-apply to the recipient
     * (correct for group-wide effects like plans and resources).
     */
    public void transfer(MindTheIndustrySession session, Player from, Player to){
        apply(session, to, to);
    }

    /** Serializes the reward for network sending. */
    public void write(Writes write){
        write.str(id);
        write.str(type.name());
        write.str(rarity.name());
        write.str(displayName);
        write.str(description);
    }

    /** Deserializes the reward (factory called client-side). */
    public static Reward read(Reads read){
        String id = read.str();
        RewardType type = RewardType.valueOf(read.str());
        Rarity rarity = Rarity.valueOf(read.str());
        String name = read.str();
        String desc = read.str();
        // Full deserialization is done by RewardSerializer
        // which knows the type-specific fields
        return RewardSerializer.readFull(id, type, rarity, name, desc, read);
    }

    @Override
    public String toString(){
        return displayName + " [" + type + " " + rarity + "]";
    }
}
