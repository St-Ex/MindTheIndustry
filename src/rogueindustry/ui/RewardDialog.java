package rogueindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import rogueindustry.RogueIndustryMod;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Reward choice dialog - Slay the Spire style.
 *
 * Shows 3 cards side by side with:
 * - The reward type (color)
 * - The name
 * - The description
 * - For UnitBonus: a target player selector
 *
 * The player clicks a card to choose.
 */
public class RewardDialog extends BaseDialog{

    private static RewardDialog instance;

    private final int wave;
    private final Seq<Reward> choices;
    private Table targetSelector;
    private int selectedTargetId = -1;

    /** Colors per reward type. */
    private static final Color BUILDING_COLOR = Color.valueOf("4ecdc4");
    private static final Color BUILDING_BONUS_COLOR = Color.valueOf("26de81");
    private static final Color SCHEMATIC_COLOR = Color.valueOf("a55eea");
    private static final Color UNIT_BONUS_COLOR = Color.valueOf("ff6b6b");
    private static final Color GLOBAL_MOD_COLOR = Color.valueOf("4a90d9");
    private static final Color SYNERGY_COLOR = Color.valueOf("f7b731");

    public RewardDialog(int wave, Seq<Reward> choices){
        super("[accent]RogueIndustry[] - Wave " + (wave + 1) + " complete!");
        this.wave = wave;
        this.choices = choices;

        setup();
    }

    private void setup(){
        cont.pane(table -> {
            table.margin(20f);

            // Title
            table.label(() -> "[accent]Wave " + (wave + 1) + " complete![] Choose your reward:")
                .fontScale(1.2f).padBottom(20f).row();

            // The 3 cards
            table.table(cards -> {
                cards.defaults().growY().width(220f).pad(10f);

                for(int i = 0; i < choices.size; i++){
                    final int index = i;
                    Reward reward = choices.get(i);
                    cards.table(card -> {
                        card.background(Tex.button);
                        card.margin(10f);
                        card.addListener(new ClickListener(){
                            @Override
                            public void clicked(InputEvent event, float x, float y){
                                selectReward(index);
                            }
                        });

                        // Color banner per type
                        Color typeColor = getColor(reward.type);
                        card.table(header -> {
                            header.background(Tex.whiteui);
                            header.setColor(typeColor);
                            header.label(() -> getTypeLabel(reward.type))
                                .fontScale(0.8f)
                                .color(Color.white)
                                .pad(5f);
                        }).growX().height(30f).row();

                        // Reward name
                        card.label(() -> reward.displayName)
                            .fontScale(1.1f)
                            .wrap()
                            .width(200f)
                            .pad(10f).row();

                        // Description
                        card.label(() -> "[lightgray]" + reward.description)
                            .fontScale(0.85f)
                            .wrap()
                            .width(200f)
                            .pad(5f).row();

                        // For UnitBonus and BuildingBonus: target selector
                        if(reward.type == RewardType.UNIT_BONUS || reward.type == RewardType.BUILDING_BONUS){
                            String cibleLabel = reward.type == RewardType.UNIT_BONUS ? "Target (units):" : "Target (buildings):";
                            card.table(selector -> {
                                selector.label(() -> "[white]" + cibleLabel).padRight(5f);
                                selector.button("Choose...", () -> showTargetSelector(index))
                                    .width(100f).height(35f);
                            }).padTop(10f).row();
                        }
                    }).growY();
                }
            }).row();

            // Note
            table.label(() -> "[gray]Each player has their own choices. Buildings are shared, schematics are personal.")
                .fontScale(0.8f).padTop(15f).row();
        }).grow();

        addCloseButton();
        addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                // Keyboard shortcuts: 1, 2, 3 to choose
                if(keycode == KeyCode.num1 && choices.size > 0){ selectReward(0); return true; }
                if(keycode == KeyCode.num2 && choices.size > 1){ selectReward(1); return true; }
                if(keycode == KeyCode.num3 && choices.size > 2){ selectReward(2); return true; }
                return false;
            }
        });
    }

    private void showTargetSelector(int rewardIndex){
        // Dialog to choose the target player (for UnitBonus)
        BaseDialog targetDialog = new BaseDialog("Choose the target");
        targetDialog.cont.pane(table -> {
            table.label(() -> "Who receives this unit bonus?").padBottom(10f).row();

            for(Player p : Groups.player){
                if(p.dead()) continue;
                String label = p == player ? p.name + " (me)" : p.name;
                table.button(label, () -> {
                    selectedTargetId = p.id;
                    targetDialog.hide();
                    selectReward(rewardIndex);
                }).width(200f).pad(3f).row();
            }
        });
        targetDialog.show();
    }

    private void selectReward(int index){
        if(index < 0 || index >= choices.size) return;

        Reward reward = choices.get(index);

        // For UnitBonus and BuildingBonus, get the selected target
        Player target = null;
        if(reward.type == RewardType.UNIT_BONUS || reward.type == RewardType.BUILDING_BONUS){
            if(selectedTargetId >= 0){
                target = Groups.player.find(p -> p.id == selectedTargetId);
            }
            // If no target selected, apply to yourself by default
            if(target == null) target = player;
        }

        // Send the vote to the server via chat (custom packets don't work in solo)
        player.sendMessage("/rogue reward " + index);

        hide();
    }

    private Color getColor(RewardType type){
        return switch(type){
            case BUILDING -> BUILDING_COLOR;
            case BUILDING_BONUS -> BUILDING_BONUS_COLOR;
            case SCHEMATIC -> SCHEMATIC_COLOR;
            case UNIT_BONUS -> UNIT_BONUS_COLOR;
            case GLOBAL_MOD -> GLOBAL_MOD_COLOR;
            case SYNERGY -> SYNERGY_COLOR;
        };
    }

    private String getTypeLabel(RewardType type){
        return switch(type){
            case BUILDING -> "BUILDING - All";
            case BUILDING_BONUS -> "BUILDING BONUS - Targeted";
            case SCHEMATIC -> "SCHEMATIC - Personal";
            case UNIT_BONUS -> "UNIT BONUS - Targeted";
            case GLOBAL_MOD -> "MODIFIER - All";
            case SYNERGY -> "SYNERGY - All";
        };
    }

    /** Shows the choice dialog (called when the server sends the rewards). */
    public static void showChoices(int wave, Seq<Reward> choices){
        arc.util.Log.info("[RogueIndustry] RewardDialog.showChoices called: wave=" + wave + ", " + choices.size + " choices");
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new RewardDialog(wave, choices);
        instance.show();
        arc.util.Log.info("[RogueIndustry] RewardDialog shown, hasParent=" + instance.hasParent());
    }
}
