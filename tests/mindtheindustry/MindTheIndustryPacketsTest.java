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

    @Test
    void rewardChoicesPacketRoundTrip(){
        Seq<Reward> choices = Seq.with(
            new TradeoffReward("unit_damage", 0.2f, "build_speed", -0.2f, Rarity.RARE, "n1", "d1"),
            new PlanReward(Seq.with("block:salvo", "unit:flare"), Rarity.NORMAL, "n2", "d2"),
            new ResourceReward(Seq.with("copper"), Seq.with(200), Rarity.EPIC, "n3", "d3")
        );
        RewardChoicesPacket original = new RewardChoicesPacket(7, choices);

        RewardChoicesPacket back = new RewardChoicesPacket();
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes(original)))));

        assertEquals(7, back.wave);
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
    void emptyChoicesPacketRoundTrip(){
        RewardChoicesPacket original = new RewardChoicesPacket(3, new Seq<>());
        RewardChoicesPacket back = new RewardChoicesPacket();
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes(original)))));

        assertEquals(3, back.wave);
        assertTrue(back.choices.isEmpty(), "empty list = hide the choice box");
    }

    @Test
    void rewardVotePacketRoundTrip(){
        RewardVotePacket back = new RewardVotePacket();
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes(new RewardVotePacket(2))))));
        assertEquals(2, back.index);
    }
}
