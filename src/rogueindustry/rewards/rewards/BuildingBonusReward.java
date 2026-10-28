package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;

/**
 * Récompense: applique un bonus permanent sur un type de bâtiment pour un joueur CIBLE.
 * Le joueur cible est choisi au moment du vote (soi-même ou un allié).
 *
 * Exemples:
 * - "+15% dégâts Salvo" → cible: Alice → seules les Salvo d'Alice ont +15% dégâts
 * - "+20% vitesse Multi-Press" → cible: Bob → seules les Multi-Press de Bob produisent plus vite
 * - "+10% portée toutes tourelles" → cible: soi-même
 */
public class BuildingBonusReward extends Reward{

    public final BuildingBonus.BonusType bonusType;
    public final float value;

    /** Nom du bâtiment concerné (ex: "salvo"). null = tous les bâtiments d'un type. */
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
