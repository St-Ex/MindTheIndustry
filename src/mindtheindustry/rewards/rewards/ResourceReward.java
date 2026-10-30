package mindtheindustry.rewards.rewards;

import arc.struct.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Reward: a gift of resources delivered to the team cores.
 * One or more item types; each amount scales with the rarity (x1 / x2 / x4).
 */
public class ResourceReward extends Reward{

    /** Item internal names (parallel to {@link #amounts}). */
    public final Seq<String> itemNames;

    /** Amount per item (parallel to {@link #itemNames}). */
    public final Seq<Integer> amounts;

    public ResourceReward(Seq<String> itemNames, Seq<Integer> amounts, Rarity rarity,
                          String displayName, String description){
        super("resource:" + itemNames.toString(",") + ":" + amounts.toString(","),
              RewardType.RESOURCE, rarity, displayName, description);
        this.itemNames = itemNames;
        this.amounts = amounts;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        Team team = state.rules.defaultTeam;
        if(team == null) return;
        for(int i = 0; i < itemNames.size; i++){
            Item item = content.item(itemNames.get(i));
            if(item == null) continue;
            int amount = amounts.get(i);
            for(CoreBuild core : team.cores()){
                if(core.items != null){
                    core.items.add(item, amount);
                }
            }
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        RewardSerializer.writeSpecific(this, write);
    }

    public static ResourceReward readSpecific(String id, Rarity rarity, String name, String desc, Reads read){
        int n = read.i();
        Seq<String> items = new Seq<>(n);
        Seq<Integer> amounts = new Seq<>(n);
        for(int i = 0; i < n; i++){
            items.add(read.str());
            amounts.add(read.i());
        }
        return new ResourceReward(items, amounts, rarity, name, desc);
    }
}
