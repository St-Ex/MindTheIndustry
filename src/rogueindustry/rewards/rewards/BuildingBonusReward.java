package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;

/**
 * Reward: applies a permanent bonus on a building type for a TARGET player.
 * The target player is chosen at vote time (yourself or an ally).
 *
 * Examples:
 * - "+15% Salvo damage" → target: Alice → only Alice's Salvos have +15% damage
 * - "+20% Multi-Press speed" → target: Bob → only Bob's Multi-Presses produce faster
 * - "+10% range all turrets" → target: yourself
 */
public class BuildingBonusReward extends Reward{

    public final BuildingBonus.BonusType bonusType;
    public final float value;

    /** Name of the building concerned (e.g. "salvo"). null = all buildings of a type. */
    public final String blockName;

    public BuildingBonusReward(BuildingBonus.BonusType bonusType, float value, String blockName,
                                String displayName, String description){
        super("building_bonus:" + bonusType.name().toLowerCase() + ":" + blockName + ":" + value,
              RewardType.BUILDING_BONUS, displayName, description);
        this.bonusType = bonusType;
        this.value = value;
        this.blockName = blockName;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        Player actualTarget = target != null ? target : chooser;
        BuildingBonus bonus = new BuildingBonus(bonusType, value, blockName);
        session.addBuildingBonus(actualTarget, bonus);
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(bonusType.name());
        write.f(value);
        write.str(blockName == null ? "" : blockName);
    }

    public static BuildingBonusReward readSpecific(String id, String name, String desc, Reads read){
        BuildingBonus.BonusType type = BuildingBonus.BonusType.valueOf(read.str());
        float val = read.f();
        String block = read.str();
        return new BuildingBonusReward(type, val, block.isEmpty() ? null : block, name, desc);
    }
}
