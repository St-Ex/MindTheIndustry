package mindtheindustry;

import arc.struct.*;
import org.junit.jupiter.api.*;
import mindtheindustry.client.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the client-side reward choice state (fed by packets, rendered by the HUD). */
class ClientRewardStateTest extends HeadlessMindTheIndustryTest{

    @AfterEach
    void cleanup(){
        ClientRewardState.instance.clear();
    }

    @Test
    void setMakesChoicesVisible(){
        Seq<Reward> choices = Seq.with(
            new TradeoffReward("unit_damage", 0.1f, "build_speed", -0.1f, Rarity.NORMAL, "n", "d"));
        ClientRewardState.instance.set(5, choices);

        assertEquals(5, ClientRewardState.instance.wave);
        assertEquals(1, ClientRewardState.instance.choices.size);
        assertTrue(ClientRewardState.instance.visible);
    }

    @Test
    void emptyChoicesHideTheBox(){
        ClientRewardState.instance.set(5, Seq.with(
            new PlanReward(Seq.with("block:salvo"), Rarity.NORMAL, "n", "d")));
        assertTrue(ClientRewardState.instance.visible);

        ClientRewardState.instance.set(6, new Seq<>());

        assertEquals(6, ClientRewardState.instance.wave);
        assertTrue(ClientRewardState.instance.choices.isEmpty());
        assertFalse(ClientRewardState.instance.visible, "empty choices = hide the box");
    }

    @Test
    void clearHidesAndDrops(){
        ClientRewardState.instance.set(2, Seq.with(
            new ResourceReward(Seq.with("copper"), Seq.with(100), Rarity.NORMAL, "n", "d")));
        ClientRewardState.instance.clear();

        assertFalse(ClientRewardState.instance.visible);
        assertTrue(ClientRewardState.instance.choices.isEmpty());
    }
}
