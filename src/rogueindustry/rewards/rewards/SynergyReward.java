package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

/**
 * Récompense: bonus conditionnel pour tout le groupe.
 * S'active uniquement si un prérequis est débloqué (ex: "Si Salvo débloqué, +15% dégâts Salvo").
 */
public class SynergyReward extends Reward{

    /** ID du contenu requis (ex: "salvo"). Si débloqué dans le groupe, le bonus s'applique. */
    public final String requiredUnlockId;

    /** Description du bonus (ex: "+15% dégâts Salvo pour tous"). */
    public final String bonusDescription;

    public SynergyReward(String requiredUnlockId, String bonusDescription, String displayName, String description){
        super("synergy:" + requiredUnlockId, RewardType.SYNERGY, displayName, description);
        this.requiredUnlockId = requiredUnlockId;
        this.bonusDescription = bonusDescription;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        session.addSynergy(requiredUnlockId, bonusDescription);
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(requiredUnlockId);
        write.str(bonusDescription);
    }

    public static SynergyReward readSpecific(String id, String name, String desc, Reads read){
        String req = read.str();
        String bonus = read.str();
        return new SynergyReward(req, bonus, name, desc);
    }
}
