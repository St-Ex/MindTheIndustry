package mindtheindustry;

import java.util.function.*;
import arc.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;
import org.junit.jupiter.api.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;

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
        state.set(mindustry.core.GameState.State.menu); //in case a test entered play state
        state.gameOver = false;
        resetUnlockFlags(); //reward picks mutate the global unlock flags
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

    /**
     * The real launch sequence: Logic.play() sets wavetime from the MAP rules
     * (labyrinth: waveSpacing 7200 ticks → 2x = 14400 ticks = 240s), then fires
     * PlayEvent → the mod's startRun() must override it with the run pacing
     * (initial break 300s = 18000 ticks, waveSpacing 30s = 1800 ticks).
     */
    @Test
    void playSequenceAppliesRunPacingInTicks(){
        ServerMindTheIndustryController c = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");
        state.rules.canGameOver = false;
        Events.on(PlayEvent.class, e -> c.startRun()); //what MindTheIndustryMod does on MTI maps

        assertEquals(7200f, state.rules.waveSpacing, 0.001f, "map rule baseline: 7200 ticks = 120s");
        logic.play(); //sets wavetime from map rules, then fires PlayEvent

        assertEquals(18000f, state.wavetime, 0.001f, "startRun must override the initial break: 300s = 18000 ticks (not 300)");
        assertEquals(1800f, state.rules.waveSpacing, 0.001f, "30s inter-wave countdown = 1800 ticks");
        assertEquals(18000f, state.rules.initialWaveSpacing, 0.001f);
        assertTrue(c.session.active);
        assertTrue(state.rules.waves && state.rules.waveTimer && state.rules.waitEnemies);
        assertEquals(0, state.rules.winWave);

        c.stopRun();
    }

    /**
     * Full chapter 1 → 2 progression through the real engine loop: 20 waves
     * spawned via logic.runWave(), each wave cleared by killing its enemies,
     * wave-end detected by the live poll. Boss waves 10 and 20 must spawn a
     * boss-flagged unit of the right tier and open a boss window; all other
     * waves must reveal a regular choice without blocking the countdown.
     */
    @Test
    void chapterTwoBossFlowThroughRealEngine(){
        ServerMindTheIndustryController c = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");
        state.rules.canGameOver = false; //no player core in this test — keep checkGameState out of the way
        c.startRun();

        for(int wave = 1; wave <= 20; wave++){
            logic.runWave(); //engine spawns the wave + fires WaveEvent (syncs session.wave)
            assertEquals(wave, c.session.wave, "WaveEvent must sync session.wave to " + wave);
            assertEquals(wave + 1, state.wave);

            boolean bossWave = c.session.config.isBossWave(wave);
            if(bossWave){
                int chapter = c.session.config.currentChapter(wave);
                UnitType expected = DifficultyTuning.bossUnits[chapter - 1];
                Unit boss = Groups.unit.find(u -> u.team == state.rules.waveTeam && u.hasEffect(StatusEffects.boss));
                assertNotNull(boss, "boss wave " + wave + " must spawn a boss-flagged unit");
                assertEquals(expected, boss.type, "chapter " + chapter + " boss must be " + expected.name);
            }

            //clear the wave: last enemy of the set dies
            Groups.unit.copy().each(u -> {
                if(u.team == state.rules.waveTeam) u.kill();
            });

            if(bossWave){
                await(() -> c.session.isBossWindowOpen(), 10000);
                assertTrue(c.session.isBossWindowOpen(), "boss wave " + wave + " death must open the boss window");
                assertFalse(state.rules.waveTimer, "ceasefire: countdown frozen during the window");
                ChoiceState relic = c.session.bossWindow.relicChoices.get(p);
                assertNotNull(relic, "relic choice revealed at window open (wave " + wave + ")");
                for(Reward r : relic.proposals){
                    assertTrue(r.rarity == Rarity.RARE || r.rarity == Rarity.EPIC, "relic pool is rare/boosted");
                }
                //all-ready closes the window early → next chapter starts
                c.handleReady(p);
                assertFalse(c.session.isBossWindowOpen(), "all-ready must close the window (wave " + wave + ")");
                assertTrue(state.rules.waveTimer, "countdown resumes after the window");
                assertEquals(RunPhase.WAVE, c.session.phase);
            }else{
                final int expectedWave = wave;
                await(() -> c.session.getChoice(p) != null && c.session.getChoice(p).waveNumber == expectedWave, 10000);
                ChoiceState choice = c.session.getChoice(p);
                assertNotNull(choice, "wave " + wave + " end must reveal a choice");
                assertEquals(3, choice.proposals.size);
                assertEquals(wave, choice.waveNumber);
                assertTrue(state.rules.waveTimer, "regular wave choice must NOT freeze the countdown (wave " + wave + ")");
                //pick one this time — the box resolves, ≤1 pick
                c.handleRewardVote(p, 0, p);
                assertTrue(choice.resolved);
                assertNotNull(choice.taken);
            }
        }

        //after two boss windows we are in chapter 3 territory
        assertEquals(2, c.session.config.currentChapter(20));
        assertEquals(3, c.session.config.currentChapter(21));
        assertFalse(c.session.victory, "wave 20 boss is not the final boss");

        c.stopRun();
    }
}
