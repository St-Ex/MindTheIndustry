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
 *
 * The planet has its OWN tech tree (root = the run preset) so the vanilla campaign
 * trees are never mutated; buildability of blocks on this planet is handled directly
 * through shownPlanets in init().
 */
public class MindTheIndustryPlanet extends Planet{

    /** The singleton instance (set at construction, in loadContent). */
    public static MindTheIndustryPlanet instance;

    /** The run map preset (labyrinth-based); also the root of this planet's own tech tree. */
    public SectorPreset runPreset;

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
        runPreset = new SectorPreset("run", "default/labyrinth", this, startSector);

        //appear in the New Game planet menu without campaign progress
        alwaysUnlocked = true;

        //display in the planet menu: grayish moon
        icon = "planet";
        iconColor = mindustry.graphics.Pal.gray.cpy();
        atmosphereColor = mindustry.graphics.Pal.gray.cpy();
    }

    @Override
    public void init(){
        //own tech tree root containing only the run preset — do NOT clone Serpulo's tree:
        //the TechNode constructor reassigns content.techNode, which would leak into the
        //campaign (research objectives read content.techNode). Assigned before super.init()
        //so Planet.init() keeps it instead of looking up a root for this planet.
        techTree = TechTree.nodeRoot("mti", runPreset, () -> {});
        techTree.planet = this;
        super.init();
        //display name shown in the menu
        localizedName = "MTI";
        //buildability on this planet: the construction HUD only shows blocks whose
        //shownPlanets contains the current planet (environmentBuildable()). Register
        //directly instead of via a shared tech tree — additive only, other planets are
        //unaffected (isOnPlanet checks contains(their own planet))
        mindustry.Vars.content.blocks().each(b -> b.shownPlanets.add(this));
        mindustry.Vars.content.units().each(u -> u.shownPlanets.add(this));
    }

    /** Whether the currently loaded game is a MindTheIndustry run map. */
    public static boolean isMindTheIndustryMap(){
        return mindustry.Vars.state.rules.sector != null
            && mindustry.Vars.state.rules.sector.planet instanceof MindTheIndustryPlanet;
    }
}
