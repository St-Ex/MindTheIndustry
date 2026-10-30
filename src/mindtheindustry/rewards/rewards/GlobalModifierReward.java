package mindtheindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;

/**
 * Reward: applies a global modifier to the WHOLE group.
 * Examples: +10% production speed, +15% turret damage, etc.
 */
public class GlobalModifierReward extends Reward{

    /** Identifier of the modifier (e.g. "prod_speed", "turret_damage"). */
    public final String modifierId;

    /** Bonus value (e.g. 0.10 for +10%). */
    public final float value;

    public GlobalModifierReward(String modifierId, float value, String displayName, String description){
        super("global_mod:" + modifierId + ":" + value, RewardType.GLOBAL_MOD, displayName, description);
        this.modifierId = modifierId;
        this.value = value;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        GlobalModifier modifier = GlobalModifierRegistry.create(modifierId, value);
        if(modifier != null){
            session.addGlobalModifier(modifier);
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(modifierId);
        write.f(value);
    }

    public static GlobalModifierReward readSpecific(String id, String name, String desc, Reads read){
        String modId = read.str();
        float val = read.f();
        return new GlobalModifierReward(modId, val, name, desc);
    }
}
