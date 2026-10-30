package mindtheindustry.meta;

import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import mindtheindustry.MindTheIndustryMod;

import static mindustry.Vars.*;

/**
 * Commander selection dialog at the start of a run.
 * Displays all unlocked commanders as cards.
 * The player chooses a commander, their ability will be applied at start.
 */
public class CommanderSelectionDialog extends BaseDialog{

    private static CommanderSelectionDialog instance;

    private final Seq<Commander> commanders;
    private Commander selected;

    /** Constructor with all unlocked commanders. */
    public CommanderSelectionDialog(){
        super("[accent]MindTheIndustry[] - Choose your commander");
        this.commanders = MetaProgression.getUnlocked();
        setup();
    }

    /** Constructor with 3 specific choices (received from the server). */
    public CommanderSelectionDialog(Seq<Commander> choices){
        super("[accent]MindTheIndustry[] - Choose your commander");
        this.commanders = choices;
        setup();
    }

    private void setup(){
        cont.pane(table -> {
            table.margin(20f);

            // Title
            table.label(() -> "[accent]MindTheIndustry[] - Choose your commander")
                .fontScale(1.3f).padBottom(5f).row();
            table.label(() -> "[gray]Your commander's ability will be active for the whole run.")
                .fontScale(0.9f).padBottom(20f).row();

            // Meta progression
            table.label(() -> "[lightgray]Unlocked commanders: [white]" +
                    MetaProgression.unlockedCount() + "/" + MetaProgression.totalCount())
                .fontScale(0.85f).padBottom(15f).row();

            // Commander grid
            table.table(cards -> {
                cards.defaults().width(200f).pad(8f);
                int perRow = 3;
                for(int i = 0; i < commanders.size; i++){
                    final Commander cmdr = commanders.get(i);
                    cards.table(card -> {
                        card.background(Tex.button);
                        card.margin(10f);
                        card.addListener(new ClickListener(){
                            @Override
                            public void clicked(InputEvent event, float x, float y){
                                selectCommander(cmdr);
                            }
                        });

                        // Colored banner
                        card.table(header -> {
                            header.background(Tex.whiteui);
                            header.setColor(cmdr.color);
                            header.label(() -> cmdr.displayName)
                                .fontScale(1f)
                                .color(Color.white)
                                .pad(5f);
                        }).growX().height(35f).row();

                        // Description
                        card.label(() -> "[lightgray]" + cmdr.description)
                            .fontScale(0.8f)
                            .wrap()
                            .width(180f)
                            .pad(5f).row();

                        // Ability
                        card.table(ability -> {
                            ability.background(Tex.whiteui);
                            ability.setColor(Color.darkGray);
                            ability.label(() -> "[accent]" + cmdr.ability.name())
                                .fontScale(0.85f)
                                .pad(3f).row();
                            ability.label(() -> "[white]" + cmdr.ability.description())
                                .fontScale(0.75f)
                                .wrap()
                                .width(180f)
                                .pad(3f);
                        }).growX().pad(5f).row();

                        // Tier indicator
                        card.label(() -> "[gray]Tier " + cmdr.tier)
                            .fontScale(0.7f)
                            .padTop(5f).row();
                    }).growY();

                    if((i + 1) % perRow == 0) cards.row();
                }
            }).row();

            // Buttons
            table.table(buttons -> {
                buttons.defaults().width(150f).height(45f).pad(5f);
                buttons.button("[gray]Random", () -> {
                    selectCommander(commanders.random());
                });
                buttons.button("[green]Confirm", () -> {
                    confirmSelection();
                });
            }).padTop(15f).row();
        }).grow();

        addCloseButton();
    }

    private void selectCommander(Commander commander){
        selected = commander;
        // Update the confirm button
        // (simplified: confirm directly on next click)
    }

    private void confirmSelection(){
        if(selected == null){
            if(player != null) player.sendMessage("[scarlet]Please select a commander first.");
            return;
        }

        // Find the index of the selected commander in the choices list
        int index = commanders.indexOf(selected);
        if(index < 0) return;

        // Send the vote to the server via chat (custom packets don't work in solo)
        player.sendMessage("/mindtheindustry vote " + index);

        hide();
    }

    /** Shows the selection dialog with all unlocked commanders. */
    public static void showSelection(){
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new CommanderSelectionDialog();
        instance.show();
    }

    /** Shows the dialog with 3 specific choices received from the server. */
    public static void showChoices(Seq<Commander> choices){
        arc.util.Log.info("[MindTheIndustry] CommanderSelectionDialog.showChoices called with " + choices.size + " choices");
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new CommanderSelectionDialog(choices);
        arc.util.Log.info("[MindTheIndustry] Dialog created, calling show()... ui=" + (mindustry.Vars.ui != null));
        instance.show();
        arc.util.Log.info("[MindTheIndustry] Dialog shown, hasParent=" + instance.hasParent());
    }
}
