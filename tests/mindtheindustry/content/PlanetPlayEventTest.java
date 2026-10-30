package mindtheindustry.content;

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
    }

    @Test
    void playEventOnMtiMapAutoStartsRunAndUnlocksBuildMenu(){
        assertNotNull(MindTheIndustryPlanet.instance, "planet must be registered by loadContent");

        Planet planet = MindTheIndustryPlanet.instance;
        Sector sector = planet.sectors.find(s -> s.preset != null);
        assertNotNull(sector, "planet must have a sector bound to a preset");

        //simulate a campaign launch of the MTI sector: rules carry the sector
        state.rules = new Rules();
        state.rules.sector = sector;

        Events.fire(new WorldLoadEvent());
        Events.fire(new PlayEvent());

        assertNotNull(MindTheIndustryMod.serverController, "controller must be created by the fallbacks");
        assertTrue(MindTheIndustryMod.serverSession.active, "run must auto-start on an MTI map");

        //the exact gate used by the construction menu (PlacementFragment.unlocked)
        Block duo = content.block("duo");
        assertNotNull(duo);
        assertTrue(state.rules.sector != null, "campaign context for the menu gate");
        assertTrue(duo.unlockedNowHost(), "build menu gate (unlockedNowHost) must pass on the host");
        assertTrue(duo.unlocked(), "host-side unlocked flag must be set");
        assertTrue(state.rules.researched.contains(duo), "rules.researched must carry the unlock (client sync)");

        Block duct = content.block("duct");
        assertTrue(duct.unlockedNowHost(), "category starter must pass the build menu gate: duct");

        Block battery = content.block("battery");
        assertFalse(battery.unlockedNowHost(), "batteries must stay locked: battery");
    }
}
