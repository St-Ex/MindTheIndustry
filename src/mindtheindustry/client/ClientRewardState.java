package mindtheindustry.client;

import arc.struct.*;
import mindtheindustry.rewards.*;

/**
 * Client-side mirror of the run state, fed by the server packets.
 *
 * Pure data holder — no arc.scene — so it is headless-testable. The UI layer
 * ({@code ui/RewardChoicePanel}) only renders this state and sends intents.
 */
public class ClientRewardState{

    /** Singleton accessed by packets and the HUD panel. */
    public static final ClientRewardState instance = new ClientRewardState();

    /** Wave the choices belong to. */
    public int wave;

    /** The 3 proposals currently offered (empty = box hidden). */
    public final Seq<Reward> choices = new Seq<>();

    /** True when the proposals are boss-window relic choices (instead of wave choices). */
    public boolean relic = false;

    /** Whether the choice box should be visible. */
    public boolean visible = false;

    /** Whether a boss window (ceasefire) is currently open server-side. */
    public boolean bossWindowOpen = false;

    /** Remaining boss window time in engine ticks (duration cap). */
    public float windowTimeLeft = 0f;

    /** The local player's already-taken rewards (tradeable during a boss window). */
    public final Seq<Reward> takenRewards = new Seq<>();

    /** Sets the choices for a wave (empty list clears/hides the box). */
    public void set(int wave, Seq<Reward> newChoices, boolean relic){
        this.wave = wave;
        this.relic = relic;
        this.choices.clear();
        if(newChoices != null){
            for(Reward r : newChoices){
                this.choices.add(r);
            }
        }
        this.visible = !this.choices.isEmpty();
        //diagnostic: prove the panel reads the same instance (identity hash)
        arc.util.Log.info("[MindTheIndustry] ClientRewardState.set wave=" + wave + " visible=" + visible + " choices=" + choices.size + " instance=" + System.identityHashCode(this) + " cl=" + getClass().getClassLoader());
    }

    /** Sets the choices for a wave (standard wave choice). */
    public void set(int wave, Seq<Reward> newChoices){
        set(wave, newChoices, false);
    }

    /** Sets the boss window state. */
    public void setBossWindow(boolean open, float timeLeft){
        setBossWindow(open, timeLeft, new Seq<>());
    }

    /** Sets the boss window state + the local player's taken rewards (for the trade UI). */
    public void setBossWindow(boolean open, float timeLeft, Seq<Reward> taken){
        this.bossWindowOpen = open;
        this.windowTimeLeft = timeLeft;
        this.takenRewards.clear();
        if(taken != null){
            for(Reward r : taken){
                this.takenRewards.add(r);
            }
        }
        if(!open){
            this.takenRewards.clear();
        }
    }

    /** Hides the box and drops the choices. */
    public void clear(){
        choices.clear();
        relic = false;
        visible = false;
    }
}
