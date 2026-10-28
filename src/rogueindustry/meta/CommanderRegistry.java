package rogueindustry.meta;

import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.world.*;
import rogueindustry.core.*;

/**
 * Registre de tous les commandants disponibles.
 * Contient les définitions de chaque commandant avec son pouvoir unique.
 *
 * Les commandants sont débloqués progressivement en meta-progression.
 * Au début de chaque run, le joueur choisit un commandant parmi ceux débloqués.
 */
public class CommanderRegistry{

    private static final Seq<Commander> all = new Seq<>();

    static{
        // ===== TIER 1: Commandants de départ (débloqués par défaut) =====

        register(new Commander(
            "commander-vanguard",
            "[red]Vanguard",
            "Un vétéran des guerres de Serpulo. Ses unités frappent plus fort dès le départ.",
            Color.valueOf("ff6b6b"),
            new CommanderAbility(){
                public String name(){ return "Frappe d vanguard"; }
                public String description(){ return "+10% dégâts pour toutes les unités (tous joueurs)"; }
                public void apply(RogueSession session){
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "unit_damage", 0.10f, "Dégâts unités (Vanguard)"));
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.10f;
                }
            },
            "Débloqué par défaut",
            1
        ));

        register(new Commander(
            "commander-architect",
            "[green]Architecte",
            "Maître bâtisseur. Ses constructions sont plus rapides et solides.",
            Color.valueOf("26de81"),
            new CommanderAbility(){
                public String name(){ return "Génie civil"; }
                public String description(){ return "+20% vitesse de construction et +15% vie des bâtiments (tous joueurs)"; }
                public void apply(RogueSession session){
                    mindustry.Vars.state.rules.buildSpeedMultiplier *= 1.20f;
                    // +15% vie bâtiments sera géré via hook sur BlockBuildEndEvent (Phase 3)
                }
            },
            "Débloqué par défaut",
            1
        ));

        register(new Commander(
            "commander-industrialist",
            "[yellow]Industriel",
            "Magnat de l'industrie. Ses usines produisent plus vite.",
            Color.valueOf("f7b731"),
            new CommanderAbility(){
                public String name(){ return "Chaîne optimisée"; }
                public String description(){ return "+15% vitesse de production pour tous les joueurs"; }
                public void apply(RogueSession session){
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.15f, "Vitesse production (Industriel)"));
                }
            },
            "Débloqué par défaut",
            1
        ));

        // ===== TIER 2: Débloqués en atteignant la vague 10 =====

        register(new Commander(
            "commander-overmind",
            "[purple]Overmind IA",
            "Une intelligence artificielle stratégique. Débloque des options avancées dès le départ.",
            Color.valueOf("a55eea"),
            new CommanderAbility(){
                public String name(){ return "Protocole avancé"; }
                public String description(){ return "Commence avec [accent]Salvo[] et [accent]Multi-Press[] débloqués pour tous"; }
                public void apply(RogueSession session){
                    Block salvo = mindustry.Vars.content.block("salvo");
                    Block multiPress = mindustry.Vars.content.block("multi-press");
                    if(salvo != null) session.unlockForGroup(salvo);
                    if(multiPress != null) session.unlockForGroup(multiPress);
                }
            },
            "Atteindre la vague 10",
            2
        ));

        register(new Commander(
            "commander-fortress",
            "[blue]Forteresse",
            "Spécialiste défensif. Ses défenses sont surpuissantes.",
            Color.valueOf("4a90d9"),
            new CommanderAbility(){
                public String name(){ return "Muraille imprenable"; }
                public String description(){ return "+25% dégâts et +25% vie pour toutes les tourelles (tous joueurs)"; }
                public void apply(RogueSession session){
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "turret_damage", 0.25f, "Dégâts tourelles (Forteresse)"));
                }
            },
            "Atteindre la vague 10",
            2
        ));

        register(new Commander(
            "commander-merchant",
            "[orange]Marchand",
            "Homme d'affaires interplanétaire. Les ressources arrivent plus vite.",
            Color.valueOf("ff9f43"),
            new CommanderAbility(){
                public String name(){ return "Logistique express"; }
                public String description(){ return "+20% rendement des foreuses et +100 ressources bonus au départ"; }
                public void apply(RogueSession session){
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "resource_yield", 0.20f, "Rendement foreuses (Marchand)"));
                    // Les 100 ressources bonus seront gérées dans le démarrage de la run (Phase 3)
                }
            },
            "Atteindre la vague 10",
            2
        ));

        // ===== TIER 3: Débloqués en atteignant la vague 20 =====

        register(new Commander(
            "commander-warlord",
            "[scarlet]Seigneur de guerre",
            "Général légendaire. Ses armées sont redoutables.",
            Color.valueOf("c0392b"),
            new CommanderAbility(){
                public String name(){ return "Armée de fer"; }
                public String description(){ return "+20% vie, +15% dégâts, +10% vitesse pour toutes les unités (tous joueurs)"; }
                public void apply(RogueSession session){
                    mindustry.Vars.state.rules.unitHealthMultiplier *= 1.20f;
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.15f;
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "unit_damage", 0.15f, "Dégâts unités (Warlord)"));
                }
            },
            "Atteindre la vague 20",
            3
        ));

        register(new Commander(
            "commander-overclocker",
            "[cyan]Overclocker",
            "Ingénieur fou. Ses machines dépassent leurs limites.",
            Color.valueOf("00d2d3"),
            new CommanderAbility(){
                public String name(){ return "Overclocking"; }
                public String description(){ return "+30% vitesse de tir des tourelles, +20% vitesse de production (tous joueurs)"; }
                public void apply(RogueSession session){
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "turret_reload", 0.30f, "Vitesse tir (Overclocker)"));
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.20f, "Vitesse production (Overclocker)"));
                }
            },
            "Atteindre la vague 20",
            3
        ));

        // ===== TIER 4: Débloqués en gagnant une run (vague 30+) =====

        register(new Commander(
            "commander-immortal",
            "[gold]Immortel",
            "Être d'une autre époque. Son pouvoir dépasse l'entendement.",
            Color.valueOf("f9ca24"),
            new CommanderAbility(){
                public String name(){ return "Immortalité"; }
                public String description(){ return "+50% à TOUTES les stats (dégâts, vie, vitesse, production, construction)"; }
                public void apply(RogueSession session){
                    mindustry.Vars.state.rules.unitDamageMultiplier *= 1.50f;
                    mindustry.Vars.state.rules.unitHealthMultiplier *= 1.50f;
                    mindustry.Vars.state.rules.buildSpeedMultiplier *= 1.50f;
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "prod_speed", 0.50f, "Vitesse production (Immortel)"));
                    session.getGlobalModifiers().add(new rogueindustry.modifiers.GlobalModifier(
                        "turret_damage", 0.50f, "Dégâts tourelles (Immortel)"));
                }
            },
            "Gagner une run (vague 30+)",
            4
        ));
    }

    private static void register(Commander commander){
        all.add(commander);
    }

    /** Retourne tous les commandants. */
    public static Seq<Commander> all(){
        return all;
    }

    /** Retourne un commandant par son ID. */
    public static Commander get(String id){
        return all.find(c -> c.id.equals(id));
    }

    /** Retourne les commandants d'un tier spécifique. */
    public static Seq<Commander> byTier(int tier){
        return all.select(c -> c.tier == tier);
    }

    /** Retourne les commandants de départ (tier 1, toujours débloqués). */
    public static Seq<Commander> starters(){
        return byTier(1);
    }
}
