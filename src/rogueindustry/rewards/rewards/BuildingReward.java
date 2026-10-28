package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

/**
 * Reward: unlocks a building for the WHOLE group.
 * The building is added to state.rules.researched, making it available
 * to all players in the game.
 */
public class BuildingReward extends Reward{

    /** Internal name of the block (e.g. "salvo", "laser-drill"). */
    public final String blockName;

    /** The unlocked block (resolved by name, since content is loaded at runtime). */
    public transient Block block;

    public BuildingReward(String blockName, String displayName, String description){
        super("building:" + blockName, RewardType.BUILDING, displayName, description);
        this.blockName = blockName;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        if(block == null){
            block = mindustry.Vars.content.block(blockName);
        }
        if(block != null){
            session.unlockForGroup(block);
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(blockName);
    }

    public static BuildingReward readSpecific(String id, String name, String desc, Reads read){
        String blockName = read.str();
        BuildingReward r = new BuildingReward(blockName, name, desc);
        r.block = mindustry.Vars.content.block(blockName);
        return r;
    }
}
