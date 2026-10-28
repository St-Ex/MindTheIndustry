package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;

/**
 * Récompense: applique un bonus permanent aux unités d'un joueur CIBLE.
 * Le joueur cible est choisi au moment du vote (soi-même ou un allié).
 *
 * Types de bonus:
 * - DAMAGE: +X% dégâts infligés par les unités du joueur
 * - HEALTH: +X% vie max des unités du joueur
 * - SPEED: +X% vitesse des unités du joueur
 * - RELOAD: +X% vitesse de tir des unités du joueur
 * - ARMOR: +X armure des unités du joueur
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
    public void apply(RogueSession session, Player chooser, Player target){
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
