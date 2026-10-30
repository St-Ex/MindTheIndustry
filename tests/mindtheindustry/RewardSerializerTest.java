package mindtheindustry;

import arc.struct.*;
import arc.util.io.*;
import org.junit.jupiter.api.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for reward network serialization (round-trip write → read). */
class RewardSerializerTest extends HeadlessMindTheIndustryTest{

    /** Serializes then deserializes a reward. */
    private static Reward roundTrip(Reward reward){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reward.write(new Writes(new DataOutputStream(baos)));
        return Reward.read(new Reads(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))));
    }

    private static void assertBaseFields(Reward original, Reward back){
        assertEquals(original.id, back.id);
        assertEquals(original.type, back.type);
        assertEquals(original.rarity, back.rarity);
        assertEquals(original.displayName, back.displayName);
        assertEquals(original.description, back.description);
    }

    @Test
    void tradeoffRewardRoundTrip(){
        TradeoffReward original = new TradeoffReward("unit_damage", 0.4f, "build_speed", -0.4f,
            Rarity.EPIC, "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(TradeoffReward.class, back);
        assertBaseFields(original, back);
        TradeoffReward r = (TradeoffReward)back;
        assertEquals("unit_damage", r.bonusId);
        assertEquals(0.4f, r.bonusValue, 0.0001f);
        assertEquals("build_speed", r.malusId);
        assertEquals(-0.4f, r.malusValue, 0.0001f);
        assertEquals(Rarity.EPIC, r.rarity);
    }

    @Test
    void planRewardRoundTrip(){
        Seq<String> names = Seq.with("block:spectre", "unit:flare", "block:salvo", "unit:eclipse");
        PlanReward original = new PlanReward(names, Rarity.EPIC, "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(PlanReward.class, back);
        assertBaseFields(original, back);
        PlanReward r = (PlanReward)back;
        assertEquals(names, r.contentNames);
        assertEquals(Rarity.EPIC, r.rarity);
        // names must resolve to real content
        for(String name : r.contentNames){
            assertNotNull(PlanReward.resolve(name), "unresolvable content: " + name);
        }
    }

    @Test
    void resourceRewardRoundTrip(){
        Seq<String> items = Seq.with("copper", "titanium");
        Seq<Integer> amounts = Seq.with(400, 400);
        ResourceReward original = new ResourceReward(items, amounts, Rarity.RARE, "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(ResourceReward.class, back);
        assertBaseFields(original, back);
        ResourceReward r = (ResourceReward)back;
        assertEquals(items, r.itemNames);
        assertEquals(amounts, r.amounts);
        assertEquals(Rarity.RARE, r.rarity);
    }
}
