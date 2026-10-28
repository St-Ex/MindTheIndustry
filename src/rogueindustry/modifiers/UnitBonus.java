package rogueindustry.modifiers;

import rogueindustry.rewards.rewards.*;

/**
 * Permanent bonus applied to the units of a specific player.
 * Stored in RogueSession and applied to each new unit of the player.
 */
public class UnitBonus{

    public final UnitBonusReward.BonusType type;
    public final float value;

    public UnitBonus(UnitBonusReward.BonusType type, float value){
        this.type = type;
        this.value = value;
    }

    public String describe(){
        String sign = value >= 0 ? "+" : "";
        return switch(type){
            case DAMAGE -> sign + (int)(value * 100) + "% damage";
            case HEALTH -> sign + (int)(value * 100) + "% health";
            case SPEED -> sign + (int)(value * 100) + "% speed";
            case RELOAD -> sign + (int)(value * 100) + "% fire rate";
            case ARMOR -> sign + (int)value + " armor";
        };
    }
}
