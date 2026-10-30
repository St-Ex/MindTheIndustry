package mindtheindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;

/**
 * Reward: applies a permanent bonus to a TARGET player's units.
 * The target player is chosen at vote time (yourself or an ally).
 *
 * Bonus types:
 * - DAMAGE: +X% damage dealt by the player's units
 * - HEALTH: +X% max health of the player's units
 * - SPEED: +X% speed of the player's units
 * - RELOAD: +X% fire rate of the player's units
 * - ARMOR: +X armor of the player's units
 */
public class UnitBonusReward extends Reward{

    public enum BonusType{
        DAMAGE, HEALTH, SPEED, RELOAD, ARMOR
    }

    public final BonusType bonusType;
    public final float value;

    public UnitBonusReward(BonusType bonusType, float value, String displayName, String description){
        super("unit_bonus:" + bonusType.name().toLowerCase() + ":" + value, RewardType.UNIT_BONUS, displayName, description);
        this.bonusType = bonusType;
        this.value = value;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        Player actualTarget = target != null ? target : chooser;
        UnitBonus bonus = new UnitBonus(bonusType, value);
        session.addUnitBonus(actualTarget, bonus);
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(bonusType.name());
        write.f(value);
    }

    public static UnitBonusReward readSpecific(String id, String name, String desc, Reads read){
        BonusType type = BonusType.valueOf(read.str());
        float val = read.f();
        return new UnitBonusReward(type, val, name, desc);
    }
}
