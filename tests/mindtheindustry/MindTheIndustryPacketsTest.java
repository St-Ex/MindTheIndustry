package mindtheindustry;

import arc.util.io.*;
import mindustry.net.*;
import org.junit.jupiter.api.*;
import mindtheindustry.net.MindTheIndustryPackets.*;

import java.io.*;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for custom network packet serialization (round-trip write → read).
 * Only SchematicUnlockPacket remains: everything else travels through chat + commands.
 */
class MindTheIndustryPacketsTest extends HeadlessMindTheIndustryTest{

    /** Serializes then deserializes a packet. */
    private static <T extends Packet> T roundTrip(T packet, Supplier<T> factory){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packet.write(new Writes(new DataOutputStream(baos)));
        T back = factory.get();
        back.read(new Reads(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))));
        return back;
    }

    @Test
    void schematicUnlockPacketRoundTrip(){
        SchematicUnlockPacket p = new SchematicUnlockPacket();
        p.schematicName = "fortress";
        p.schematicData = "bXN0cmluZy1kYXRhLXdpdGgtc3BlY2lhbC1jaGFycyE=";

        SchematicUnlockPacket back = roundTrip(p, SchematicUnlockPacket::new);

        assertEquals("fortress", back.schematicName);
        assertEquals(p.schematicData, back.schematicData);
    }

    @Test
    void schematicUnlockPacketRoundTripEmptyData(){
        SchematicUnlockPacket p = new SchematicUnlockPacket();
        p.schematicName = "empty";
        p.schematicData = "";

        SchematicUnlockPacket back = roundTrip(p, SchematicUnlockPacket::new);

        assertEquals("empty", back.schematicName);
        assertEquals("", back.schematicData);
    }
}
