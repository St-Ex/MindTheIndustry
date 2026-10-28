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
 * Widget shown when a building or units are selected.
 *
 * - Single building: radio buttons per player, only one active.
 * - Unit group: radio buttons + counters (e.g. "Alice: 5, Bob: 3").
 * - Hover tooltip: list of bonuses applicable for this player on this entity.
 * - Any player can change the assignment.
 *
 * By default, the owner (builder/controller) is the active player.
 */
public class EntityPlayerSelector extends Table{

    private static EntityPlayerSelector instance;

    // Current selection
    private Building selectedBuilding;
    private final Seq<Unit> selectedUnits = new Seq<>();

    // Local cache of the active player (for the UI)
    private Player currentActive;

    // Counts per active player (for multi-selection)
    private final ObjectMap<Player, Integer> activeCounts = new ObjectMap<>();
    private int noneCount = 0;

    public EntityPlayerSelector(){
        visible = false;
        bottom().left();
        margin(10f);
        background(Tex.button);
    }

    /** Rebuilds the widget content. */
    private void rebuild(){
        clear();

        // === Line 1: Title + selection info ===
        table(header -> {
            header.left();

            String title;
            if(selectedBuilding != null){
                title = "[accent]Active player: [white]" + selectedBuilding.block.localizedName;
            }else if(!selectedUnits.isEmpty()){
                title = "[accent]Active player: [white]" + selectedUnits.size + " unit(s)";
            }else{
                title = "[accent]Active player";
            }
            header.label(() -> title).fontScale(0.9f).padRight(10f);

            // Counters (multi-selection only)
            if(!selectedUnits.isEmpty() && selectedUnits.size > 1){
                header.label(() -> buildCountersText()).fontScale(0.8f).color(Color.lightGray);
            }
        }).growX().row();

        // === Line 2: Radio buttons per player ===
        table(buttons -> {
            buttons.left();

            // "None" button
            TextButton noneBtn = new TextButton("None");
            noneBtn.setChecked(currentActive == null);
            noneBtn.clicked(() -> selectPlayer(null));
            noneBtn.update(() -> noneBtn.setChecked(currentActive == null));
            buttons.add(noneBtn).size(70f, 35f).pad(2f);

            // Buttons per player
            for(Player p : Groups.player){
                if(p.dead()) continue;

                TextButton btn = new TextButton(p.name);
                btn.setChecked(currentActive == p);
                btn.clicked(() -> selectPlayer(p));
                btn.update(() -> btn.setChecked(currentActive == p));

                // Hover tooltip
                addTooltip(btn, p);

                // Counter badge (multi-selection)
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

    /** Builds the counters text for multi-selection. */
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
            sb.append("None: [white]").append(noneCount);
        }
        sb.append("[/]");
        return sb.toString();
    }

    /** Adds a bonus tooltip on hover of a player button. */
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
                // Style with background
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

    /** Builds the tooltip text for a player. */
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
                sb.append("[gray]No bonus for this building");
            }else{
                for(BuildingBonus b : bonuses){
                    sb.append(b.value >= 0 ? "[green]" : "[red]").append(b.describe()).append("\n");
                }
            }
        }else if(!selectedUnits.isEmpty()){
            Seq<UnitBonus> bonuses = session.getApplicableUnitBonuses(p);
            if(bonuses.isEmpty()){
                sb.append("[gray]No bonus for units");
            }else{
                for(UnitBonus b : bonuses){
                    sb.append(b.value >= 0 ? "[green]" : "[red]").append(b.describe()).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    /** Selects a player as active for the current selection. */
    private void selectPlayer(Player target){
        currentActive = target;

        // Send to the server
        if(RogueIndustryMod.clientController != null){
            RogueIndustryMod.clientController.requestActivePlayer(selectedBuilding, selectedUnits, target);
        }

        // Update local counts (optimistic)
        updateLocalCounts(target);
        rebuild();
    }

    /** Updates local counts after a change (optimistic). */
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

    /** Sets the selected building (called by the BuildSelectEvent event). */
    public void setSelectedBuilding(Building building){
        selectedBuilding = building;
        selectedUnits.clear();
        refreshActiveState();
        visible = building != null && RogueIndustryMod.clientController != null
                  && RogueIndustryMod.clientController.session.active;
        if(visible) rebuild();
    }

    /** Sets the selected units (called periodically). */
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

    /** Refreshes the active player state and counts from the session. */
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

    /** Initializes the selector (added to the HUD). */
    public static void init(){
        if(instance != null) return;
        instance = new EntityPlayerSelector();
        instance.setFillParent(true);

        // Add itself to the HUD when a game starts (ui.hudGroup exists then)
        Events.on(WorldLoadEvent.class, e -> {
            if(ui.hudGroup != null && instance != null && !instance.hasParent()){
                ui.hudGroup.addChild(instance);
                Log.info("[RogueIndustry] EntityPlayerSelector added to the HUD.");
            }
        });

        // Clean up at end of game
        Events.on(ResetEvent.class, e -> {
            if(instance != null) instance.remove();
        });

        // Listen for building selection
        Events.on(BuildSelectEvent.class, e -> {
            if(e.tile != null && e.tile.build != null){
                instance.setSelectedBuilding(e.tile.build);
            }else{
                instance.setSelectedBuilding(null);
            }
        });

        // Periodic update for units (multi-selection)
        Timer.schedule(() -> {
            if(instance != null){
                if(!control.input.selectedUnits.isEmpty()){
                    instance.setSelectedUnits(control.input.selectedUnits);
                }
            }
        }, 0f, 0.1f);

        Log.info("[RogueIndustry] EntityPlayerSelector initialized.");
    }
}
