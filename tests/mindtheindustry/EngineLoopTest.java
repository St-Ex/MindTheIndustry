package mindtheindustry;

import java.util.function.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import org.junit.jupiter.api.*;
import mindtheindustry.core.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test on the REAL run map (default/labyrinth, the MTI start
 * sector's map): drives the actual engine pieces the live game uses
 * (logic.runWave() → WaveEvent → controller sync; Trigger.update poll →
 * wave-end detection → choice reveal) instead of calling controller methods
 * directly.
 */
class EngineLoopTest extends HeadlessMindTheIndustryTest{

    @BeforeEach
    void setup(){
        Map map = maps.loadInternalMap("default/labyrinth");
        world.loadMap(map);
        state.rules.unitCap = 10000;
    }

    @AfterEach
    void cleanup(){
        for(Player p : Groups.player.copy()){
            if(p.unit() != null) p.unit().remove();
            p.remove();
        }
        for(Unit u : Groups.unit.copy()){
            if(u.team == Team.crux) u.remove();
        }
        state.rules = new Rules();
    }

    private Player addPlayer(String name){
        Player p = Player.create();
        p.name = name;
        Unit u = UnitTypes.dagger.create(Team.sharded);
        u.add();
        p.unit(u);
        p.add();
        return p;
    }

    private static void await(BooleanSupplier cond, long timeoutMs){
        long deadline = System.currentTimeMillis() + timeoutMs;
        while(!cond.getAsBoolean() && System.currentTimeMillis() < deadline){
            try{
                Thread.sleep(50);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Test
    void labyrinthMapRulesBaseline(){
        //what the map itself carries before the mod's run rules apply
        System.out.println("[test] labyrinth map rules: waves=" + state.rules.waves +
            " waveTimer=" + state.rules.waveTimer + " waveSpacing=" + state.rules.waveSpacing +
            " initialWaveSpacing=" + state.rules.initialWaveSpacing + " winWave=" + state.rules.winWave +
            " waitEnemies=" + state.rules.waitEnemies);
        int cruxCores = Groups.build.count(b -> b.team == state.rules.waveTeam && b instanceof CoreBuild);
        int cruxUnits = Groups.unit.count(u -> u.team == state.rules.waveTeam);
        System.out.println("[test] labyrinth standing wave-team cores=" + cruxCores + " units=" + cruxUnits +
            " spawnPoints=" + spawner.countSpawns());
    }

    @Test
    void fullEngineLoopStartRunWaveEndChoice(){
        ServerMindTheIndustryController c = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");

        c.startRun();

        //run pacing rules applied (engine fields are in ticks, 60/s; config is in seconds)
        assertEquals(1800f, state.rules.waveSpacing, 0.001f, "inter-wave countdown = 30s = 1800 ticks");
        assertEquals(18000f, state.rules.initialWaveSpacing, 0.001f);
        assertEquals(18000f, state.wavetime, 0.001f, "5-minute break before wave 1 = 18000 ticks");
        assertTrue(state.rules.waves && state.rules.waveTimer && state.rules.waitEnemies);
        assertEquals(0, state.rules.winWave);
        assertFalse(state.rules.spawns.isEmpty(), "wave 1 spawn groups installed");
        assertEquals(1, state.wave);
        assertEquals(0, c.session.wave);

        //the engine spawns wave 1 (as Logic.update does when wavetime hits 0)
        logic.runWave();

        assertEquals(2, state.wave);
        assertEquals(1, c.session.wave, "WaveEvent handler must sync the session wave");
        assertEquals(1800f, state.wavetime, 0.001f, "30s countdown (1800 ticks) installed after the wave spawned");
        int enemies = Groups.unit.count(u -> u.team == state.rules.waveTeam && u.isValid());
        assertTrue(enemies > 0, "wave 1 must actually spawn enemies on the real map");
        assertFalse(state.rules.spawns.isEmpty(), "next wave's spawn groups installed after WaveEvent");

        //the wave's last enemy dies → the Trigger.update poll must detect it and reveal a choice
        int standingCores = Groups.build.count(b -> b.team == state.rules.waveTeam && b instanceof CoreBuild);
        Groups.unit.each(u -> {
            if(u.team == state.rules.waveTeam) u.kill();
        });

        await(() -> c.session.getChoice(p) != null, 10000);
        assertNotNull(c.session.getChoice(p),
            "wave end must be detected by the update poll and reveal a choice (standing wave-team cores on map: " + standingCores + ")");
        assertEquals(3, c.session.getChoice(p).proposals.size);
        assertTrue(state.rules.waveTimer, "the choice must NOT freeze the countdown (non-blocking)");

        c.stopRun();
    }
}
