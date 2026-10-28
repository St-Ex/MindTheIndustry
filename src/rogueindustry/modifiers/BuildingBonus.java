package rogueindustry.modifiers;

/**
 * Bonus permanent appliqué aux bâtiments d'un joueur spécifique.
 * Stocké dans RogueSession et appliqué à chaque nouveau bâtiment construit par le joueur.
 *
 * Types de bonus:
 * - DAMAGE: +X% dégâts des tourelles de ce type
 * - RELOAD: +X% vitesse de tir des tourelles de ce type
 * - RANGE: +X% portée des tourelles de ce type
 * - PRODUCTION: +X% vitesse de production des usines de ce type
 * - POWER: +X% production d'énergie des générateurs de ce type
 * - EFFICIENCY: +X% efficacité (moins de ressources consommées)
 */
public class BuildingBonus{

    public enum BonusType{
        DAMAGE, RELOAD, RANGE, PRODUCTION, POWER, EFFICIENCY
    }

    /** Type de bonus. */
    public final BonusType type;

    /** Valeur du bonus (ex: 0.15 pour +15%). */
    public final float value;

    /** Nom du bâtiment concerné (ex: "salvo", "multi-press"). null = tous les bâtiments. */
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
            case DAMAGE -> sign + (int)(value * 100) + "% dégâts" + target;
            case RELOAD -> sign + (int)(value * 100) + "% vitesse de tir" + target;
            case RANGE -> sign + (int)(value * 100) + "% portée" + target;
            case PRODUCTION -> sign + (int)(value * 100) + "% production" + target;
            case POWER -> sign + (int)(value * 100) + "% énergie" + target;
            case EFFICIENCY -> sign + (int)(value * 100) + "% efficacité" + target;
        };
    }
}
