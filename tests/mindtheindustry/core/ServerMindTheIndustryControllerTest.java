package mindtheindustry.core;

import arc.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;
import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;
import mindtheindustry.client.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the M1 server controller flow (no UI, no packets — driven directly):
 * run start → wave end → non-blocking choice overlay → boss window → victory.
 */
class ServerMindTheIndustryControllerTest extends HeadlessMindTheIndustryTest{

    @BeforeEach
    void setup(){
        state.rules = new Rules();
        state.gameOver = false;
        // avoid unitCapDeath killing the test unit when it is added
        state.rules.unitCap = 10000;
    }

    @AfterEach
    void cleanup(){
        for(Player p : Groups.player.copy()){
            if(p.unit() != null) p.unit().remove();
            p.remove();
        }
        state.rules = new Rules();
        state.gameOver = false;
        resetUnlockFlags(); //reward picks mutate the global unlock flags
        ClientRewardState.instance.clear();
        ClientRewardState.instance.setBossWindow(false, 0f);
    }

    /** Creates a player with a living unit, registered in Groups.player. */
    private Player addPlayer(String name){
        Player p = Player.create();
        p.name = name;
        Unit u = UnitTypes.dagger.create(Team.sharded);
        u.add();
        p.unit(u);
        p.add();
        return p;
    }

    /** A controller with an active seeded run and a generator, without the live update loop interfering. */
    private ServerMindTheIndustryController newRunController(long seed){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        controller.lastProcessedWave = Integer.MAX_VALUE; //drive wave completion manually in tests
        controller.session.start(seed);
        controller.generator = new RewardGenerator(controller.session.rand);
        return controller;
    }

    /** Registers a sharded core so resource grants have somewhere to land. */
    private CoreBuild addCore(){
        CoreBuild core = (CoreBuild)content.block("core-shard").newBuilding();
        core.team = Team.sharded;
        if(core.items == null) core.items = new ItemModule();
        state.teams.registerCore(core);
        return core;
    }

    // --- Run start & pacing ---

    @Test
    void startRunSchedulesWaveOneAfterInitialBreak(){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        controller.lastProcessedWave = Integer.MAX_VALUE;
        addPlayer("tester");

        controller.startRun();

        assertTrue(controller.session.active);
        assertEquals(1, state.wave, "the engine's next wave to spawn is wave 1");
        assertEquals(0, controller.session.wave, "no wave spawned yet");
        assertEquals(1800f, state.rules.waveSpacing, 0.001f, "inter-wave countdown overridden to 30s (engine ticks: 30 * 60)");
        assertEquals(18000f, state.rules.initialWaveSpacing, 0.001f, "5 min initial break configured (300s in ticks)");
        assertEquals(18000f, state.wavetime, 0.001f, "the break before wave 1 is the initial countdown (ticks)");
        assertEquals(0, state.rules.winWave, "the mod owns victory — vanilla winWave stays off");
        assertTrue(state.rules.waves && state.rules.waveTimer && state.rules.waitEnemies);
        assertFalse(state.rules.spawns.isEmpty(), "wave 1 spawn groups must be installed");
        assertFalse(ClientRewardState.instance.visible, "no choice box before the first wave ends");

        controller.stopRun();
        assertFalse(controller.session.active);
    }

    // --- Wave-end choice overlay (non-blocking) ---

    @Test
    void waveEndRevealsThreeProposalsPerPlayerWithoutBlocking(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        controller.session.syncWave(1);

        controller.onWaveComplete(1);

        ChoiceState choice = controller.session.getChoice(p);
        assertNotNull(choice, "each player gets a choice at wave end");
        assertEquals(3, choice.proposals.size);
        assertEquals(1, choice.waveNumber);
        assertTrue(choice.isOpen());
        // non-blocking: the wave countdown keeps running under the choice overlay
        assertTrue(state.rules.waveTimer, "the choice must NOT freeze the wave timer");
        assertEquals(RunPhase.WAVE, controller.session.phase);
        // solo/headless: delivered to the local client state
        assertTrue(ClientRewardState.instance.visible);
        assertEquals(3, ClientRewardState.instance.choices.size);
        assertFalse(ClientRewardState.instance.relic);

        controller.stopRun();
    }

    @Test
    void waveEndChoicesAreDeterministicPerSeed(){
        ServerMindTheIndustryController c1 = newRunController(42);
        Player p1 = addPlayer("a");
        c1.session.syncWave(1);
        c1.onWaveComplete(1);
        Seq<String> ids1 = c1.session.getChoice(p1).proposals.map(r -> r.id);

        //remove p1 so the second run's wave-end generation iterates the same single player
        p1.unit().remove();
        p1.remove();

        ServerMindTheIndustryController c2 = newRunController(42);
        Player p2 = addPlayer("b");
        c2.session.syncWave(1);
        c2.onWaveComplete(1);
        Seq<String> ids2 = c2.session.getChoice(p2).proposals.map(r -> r.id);

        assertEquals(ids1, ids2, "same seed → same proposals at wave end");

        c1.stopRun();
        c2.stopRun();
    }

    @Test
    void pickingOneProposalAppliesItAndRejectsSecondPick(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        controller.session.syncWave(1);
        controller.onWaveComplete(1);

        controller.handleRewardVote(p, 0, p);

        ChoiceState choice = controller.session.getChoice(p);
        assertTrue(choice.resolved);
        assertNotNull(choice.taken);
        assertEquals(1, controller.session.getTakenRewards(p).size, "the taken reward is recorded (tradeable)");
        assertEquals(choice.taken, controller.session.getTakenRewards(p).first());
        assertNull(controller.session.getOpenChoice(p), "choice closed after the pick");
        assertFalse(ClientRewardState.instance.visible, "solo: box hides after picking");

        // taking 2 of 3 is rejected
        Reward second = controller.session.pickChoice(p, 1);
        assertNull(second, "a second pick must be rejected");
        assertEquals(1, controller.session.getTakenRewards(p).size);

        controller.stopRun();
    }

    @Test
    void takeNoneConvertsToFallbackGrant(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        CoreBuild core = addCore();
        controller.session.syncWave(1);
        controller.onWaveComplete(1);

        int before = core.items.get(Items.copper);
        controller.handleRewardVote(p, -1, p); //explicit "take none"

        assertNull(controller.session.getOpenChoice(p));
        assertEquals(before + controller.session.config.fallbackAmount, core.items.get(Items.copper),
            "taking none converts to the fallback resource grant on the group cores");
        assertEquals(0, controller.session.getTakenRewards(p).size, "nothing recorded as taken");

        state.teams.unregisterCore(core);
        controller.stopRun();
    }

    @Test
    void replacementByNextChoiceConvertsThePreviousOne(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        CoreBuild core = addCore();
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        ChoiceState first = controller.session.getChoice(p);

        int before = core.items.get(Items.copper);
        controller.session.syncWave(2);
        controller.onWaveComplete(2); //next wave end → new choice appears

        assertTrue(first.resolved, "the unchosen previous choice is resolved by the replacement");
        assertNull(first.taken, "converted, not taken");
        assertEquals(before + controller.session.config.fallbackAmount, core.items.get(Items.copper),
            "conversion on replacement grants the fallback to the group cores");
        ChoiceState second = controller.session.getChoice(p);
        assertNotSame(first, second);
        assertTrue(second.isOpen());
        assertEquals(2, second.waveNumber);

        state.teams.unregisterCore(core);
        controller.stopRun();
    }

    @Test
    void choiceStaysOpenThroughNextWaveCombat(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        ChoiceState choice = controller.session.getChoice(p);

        //the next wave spawns and fights while the choice is still pending
        controller.session.syncWave(2);

        assertSame(choice, controller.session.getOpenChoice(p),
            "a choice opened at wave N is still open during wave N+1 combat");
        assertTrue(choice.isOpen());

        controller.stopRun();
    }

    // --- Boss window ---

    @Test
    void bossWaveOpensCeasefireWindowWithRelicChoices(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        CoreBuild core = addCore();
        controller.session.syncWave(9);
        controller.onWaveComplete(9); //pending wave choice on wave 9
        ChoiceState waveChoice = controller.session.getChoice(p);
        int before = core.items.get(Items.copper);

        controller.session.syncWave(10); //chapter 1 boss wave
        controller.onWaveComplete(10);

        assertTrue(controller.session.isBossWindowOpen());
        assertEquals(RunPhase.BOSS_WINDOW, controller.session.phase);
        assertFalse(state.rules.waveTimer, "ceasefire: wave countdown frozen");
        assertTrue(waveChoice.resolved, "pending wave choices convert at window open");
        assertNull(waveChoice.taken);
        assertEquals(before + controller.session.config.fallbackAmount, core.items.get(Items.copper));

        ChoiceState relic = controller.session.bossWindow.relicChoices.get(p);
        assertNotNull(relic, "relic choice revealed per player at window open");
        assertEquals(3, relic.proposals.size);
        for(Reward r : relic.proposals){
            assertTrue(r.rarity == Rarity.RARE || r.rarity == Rarity.EPIC,
                "relic pool is rare/boosted, got " + r.rarity);
        }
        assertTrue(ClientRewardState.instance.visible && ClientRewardState.instance.relic,
            "solo: relic choice shown in the box");
        assertTrue(ClientRewardState.instance.bossWindowOpen);

        state.teams.unregisterCore(core);
        controller.stopRun();
    }

    @Test
    void windowClosesEarlyWhenAllPlayersReady(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        controller.session.syncWave(10);
        controller.onWaveComplete(10);
        assertTrue(controller.session.isBossWindowOpen());

        controller.handleReady(p);

        assertFalse(controller.session.isBossWindowOpen(), "all-ready closes the window early");
        assertEquals(RunPhase.WAVE, controller.session.phase);
        assertTrue(state.rules.waveTimer, "countdown resumes for the next chapter");
        assertFalse(ClientRewardState.instance.bossWindowOpen);

        controller.stopRun();
    }

    @Test
    void windowClosesAtDurationCap(){
        //bare session (no controller update loop ticking it concurrently)
        state.rules = new Rules();
        MindTheIndustrySession session = new MindTheIndustrySession();
        session.start(42);
        session.openBossWindow();

        float cap = session.config.ticks(session.config.bossWindowTimeout); //ticks
        assertTrue(session.isBossWindowOpen());
        assertFalse(session.tick(cap - 60f), "still time left before the cap");
        assertTrue(session.tick(120f), "duration cap reached → the window must close");

        session.stop();
    }

    @Test
    void oneTradePerWindowWithMutualConfirmation(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p1 = addPlayer("one");
        Player p2 = addPlayer("two");
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        controller.handleRewardVote(p1, 0, p1); //p1 takes a reward
        Reward taken = controller.session.getTakenRewards(p1).first();

        controller.session.syncWave(10);
        controller.onWaveComplete(10); //boss window opens

        //outside rules: offering a reward not taken is rejected
        assertNull(controller.session.offerTrade(p1, p2, "resource:not:taken"));
        assertTrue(controller.offerTrade(p1, p2, taken.id));
        assertNotNull(controller.session.bossWindow.pendingTrade);

        controller.confirmTrade(p1);
        assertFalse(controller.session.bossWindow.tradeUsed, "one confirmation is not enough");
        assertTrue(controller.session.hasTaken(p1, taken.id));
        controller.confirmTrade(p2); //mutual confirmation → executed

        assertTrue(controller.session.bossWindow.tradeUsed);
        assertNull(controller.session.bossWindow.pendingTrade);
        assertFalse(controller.session.hasTaken(p1, taken.id), "reward leaves the donor");
        assertTrue(controller.session.hasTaken(p2, taken.id), "reward arrives at the recipient");

        //second trade in the same window is rejected
        controller.session.recordTaken(p1, taken);
        assertNull(controller.session.offerTrade(p1, p2, taken.id), "at most one trade per boss window");

        controller.stopRun();
    }

    @Test
    void tradeRejectedOutsideWindow(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p1 = addPlayer("one");
        Player p2 = addPlayer("two");
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        controller.handleRewardVote(p1, 0, p1);
        Reward taken = controller.session.getTakenRewards(p1).first();

        assertNull(controller.session.offerTrade(p1, p2, taken.id),
            "no trade outside the boss window");

        controller.stopRun();
    }

    @Test
    void assignmentSwitchOnlyDuringBossWindow(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p1 = addPlayer("one");
        Player p2 = addPlayer("two");
        Unit u = p1.unit();
        controller.session.setActivePlayer(u, p1);

        //outside the window: rejected
        controller.setActivePlayer(u.id, false, p2, p1);
        assertSame(p1, controller.session.getActivePlayer(u), "switch rejected outside the boss window");

        //inside the window: allowed
        controller.session.syncWave(10);
        controller.onWaveComplete(10);
        controller.setActivePlayer(u.id, false, p2, p1);
        assertSame(p2, controller.session.getActivePlayer(u), "switch allowed during the boss window");

        controller.stopRun();
    }

    // --- Victory ---

    @Test
    void finalBossDeathGoesStraightToVictory(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        boolean[] gameOverFired = {false};
        Events.on(GameOverEvent.class, e -> gameOverFired[0] = true);

        int winWave = controller.session.config.winWave(); //default 3 chapters x 10
        assertEquals(30, winWave);
        controller.session.syncWave(winWave);
        controller.onWaveComplete(winWave);

        assertTrue(controller.session.victory, "final boss death → victory");
        assertTrue(state.gameOver);
        assertTrue(gameOverFired[0], "the victory screen event fires");
        assertFalse(state.rules.waves, "no wave 31");
        assertFalse(controller.session.isBossWindowOpen(), "no break after the final boss");
        assertEquals(RunPhase.WAVE, controller.session.phase);

        controller.stopRun();
    }

    @Test
    void disconnectConvertsPendingChoice(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        CoreBuild core = addCore();
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        ChoiceState choice = controller.session.getChoice(p);
        assertTrue(choice.isOpen());

        int before = core.items.get(Items.copper);
        arc.Events.fire(new PlayerLeave(p));

        assertTrue(choice.resolved, "disconnect resolves the pending choice");
        assertNull(choice.taken, "converted, not taken");
        assertEquals(before + controller.session.config.fallbackAmount, core.items.get(Items.copper),
            "disconnect → fallback grant to the group cores");

        state.teams.unregisterCore(core);
        controller.stopRun();
    }

    @Test
    void lateJoinerGetsFirstChoiceAtNextWaveEnd(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p1 = addPlayer("one");
        controller.session.syncWave(1);
        controller.onWaveComplete(1);
        assertNotNull(controller.session.getChoice(p1));

        //a second player joins mid-run: no choice until the next wave end
        Player p2 = addPlayer("two");
        assertNull(controller.session.getChoice(p2), "late joiner gets no retroactive choice");

        controller.session.syncWave(2);
        controller.onWaveComplete(2);

        assertNotNull(controller.session.getChoice(p2), "late joiner gets a choice at the next wave end");
        assertEquals(2, controller.session.getChoice(p2).waveNumber);
        assertEquals(2, controller.session.getChoice(p1).waveNumber, "p1's choice was replaced (wave-1 one converted)");

        controller.stopRun();
    }

    @Test
    void relicPickDuringBossWindow(){
        ServerMindTheIndustryController controller = newRunController(42);
        Player p = addPlayer("tester");
        controller.session.syncWave(10);
        controller.onWaveComplete(10);
        assertTrue(controller.session.isBossWindowOpen());
        ChoiceState relic = controller.session.bossWindow.relicChoices.get(p);

        controller.handleRewardVote(p, 1, p);

        assertTrue(relic.resolved, "relic choice resolved by the pick");
        assertNotNull(relic.taken);
        assertEquals(1, controller.session.getTakenRewards(p).size, "relic recorded as taken (tradeable)");
        assertNull(controller.session.getOpenChoice(p), "no open choice after the relic pick");

        controller.stopRun();
    }

    @Test
    void stopRunDuringBossWindowRestoresCountdown(){
        ServerMindTheIndustryController controller = newRunController(42);
        addPlayer("tester");
        controller.session.syncWave(10);
        controller.onWaveComplete(10);
        assertTrue(controller.session.isBossWindowOpen());
        assertFalse(state.rules.waveTimer);

        controller.stopRun();

        assertFalse(controller.session.active);
        assertFalse(controller.session.isBossWindowOpen());
        assertTrue(state.rules.waveTimer, "stopping the run restores the wave countdown");
    }

    @Test
    void nonFinalBossWaveDoesNotTriggerVictory(){
        ServerMindTheIndustryController controller = newRunController(42);
        addPlayer("tester");
        controller.session.syncWave(10);
        controller.onWaveComplete(10);

        assertFalse(controller.session.victory);
        assertFalse(state.gameOver);
        assertTrue(controller.session.isBossWindowOpen());

        controller.stopRun();
    }
}
