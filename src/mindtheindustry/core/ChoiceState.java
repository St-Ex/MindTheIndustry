package mindtheindustry.core;

import arc.struct.*;
import mindustry.gen.*;
import mindtheindustry.rewards.*;

/**
 * Per-player choice (ubiquitous language): the 3 proposals offered at wave end.
 *
 * At most 1 may be taken; taking none (explicit decision, replacement by the
 * next choice, disconnect, or boss window open) converts to the fallback grant.
 * A choice stays open through the inter-wave countdown AND the next wave's
 * combat — it is not a blocking phase and has no timer.
 */
public class ChoiceState{

    public final Player player;

    /** Wave number at whose end this choice was revealed. */
    public final int waveNumber;

    /** The 3 proposals. */
    public final Seq<Reward> proposals;

    /** The taken proposal, or null if none was taken. */
    public Reward taken = null;

    /** True once resolved (taken or converted). */
    public boolean resolved = false;

    public ChoiceState(Player player, int waveNumber, Seq<Reward> proposals){
        this.player = player;
        this.waveNumber = waveNumber;
        this.proposals = proposals;
    }

    /** Takes proposal {@code index} (0-based). @return the reward, or null if already resolved / invalid index (taking 2 of 3 is rejected). */
    public Reward pick(int index){
        if(resolved || index < 0 || index >= proposals.size) return null;
        taken = proposals.get(index);
        resolved = true;
        return taken;
    }

    /** Explicit "take none" decision. */
    public void takeNone(){
        resolved = true;
        taken = null;
    }

    public boolean isOpen(){
        return !resolved;
    }
}
