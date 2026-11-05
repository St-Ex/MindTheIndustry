package mindtheindustry;

import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
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
        resetUnlockFlags();
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
    void tradeoffScopesEntityStatsToTheTakingPlayer(){
        MindTheIndustrySession session = newSession(1);
        Player p = Player.create();
        p.name = "tester";
        Player other = Player.create();
        other.name = "other";
        TradeoffReward reward = new TradeoffReward("unit_damage", 0.2f, "build_speed", -0.2f,
            Rarity.RARE, "n", "d");

        reward.apply(session, p, p);

        // unit_damage has a per-entity hook → player bonus via entity assignment, NOT a global modifier
        assertEquals(1, session.getUnitBonuses(p).size);
        assertEquals(mindtheindustry.modifiers.UnitBonusType.DAMAGE, session.getUnitBonuses(p).first().type);
        assertEquals(0.2f, session.getUnitBonuses(p).first().value, 0.001f);
        assertTrue(session.getUnitBonuses(other).isEmpty(), "the other player gets nothing");
        // build_speed has no per-entity hook → stays a global (group-wide) modifier
        assertEquals(1, session.getGlobalModifiers().size);
        assertEquals("build_speed", session.getGlobalModifiers().first().id);
        assertEquals(0.8f, state.rules.buildSpeedMultiplier, 0.001f);
    }

    @Test
    void tradeTransfersEntityScopedBonusToTheRecipient(){
        MindTheIndustrySession session = newSession(1);
        Player from = Player.create();
        from.name = "from";
        Player to = Player.create();
        to.name = "to";
        TradeoffReward reward = new TradeoffReward("unit_damage", 0.2f, "unit_health", -0.1f,
            Rarity.RARE, "n", "d");
        reward.apply(session, from, from);

        reward.transfer(session, from, to);

        assertTrue(session.getUnitBonuses(from).isEmpty(), "donor loses the bonus-malus");
        assertEquals(2, session.getUnitBonuses(to).size, "recipient gains bonus and malus");
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
