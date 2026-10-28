package rogueindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import rogueindustry.RogueIndustryMod;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;

import static mindustry.Vars.*;

/**
 * Widget affiché quand un bâtiment ou des unités sont sélectionnés.
 *
 * - Bâtiment seul: boutons radio par joueur, un seul actif.
 * - Groupe d'unités: boutons radio + compteurs (ex: "Alice: 5, Bob: 3").
 * - Tooltip au survol: liste des bonus applicables pour ce joueur sur cette entité.
 * - N'importe quel joueur peut changer l'assignation.
 *
 * Par défaut, le propriétaire (constructeur/contrôleur) est le joueur actif.
 */
public class EntityPlayerSelector extends Table{

    private static EntityPlayerSelector instance;

    // Sélection courante
    private Building selectedBuilding;
    private final Seq<Unit> selectedUnits = new Seq<>();

    // Cache local du joueur actif (pour l'UI)
    private Player currentActive;

    // Compteurs par joueur actif (pour sélection multiple)
    private final ObjectMap<Player, Integer> activeCounts = new ObjectMap<>();
    private int noneCount = 0;

    public EntityPlayerSelector(){
        visible = false;
        bottom().left();
        margin(10f);
        background(Tex.button);
    }

    /** Reconstruit le contenu du widget. */
    private void rebuild(){
        clear();

        // === Ligne 1: Titre + info sélection ===
        table(header -> {
            header.left();

            String title;
            if(selectedBuilding != null){
                title = "[accent]Joueur actif: [white]" + selectedBuilding.block.localizedName;
            }else if(!selectedUnits.isEmpty()){
                title = "[accent]Joueur actif: [white]" + selectedUnits.size + " unité(s)";
            }else{
                title = "[accent]Joueur actif";
            }
            header.label(() -> title).fontScale(0.9f).padRight(10f);

            // Compteurs (sélection multiple uniquement)
            if(!selectedUnits.isEmpty() && selectedUnits.size > 1){
                header.label(() -> buildCountersText()).fontScale(0.8f).color(Color.lightGray);
            }
        }).growX().row();

        // === Ligne 2: Boutons radio par joueur ===
        table(buttons -> {
            buttons.left();

            // Bouton "Aucun"
            TextButton noneBtn = new TextButton("Aucun");
            noneBtn.setChecked(currentActive == null);
            noneBtn.clicked(() -> selectPlayer(null));
            noneBtn.update(() -> noneBtn.setChecked(currentActive == null));
            buttons.add(noneBtn).size(70f, 35f).pad(2f);

            // Boutons par joueur
            for(Player p : Groups.player){
                if(p.dead()) continue;

                TextButton btn = new TextButton(p.name);
                btn.setChecked(currentActive == p);
                btn.clicked(() -> selectPlayer(p));
                btn.update(() -> btn.setChecked(currentActive == p));

                // Tooltip au survol
                addTooltip(btn, p);

                // Badge compteur (sélection multiple)
                if(!selectedUnits.isEmpty() && selectedUnits.size > 1){
                    int count = activeCounts.get(p, 0);
                    if(count > 0){
                        Label badge = new Label(String.valueOf(count));
                        badge.setFontScale(0.7f);
                        badge.setColor(Pal.accent);                        btn.stack(badge).grow();
                    }
                }

                buttons.add(btn).size(90f, 35f).pad(2f);
            }
        }).growX().padTop(5f).row();
    }

    /** Construit le texte des compteurs pour la sélection multiple. */
    private String buildCountersText(){
        StringBuilder sb = new StringBuilder();
        sb.append("[gray]");
        boolean first = true;
        for(Player p : Groups.player){
            int count = activeCounts.get(p, 0);
            if(count > 0){
                if(!first) sb.append(", ");
                sb.append("[#").append(p.color.toString()).append("]")
                  .append(p.name).append(": [white]").append(count);
                first = false;
            }
        }
        if(noneCount > 0){
            if(!first) sb.append(", ");
            sb.append("Aucun: [white]").append(noneCount);
        }
        sb.append("[/]");
        return sb.toString();
    }

    /** Ajoute un tooltip de bonus au survol d'un bouton joueur. */
    private void addTooltip(Element btn, Player p){
        btn.addListener(new InputListener(){
            private Label tooltipLabel;

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element from){
                String text = buildTooltip(p);
                if(text.isEmpty()) return;

                tooltipLabel = new Label(text);
                tooltipLabel.setFontScale(0.75f);
                tooltipLabel.setColor(Color.white);
                tooltipLabel.setAlignment(Align.left);
                // Style avec fond
                Label.LabelStyle style = new Label.LabelStyle(Fonts.def, Color.white);
                style.background = Tex.button;
                tooltipLabel.setStyle(style);
                tooltipLabel.pack();
                tooltipLabel.setPosition(
                    btn.getX(Align.center) - tooltipLabel.getWidth() / 2f,
                    btn.getY(Align.top) + btn.getHeight() + 5f
                );
                Core.scene.add(tooltipLabel);
                tooltipLabel.toFront();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element to){
                if(tooltipLabel != null){
                    tooltipLabel.remove();
                    tooltipLabel = null;
                }
            }
        });
    }

    /** Construit le texte du tooltip pour un joueur. */
    private String buildTooltip(Player p){
        RogueSession session = RogueIndustryMod.serverController != null
            ? RogueIndustryMod.serverController.session
            : (RogueIndustryMod.clientController != null ? RogueIndustryMod.clientController.session : null);
        if(session == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("[#").append(p.color.toString()).append("]").append(p.name).append("\n");

        if(selectedBuilding != null){
            String blockName = selectedBuilding.block.name;
            Seq<BuildingBonus> bonuses = session.getApplicableBuildingBonuses(p, blockName);
            if(bonuses.isEmpty()){
                sb.append("[gray]Aucun bonus pour ce bâtiment");
            }else{
                for(BuildingBonus b : bonuses){
                    sb.append(b.value >= 0 ? "[green]" : "[red]").append(b.describe()).append("\n");
                }
            }
        }else if(!selectedUnits.isEmpty()){
            Seq<UnitBonus> bonuses = session.getApplicableUnitBonuses(p);
            if(bonuses.isEmpty()){
                sb.append("[gray]Aucun bonus pour les unités");
            }else{
                for(UnitBonus b : bonuses){
                    sb.append(b.value >= 0 ? "[green]" : "[red]").append(b.describe()).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    /** Sélectionne un joueur comme actif pour la sélection courante. */
    private void selectPlayer(Player target){
        currentActive = target;

        // Envoyer au serveur
        if(RogueIndustryMod.clientController != null){
            RogueIndustryMod.clientController.requestActivePlayer(selectedBuilding, selectedUnits, target);
        }

        // Mettre à jour les compteurs locaux (optimiste)
        updateLocalCounts(target);
        rebuild();
    }

    /** Met à jour les compteurs locaux après un changement (optimiste). */
    private void updateLocalCounts(Player target){
        activeCounts.clear();
        noneCount = 0;

        if(!selectedUnits.isEmpty()){
            for(int i = 0; i < selectedUnits.size; i++){
                if(target == null){
                    noneCount++;
                }else{
                    activeCounts.put(target, activeCounts.get(target, 0) + 1);
                }
            }
        }else if(selectedBuilding != null){
            if(target == null){
                noneCount = 1;
            }else{
                activeCounts.put(target, 1);
            }
        }
    }

    /** Définit le bâtiment sélectionné (appelé par l'événement BuildSelectEvent). */
    public void setSelectedBuilding(Building building){
        selectedBuilding = building;
        selectedUnits.clear();
        refreshActiveState();
        visible = building != null && RogueIndustryMod.clientController != null
                  && RogueIndustryMod.clientController.session.active;
        if(visible) rebuild();
    }

    /** Définit les unités sélectionnées (appelé périodiquement). */
    public void setSelectedUnits(Seq<Unit> units){
        if(units == null) units = new Seq<>();

        boolean changed = !units.equals(selectedUnits) || selectedBuilding != null;
        selectedUnits.clear();
        selectedUnits.addAll(units);
        selectedBuilding = null;

        if(changed){
            refreshActiveState();
            visible = !selectedUnits.isEmpty() && RogueIndustryMod.clientController != null
                      && RogueIndustryMod.clientController.session.active;
            if(visible) rebuild();
        }
    }

    /** Rafraîchit l'état du joueur actif et les compteurs depuis la session. */
    private void refreshActiveState(){
        RogueSession session = RogueIndustryMod.serverController != null
            ? RogueIndustryMod.serverController.session
            : RogueIndustryMod.clientController.session;

        activeCounts.clear();
        noneCount = 0;

        if(selectedBuilding != null){
            currentActive = session.getActivePlayer(selectedBuilding);
            if(currentActive == null){
                noneCount = 1;
            }else{
                activeCounts.put(currentActive, 1);
            }
        }else if(!selectedUnits.isEmpty()){
            Player firstActive = null;
            boolean allSame = true;
            for(Unit u : selectedUnits){
                Player active = session.getActivePlayer(u);
                if(firstActive == null && active != null){
                    firstActive = active;
                }
                if(active != firstActive){
                    allSame = false;
                }
                if(active == null){
                    noneCount++;
                }else{
                    activeCounts.put(active, activeCounts.get(active, 0) + 1);
                }
            }
            currentActive = allSame ? firstActive : null;
        }
    }

    /** Initialise le sélecteur (ajouté au HUD). */
    public static void init(){
        if(instance != null) return;
        instance = new EntityPlayerSelector();
        instance.setFillParent(true);

        // S'ajouter au HUD quand une partie démarre (ui.hudGroup existe alors)
        Events.on(WorldLoadEvent.class, e -> {
            if(ui.hudGroup != null && instance != null && !instance.hasParent()){
                ui.hudGroup.addChild(instance);
                Log.info("[RogueIndustry] EntityPlayerSelector ajouté au HUD.");
            }
        });

        // Nettoyer à la fin de partie
        Events.on(ResetEvent.class, e -> {
            if(instance != null) instance.remove();
        });

        // Écouter la sélection de bâtiment
        Events.on(BuildSelectEvent.class, e -> {
            if(e.tile != null && e.tile.build != null){
                instance.setSelectedBuilding(e.tile.build);
            }else{
                instance.setSelectedBuilding(null);
            }
        });

        // Mise à jour périodique pour les unités (sélection multiple)
        Timer.schedule(() -> {
            if(instance != null){
                if(!control.input.selectedUnits.isEmpty()){
                    instance.setSelectedUnits(control.input.selectedUnits);
                }
            }
        }, 0f, 0.1f);

        Log.info("[RogueIndustry] EntityPlayerSelector initialisé.");
    }
}
