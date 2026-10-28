package rogueindustry.core;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/**
 * Applique les bonus de joueur actif aux stats réelles du jeu.
 *
 * Hooks:
 * - BulletCreateEvent: dégâts des balles (tourelles + unités)
 * - Trigger.update: vitesse de tir et production des bâtiments (via applyBoost)
 */
public class BonusApplier{

    private final RogueSession session;

    public BonusApplier(RogueSession session){
        this.session = session;

        // ===== Dégâts des balles =====
        Events.on(BulletCreateEvent.class, e -> {
            if(!session.active) return;
            Bullet bullet = e.bullet;
            if(bullet == null || bullet.owner == null) return;

            float damageMult = 1f;

            if(bullet.owner instanceof Building building){
                // Balle tirée par une tourelle: bonus du joueur actif sur ce bâtiment
                damageMult = session.getBuildingDamageMultiplier(building);

                // + modificateurs globaux de dégâts tourelles
                for(var mod : session.getGlobalModifiers()){
                    if(mod.id.equals("turret_damage")){
                        damageMult *= (1f + mod.value);
                    }
                }
            }else if(bullet.owner instanceof Unit unit){
                // Balle tirée par une unité: bonus du joueur actif sur cette unité
                damageMult = session.getUnitDamageMultiplier(unit);
            }

            if(damageMult != 1f){
                bullet.damage *= damageMult;
            }
        });

        // ===== Vitesse de tir et production des bâtiments =====
        Events.run(Trigger.update, () -> {
            if(!session.active) return;

            for(Building building : Groups.build){
                if(building.team != state.rules.defaultTeam) continue;
                if(!building.isValid()) continue;

                Player active = session.getActivePlayer(building);
                if(active == null) continue;

                // Bonus de vitesse de tir (reload)
                float reloadMult = session.getBuildingReloadMultiplier(building);
                if(reloadMult > 1f){
                    // applyBoost(intensity, duration) accélère le bâtiment
                    // intensity = multiplicateur de vitesse (ex: 1.3 = +30%)
                    // On appelle avec une durée de 0.1s pour maintenir le boost
                    building.applyBoost(reloadMult, 0.15f);
                }

                // Bonus de production
                float prodMult = session.getBuildingProductionMultiplier(building);
                if(prodMult > 1f){
                    building.applyBoost(prodMult, 0.15f);
                }
            }
        });
    }
}
