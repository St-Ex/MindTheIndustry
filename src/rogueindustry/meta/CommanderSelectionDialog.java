package rogueindustry.meta;

import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import rogueindustry.RogueIndustryMod;

import static mindustry.Vars.*;

/**
 * Dialogue de sélection du commandant au début d'une run.
 * Affiche tous les commandants débloqués sous forme de cartes.
 * Le joueur choisit un commandant, son pouvoir sera appliqué au démarrage.
 */
public class CommanderSelectionDialog extends BaseDialog{

    private static CommanderSelectionDialog instance;

    private final Seq<Commander> commanders;
    private Commander selected;

    /** Constructeur avec tous les commandants débloqués. */
    public CommanderSelectionDialog(){
        super("[accent]RogueIndustry[] - Choisissez votre commandant");
        this.commanders = MetaProgression.getUnlocked();
        setup();
    }

    /** Constructeur avec 3 choix spécifiques (reçus du serveur). */
    public CommanderSelectionDialog(Seq<Commander> choices){
        super("[accent]RogueIndustry[] - Choisissez votre commandant");
        this.commanders = choices;
        setup();
    }

    private void setup(){
        cont.pane(table -> {
            table.margin(20f);

            // Titre
            table.label(() -> "[accent]RogueIndustry[] - Choisissez votre commandant")
                .fontScale(1.3f).padBottom(5f).row();
            table.label(() -> "[gray]Le pouvoir de votre commandant sera actif pour toute la run.")
                .fontScale(0.9f).padBottom(20f).row();

            // Progression meta
            table.label(() -> "[lightgray]Commandants débloqués: [white]" +
                    MetaProgression.unlockedCount() + "/" + MetaProgression.totalCount())
                .fontScale(0.85f).padBottom(15f).row();

            // Grille de commandants
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

                        // Bandeau coloré
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

                        // Pouvoir
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

            // Boutons
            table.table(buttons -> {
                buttons.defaults().width(150f).height(45f).pad(5f);
                buttons.button("[gray]Aléatoire", () -> {
                    selectCommander(commanders.random());
                });
                buttons.button("[green]Confirmer", () -> {
                    confirmSelection();
                });
            }).padTop(15f).row();
        }).grow();

        addCloseButton();
    }

    private void selectCommander(Commander commander){
        selected = commander;
        // Mettre à jour le bouton confirmer
        // (simplifié: on valide directement au clic suivant)
    }

    private void confirmSelection(){
        if(selected == null){
            if(player != null) player.sendMessage("[scarlet]Veuillez d'abord sélectionner un commandant.");
            return;
        }

        // Trouver l'index du commandant sélectionné dans la liste des choix
        int index = commanders.indexOf(selected);
        if(index < 0) return;

        // Envoyer le vote au serveur via chat (les packets custom ne passent pas en solo)
        player.sendMessage("/rogue vote " + index);

        hide();
    }

    /** Affiche le dialogue de sélection avec tous les commandants débloqués. */
    public static void showSelection(){
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new CommanderSelectionDialog();
        instance.show();
    }

    /** Affiche le dialogue avec 3 choix spécifiques reçus du serveur. */
    public static void showChoices(Seq<Commander> choices){
        arc.util.Log.info("[RogueIndustry] CommanderSelectionDialog.showChoices appelé avec " + choices.size + " choix");
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new CommanderSelectionDialog(choices);
        arc.util.Log.info("[RogueIndustry] Dialogue créé, appel show()... ui=" + (mindustry.Vars.ui != null));
        instance.show();
        arc.util.Log.info("[RogueIndustry] Dialogue affiché, hasParent=" + instance.hasParent());
    }
}
