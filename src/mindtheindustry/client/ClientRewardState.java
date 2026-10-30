package mindtheindustry.client;

import arc.struct.*;
import mindtheindustry.rewards.*;

/**
 * Client-side mirror of the pending reward choices, fed by
 * {@link mindtheindustry.net.MindTheIndustryPackets.RewardChoicesPacket}.
 *
 * Pure data holder — no arc.scene — so it is headless-testable. The UI layer
 * ({@code ui/RewardChoicePanel}) only renders this state and sends vote intents.
 */
public class ClientRewardState{

    /** Singleton accessed by packets and the HUD panel. */
    public static final ClientRewardState instance = new ClientRewardState();

    /** Wave the choices belong to. */
    public int wave;

    /** The 3 choices currently offered (empty = box hidden). */
    public final Seq<Reward> choices = new Seq<>();

    /** Whether the choice box should be visible. */
    public boolean visible = false;

    /** Sets the choices for a wave (empty list clears/hides the box). */
    public void set(int wave, Seq<Reward> newChoices){
        this.wave = wave;
        this.choices.clear();
        if(newChoices != null){
            for(Reward r : newChoices){
                this.choices.add(r);
            }
        }
        this.visible = !this.choices.isEmpty();
    }

    /** Hides the box and drops the choices. */
    public void clear(){
        choices.clear();
        visible = false;
    }
}
