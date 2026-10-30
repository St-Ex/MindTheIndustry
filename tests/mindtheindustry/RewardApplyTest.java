package mindtheindustry;

import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for applying rewards to a session (gameplay effect of each category). */
class RewardApplyTest extends HeadlessMindTheIndustryTest{

    private MindTheIndustrySession newSession(long seed){
        state.rules = new Rules();
        MindTheIndustrySession session = new MindTheIndustrySession();
        session.start(seed);
        return session;
    }

    @Test
    void tradeoffRewardAppliesBonusAndMalus(){
        MindTheIndustrySession session = newSession(1);
        TradeoffReward reward = new TradeoffReward("build_speed", 0.2f, "unit_damage", -0.2f,
            Rarity.RARE, "n", "d");

        reward.apply(session, null, null);

        assertEquals(1.2f, state.rules.buildSpeedMultiplier, 0.001f);
        assertEquals(0.8f, state.rules.unitDamageMultiplier, 0.001f);
        assertEquals(2, session.getGlobalModifiers().size);
        assertEquals("build_speed", session.getGlobalModifiers().get(0).id);
        assertEquals("unit_damage", session.getGlobalModifiers().get(1).id);
    }

    @Test
    void planRewardUnlocksForGroup(){
        MindTheIndustrySession session = newSession(1);
        PlanReward reward = new PlanReward(Seq.with("block:spectre", "unit:flare"), Rarity.RARE, "n", "d");

        reward.apply(session, null, null);

        assertTrue(session.isUnlockedForGroup(content.block("spectre")));
        assertTrue(session.isUnlockedForGroup(content.unit("flare")));
        assertTrue(state.rules.researched.contains(content.block("spectre")));
        assertTrue(state.rules.researched.contains(content.unit("flare")));
    }

    @Test
    void resourceRewardAddsToTeamCores(){
        MindTheIndustrySession session = newSession(1);
        state.rules.defaultTeam = Team.sharded;

        CoreBuild core = (CoreBuild)content.block("core-shard").newBuilding();
        core.block = content.block("core-shard");
        core.team = Team.sharded;
        if(core.items == null) core.items = new ItemModule();
        state.teams.registerCore(core);

        ResourceReward reward = new ResourceReward(Seq.with("copper"), Seq.with(400), Rarity.RARE, "n", "d");
        reward.apply(session, null, null);

        assertEquals(400, core.items.get(Items.copper));

        // cleanup: don't leak the core into other tests' global state
        state.teams.unregisterCore(core);
        state.rules.defaultTeam = Team.sharded;
    }

    @Test
    void resourceRewardWithoutCoresDoesNotCrash(){
        MindTheIndustrySession session = newSession(1);
        // a team with no registered cores
        state.rules.defaultTeam = Team.crux;

        ResourceReward reward = new ResourceReward(Seq.with("copper", "titanium"), Seq.with(100, 100),
            Rarity.NORMAL, "n", "d");
        reward.apply(session, null, null); // must not throw

        state.rules.defaultTeam = Team.sharded;
    }
}
