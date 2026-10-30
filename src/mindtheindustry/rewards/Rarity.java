package mindtheindustry.rewards;

import arc.math.*;

/**
 * Rarity of a reward choice. The rarity multiplies the magnitude of the reward:
 * trade-off percentage, number of plans, or resource quantity.
 *
 * NORMAL (grey) x1, RARE (blue) x2, EPIC (violet) x4.
 */
public enum Rarity{
    NORMAL("[#9d9d9d]", 1),
    RARE("[#4d7dff]", 2),
    EPIC("[#a64dff]", 4);

    /** Chat color markup prefix used in display names/descriptions. */
    public final String color;

    /** Magnitude multiplier applied to the reward (x1 / x2 / x4). */
    public final int multiplier;

    Rarity(String color, int multiplier){
        this.color = color;
        this.multiplier = multiplier;
    }

    // --- Weighted roll knobs ---

    /** Probability weight of NORMAL. */
    public static float normalWeight = 0.60f;
    /** Probability weight of RARE. */
    public static float rareWeight = 0.30f;
    /** Probability weight of EPIC. */
    public static float epicWeight = 0.10f;

    /** Rolls a rarity using the weighted distribution (deterministic per Rand). */
    public static Rarity roll(Rand rand){
        float total = normalWeight + rareWeight + epicWeight;
        float roll = rand.random(total);
        if(roll < normalWeight) return NORMAL;
        roll -= normalWeight;
        if(roll < rareWeight) return RARE;
        return EPIC;
    }
}
