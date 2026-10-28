package rogueindustry.modifiers;

/**
 * Permanent bonus applied to the buildings of a specific player.
 * Stored in RogueSession and applied to each new building built by the player.
 *
 * Bonus types:
 * - DAMAGE: +X% damage of this type of turrets
 * - RELOAD: +X% fire rate of this type of turrets
 * - RANGE: +X% range of this type of turrets
 * - PRODUCTION: +X% production speed of this type of factories
 * - POWER: +X% power production of this type of generators
 * - EFFICIENCY: +X% efficiency (fewer resources consumed)
 */
public class BuildingBonus{

    public enum BonusType{
        DAMAGE, RELOAD, RANGE, PRODUCTION, POWER, EFFICIENCY
    }

    /** Bonus type. */
    public final BonusType type;

    /** Bonus value (e.g. 0.15 for +15%). */
    public final float value;

    /** Name of the building concerned (e.g. "salvo", "multi-press"). null = all buildings. */
    public final String blockName;

    public BuildingBonus(BonusType type, float value, String blockName){
        this.type = type;
        this.value = value;
        this.blockName = blockName;
    }

    public String describe(){
        String sign = value >= 0 ? "+" : "";
        String target = blockName != null ? " [" + blockName + "]" : "";
        return switch(type){
            case DAMAGE -> sign + (int)(value * 100) + "% damage" + target;
            case RELOAD -> sign + (int)(value * 100) + "% fire rate" + target;
            case RANGE -> sign + (int)(value * 100) + "% range" + target;
            case PRODUCTION -> sign + (int)(value * 100) + "% production" + target;
            case POWER -> sign + (int)(value * 100) + "% power" + target;
            case EFFICIENCY -> sign + (int)(value * 100) + "% efficiency" + target;
        };
    }
}
