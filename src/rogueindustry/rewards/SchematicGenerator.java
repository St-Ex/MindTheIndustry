package rogueindustry.rewards;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

/**
 * Génère des schematics procéduraux pour les récompenses.
 * Crée des patterns de bâtiments intéressants (tourelles, usines, défenses).
 */
public class SchematicGenerator{

    /** Liste des schematics prédéfinis (nom + taille + contenu). */
    private static final Seq<SchematicDef> presets = new Seq<>();

    static{
        // Pattern: 2x2 de tourelles Duo
        presets.add(new SchematicDef(
            "Duo Battery",
            "Une batterie de 4 Duo pour défendre un point chaud.",
            2, 2,
            new String[][]{
                {"duo", "duo"},
                {"duo", "duo"}
            }
        ));

        // Pattern: Ligne de Scorch
        presets.add(new SchematicDef(
            "Scorch Line",
            "3 Scorch en ligne pour brûler les ennemis.",
            3, 1,
            new String[][]{
                {"scorch", "scorch", "scorch"}
            }
        ));

        // Pattern: 2x2 de Hail
        presets.add(new SchematicDef(
            "Hail Barrage",
            "4 Hail pour un barrage d'artillerie.",
            2, 2,
            new String[][]{
                {"hail", "hail"},
                {"hail", "hail"}
            }
        ));

        // Pattern: Usine de graphite
        presets.add(new SchematicDef(
            "Graphite Press",
            "2 Graphite Press avec convoyeurs.",
            3, 2,
            new String[][]{
                {"conveyor", "graphite-press", "conveyor"},
                {"conveyor", "graphite-press", "conveyor"}
            }
        ));

        // Pattern: Mur de cuivre
        presets.add(new SchematicDef(
            "Copper Wall Pack",
            "Ligne de murs en cuivre pour se protéger rapidement.",
            5, 1,
            new String[][]{
                {"copper-wall", "copper-wall", "copper-wall", "copper-wall", "copper-wall"}
            }
        ));

        // Pattern: Node de power
        presets.add(new SchematicDef(
            "Power Node Cluster",
            "3 Power Nodes connectés pour alimenter une zone.",
            3, 1,
            new String[][]{
                {"power-node", "power-node", "power-node"}
            }
        ));

        // Pattern: Drill row
        presets.add(new SchematicDef(
            "Drill Row",
            "3 foreuses mécaniques en ligne.",
            3, 1,
            new String[][]{
                {"mechanical-drill", "mechanical-drill", "mechanical-drill"}
            }
        ));

        // Pattern: Salvo pair
        presets.add(new SchematicDef(
            "Salvo Pair",
            "2 Salvo pour des dégâts sérieux.",
            2, 1,
            new String[][]{
                {"salvo", "salvo"}
            }
        ));

        // Pattern: Mender + Projector
        presets.add(new SchematicDef(
            "Repair Station",
            "Mend Projector + Overdrive Projector pour soigner et booster.",
            2, 1,
            new String[][]{
                {"mend-projector", "overdrive-projector"}
            }
        ));

        // Pattern: 3x3 Scatter
        presets.add(new SchematicDef(
            "Scatter AA",
            "4 Scatter pour la défense aérienne.",
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
            "2 Mass Drivers pour transférer des ressources.",
            2, 1,
            new String[][]{
                {"mass-driver", "mass-driver"}
            }
        ));
    }

    /** Génère un schematic aléatoire parmi les presets. Retourne null si aucun preset disponible. */
    public static Schematic generateRandom(Rand rand){
        if(presets.isEmpty()) return null;
        SchematicDef def = presets.get(rand.random(presets.size - 1));
        return buildSchematic(def);
    }

    /** Génère un schematic aléatoire avec un nom unique. */
    public static Schematic generateRandom(){
        return generateRandom(new Rand());
    }

    /** Construit un Schematic à partir d'une définition. */
    private static Schematic buildSchematic(SchematicDef def){
        int width = def.width;
        int height = def.height;
        Seq<Schematic.Stile> tiles = new Seq<>(width * height);

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                String blockName = def.blocks[y][x];
                Block block = content.block(blockName);
                if(block == null){
                    Log.warn("[RogueIndustry] Block '" + blockName + "' not found, skipping.");
                    continue;
                }
                tiles.add(new Schematic.Stile(block, x, y, (byte)0, (byte)0));
            }
        }

        arc.struct.StringMap tags = new arc.struct.StringMap();
        tags.put("name", def.name);
        tags.put("description", def.description);

        return new Schematic(tiles, tags, width, height);
    }

    /** Sérialise un schematic en base64 pour l'envoyer via packet. */
    public static String toBase64(Schematic schematic){
        return schematics.writeBase64(schematic);
    }

    /** Désérialise un schematic depuis base64 (côté client). */
    public static Schematic fromBase64(String base64){
        return Schematics.readBase64(base64);
    }

    /** Définition d'un schematic prédéfini. */
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
