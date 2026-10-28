package rogueindustry;

import arc.struct.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.net.*;
import org.junit.jupiter.api.*;
import rogueindustry.core.*;
import rogueindustry.net.RoguePackets.*;
import rogueindustry.rewards.*;

import java.io.*;
import java.util.function.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests de sérialisation des packets réseau custom (round-trip write → read). */
class RoguePacketsTest extends HeadlessRogueTest{

    /** Sérialise puis désérialise un packet. */
    private static <T extends Packet> T roundTrip(T packet, Supplier<T> factory){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packet.write(new Writes(new DataOutputStream(baos)));
        T back = factory.get();
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))));
        return back;
    }

    @Test
    void rogueStartPacketRoundTrip(){
        RogueStartPacket p = new RogueStartPacket();
        p.seed = 123456789012345L;
        p.wave = 7;

        RogueStartPacket back = roundTrip(p, RogueStartPacket::new);

        assertEquals(p.seed, back.seed);
        assertEquals(p.wave, back.wave);
    }

    @Test
    void rewardAppliedPacketRoundTrip(){
        RewardAppliedPacket p = new RewardAppliedPacket();
        p.playerName = "alice";
        p.rewardName = "Salvo";
        p.rewardType = "BUILDING";
        p.targetName = null;

        RewardAppliedPacket back = roundTrip(p, RewardAppliedPacket::new);

        assertEquals("alice", back.playerName);
        assertEquals("Salvo", back.rewardName);
        assertEquals("BUILDING", back.rewardType);
        assertNull(back.targetName, "un targetName null doit survivre (encodé comme chaîne vide)");
    }

    @Test
    void rogueStatePacketRoundTrip(){
        RogueStatePacket p = new RogueStatePacket();
        p.seed = 42;
        p.wave = 3;
        p.groupUnlocks = Seq.with("duo", "hail");
        p.globalModifiers = Seq.with("build_speed");

        RogueStatePacket back = roundTrip(p, RogueStatePacket::new);

        assertEquals(42, back.seed);
        assertEquals(3, back.wave);
        assertEquals(Seq.with("duo", "hail"), back.groupUnlocks);
        assertEquals(Seq.with("build_speed"), back.globalModifiers);
    }

    @Test
    void setActivePlayerPacketRoundTrip(){
        SetActivePlayerPacket p = new SetActivePlayerPacket();
        p.entityIds = new int[]{1, 2, 3};
        p.isBuilding = true;
        p.playerId = 5;

        SetActivePlayerPacket back = roundTrip(p, SetActivePlayerPacket::new);

        assertArrayEquals(new int[]{1, 2, 3}, back.entityIds);
        assertTrue(back.isBuilding);
        assertEquals(5, back.playerId);
    }

    @Test
    void rewardVotePacketRoundTrip(){
        RewardVotePacket p = new RewardVotePacket();
        p.choiceIndex = 2;
        p.targetPlayerId = -1;

        RewardVotePacket back = roundTrip(p, RewardVotePacket::new);

        assertEquals(2, back.choiceIndex);
        assertEquals(-1, back.targetPlayerId);
    }

    @Test
    void commanderChoicesPacketRoundTrip(){
        CommanderChoicesPacket p = new CommanderChoicesPacket();
        p.commanderIds = new String[]{"guerrier", "mage"};

        CommanderChoicesPacket back = roundTrip(p, CommanderChoicesPacket::new);

        assertArrayEquals(new String[]{"guerrier", "mage"}, back.commanderIds);
    }

    @Test
    void rewardChoicesPacketRoundTrip(){
        state.rules = new Rules();
        RogueSession session = new RogueSession();
        session.start(42);
        RewardGenerator gen = new RewardGenerator(session.rand);
        Seq<Reward> choices = gen.generateChoices(session, null, 3);

        RewardChoicesPacket p = new RewardChoicesPacket();
        p.wave = 3;
        p.choices = choices;

        RewardChoicesPacket back = roundTrip(p, RewardChoicesPacket::new);

        assertEquals(3, back.wave);
        assertEquals(choices.size, back.choices.size);
        for(int i = 0; i < choices.size; i++){
            assertEquals(choices.get(i).id, back.choices.get(i).id);
            assertEquals(choices.get(i).type, back.choices.get(i).type);
            assertEquals(choices.get(i).displayName, back.choices.get(i).displayName);
        }
    }
}
