package mindtheindustry.rewards.rewards;

import arc.struct.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;

/**
 * Reward: unlocks building or unit "plans" for the WHOLE group.
 * The number of plans scales with the rarity (x1 / x2 / x4).
 *
 * Content names are stored with a prefix ("block:" or "unit:") so
 * serialization and application are unambiguous.
 */
public class PlanReward extends Reward{

    /** Content names to unlock, prefixed with "block:" or "unit:". */
    public final Seq<String> contentNames;

    public PlanReward(Seq<String> contentNames, Rarity rarity, String displayName, String description){
        super("plan:" + contentNames.toString(","), RewardType.PLAN, rarity, displayName, description);
        this.contentNames = contentNames;
    }

    /** Resolves a prefixed content name ("block:x" / "unit:x") to UnlockableContent. */
    public static UnlockableContent resolve(String prefixedName){
        if(prefixedName.startsWith("block:")){
            return mindustry.Vars.content.block(prefixedName.substring("block:".length()));
        }
        if(prefixedName.startsWith("unit:")){
            return mindustry.Vars.content.unit(prefixedName.substring("unit:".length()));
        }
        return null;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        for(String name : contentNames){
            UnlockableContent content = resolve(name);
            if(content != null){
                session.unlockForGroup(content);
            }
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        RewardSerializer.writeSpecific(this, write);
    }

    public static PlanReward readSpecific(String id, Rarity rarity, String name, String desc, Reads read){
        int n = read.i();
        Seq<String> names = new Seq<>(n);
        for(int i = 0; i < n; i++){
            names.add(read.str());
        }
        return new PlanReward(names, rarity, name, desc);
    }
}
