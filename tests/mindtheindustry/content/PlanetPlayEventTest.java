package mindtheindustry.content;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.world.*;
import arc.*;
import org.junit.jupiter.api.*;

import mindtheindustry.MindTheIndustryMod;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simulates the real launch flow of an MTI run (New Game menu -> MTI planet ->
 * start sector -> WorldLoadEvent + PlayEvent) and checks that the run auto-starts
 * and that starter blocks pass the build-menu gate (PlacementFragment uses
 * block.unlockedNowHost() in campaign, which on the host reads the content's
 * 'unlocked' flag).
 */
class PlanetPlayEventTest extends mindtheindustry.HeadlessMindTheIndustryTest{

    private static boolean modRegistered;

    @BeforeAll
    static void registerMod(){
        if(modRegistered) return;
        modRegistered = true;
        //register the mod's event handlers + content, like the game does at startup
        MindTheIndustryMod mod = new MindTheIndustryMod();
        mod.loadContent();
        //ContentLoader normally calls init() on all content after loadContent
        MindTheIndustryPlanet.instance.init();
    }

    @Test
    void playEventOnMtiMapAutoStartsRunAndUnlocksBuildMenu(){
        assertNotNull(MindTheIndustryPlanet.instance, "planet must be registered by loadContent");

        Planet planet = MindTheIndustryPlanet.instance;
        //own tech tree, not Serpulo's shared one (cloning would reassign content.techNode)
        assertNotSame(Planets.serpulo.techTree, planet.techTree, "MTI must have its own tech tree root");
        assertSame(planet, planet.techTree.planet, "tech tree root must be bound to the MTI planet");
        assertSame(MindTheIndustryPlanet.instance.runPreset, planet.techTree.content, "tech tree root content is the run preset");
        assertNotSame(planet.techTree, content.block("duo").techNode, "vanilla content techNode must stay on Serpulo's tree");
        Sector sector = planet.sectors.find(s -> s.preset != null);
        assertNotNull(sector, "planet must have a sector bound to a preset");

        //simulate a campaign launch of the MTI sector: rules carry the sector
        state.rules = new Rules();
        state.rules.sector = sector;
        state.rules.planet = planet;
        state.rules.env = planet.defaultEnv; //as World.setSectorRules does

        Events.fire(new WorldLoadEvent());
        Events.fire(new PlayEvent());

        assertNotNull(MindTheIndustryMod.serverController, "controller must be created by the fallbacks");
        assertTrue(MindTheIndustryMod.serverSession.active, "run must auto-start on an MTI map");

        //the exact gate used by the construction menu (PlacementFragment.unlocked):
        //unlockedNowHost() && placeablePlayer && environmentBuildable() && supportsEnv(env)
        Block duo = content.block("duo");
        assertNotNull(duo);
        assertTrue(state.rules.sector != null, "campaign context for the menu gate");
        assertTrue(duo.unlockedNowHost(), "build menu gate (unlockedNowHost) must pass on the host");
        assertTrue(duo.unlocked(), "host-side unlocked flag must be set");
        assertTrue(state.rules.researched.contains(duo), "rules.researched must carry the unlock (client sync)");
        assertTrue(duo.shownPlanets.contains(planet), "planet.init() must add MTI to shownPlanets via the tech tree");
        assertTrue(duo.environmentBuildable(), "environmentBuildable() gates the menu on the current planet");
        assertTrue(duo.placeablePlayer && duo.supportsEnv(state.rules.env), "placeable + env must pass");

        Block duct = content.block("duct");
        assertTrue(duct.unlockedNowHost() && duct.environmentBuildable(), "category starter must pass the build menu gate: duct");

        Block battery = content.block("battery");
        assertFalse(battery.unlockedNowHost(), "batteries must stay locked: battery");
    }
}
