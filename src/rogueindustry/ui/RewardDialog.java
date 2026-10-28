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
 * Dialogue de choix de récompenses - style Slay the Spire.
 *
 * Affiche 3 cartes côte à côte avec:
 * - Le type de récompense (couleur)
 * - Le nom
 * - La description
 * - Pour UnitBonus: un sélecteur de joueur cible
 *
 * Le joueur clique sur une carte pour choisir.
 */
public class RewardDialog extends BaseDialog{

    private static RewardDialog instance;

    private final int wave;
    private final Seq<Reward> choices;
    private Table targetSelector;
    private int selectedTargetId = -1;

    /** Couleurs par type de récompense. */
    private static final Color BUILDING_COLOR = Color.valueOf("4ecdc4");
    private static final Color BUILDING_BONUS_COLOR = Color.valueOf("26de81");
    private static final Color SCHEMATIC_COLOR = Color.valueOf("a55eea");
    private static final Color UNIT_BONUS_COLOR = Color.valueOf("ff6b6b");
    private static final Color GLOBAL_MOD_COLOR = Color.valueOf("4a90d9");
    private static final Color SYNERGY_COLOR = Color.valueOf("f7b731");

    public RewardDialog(int wave, Seq<Reward> choices){
        super("[accent]RogueIndustry[] - Vague " + (wave + 1) + " réussie!");
        this.wave = wave;
        this.choices = choices;

        setup();
    }

    private void setup(){
        cont.pane(table -> {
            table.margin(20f);

            // Titre
            table.label(() -> "[accent]Vague " + (wave + 1) + " terminée![] Choisissez votre récompense:")
                .fontScale(1.2f).padBottom(20f).row();

            // Les 3 cartes
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

                        // Bandeau de couleur par type
                        Color typeColor = getColor(reward.type);
                        card.table(header -> {
                            header.background(Tex.whiteui);
                            header.setColor(typeColor);
                            header.label(() -> getTypeLabel(reward.type))
                                .fontScale(0.8f)
                                .color(Color.white)
                                .pad(5f);
                        }).growX().height(30f).row();

                        // Nom de la récompense
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

                        // Pour UnitBonus et BuildingBonus: sélecteur de cible
                        if(reward.type == RewardType.UNIT_BONUS || reward.type == RewardType.BUILDING_BONUS){
                            String cibleLabel = reward.type == RewardType.UNIT_BONUS ? "Cible (unités):" : "Cible (bâtiments):";
                            card.table(selector -> {
                                selector.label(() -> "[white]" + cibleLabel).padRight(5f);
                                selector.button("Choisir...", () -> showTargetSelector(index))
                                    .width(100f).height(35f);
                            }).padTop(10f).row();
                        }
                    }).growY();
                }
            }).row();

            // Note
            table.label(() -> "[gray]Chaque joueur a ses propres choix. Les bâtiments sont partagés, les plans sont personnels.")
                .fontScale(0.8f).padTop(15f).row();
        }).grow();

        addCloseButton();
        addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                // Raccourcis clavier: 1, 2, 3 pour choisir
                if(keycode == KeyCode.num1 && choices.size > 0){ selectReward(0); return true; }
                if(keycode == KeyCode.num2 && choices.size > 1){ selectReward(1); return true; }
                if(keycode == KeyCode.num3 && choices.size > 2){ selectReward(2); return true; }
                return false;
            }
        });
    }

    private void showTargetSelector(int rewardIndex){
        // Dialogue pour choisir le joueur cible (pour UnitBonus)
        BaseDialog targetDialog = new BaseDialog("Choisir la cible");
        targetDialog.cont.pane(table -> {
            table.label(() -> "Qui reçoit ce bonus d'unités?").padBottom(10f).row();

            for(Player p : Groups.player){
                if(p.dead()) continue;
                String label = p == player ? p.name + " (moi)" : p.name;
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

        // Pour UnitBonus et BuildingBonus, récupérer la cible sélectionnée
        Player target = null;
        if(reward.type == RewardType.UNIT_BONUS || reward.type == RewardType.BUILDING_BONUS){
            if(selectedTargetId >= 0){
                target = Groups.player.find(p -> p.id == selectedTargetId);
            }
            // Si pas de cible sélectionnée, appliquer à soi-même par défaut
            if(target == null) target = player;
        }

        // Envoyer le vote au serveur via chat (les packets custom ne passent pas en solo)
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
            case BUILDING -> "BÂTIMENT - Tous";
            case BUILDING_BONUS -> "BONUS BÂTIMENT - Ciblé";
            case SCHEMATIC -> "PLAN - Personnel";
            case UNIT_BONUS -> "BONUS UNITÉ - Ciblé";
            case GLOBAL_MOD -> "MODIFICATEUR - Tous";
            case SYNERGY -> "SYNERGIE - Tous";
        };
    }

    /** Affiche le dialogue de choix (appelé quand le serveur envoie les récompenses). */
    public static void showChoices(int wave, Seq<Reward> choices){
        arc.util.Log.info("[RogueIndustry] RewardDialog.showChoices appelé: vague=" + wave + ", " + choices.size + " choix");
        if(instance != null && instance.hasParent()){
            instance.hide();
        }
        instance = new RewardDialog(wave, choices);
        instance.show();
        arc.util.Log.info("[RogueIndustry] RewardDialog affiché, hasParent=" + instance.hasParent());
    }
}
