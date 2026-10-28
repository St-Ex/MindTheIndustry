package rogueindustry.rewards;

import arc.util.io.*;
import rogueindustry.rewards.rewards.*;

/**
 * Sérialise/désérialise les récompenses complètes sur le réseau.
 * Chaque type de récompense a des champs spécifiques qui sont écrits après les champs de base.
 */
public class RewardSerializer{

    /** Désérialise une récompense complète (champs de base déjà lus). */
    public static Reward readFull(String id, RewardType type, String name, String desc, Reads read){
        return switch(type){
            case BUILDING -> BuildingReward.readSpecific(id, name, desc, read);
            case BUILDING_BONUS -> BuildingBonusReward.readSpecific(id, name, desc, read);
            case SCHEMATIC -> SchematicReward.readSpecific(id, name, desc, read);
            case UNIT_BONUS -> UnitBonusReward.readSpecific(id, name, desc, read);
            case GLOBAL_MOD -> GlobalModifierReward.readSpecific(id, name, desc, read);
            case SYNERGY -> SynergyReward.readSpecific(id, name, desc, read);
        };
    }

    /** Écrit les champs spécifiques d'une récompense (après les champs de base). */
    public static void writeSpecific(Reward reward, Writes write){
        if(reward instanceof BuildingReward r){
            write.str(r.blockName);
        }else if(reward instanceof BuildingBonusReward r){
            write.str(r.bonusType.name());
            write.f(r.value);
            write.str(r.blockName == null ? "" : r.blockName);
        }else if(reward instanceof SchematicReward r){
            write.str(r.schematicData);
            write.str(r.schematicName);
        }else if(reward instanceof UnitBonusReward r){
            write.str(r.bonusType.name());
            write.f(r.value);
        }else if(reward instanceof GlobalModifierReward r){
            write.str(r.modifierId);
            write.f(r.value);
        }else if(reward instanceof SynergyReward r){
            write.str(r.requiredUnlockId);
            write.str(r.bonusDescription);
        }
    }
}
