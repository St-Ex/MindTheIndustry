package mindtheindustry.ui;

import arc.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindtheindustry.client.*;
import mindtheindustry.net.*;
import mindtheindustry.net.MindTheIndustryPackets.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * HUD panel shown during a boss window (ceasefire between chapters):
 * countdown, a Ready button, and the one trade of the window (offer one of
 * your already-taken rewards to another player; mutual confirmation).
 *
 * Pure presentation: all state comes from {@link ClientRewardState} (fed by
 * the server) and every action is an intent packet ({@code sendIntent} also
 * works in solo, where it is delivered to the integrated server directly).
 * Assignment switches stay on /mindtheindustry active (they target an entity).
 */
public class BossWindowPanel extends Table{

    private final Label title = new Label("");
    private final Label time = new Label("");
    private final TextField targetField = new TextField("");
    private final Table tradeButtons = new Table();
    private boolean built = false;

    /** All live panels, driven from Trigger.update (robust even if scene act() dispatch is unreliable). */
    private static final arc.struct.Seq<BossWindowPanel> instances = new arc.struct.Seq<>();

    public BossWindowPanel(){
        background(Styles.black6);
        margin(6f);
        touchable = Touchable.childrenOnly;
        instances.add(this);

        add(title).padBottom(2f).row();
        add(time).padBottom(4f).row();

        TextButton ready = new TextButton(Core.bundle.get("mti.bosswindow.ready"));
        ready.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                MindTheIndustryPackets.sendIntent(player, new ReadyPacket());
            }
        });
        add(ready).padBottom(4f).row();

        add(new Label(Core.bundle.get("mti.trade.title"))).padBottom(2f).row();
        add(targetField).width(180f).padBottom(2f).row();
        add(tradeButtons).padBottom(2f).row();
        TextButton accept = new TextButton(Core.bundle.get("mti.trade.accept"));
        accept.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                MindTheIndustryPackets.sendIntent(player, new TradeConfirmPacket());
            }
        });
        add(accept).row();
    }

    /** Creates the container that anchors this panel at the bottom-left of the HUD. */
    public static Table createContainer(){
        Table container = new Table();
        container.setFillParent(true);
        container.bottom().left();
        container.touchable = Touchable.childrenOnly;
        container.add(new BossWindowPanel()).padBottom(90f).padLeft(10f);
        return container;
    }

    /** Drives every panel from the game update loop (fires whenever the world simulates). */
    public static void tickAll(){
        for(BossWindowPanel p : instances){
            p.tick();
            // keep the container on top of gameplay-added HUD elements (z-order = child order)
            if(p.parent != null) p.parent.toFront();
        }
    }

    @Override
    public void act(float delta){
        super.act(delta);
        tick();
    }

    private void tick(){
        ClientRewardState st = ClientRewardState.instance;
        visible = st.bossWindowOpen && state.isGame();
        if(!visible) return;

        title.setText(Core.bundle.get("mti.bosswindow.title"));
        time.setText(Core.bundle.format("mti.bosswindow.time", (int)(st.windowTimeLeft / 60f)));

        if(!built || tradeButtons.getChildren().size != st.takenRewards.size){
            built = true;
            tradeButtons.clearChildren();
            for(Reward r : st.takenRewards){
                TextButton b = new TextButton(r.rarity.color + r.type.name());
                b.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y){
                        String target = targetField.getText();
                        if(target == null || target.isBlank()) return;
                        MindTheIndustryPackets.sendIntent(player, new TradeOfferPacket(target.trim(), r.id));
                    }
                });
                tradeButtons.add(b).pad(2f);
            }
        }
    }
}
