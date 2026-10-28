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
 * HUD du mode RogueIndustry.
 *
 * Deux parties:
 * 1. Bouton Démarrer/Arrêter en haut au centre (sous l'affichage des vagues)
 * 2. Panneau d'info en haut à gauche (visible pendant une run)
 *
 * Utilise WorldLoadEvent pour s'ajouter proprement au début de chaque partie,
 * et ResetEvent pour se nettoyer à la fin.
 */
public class RogueHUD{

    private static Table buttonTable;
    private static Table hud;
    private static boolean initialized = false;

    /** Initialise le HUD (à appeler au chargement client). */
    public static void init(){
        if(initialized) return;
        initialized = true;

        // Créer le bouton (sera ajouté à ui.hudGroup au WorldLoadEvent)
        buttonTable = new Table();
        buttonTable.setFillParent(true);
        buttonTable.top();
        buttonTable.touchable = Touchable.childrenOnly;

        // Créer les deux boutons une seule fois (ne pas recréer à chaque frame !)
        final arc.scene.ui.Button[] startBtn = new arc.scene.ui.Button[1];
        final arc.scene.ui.Button[] stopBtn = new arc.scene.ui.Button[1];

        buttonTable.table(t -> {
            t.top();
            startBtn[0] = t.button("[accent]▶ Démarrer Rogue", () -> {}).width(160f).height(35f).get();
            startBtn[0].addListener(new arc.scene.event.ClickListener(){
                @Override
                public void clicked(arc.scene.event.InputEvent event, float x, float y){
                    arc.util.Log.info("[RogueIndustry] CLIC DETECTE sur Démarrer Rogue!");
                    if(player != null){
                        player.sendMessage("/rogue start");
                    }
                }
            });

            stopBtn[0] = t.button("[scarlet]■ Arrêter Rogue", () -> {}).width(160f).height(35f).get();
            stopBtn[0].addListener(new arc.scene.event.ClickListener(){
                @Override
                public void clicked(arc.scene.event.InputEvent event, float x, float y){
                    arc.util.Log.info("[RogueIndustry] CLIC DETECTE sur Arrêter Rogue!");
                    if(player != null){
                        player.sendMessage("/rogue stop");
                    }
                }
            });
        }).padTop(55f).center().row();

        // Basculer la visibilité selon l'état (appelé à chaque frame, mais ne recrée pas les boutons)
        buttonTable.update(() -> {
            if(startBtn[0] == null || stopBtn[0] == null) return;
            RogueSession session = RogueIndustryMod.clientSession;
            boolean active = session != null && session.active;
            startBtn[0].visible = !active;
            stopBtn[0].visible = active;
        });

        // Créer le panneau d'info
        hud = new Table();
        hud.setFillParent(true);
        hud.top().left();
        hud.margin(10f);
        hud.touchable = Touchable.disabled; // ne bloque pas les clics
        hud.visible(() -> RogueIndustryMod.clientSession != null && RogueIndustryMod.clientSession.active);

        hud.update(() -> {
            if(hud == null) return;
            RogueSession session = RogueIndustryMod.clientSession;
            if(session == null || !session.active) return;

            hud.clear();

            // === Vague ===
            hud.table(Tex.button, t -> {
                t.margin(6f);
                t.label(() -> "[accent]Vague " + session.wave)
                    .fontScale(1.1f);
            }).left().row();

            // === Modificateurs globaux ===
            Seq<GlobalModifier> mods = session.getGlobalModifiers();
            if(mods.size > 0){
                hud.table(Tex.button, t -> {
                    t.margin(4f);
                    t.label(() -> "[yellow]Modificateurs globaux:").fontScale(0.8f).left().row();
                    for(GlobalModifier mod : mods){
                        t.label(() -> "  [lightgray]" + mod.describe())
                            .fontScale(0.7f).left().row();
                    }
                }).left().padTop(3f).row();
            }

            // === Bonus du joueur local ===
            Player local = player;
            if(local != null){
                Seq<UnitBonus> unitBonuses = session.getUnitBonuses(local);
                Seq<BuildingBonus> buildingBonuses = session.getBuildingBonuses(local);

                if(unitBonuses.size > 0 || buildingBonuses.size > 0){
                    hud.table(Tex.button, t -> {
                        t.margin(4f);
                        t.label(() -> "[green]Vos bonus:").fontScale(0.8f).left().row();

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

            // === Joueurs connectés et leurs bonus (résumé) ===
            int playerCount = Groups.player.size();
            if(playerCount > 1){
                hud.table(Tex.button, t -> {
                    t.margin(4f);
                    t.label(() -> "[blue]Joueurs: " + playerCount).fontScale(0.8f).left().row();
                    for(Player p : Groups.player){
                        if(p == local) continue;
                        int ub = session.getUnitBonuses(p).size;
                        int bb = session.getBuildingBonuses(p).size;
                        t.label(() -> "  [lightgray]" + p.name + ": " + ub + " bonus unités, " + bb + " bonus bâtiments")
                            .fontScale(0.7f).left().row();
                    }
                }).left().padTop(3f).row();
            }
        });

        // S'ajouter au début de chaque partie (propre, pas de hack par frame)
        Events.on(WorldLoadEvent.class, e -> {
            if(ui.hudGroup == null) return;
            if(buttonTable != null && !buttonTable.hasParent()){
                ui.hudGroup.addChild(buttonTable);
            }
            if(hud != null && !hud.hasParent()){
                ui.hudGroup.addChild(hud);
            }
            Log.info("[RogueIndustry] RogueHUD ajouté au HUD.");
        });

        // Nettoyer à la fin de partie
        Events.on(ResetEvent.class, e -> {
            if(buttonTable != null) buttonTable.remove();
            if(hud != null) hud.remove();
        });

        Log.info("[RogueIndustry] RogueHUD initialisé.");
    }

    /** Supprime le HUD. */
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
