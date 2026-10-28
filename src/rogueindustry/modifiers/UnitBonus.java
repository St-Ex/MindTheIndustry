package rogueindustry.modifiers;

import rogueindustry.rewards.rewards.*;

/**
 * Bonus permanent appliqué aux unités d'un joueur spécifique.
 * Stocké dans RogueSession et appliqué à chaque nouvelle unité du joueur.
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
            case DAMAGE -> sign + (int)(value * 100) + "% dégâts";
            case HEALTH -> sign + (int)(value * 100) + "% vie";
            case SPEED -> sign + (int)(value * 100) + "% vitesse";
            case RELOAD -> sign + (int)(value * 100) + "% vitesse de tir";
            case ARMOR -> sign + (int)value + " armure";
        };
    }
}
