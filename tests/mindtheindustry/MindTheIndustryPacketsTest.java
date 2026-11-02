package mindtheindustry;

import arc.struct.*;
import arc.util.io.*;
import mindustry.net.*;
import org.junit.jupiter.api.*;
import mindtheindustry.net.MindTheIndustryPackets.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the mod's network packets (round-trip write → read). */
class MindTheIndustryPacketsTest extends HeadlessMindTheIndustryTest{

    private static byte[] bytes(Packet p){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.write(new Writes(new DataOutputStream(baos)));
        return baos.toByteArray();
    }

    private static <T extends Packet> T roundTrip(T original, T back){
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes(original)))));
        return back;
    }

    @Test
    void rewardChoicesPacketRoundTrip(){
        Seq<Reward> choices = Seq.with(
            new TradeoffReward("unit_damage", 0.2f, "build_speed", -0.2f, Rarity.RARE, "n1", "d1"),
            new PlanReward(Seq.with("block:salvo", "unit:flare"), Rarity.NORMAL, "n2", "d2"),
            new ResourceReward(Seq.with("copper"), Seq.with(200), Rarity.EPIC, "n3", "d3")
        );
        RewardChoicesPacket original = new RewardChoicesPacket(7, choices, false);

        RewardChoicesPacket back = roundTrip(original, new RewardChoicesPacket());

        assertEquals(7, back.wave);
        assertFalse(back.relic);
        assertEquals(3, back.choices.size);
        for(int i = 0; i < 3; i++){
            assertEquals(original.choices.get(i).id, back.choices.get(i).id);
            assertEquals(original.choices.get(i).type, back.choices.get(i).type);
            assertEquals(original.choices.get(i).rarity, back.choices.get(i).rarity);
            assertEquals(original.choices.get(i).displayName, back.choices.get(i).displayName);
        }
        // type-specific fields survive
        TradeoffReward t = (TradeoffReward)back.choices.get(0);
        assertEquals("unit_damage", t.bonusId);
        assertEquals(0.2f, t.bonusValue, 0.0001f);
        PlanReward p = (PlanReward)back.choices.get(1);
        assertEquals(Seq.with("block:salvo", "unit:flare"), p.contentNames);
        ResourceReward r = (ResourceReward)back.choices.get(2);
        assertEquals(Seq.with("copper"), r.itemNames);
        assertEquals(Seq.with(200), r.amounts);
    }

    @Test
    void relicChoicesPacketCarriesTheRelicFlag(){
        RewardChoicesPacket original = new RewardChoicesPacket(10, Seq.with(
            new ResourceReward(Seq.with("copper"), Seq.with(800), Rarity.EPIC, "n", "d")
        ), true);

        RewardChoicesPacket back = roundTrip(original, new RewardChoicesPacket());

        assertTrue(back.relic, "relic flag survives the round trip");
        assertEquals(10, back.wave);
        assertEquals(1, back.choices.size);
    }

    @Test
    void emptyChoicesPacketRoundTrip(){
        RewardChoicesPacket back = roundTrip(new RewardChoicesPacket(3, new Seq<>(), false), new RewardChoicesPacket());

        assertEquals(3, back.wave);
        assertTrue(back.choices.isEmpty(), "empty list = hide the choice box");
    }

    @Test
    void rewardVotePacketRoundTrip(){
        RewardVotePacket back = roundTrip(new RewardVotePacket(2), new RewardVotePacket());
        assertEquals(2, back.index);

        RewardVotePacket none = roundTrip(new RewardVotePacket(-1), new RewardVotePacket());
        assertEquals(-1, none.index, "-1 = explicit take none (conversion)");
    }

    @Test
    void bossWindowPacketRoundTrip(){
        BossWindowPacket open = roundTrip(new BossWindowPacket(true, 287.5f, Seq.with(
            new ResourceReward(Seq.with("copper"), Seq.with(400), Rarity.EPIC, "n", "d")
        )), new BossWindowPacket());
        assertTrue(open.open);
        assertEquals(287.5f, open.timeLeft, 0.001f);
        assertEquals(1, open.taken.size, "taken rewards (tradeable) ride along");
        assertEquals("resource:copper:400", open.taken.first().id);

        BossWindowPacket closed = roundTrip(new BossWindowPacket(false, 0f, new Seq<>()), new BossWindowPacket());
        assertFalse(closed.open);
        assertTrue(closed.taken.isEmpty());
    }

    @Test
    void readyPacketRoundTrip(){
        ReadyPacket back = roundTrip(new ReadyPacket(), new ReadyPacket());
        assertNotNull(back);
    }

    @Test
    void tradeOfferPacketRoundTrip(){
        TradeOfferPacket back = roundTrip(new TradeOfferPacket("two", "resource:copper:400"), new TradeOfferPacket());
        assertEquals("two", back.targetName);
        assertEquals("resource:copper:400", back.rewardId);
    }

    @Test
    void tradeConfirmPacketRoundTrip(){
        TradeConfirmPacket back = roundTrip(new TradeConfirmPacket(), new TradeConfirmPacket());
        assertNotNull(back);
    }

    @Test
    void switchPacketRoundTrip(){
        SwitchPacket back = roundTrip(new SwitchPacket(1234, true, "two"), new SwitchPacket());
        assertEquals(1234, back.entityId);
        assertTrue(back.isBuilding);
        assertEquals("two", back.targetName);

        SwitchPacket clear = roundTrip(new SwitchPacket(5, false, "none"), new SwitchPacket());
        assertEquals("none", clear.targetName);
        assertFalse(clear.isBuilding);
    }
}
