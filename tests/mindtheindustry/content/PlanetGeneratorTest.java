package mindtheindustry.content;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MindTheIndustry planet generator: a non-zero seedOffset is kept
 * (deterministic, used by the map editor), a zero seedOffset is randomized so
 * each launch of the sector produces a fresh map.
 */
class PlanetGeneratorTest extends mindtheindustry.HeadlessMindTheIndustryTest{

    /** Map generation indexes ores through the BlockIndexer; it needs a filled world and a WorldLoadEvent. */
    @BeforeAll
    static void setupWorld(){
        world.beginMapLoad();
        world.resize(96, 96);
        for(int y = 0; y < 96; y++){
            for(int x = 0; x < 96; x++){
                world.tiles.set(x, y, new Tile(x, y));
            }
        }
        world.endMapLoad(); // fires WorldLoadEvent → allocates the BlockIndexer ore tables
    }

    /** A sector with the default (empty) rect, on the vanilla serpulo planet. */
    private Sector testSector(){
        return new Sector(Planets.serpulo, Ptile.empty);
    }

    private String mapSignature(Tiles tiles){
        StringBuilder sb = new StringBuilder();
        for(int y = 0; y < tiles.height; y++){
            for(int x = 0; x < tiles.width; x++){
                Tile t = tiles.get(x, y);
                sb.append(t.floor().id).append(',').append(t.overlay().id).append(',').append(t.block().id).append(';');
            }
        }
        return sb.toString();
    }

    @Test
    void nonZeroSeedOffsetIsKeptAndDeterministic(){
        state.rules = new Rules();
        MindTheIndustryPlanetGenerator gen = new MindTheIndustryPlanetGenerator();
        Sector sector = testSector();

        WorldParams params = new WorldParams();
        params.seedOffset = 12345;

        Tiles a = new Tiles(96, 96);
        gen.generate(a, sector, params);
        assertEquals(12345, params.seedOffset, "an explicit seedOffset (map editor) must be kept");

        Tiles b = new Tiles(96, 96);
        gen.generate(b, sector, params);

        assertEquals(mapSignature(a), mapSignature(b), "same seedOffset must generate the identical map");
    }

    @Test
    void zeroSeedOffsetIsRandomized(){
        state.rules = new Rules();
        MindTheIndustryPlanetGenerator gen = new MindTheIndustryPlanetGenerator();
        Sector sector = testSector();

        WorldParams params = new WorldParams();
        assertEquals(0, params.seedOffset);

        Tiles tiles = new Tiles(96, 96);
        gen.generate(tiles, sector, params);

        assertTrue(params.seedOffset != 0, "a normal launch (seedOffset 0) must get a fresh random offset");
    }

    @Test
    void differentSeedOffsetsGenerateDifferentMaps(){
        state.rules = new Rules();
        MindTheIndustryPlanetGenerator gen = new MindTheIndustryPlanetGenerator();
        Sector sector = testSector();

        Tiles a = new Tiles(96, 96);
        WorldParams pa = new WorldParams();
        pa.seedOffset = 1;
        gen.generate(a, sector, pa);

        Tiles b = new Tiles(96, 96);
        WorldParams pb = new WorldParams();
        pb.seedOffset = 2;
        gen.generate(b, sector, pb);

        assertNotEquals(mapSignature(a), mapSignature(b), "different seedOffsets must generate different maps");
    }

    @Test
    void generatedMapIsNotEmpty(){
        state.rules = new Rules();
        MindTheIndustryPlanetGenerator gen = new MindTheIndustryPlanetGenerator();

        Tiles tiles = new Tiles(96, 96);
        WorldParams params = new WorldParams();
        params.seedOffset = 7;
        gen.generate(tiles, testSector(), params);

        boolean hasFloor = false;
        for(int y = 0; y < tiles.height && !hasFloor; y++){
            for(int x = 0; x < tiles.width; x++){
                Tile t = tiles.get(x, y);
                if(t.floor() != mindustry.content.Blocks.air || t.block() != mindustry.content.Blocks.air){
                    hasFloor = true;
                    break;
                }
            }
        }
        assertTrue(hasFloor, "the generated map must contain non-air tiles");
    }
}
