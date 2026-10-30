package mindtheindustry.ui;

import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindtheindustry.client.*;
import mindtheindustry.net.MindTheIndustryPackets.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static mindustry.Vars.*;

/**
 * HUD box shown after a wave is cleared: 3 icon buttons, one per reward choice.
 *
 * - hover an icon → tooltip shows the reward name + description (rarity-colored)
 * - click an icon → sends {@link RewardVotePacket} to the server
 *
 * This is pure presentation: all state comes from {@link ClientRewardState}
 * (fed by the server) and all decisions are made server-side.
 */
public class RewardChoicePanel extends Table{

    private final Label title = new Label("");
    private final Label tip = new Label("");
    private final Table buttons = new Table();

    private int builtWave = -1;

    public RewardChoicePanel(){
        background(Styles.black6);
        margin(6f);
        touchable = Touchable.childrenOnly;

        add(title).padBottom(4f).row();
        add(buttons).row();
        add(tip).padTop(4f).growX().row();
    }

    /** Creates the container that anchors this panel at the top-center of the HUD. */
    public static Table createContainer(){
        Table container = new Table();
        container.setFillParent(true);
        container.top();
        container.touchable = Touchable.childrenOnly;
        container.add(new RewardChoicePanel()).padTop(90f);
        return container;
    }

    @Override
    public void act(float delta){
        super.act(delta);
        ClientRewardState st = ClientRewardState.instance;
        // only show while playing on a server that runs the mod (solo = integrated server)
        visible = st.visible && (net.client() || net.server()) && state.isGame();
        if(visible && builtWave != st.wave){
            builtWave = st.wave;
            rebuild(st);
        }
    }

    private void rebuild(ClientRewardState st){
        title.setText("Wave " + st.wave + " — choose a reward");
        tip.setText("");
        buttons.clearChildren();

        for(int i = 0; i < st.choices.size; i++){
            Reward r = st.choices.get(i);
            int index = i;

            Button b = new Button();
            b.margin(4f);
            b.add(new Image(iconOf(r))).size(40f).row();
            Image bar = new Image();
            bar.setColor(r.rarity.hex);
            b.add(bar).height(4f).growX();

            b.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    vote(index);
                }
            });
            b.addListener(new InputListener(){
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                    tip.setText(r.displayName + "\n[gray]" + r.description);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                    tip.setText("");
                }
            });

            buttons.add(b).pad(4f);
        }
    }

    /** Sends the vote to the server and hides the box locally (server confirms). */
    private void vote(int index){
        net.send(new RewardVotePacket(index), true);
        ClientRewardState.instance.clear();
        visible = false;
    }

    /** Icon representing a reward: category icon or the actual content/item icon. */
    private Drawable iconOf(Reward r){
        try{
            return switch(r.type){
                case TRADEOFF -> Icon.chartBar;
                case PLAN -> {
                    PlanReward p = (PlanReward)r;
                    UnlockableContent c = p.contentNames.isEmpty() ? null : PlanReward.resolve(p.contentNames.first());
                    yield c != null && c.uiIcon.found() ? new TextureRegionDrawable(c.uiIcon) : Icon.hammer;
                }
                case RESOURCE -> {
                    ResourceReward res = (ResourceReward)r;
                    Item item = res.itemNames.isEmpty() ? null : content.item(res.itemNames.first());
                    yield item != null && item.uiIcon.found() ? new TextureRegionDrawable(item.uiIcon) : Icon.distribution;
                }
            };
        }catch(Throwable t){
            return Icon.warning;
        }
    }
}
