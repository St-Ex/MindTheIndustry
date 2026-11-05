package mindtheindustry.core;

import arc.struct.*;
import org.junit.jupiter.api.*;
import mindtheindustry.HeadlessMindTheIndustryTest;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the per-player choice (at most 1 of 3 proposals, take-none, conversion on replacement). */
class ChoiceStateTest extends HeadlessMindTheIndustryTest{

    private Seq<Reward> proposals(){
        return Seq.with(
            new TradeoffReward("unit_damage", 0.2f, "build_speed", -0.2f, Rarity.RARE, "n1", "d1"),
            new PlanReward(Seq.with("block:salvo"), Rarity.NORMAL, "n2", "d2"),
            new ResourceReward(Seq.with("copper"), Seq.with(200), Rarity.EPIC, "n3", "d3")
        );
    }

    @Test
    void atMostOneProposalMayBeTaken(){
        ChoiceState c = new ChoiceState(null, 3, proposals());
        assertTrue(c.isOpen());

        Reward taken = c.pick(1);
        assertNotNull(taken);
        assertSame(c.proposals.get(1), taken);
        assertTrue(c.resolved);
        assertFalse(c.isOpen());

        assertNull(c.pick(0), "taking a second proposal is rejected");
        assertNull(c.pick(2));
        assertSame(taken, c.taken, "the first pick stands");
    }

    @Test
    void invalidIndicesAreRejected(){
        ChoiceState c = new ChoiceState(null, 1, proposals());
        assertNull(c.pick(-1));
        assertNull(c.pick(3));
        assertNull(c.pick(99));
        assertTrue(c.isOpen(), "rejected picks leave the choice open");
    }

    @Test
    void takeNoneResolvesWithoutATakenReward(){
        ChoiceState c = new ChoiceState(null, 2, proposals());
        c.takeNone();
        assertTrue(c.resolved);
        assertNull(c.taken);
        assertFalse(c.isOpen());
        assertNull(c.pick(0), "no pick after take-none");
    }
}
