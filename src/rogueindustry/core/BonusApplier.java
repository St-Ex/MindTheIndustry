package rogueindustry.core;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/**
 * Applies active-player bonuses to the actual game stats.
 *
 * Hooks:
 * - BulletCreateEvent: bullet damage (turrets + units)
 * - Trigger.update: fire rate and building production (via applyBoost)
 */
public class BonusApplier{

    private final RogueSession session;

    public BonusApplier(RogueSession session){
        this.session = session;

        // ===== Bullet damage =====
        Events.on(BulletCreateEvent.class, e -> {
            if(!session.active) return;
            Bullet bullet = e.bullet;
            if(bullet == null || bullet.owner == null) return;

            float damageMult = 1f;

            if(bullet.owner instanceof Building building){
                // Bullet fired by a turret: bonus of the active player on this building
                damageMult = session.getBuildingDamageMultiplier(building);

                // + global turret damage modifiers
                for(var mod : session.getGlobalModifiers()){
                    if(mod.id.equals("turret_damage")){
                        damageMult *= (1f + mod.value);
                    }
                }
            }else if(bullet.owner instanceof Unit unit){
                // Bullet fired by a unit: bonus of the active player on this unit
                damageMult = session.getUnitDamageMultiplier(unit);
            }

            if(damageMult != 1f){
                bullet.damage *= damageMult;
            }
        });

        // ===== Fire rate and building production =====
        Events.run(Trigger.update, () -> {
            if(!session.active) return;

            for(Building building : Groups.build){
                if(building.team != state.rules.defaultTeam) continue;
                if(!building.isValid()) continue;

                Player active = session.getActivePlayer(building);
                if(active == null) continue;

                // Fire rate bonus (reload)
                float reloadMult = session.getBuildingReloadMultiplier(building);
                if(reloadMult > 1f){
                    // applyBoost(intensity, duration) speeds up the building
                    // intensity = speed multiplier (e.g. 1.3 = +30%)
                    // Called with a duration of 0.1s to maintain the boost
                    building.applyBoost(reloadMult, 0.15f);
                }

                // Production bonus
                float prodMult = session.getBuildingProductionMultiplier(building);
                if(prodMult > 1f){
                    building.applyBoost(prodMult, 0.15f);
                }
            }
        });
    }
}
