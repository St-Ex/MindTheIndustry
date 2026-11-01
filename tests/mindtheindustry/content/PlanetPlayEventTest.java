package mindtheindustry.content;

import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.world.*;
import arc.*;
import org.junit.jupiter.api.*;

import mindtheindustry.MindTheIndustryMod;
import mindtheindustry.core.*;

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

        //MTI unlocks must not leak into the campaign: no write to global settings
        assertFalse(Core.settings.getBool("duo-unlocked", false), "MTI unlocks must not be persisted to global settings");

        //reverse direction: content unlocked in the campaign must NOT show on MTI
        UnlockableContent meltdown = content.block("meltdown");
        UnlockFlags.set(meltdown, true); //simulate campaign progress (in-memory)
        Events.fire(new WorldLoadEvent()); //still on the MTI map → isolate
        assertFalse(meltdown.unlocked(), "campaign-unlocked content must be hidden on MTI");
        assertTrue(duo.unlocked(), "starter must stay unlocked on MTI");

        //leaving the MTI map restores the campaign's unlocked flags
        state.rules = new Rules(); //sector == null → not an MTI map
        Events.fire(new WorldLoadEvent());
        assertTrue(meltdown.unlocked(), "campaign unlocks must be restored when leaving MTI");
    }

    /** A SaveSlot instance without running the Saves constructor (headless has no Core.assets). */
    private static Saves.SaveSlot fakeSaveSlot() throws Exception{
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe)f.get(null);
        return (Saves.SaveSlot)unsafe.allocateInstance(Saves.SaveSlot.class);
    }

    @Test
    void mtiSectorSaveIsDroppedInMenuSoEveryRunIsFresh() throws Exception{
        //a previous run leaves a sector save slot assigned (buildings = "ruins",
        //and the saved wavetime) — the mod must drop it while in the menu so
        //Control.playSector always takes the fresh-map path (playNewSector)
        Sector sector = MindTheIndustryPlanet.instance.sectors.first();
        sector.save = fakeSaveSlot();
        state.set(mindustry.core.GameState.State.menu);

        //the mod's Trigger.update hook runs on the headless loop — give it a moment
        long deadline = System.currentTimeMillis() + 5000;
        while(sector.save != null && System.currentTimeMillis() < deadline){
            try{
                Thread.sleep(50);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertNull(sector.save, "MTI sector save slot must be dropped in the menu → next launch regenerates a fresh map (no ruins, fresh wavetime)");
    }
}
