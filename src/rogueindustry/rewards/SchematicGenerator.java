package rogueindustry.rewards;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

/**
 * Generates procedural schematics for rewards.
 * Creates interesting building patterns (turrets, factories, defenses).
 */
public class SchematicGenerator{

    /** List of predefined schematics (name + size + content). */
    private static final Seq<SchematicDef> presets = new Seq<>();

    static{
        // Pattern: 2x2 Duo turrets
        presets.add(new SchematicDef(
            "Duo Battery",
            "A battery of 4 Duos to defend a hot spot.",
            2, 2,
            new String[][]{
                {"duo", "duo"},
                {"duo", "duo"}
            }
        ));

        // Pattern: Scorch line
        presets.add(new SchematicDef(
            "Scorch Line",
            "3 Scorches in a row to burn enemies.",
            3, 1,
            new String[][]{
                {"scorch", "scorch", "scorch"}
            }
        ));

        // Pattern: 2x2 Hail
        presets.add(new SchematicDef(
            "Hail Barrage",
            "4 Hails for an artillery barrage.",
            2, 2,
            new String[][]{
                {"hail", "hail"},
                {"hail", "hail"}
            }
        ));

        // Pattern: Graphite factory
        presets.add(new SchematicDef(
            "Graphite Press",
            "2 Graphite Presses with conveyors.",
            3, 2,
            new String[][]{
                {"conveyor", "graphite-press", "conveyor"},
                {"conveyor", "graphite-press", "conveyor"}
            }
        ));

        // Pattern: Copper wall
        presets.add(new SchematicDef(
            "Copper Wall Pack",
            "A line of copper walls to get protected quickly.",
            5, 1,
            new String[][]{
                {"copper-wall", "copper-wall", "copper-wall", "copper-wall", "copper-wall"}
            }
        ));

        // Pattern: Power node
        presets.add(new SchematicDef(
            "Power Node Cluster",
            "3 Power Nodes connected to power an area.",
            3, 1,
            new String[][]{
                {"power-node", "power-node", "power-node"}
            }
        ));

        // Pattern: Drill row
        presets.add(new SchematicDef(
            "Drill Row",
            "3 Mechanical Drills in a row.",
            3, 1,
            new String[][]{
                {"mechanical-drill", "mechanical-drill", "mechanical-drill"}
            }
        ));

        // Pattern: Salvo pair
        presets.add(new SchematicDef(
            "Salvo Pair",
            "2 Salvos for serious damage.",
            2, 1,
            new String[][]{
                {"salvo", "salvo"}
            }
        ));

        // Pattern: Mender + Projector
        presets.add(new SchematicDef(
            "Repair Station",
            "Mend Projector + Overdrive Projector to heal and boost.",
            2, 1,
            new String[][]{
                {"mend-projector", "overdrive-projector"}
            }
        ));

        // Pattern: 3x3 Scatter
        presets.add(new SchematicDef(
            "Scatter AA",
            "4 Scatters for anti-air defense.",
            2, 2,
            new String[][]{
                {"scatter", "scatter"},
                {"scatter", "scatter"}
            }
        ));

        // Pattern: Solar farm
        presets.add(new SchematicDef(
            "Solar Farm",
            "4 Solar Panels + 1 Battery.",
            3, 2,
            new String[][]{
                {"solar-panel", "solar-panel", "solar-panel"},
                {"solar-panel", "battery", "conveyor"}
            }
        ));

        // Pattern: Mass driver pair
        presets.add(new SchematicDef(
            "Mass Driver Link",
            "2 Mass Drivers to transfer resources.",
            2, 1,
            new String[][]{
                {"mass-driver", "mass-driver"}
            }
        ));
    }

    /** Generates a random schematic among the presets. Returns null if no preset is available. */
    public static Schematic generateRandom(Rand rand){
        if(presets.isEmpty()) return null;
        SchematicDef def = presets.get(rand.random(presets.size - 1));
        return buildSchematic(def);
    }

    /** Generates a random schematic with a unique name. */
    public static Schematic generateRandom(){
        return generateRandom(new Rand());
    }

    /** Builds a Schematic from a definition. */
    private static Schematic buildSchematic(SchematicDef def){
        int width = def.width;
        int height = def.height;
        Seq<Schematic.Stile> tiles = new Seq<>(width * height);

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                String blockName = def.blocks[y][x];
                Block block = content.block(blockName);
                if(block == null){
                    Log.warn("[MindTheIndustry] Block '" + blockName + "' not found, skipping.");
                    continue;
                }
                // config null: preset blocks have no configuration (serializable, unlike a Byte)
                tiles.add(new Schematic.Stile(block, x, y, null, (byte)0));
            }
        }

        arc.struct.StringMap tags = new arc.struct.StringMap();
        tags.put("name", def.name);
        tags.put("description", def.description);

        return new Schematic(tiles, tags, width, height);
    }

    /** Serializes a schematic to base64 to send it via packet. */
    public static String toBase64(Schematic schematic){
        return schematics.writeBase64(schematic);
    }

    /** Deserializes a schematic from base64 (client-side). */
    public static Schematic fromBase64(String base64){
        return Schematics.readBase64(base64);
    }

    /** Definition of a predefined schematic. */
    private static class SchematicDef{
        final String name;
        final String description;
        final int width;
        final int height;
        final String[][] blocks; // [y][x] = block name

        SchematicDef(String name, String description, int width, int height, String[][] blocks){
            this.name = name;
            this.description = description;
            this.width = width;
            this.height = height;
            this.blocks = blocks;
        }
    }
}
