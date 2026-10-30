package mindtheindustry.rewards;

import arc.util.io.*;
import mindtheindustry.rewards.rewards.*;

/**
 * Serializes/deserializes full rewards over the network.
 * Each reward type has specific fields that are written after the base fields
 * (id, type, rarity, displayName, description).
 */
public class RewardSerializer{

    /** Deserializes a full reward (base fields already read). */
    public static Reward readFull(String id, RewardType type, Rarity rarity, String name, String desc, Reads read){
        return switch(type){
            case TRADEOFF -> TradeoffReward.readSpecific(id, rarity, name, desc, read);
            case PLAN -> PlanReward.readSpecific(id, rarity, name, desc, read);
            case RESOURCE -> ResourceReward.readSpecific(id, rarity, name, desc, read);
        };
    }

    /** Writes the type-specific fields of a reward (after the base fields). */
    public static void writeSpecific(Reward reward, Writes write){
        if(reward instanceof TradeoffReward r){
            write.str(r.bonusId);
            write.f(r.bonusValue);
            write.str(r.malusId);
            write.f(r.malusValue);
        }else if(reward instanceof PlanReward r){
            write.i(r.contentNames.size);
            for(String name : r.contentNames){
                write.str(name);
            }
        }else if(reward instanceof ResourceReward r){
            write.i(r.itemNames.size);
            for(int i = 0; i < r.itemNames.size; i++){
                write.str(r.itemNames.get(i));
                write.i(r.amounts.get(i));
            }
        }
    }
}
