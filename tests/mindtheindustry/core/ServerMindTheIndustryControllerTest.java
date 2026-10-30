package mindtheindustry.core;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the server controller flow, driven without any UI or packets:
 * commander selection → wave complete → reward votes → next wave.
 */
class ServerMindTheIndustryControllerTest extends HeadlessMindTheIndustryTest{

    @BeforeEach
    void setup(){
        state.rules = new Rules();
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

    @Test
    void commanderSelectionFlow(){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");

        controller.startCommanderSelection();

        assertTrue(controller.session.active);
        assertTrue(controller.waitingForCommanderVotes);
        assertTrue(controller.pendingCommanderChoices.containsKey(p));
        assertEquals(3, controller.pendingCommanderChoices.get(p).size);

        controller.handleCommanderVote(p, 0);

        // single player → all votes received → selection finalized
        assertFalse(controller.waitingForCommanderVotes);
        assertEquals(1, controller.commanderVotes.size);
        assertFalse(controller.pendingCommanderChoices.containsKey(p));

        controller.stopRun();
    }

    @Test
    void rewardVoteFlowAdvancesWave(){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");

        controller.session.start(42);
        controller.generator = new RewardGenerator(controller.session.rand);
        controller.session.nextWave();
        state.wave = controller.session.wave;

        controller.onWaveComplete();

        assertTrue(controller.waitingForVotes);
        assertTrue(controller.pendingChoices.containsKey(p));
        assertEquals(3, controller.pendingChoices.get(p).size);
        assertFalse(state.rules.waveTimer, "wave timer must pause during the strategy phase");

        controller.handleRewardVote(p, 0, p);

        assertFalse(controller.pendingChoices.containsKey(p));
        assertTrue(controller.votes.containsKey(p));
        // single player voted → next wave starts
        assertEquals(2, controller.session.wave);
        assertFalse(controller.waitingForVotes);
        assertTrue(state.rules.waveTimer);

        controller.stopRun();
    }

    @Test
    void invalidVotesAreIgnored(){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");

        controller.session.start(42);
        controller.generator = new RewardGenerator(controller.session.rand);
        controller.session.nextWave();
        state.wave = controller.session.wave;
        controller.onWaveComplete();

        controller.handleRewardVote(p, 99, p); // out of range
        controller.handleRewardVote(p, -1, p); // negative

        assertTrue(controller.pendingChoices.containsKey(p), "invalid votes must not consume the choice");
        assertFalse(controller.votes.containsKey(p));
        assertEquals(1, controller.session.wave, "wave must not advance on invalid votes");

        controller.stopRun();
    }

    @Test
    void votesIgnoredWhenNotWaiting(){
        ServerMindTheIndustryController controller = new ServerMindTheIndustryController();
        Player p = addPlayer("tester");

        // no run active, no vote phase: handleVote must be a no-op
        controller.handleVote(p, 0, p);

        assertFalse(controller.votes.containsKey(p));
        assertEquals(0, controller.session.wave);
    }
}
