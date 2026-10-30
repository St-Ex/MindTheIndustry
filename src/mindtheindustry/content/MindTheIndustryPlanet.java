package mindtheindustry.content;

import mindustry.content.*;
import mindustry.type.*;

/**
 * The MindTheIndustry "planet": appears in the New Game planet menu.
 *
 * Launching its start sector loads a map based on the vanilla "labyrinth" map
 * (via a SectorPreset) and auto-starts the roguelike run (see MindTheIndustryMod's
 * PlayEvent handler). The planet also carries a procedural generator
 * ({@link MindTheIndustryPlanetGenerator}) used for any sector without a preset.
 */
public class MindTheIndustryPlanet extends Planet{

    /** The singleton instance (set at construction, in loadContent). */
    public static MindTheIndustryPlanet instance;

    public MindTheIndustryPlanet(){
        super("mindtheindustry", Planets.sun, 1f, 3);
        instance = this;

        //procedural fallback for sectors without a fixed map preset
        generator = new MindTheIndustryPlanetGenerator();

        //the grid constructor auto-creates one sector per grid tile; the first one is the launch sector
        startSector = sectors.first().id;

        //the run map: based on the vanilla "labyrinth" map (maps/default/labyrinth.msav)
        new SectorPreset("run", "default/labyrinth", this, startSector);

        //appear in the New Game planet menu without campaign progress
        alwaysUnlocked = true;

        //display in the planet menu
        icon = "planet";
        iconColor = mindustry.graphics.Pal.accent.cpy();
        atmosphereColor = mindustry.graphics.Pal.accent.cpy();
    }

    /** Whether the currently loaded game is a MindTheIndustry run map. */
    public static boolean isMindTheIndustryMap(){
        return mindustry.Vars.state.rules.sector != null
            && mindustry.Vars.state.rules.sector.planet instanceof MindTheIndustryPlanet;
    }
}
