package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;

/**
 * Récompense: applique un modificateur global à TOUT le groupe.
 * Exemples: +10% vitesse de production, +15% dégâts des tourelles, etc.
 */
public class GlobalModifierReward extends Reward{

    /** Identifiant du modificateur (ex: "prod_speed", "turret_damage"). */
    public final String modifierId;

    /** Valeur du bonus (ex: 0.10 pour +10%). */
    public final float value;

    public GlobalModifierReward(String modifierId, float value, String displayName, String description){
        super("global_mod:" + modifierId + ":" + value, RewardType.GLOBAL_MOD, displayName, description);
        this.modifierId = modifierId;
        this.value = value;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        GlobalModifier modifier = GlobalModifierRegistry.create(modifierId, value);
        if(modifier != null){
            session.addGlobalModifier(modifier);
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(modifierId);
        write.f(value);
    }

    public static GlobalModifierReward readSpecific(String id, String name, String desc, Reads read){
        String modId = read.str();
        float val = read.f();
        return new GlobalModifierReward(modId, val, name, desc);
    }
}
