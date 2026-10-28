package rogueindustry.modifiers;

/**
 * Modificateur global appliqué à toute la partie (tous les joueurs).
 * Exemples: vitesse de production, dégâts des tourelles, etc.
 */
public class GlobalModifier{

    /** Identifiant unique (ex: "prod_speed", "turret_damage", "build_speed"). */
    public final String id;

    /** Valeur du bonus (ex: 0.10 pour +10%). */
    public final float value;

    /** Nom d'affichage. */
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
