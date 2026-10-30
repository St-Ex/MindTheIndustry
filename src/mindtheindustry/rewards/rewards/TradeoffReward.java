package mindtheindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;

/**
 * Reward: a trade-off between two stats for the WHOLE group.
 * Always +X% on one stat and -X% on another (X = 10% x rarity multiplier).
 * Applied through the session's global modifiers.
 */
public class TradeoffReward extends Reward{

    /** Stat id receiving the bonus (e.g. "unit_damage"). */
    public final String bonusId;

    /** Bonus value, always positive (e.g. 0.10 for +10%). */
    public final float bonusValue;

    /** Stat id receiving the malus (e.g. "build_speed"). */
    public final String malusId;

    /** Malus value, always negative (e.g. -0.10 for -10%). */
    public final float malusValue;

    public TradeoffReward(String bonusId, float bonusValue, String malusId, float malusValue,
                          Rarity rarity, String displayName, String description){
        super("tradeoff:" + bonusId + ":" + bonusValue + ":" + malusId + ":" + malusValue,
              RewardType.TRADEOFF, rarity, displayName, description);
        this.bonusId = bonusId;
        this.bonusValue = bonusValue;
        this.malusId = malusId;
        this.malusValue = malusValue;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        session.addGlobalModifier(new GlobalModifier(bonusId, bonusValue, GlobalModifierRegistry.nameOf(bonusId)));
        session.addGlobalModifier(new GlobalModifier(malusId, malusValue, GlobalModifierRegistry.nameOf(malusId)));
    }

    @Override
    public void write(Writes write){
        super.write(write);
        RewardSerializer.writeSpecific(this, write);
    }

    public static TradeoffReward readSpecific(String id, Rarity rarity, String name, String desc, Reads read){
        String bonusId = read.str();
        float bonusValue = read.f();
        String malusId = read.str();
        float malusValue = read.f();
        return new TradeoffReward(bonusId, bonusValue, malusId, malusValue, rarity, name, desc);
    }
}
