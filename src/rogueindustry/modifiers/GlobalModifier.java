package rogueindustry.modifiers;

/**
 * Global modifier applied to the whole game (all players).
 * Examples: production speed, turret damage, etc.
 */
public class GlobalModifier{

    /** Unique identifier (e.g. "prod_speed", "turret_damage", "build_speed"). */
    public final String id;

    /** Bonus value (e.g. 0.10 for +10%). */
    public final float value;

    /** Display name. */
    public final String displayName;

    public GlobalModifier(String id, float value, String displayName){
        this.id = id;
        this.value = value;
        this.displayName = displayName;
    }

    public String describe(){
        String sign = value >= 0 ? "+" : "";
        return displayName + " " + sign + (int)(value * 100) + "%";
    }
}
