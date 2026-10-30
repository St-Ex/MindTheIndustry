package mindtheindustry.content;

import arc.math.*;
import mindustry.maps.planet.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * Planet generator for MindTheIndustry runs.
 *
 * Reuses the main campaign's native procedural generation
 * ({@link SerpuloPlanetGenerator}) so every run gets a Serpulo-style map.
 * The engine passes {@code seedOffset = 0} on normal launches, so we randomize
 * it here: each launch of the sector generates a different map.
 * (The map editor passes an explicit seed, which we keep.)
 */
public class MindTheIndustryPlanetGenerator extends SerpuloPlanetGenerator{

    @Override
    public void generate(Tiles tiles, Sector sec, WorldParams params){
        if(params.seedOffset == 0){
            //fresh map on every run
            params.seedOffset = 1 + Mathf.random(Integer.MAX_VALUE - 1);
        }
        super.generate(tiles, sec, params);
    }
}
