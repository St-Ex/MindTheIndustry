package mindtheindustry;

import arc.math.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import mindtheindustry.rewards.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the procedural schematic generator (rewards). */
class SchematicGeneratorTest extends HeadlessMindTheIndustryTest{

    @Test
    void generatesNonEmptySchematic(){
        Schematic schematic = SchematicGenerator.generateRandom(new Rand(42));

        assertNotNull(schematic);
        assertTrue(schematic.tiles.size > 0);
        assertTrue(schematic.width > 0);
        assertTrue(schematic.height > 0);
        assertFalse(schematic.name().isEmpty());
    }

    @Test
    void deterministicForSameSeed(){
        Schematic a = SchematicGenerator.generateRandom(new Rand(99));
        Schematic b = SchematicGenerator.generateRandom(new Rand(99));

        assertEquals(a.name(), b.name());
        assertEquals(a.tiles.size, b.tiles.size);
        assertEquals(a.width, b.width);
        assertEquals(a.height, b.height);
    }

    @Test
    void base64RoundTrip(){
        Schematic schematic = SchematicGenerator.generateRandom(new Rand(7));

        String base64 = SchematicGenerator.toBase64(schematic);
        assertFalse(base64.isEmpty());

        Schematic back = SchematicGenerator.fromBase64(base64);
        assertNotNull(back);
        assertEquals(schematic.tiles.size, back.tiles.size);
        assertEquals(schematic.width, back.width);
        assertEquals(schematic.height, back.height);
        assertEquals(schematic.name(), back.name());
    }
}
