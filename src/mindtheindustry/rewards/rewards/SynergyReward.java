package mindtheindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;

/**
 * Reward: conditional bonus for the whole group.
 * Activates only if a prerequisite is unlocked (e.g. "If Salvo unlocked, +15% Salvo damage").
 */
public class SynergyReward extends Reward{

    /** ID of the required content (e.g. "salvo"). If unlocked in the group, the bonus applies. */
    public final String requiredUnlockId;

    /** Description of the bonus (e.g. "+15% Salvo damage for all"). */
    public final String bonusDescription;

    public SynergyReward(String requiredUnlockId, String bonusDescription, String displayName, String description){
        super("synergy:" + requiredUnlockId, RewardType.SYNERGY, displayName, description);
        this.requiredUnlockId = requiredUnlockId;
        this.bonusDescription = bonusDescription;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
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
