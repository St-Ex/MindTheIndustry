package rogueindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import rogueindustry.core.*;
import rogueindustry.rewards.*;

/**
 * Récompense: débloque un bâtiment pour TOUT le groupe.
 * Le bâtiment est ajouté à state.rules.researched, ce qui le rend disponible
 * pour tous les joueurs en partie.
 */
public class BuildingReward extends Reward{

    /** Nom interne du bloc (ex: "salvo", "laser-drill"). */
    public final String blockName;

    /** Le bloc débloqué (résolu par nom, car le contenu est chargé au runtime). */
    public transient Block block;

    public BuildingReward(String blockName, String displayName, String description){
        super("building:" + blockName, RewardType.BUILDING, displayName, description);
        this.blockName = blockName;
    }

    @Override
    public void apply(RogueSession session, Player chooser, Player target){
        if(block == null){
            block = mindustry.Vars.content.block(blockName);
        }
        if(block != null){
            session.unlockForGroup(block);
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        write.str(blockName);
    }

    public static BuildingReward readSpecific(String id, String name, String desc, Reads read){
        String blockName = read.str();
        BuildingReward r = new BuildingReward(blockName, name, desc);
        r.block = mindustry.Vars.content.block(blockName);
        return r;
    }
}
