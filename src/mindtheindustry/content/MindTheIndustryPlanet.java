package mindtheindustry.content;

import mindustry.content.*;
import mindustry.type.*;

/**
 * The MindTheIndustry "planet": a small moon of Serpulo, displayed as "MTI",
 * appearing in the New Game planet menu with a single selectable sector.
 *
 * Launching the start sector loads a map based on the vanilla "labyrinth" map
 * (via a SectorPreset) and auto-starts the roguelike run (see MindTheIndustryMod's
 * PlayEvent handler). The planet also carries a procedural generator
 * ({@link MindTheIndustryPlanetGenerator}) used for any sector without a preset.
 * Its technology tree is Serpulo's.
 */
public class MindTheIndustryPlanet extends Planet{

    /** The singleton instance (set at construction, in loadContent). */
    public static MindTheIndustryPlanet instance;

    public MindTheIndustryPlanet(){
        //a dwarf moon orbiting Serpulo (small radius, planet parent)
        super("mindtheindustry", Planets.serpulo, 0.5f, 3);
        instance = this;

        //procedural fallback for sectors without a fixed map preset
        generator = new MindTheIndustryPlanetGenerator();

        //the grid constructor auto-creates one sector per grid tile; the first one is the launch sector
        //(only this sector is selectable, so the globe shows a single sector marker)
        startSector = sectors.first().id;

        //the run map: based on the vanilla "labyrinth" map (maps/default/labyrinth.msav)
        new SectorPreset("run", "default/labyrinth", this, startSector);

        //appear in the New Game planet menu without campaign progress
        alwaysUnlocked = true;

        //display in the planet menu: grayish moon
        icon = "planet";
        iconColor = mindustry.graphics.Pal.gray.cpy();
        atmosphereColor = mindustry.graphics.Pal.gray.cpy();
    }

    @Override
    public void init(){
        super.init();
        //display name shown in the menu
        localizedName = "MTI";
        //reuse Serpulo's technology tree
        techTree = Planets.serpulo.techTree;
    }

    /** Whether the currently loaded game is a MindTheIndustry run map. */
    public static boolean isMindTheIndustryMap(){
        return mindustry.Vars.state.rules.sector != null
            && mindustry.Vars.state.rules.sector.planet instanceof MindTheIndustryPlanet;
    }
}
