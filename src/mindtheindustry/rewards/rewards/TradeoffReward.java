package mindtheindustry.rewards.rewards;

import arc.util.io.*;
import mindustry.gen.*;
import mindtheindustry.core.*;
import mindtheindustry.modifiers.*;
import mindtheindustry.rewards.*;

/**
 * Reward: a trade-off between two stats — a bonus-malus scoped to the TAKING
 * player (applied via that player's entity assignment: unit bonuses to their
 * units, building bonuses to their buildings). Stat ids without a per-entity
 * hook (build_speed, core_health, resource_yield) stay group-wide global
 * modifiers. Always +X% on one stat and -X% on another (X = 10% x rarity).
 */
public class TradeoffReward extends Reward{

    /** Stat id receiving the bonus (e.g. "unit_damage"). */
    public final String bonusId;

    /** Bonus value, always positive (e.g. 0.10 for +10%). */
    public final float bonusValue;

    /** Stat id receiving the malus (e.g. "build_speed"). */
    public final String malusId;

    /** Malus value, always negative (e.g. -0.10 for -10%). */
    public final float malusValue;

    public TradeoffReward(String bonusId, float bonusValue, String malusId, float malusValue,
                          Rarity rarity, String displayName, String description){
        super("tradeoff:" + bonusId + ":" + bonusValue + ":" + malusId + ":" + malusValue,
              RewardType.TRADEOFF, rarity, displayName, description);
        this.bonusId = bonusId;
        this.bonusValue = bonusValue;
        this.malusId = malusId;
        this.malusValue = malusValue;
    }

    @Override
    public void apply(MindTheIndustrySession session, Player chooser, Player target){
        applySide(session, chooser, bonusId, bonusValue);
        applySide(session, chooser, malusId, malusValue);
    }

    /** Trade: the bonus-malus moves from the donor to the recipient. */
    @Override
    public void transfer(MindTheIndustrySession session, Player from, Player to){
        removeSide(session, from, bonusId, bonusValue);
        removeSide(session, from, malusId, malusValue);
        apply(session, to, to);
    }

    /** Applies one side of the trade-off to a player (entity-scoped when a per-entity hook exists). */
    private void applySide(MindTheIndustrySession session, Player player, String id, float value){
        if(player == null){
            session.addGlobalModifier(new GlobalModifier(id, value, GlobalModifierRegistry.nameOf(id)));
            return;
        }
        switch(id){
            case "unit_damage" -> session.addUnitBonus(player, new UnitBonus(UnitBonusType.DAMAGE, value));
            case "unit_health" -> session.addUnitBonus(player, new UnitBonus(UnitBonusType.HEALTH, value));
            case "turret_damage" -> session.addBuildingBonus(player, new BuildingBonus(BuildingBonus.BonusType.DAMAGE, value, null));
            case "turret_reload" -> session.addBuildingBonus(player, new BuildingBonus(BuildingBonus.BonusType.RELOAD, value, null));
            case "prod_speed" -> session.addBuildingBonus(player, new BuildingBonus(BuildingBonus.BonusType.PRODUCTION, value, null));
            default -> session.addGlobalModifier(new GlobalModifier(id, value, GlobalModifierRegistry.nameOf(id)));
        }
    }

    /** Removes one side of the trade-off from a player (mirror of applySide; global modifiers stay group-wide). */
    private void removeSide(MindTheIndustrySession session, Player player, String id, float value){
        if(player == null) return;
        switch(id){
            case "unit_damage" -> session.removeUnitBonus(player, UnitBonusType.DAMAGE, value);
            case "unit_health" -> session.removeUnitBonus(player, UnitBonusType.HEALTH, value);
            case "turret_damage" -> session.removeBuildingBonus(player, BuildingBonus.BonusType.DAMAGE, value, null);
            case "turret_reload" -> session.removeBuildingBonus(player, BuildingBonus.BonusType.RELOAD, value, null);
            case "prod_speed" -> session.removeBuildingBonus(player, BuildingBonus.BonusType.PRODUCTION, value, null);
            default -> { /* global modifier: group-wide, nothing to remove per player */ }
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        RewardSerializer.writeSpecific(this, write);
    }

    public static TradeoffReward readSpecific(String id, Rarity rarity, String name, String desc, Reads read){
        String bonusId = read.str();
        float bonusValue = read.f();
        String malusId = read.str();
        float malusValue = read.f();
        return new TradeoffReward(bonusId, bonusValue, malusId, malusValue, rarity, name, desc);
    }
}
