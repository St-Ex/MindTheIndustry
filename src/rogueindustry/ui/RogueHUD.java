package rogueindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import rogueindustry.RogueIndustryMod;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.net.*;

import static mindustry.Vars.*;

/**
 * HUD for RogueIndustry mode.
 *
 * Two parts:
 * 1. Start/Stop button at the top center (below the wave display)
 * 2. Info panel at the top left (visible during a run)
 *
 * Uses WorldLoadEvent to cleanly add itself at the start of each game,
 * and ResetEvent to clean up at the end.
 */
public class RogueHUD{

    private static Table buttonTable;
    private static Table hud;
    private static boolean initialized = false;

    /** Initializes the HUD (call at client load). */
    public static void init(){
        if(initialized) return;
        initialized = true;

        // Create the button (will be added to ui.hudGroup at WorldLoadEvent)
        buttonTable = new Table();
        buttonTable.setFillParent(true);
        buttonTable.top();
        buttonTable.touchable = Touchable.childrenOnly;

        // Create the two buttons once (don't recreate every frame!)
        final arc.scene.ui.Button[] startBtn = new arc.scene.ui.Button[1];
        final arc.scene.ui.Button[] stopBtn = new arc.scene.ui.Button[1];

        buttonTable.table(t -> {
            t.top();
            startBtn[0] = t.button("[accent]▶ Start Rogue", () -> {}).width(160f).height(35f).get();
            startBtn[0].addListener(new arc.scene.event.ClickListener(){
                @Override
                public void clicked(arc.scene.event.InputEvent event, float x, float y){
                    arc.util.Log.info("[RogueIndustry] CLICK DETECTED on Start Rogue!");
                    if(player != null){
                        player.sendMessage("/rogue start");
                    }
                }
            });

            stopBtn[0] = t.button("[scarlet]■ Stop Rogue", () -> {}).width(160f).height(35f).get();
            stopBtn[0].addListener(new arc.scene.event.ClickListener(){
                @Override
                public void clicked(arc.scene.event.InputEvent event, float x, float y){
                    arc.util.Log.info("[RogueIndustry] CLICK DETECTED on Stop Rogue!");
                    if(player != null){
                        player.sendMessage("/rogue stop");
                    }
                }
            });
        }).padTop(55f).center().row();

        // Toggle visibility based on state (called every frame, but doesn't recreate the buttons)
        buttonTable.update(() -> {
            if(startBtn[0] == null || stopBtn[0] == null) return;
            RogueSession session = RogueIndustryMod.clientSession;
            boolean active = session != null && session.active;
            startBtn[0].visible = !active;
            stopBtn[0].visible = active;
        });

        // Create the info panel
        hud = new Table();
        hud.setFillParent(true);
        hud.top().left();
        hud.margin(10f);
        hud.touchable = Touchable.disabled; // doesn't block clicks
        hud.visible(() -> RogueIndustryMod.clientSession != null && RogueIndustryMod.clientSession.active);

        hud.update(() -> {
            if(hud == null) return;
            RogueSession session = RogueIndustryMod.clientSession;
            if(session == null || !session.active) return;

            hud.clear();

            // === Wave ===
            hud.table(Tex.button, t -> {
                t.margin(6f);
                t.label(() -> "[accent]Wave " + session.wave)
                    .fontScale(1.1f);
            }).left().row();

            // === Global modifiers ===
            Seq<GlobalModifier> mods = session.getGlobalModifiers();
            if(mods.size > 0){
                hud.table(Tex.button, t -> {
                    t.margin(4f);
                    t.label(() -> "[yellow]Global modifiers:").fontScale(0.8f).left().row();
                    for(GlobalModifier mod : mods){
                        t.label(() -> "  [lightgray]" + mod.describe())
                            .fontScale(0.7f).left().row();
                    }
                }).left().padTop(3f).row();
            }

            // === Local player bonuses ===
            Player local = player;
            if(local != null){
                Seq<UnitBonus> unitBonuses = session.getUnitBonuses(local);
                Seq<BuildingBonus> buildingBonuses = session.getBuildingBonuses(local);

                if(unitBonuses.size > 0 || buildingBonuses.size > 0){
                    hud.table(Tex.button, t -> {
                        t.margin(4f);
                        t.label(() -> "[green]Your bonuses:").fontScale(0.8f).left().row();

                        for(UnitBonus b : unitBonuses){
                            t.label(() -> "  [lightgray]" + b.describe())
                                .fontScale(0.7f).left().row();
                        }
                        for(BuildingBonus b : buildingBonuses){
                            t.label(() -> "  [lightgray]" + b.describe())
                                .fontScale(0.7f).left().row();
                        }
                    }).left().padTop(3f).row();
                }
            }

            // === Connected players and their bonuses (summary) ===
            int playerCount = Groups.player.size();
            if(playerCount > 1){
                hud.table(Tex.button, t -> {
                    t.margin(4f);
                    t.label(() -> "[blue]Players: " + playerCount).fontScale(0.8f).left().row();
                    for(Player p : Groups.player){
                        if(p == local) continue;
                        int ub = session.getUnitBonuses(p).size;
                        int bb = session.getBuildingBonuses(p).size;
                        t.label(() -> "  [lightgray]" + p.name + ": " + ub + " unit bonuses, " + bb + " building bonuses")
                            .fontScale(0.7f).left().row();
                    }
                }).left().padTop(3f).row();
            }
        });

        // Add itself at the start of each game (clean, no per-frame hack)
        Events.on(WorldLoadEvent.class, e -> {
            if(ui.hudGroup == null) return;
            if(buttonTable != null && !buttonTable.hasParent()){
                ui.hudGroup.addChild(buttonTable);
            }
            if(hud != null && !hud.hasParent()){
                ui.hudGroup.addChild(hud);
            }
            Log.info("[RogueIndustry] RogueHUD added to the HUD.");
        });

        // Clean up at end of game
        Events.on(ResetEvent.class, e -> {
            if(buttonTable != null) buttonTable.remove();
            if(hud != null) hud.remove();
        });

        Log.info("[RogueIndustry] RogueHUD initialized.");
    }

    /** Removes the HUD. */
    public static void remove(){
        if(hud != null){
            hud.remove();
            hud = null;
        }
        if(buttonTable != null){
            buttonTable.remove();
            buttonTable = null;
        }
        initialized = false;
    }
}
