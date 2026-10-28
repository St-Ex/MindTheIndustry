package rogueindustry.core;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import rogueindustry.net.*;
import rogueindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Contrôleur serveur du mode RogueIndustry.
 *
 * Responsabilités:
 * - Démarrer/arrêter une run
 * - Détecter la fin d'une vague (tous les ennemis tués)
 * - Générer et envoyer les choix de récompenses à chaque joueur
 * - Recevoir les votes des joueurs et appliquer les récompenses
 * - Gérer la progression des vagues
 *
 * Ce contrôleur n'existe que côté serveur (ou en solo, où le joueur local fait aussi serveur).
 */
public class ServerRogueController{

    /** Session serveur (état de la run). */
    public final RogueSession session = new RogueSession();

    /** Générateur de récompenses. */
    private RewardGenerator generator;

    /** Les choix actuels par joueur (en attente de vote). */
    private final ObjectMap<Player, Seq<Reward>> pendingChoices = new ObjectMap<>();

    /** Les votes reçus pour la vague en cours. */
    private final ObjectMap<Player, Integer> votes = new ObjectMap<>();

    /** Vrai si on attend que les joueurs votent. */
    private boolean waitingForVotes = false;

    /** Vague en cours de traitement (pour éviter les doubles déclenchements). */
    private int lastProcessedWave = 0;

    /** Timer pour vérifier régulièrement s'il reste des ennemis. */
    private float checkTimer = 0f;
    private static final float CHECK_INTERVAL = 1f; // secondes

    // --- Sélection commandant (début de run) ---

    /** Les 3 choix de commandant par joueur (en attente de vote). */
    private final ObjectMap<Player, Seq<rogueindustry.meta.Commander>> pendingCommanderChoices = new ObjectMap<>();

    /** Les commandants choisis par joueur. */
    private final ObjectMap<Player, rogueindustry.meta.Commander> commanderVotes = new ObjectMap<>();

    /** Vrai si on attend les votes de commandant. */
    private boolean waitingForCommanderVotes = false;

    /** Applique les bonus de joueur actif aux stats du jeu. */
    private BonusApplier bonusApplier;

    public ServerRogueController(){
        // Écouter le début de partie
        Events.on(PlayEvent.class, e -> {
            if(!state.rules.waves) return;
            // Le mode rogue est activé manuellement via une commande ou un bouton
        });

        // Nettoyer les joueurs actifs quand une entité est détruite
        setupEntityCleanup();

        // Assigner le propriétaire par défaut à la création
        setupDefaultOwner();

        // Appliquer les bonus de joueur actif aux stats du jeu
        bonusApplier = new BonusApplier(session);

        // Démarrer automatiquement le mode rogue si demandé (bouton menu principal)
        Events.on(PlayEvent.class, e -> {
            if(rogueindustry.RogueIndustryMod.autoStartRogue && !session.active){
                rogueindustry.RogueIndustryMod.autoStartRogue = false;
                Log.info("[RogueIndustry] Démarrage automatique du mode rogue (flag menu).");
                // Délai court pour laisser la partie s'initialiser
                Timer.schedule(() -> startCommanderSelection(), 1f);
            }
        });

        // Vérifier régulièrement s'il reste des ennemis
        Events.run(Trigger.update, () -> {
            if(!session.active || waitingForVotes || waitingForCommanderVotes) return;

            checkTimer += Time.delta;
            if(checkTimer < CHECK_INTERVAL) return;
            checkTimer = 0f;

            checkWaveComplete();
        });

        // Pendant la phase de vote: désactiver tous les bâtiments (arrêt production)
        Events.run(Trigger.update, () -> {
            if(!session.active || !waitingForVotes) return;
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = false;
                }
            }
        });

        // Quand une nouvelle vague démarre, incrémenter le compteur
        Events.on(WaveEvent.class, e -> {
            if(!session.active) return;
            // La vague vient de commencer, ne rien faire ici
            // La détection de fin se fait via checkWaveComplete
        });

        // Réinitialiser à la fin de partie
        Events.on(ResetEvent.class, e -> {
            if(session.active){
                session.stop();
            }
            pendingChoices.clear();
            votes.clear();
            waitingForVotes = false;
            waitingForCommanderVotes = false;
            pendingCommanderChoices.clear();
            commanderVotes.clear();
            lastProcessedWave = 0;
            // Réactiver tous les bâtiments
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = true;
                }
            }
        });
    }

    /**
     * Démarre le flux de sélection de commandant.
     * Envoie 3 choix aléatoires par joueur parmi les commandants débloqués.
     * Une fois tous les joueurs ayant voté, envoie le compte à rebours puis démarre la run.
     */
    public void startCommanderSelection(){
        session.start(); // initialise l'état de base (active=true, seed, etc.)
        generator = new RewardGenerator(session.rand); // créer le générateur de récompenses
        waitingForCommanderVotes = true;
        pendingCommanderChoices.clear();
        commanderVotes.clear();

        Log.info("[RogueIndustry] startCommanderSelection: joueurs=" + Groups.player.size());

        Seq<rogueindustry.meta.Commander> unlocked = rogueindustry.meta.MetaProgression.getUnlocked();
        if(unlocked.isEmpty()){
            // Fallback: utiliser les starters
            unlocked = rogueindustry.meta.CommanderRegistry.starters();
        }

        for(Player p : Groups.player){
            // Générer 3 choix aléatoires distincts pour ce joueur
            Seq<rogueindustry.meta.Commander> choices = new Seq<>(3);
            Seq<rogueindustry.meta.Commander> pool = unlocked.copy();
            pool.shuffle();
            for(int i = 0; i < Math.min(3, pool.size); i++){
                choices.add(pool.get(i));
            }
            pendingCommanderChoices.put(p, choices);

            // Envoyer les choix au client
            RoguePackets.CommanderChoicesPacket packet = new RoguePackets.CommanderChoicesPacket();
            packet.commanderIds = new String[choices.size];
            for(int i = 0; i < choices.size; i++){
                packet.commanderIds[i] = choices.get(i).id;
            }
            if(p.con != null){
                p.con.send(packet, true);
            }else{
                // En solo, exécuter sur le thread client (UI thread)
                Log.info("[RogueIndustry] Solo: Core.app.post pour CommanderChoicesPacket");
                Core.app.post(() -> packet.handleClient());
            }
        }

        Log.info("[RogueIndustry] Sélection de commandant lancée. " + pendingCommanderChoices.size + " joueurs.");
        // Note: pas d'auto-vote en solo, le joueur choisit via le dialogue
    }

    /** Gère le vote d'un joueur pour son commandant. */
    public void handleCommanderVote(Player player, int choiceIndex){
        if(!waitingForCommanderVotes) return;
        if(!pendingCommanderChoices.containsKey(player)) return;

        Seq<rogueindustry.meta.Commander> choices = pendingCommanderChoices.get(player);
        if(choiceIndex < 0 || choiceIndex >= choices.size) return;

        rogueindustry.meta.Commander chosen = choices.get(choiceIndex);
        commanderVotes.put(player, chosen);
        pendingCommanderChoices.remove(player);

        Log.info("[RogueIndustry] " + player.name + " a choisi le commandant: " + chosen.displayName);
        player.sendMessage("[accent]Commandant choisi: " + chosen.displayName);
        player.sendMessage("[gray]Pouvoir: " + chosen.ability.name() + " - " + chosen.ability.description());

        checkAllCommanderVotesReceived();
    }

    /** Vérifie si tous les joueurs ont voté pour leur commandant. */
    private void checkAllCommanderVotesReceived(){
        int totalPlayers = Groups.player.size();

        if(commanderVotes.size >= totalPlayers){
            waitingForCommanderVotes = false;
            finalizeCommanderSelection();
        }
    }

    /** Applique les commandants choisis et démarre le compte à rebours. */
    private void finalizeCommanderSelection(){
        // Appliquer le commandant de chaque joueur
        for(var entry : commanderVotes.entries()){
            Player p = entry.key;
            rogueindustry.meta.Commander cmdr = entry.value;
            // Stocker le commandant dans la session (pour le HUD et les effets)
            // Note: en co-op, chaque joueur a son propre commandant
            // On stocke dans une map ou on applique directement
            Log.info("[RogueIndustry] Commandant de " + p.name + ": " + cmdr.displayName);
            cmdr.ability.apply(session);
        }

        // Notifier tous les clients que la run démarre
        RoguePackets.RogueStartPacket packet = new RoguePackets.RogueStartPacket();
        packet.seed = session.seed;
        packet.wave = session.wave;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            // En solo, exécuter sur le thread client
            Log.info("[RogueIndustry] Solo: Core.app.post pour RogueStartPacket");
            Core.app.post(() -> packet.handleClient());
        }

        // Envoyer le compte à rebours (10 secondes)
        RoguePackets.CountdownPacket countdown = new RoguePackets.CountdownPacket();
        countdown.seconds = 10;
        sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(countdown, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[RogueIndustry] Solo: Core.app.post pour CountdownPacket");
            Core.app.post(() -> countdown.handleClient());
        }

        Log.info("[RogueIndustry] Tous les commandants choisis. Compte à rebours de 10s avant la vague 1.");

        // Démarrer la première vague après le compte à rebours
        Timer.schedule(() -> {
            if(!session.active) return;
            session.nextWave();
            state.wave = session.wave;
            state.rules.waveTimer = true;
            state.rules.waves = true;
            Log.info("[RogueIndustry] Vague 1 lancée!");
        }, 10f);
    }

    /** Démarre une nouvelle run rogue (après sélection commandant). */
    public void startRun(){
        session.start();
        generator = new RewardGenerator(session.rand);
        lastProcessedWave = 0;
        waitingForVotes = false;

        // Notifier tous les clients
        RoguePackets.RogueStartPacket packet = new RoguePackets.RogueStartPacket();
        packet.seed = session.seed;
        packet.wave = session.wave;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[RogueIndustry] Solo: Core.app.post pour RogueStartPacket (startRun)");
            Core.app.post(() -> packet.handleClient());
        }

        Log.info("[RogueIndustry] Run démarrée côté serveur. Seed=" + session.seed);

        // Démarrer la première vague
        session.nextWave();
        state.wave = session.wave;
        state.rules.waveTimer = true;
        state.rules.waves = true;
    }

    /** Vérifie si la vague actuelle est terminée (plus d'ennemis). */
    private void checkWaveComplete(){
        // Ne pas déclencher si on a déjà traité cette vague
        if(session.wave <= lastProcessedWave) return;

        // Compter les ennemis vivants
        int enemies = Groups.unit.count(u -> u.team == state.rules.waveTeam && u.isValid());

        // Compter aussi les bâtiments ennemis (cores ennemies)
        int enemyCores = Groups.build.count(b -> b.team == state.rules.waveTeam && b instanceof mindustry.world.blocks.storage.CoreBlock.CoreBuild);

        if(enemies == 0 && enemyCores == 0){
            lastProcessedWave = session.wave;
            onWaveComplete();
        }
    }

    /** Appelé quand une vague est terminée (tous les ennemis tués). */
    private void onWaveComplete(){
        Log.info("[RogueIndustry] Vague " + session.wave + " terminée!");

        waitingForVotes = true;
        votes.clear();
        pendingChoices.clear();

        // Phase de stratégie: pas de nouvelle vague, les joueurs votent à leur rythme
        state.rules.waveTimer = false;
        // Générer et envoyer les choix à chaque joueur
        for(Player player : Groups.player){
            if(player.dead()) continue;

            Seq<Reward> choices = generator.generateChoices(session, player, session.wave);
            pendingChoices.put(player, choices);

            // Envoyer les choix au client
            RoguePackets.RewardChoicesPacket packet = new RoguePackets.RewardChoicesPacket();
            packet.wave = session.wave;
            packet.choices = choices;
            if(player.con != null){
                player.con.send(packet, true);
            }else{
                // En solo, exécuter sur le thread client
                Log.info("[RogueIndustry] Solo: Core.app.post pour RewardChoicesPacket");
                Core.app.post(() -> packet.handleClient());
            }
        }
    }

    /**
     * Traite le vote récompense d'un joueur (appelé via /rogue reward <index>).
     * En solo, la cible est le joueur lui-même par défaut.
     */
    public void handleRewardVote(Player player, int choiceIndex){
        // En solo, la cible est le joueur lui-même
        handleVote(player, choiceIndex, player);
    }

    /**
     * Définit le joueur actif pour une entité (appelé via /rogue active).
     */
    public void setActivePlayer(int entityId, boolean isBuilding, Player target){
        if(isBuilding){
            Building building = Groups.build.getByID(entityId);
            if(building != null){
                session.setActivePlayer(building, target);
                Log.info("[RogueIndustry] Joueur actif défini pour bâtiment #" + entityId + ": " +
                    (target != null ? target.name : "aucun"));
            }
        }else{
            Unit unit = Groups.unit.getByID(entityId);
            if(unit != null){
                session.setActivePlayer(unit, target);
                Log.info("[RogueIndustry] Joueur actif défini pour unité #" + entityId + ": " +
                    (target != null ? target.name : "aucun"));
            }
        }
    }

    /**
     * Traite le vote d'un joueur.
     *
     * @param player      le joueur qui vote
     * @param choiceIndex l'index du choix (0-2)
     * @param target      le joueur cible (pour UnitBonus; null sinon)
     */
    public void handleVote(Player player, int choiceIndex, Player target){
        if(!waitingForVotes) return;
        if(!pendingChoices.containsKey(player)) return;

        Seq<Reward> choices = pendingChoices.get(player);
        if(choiceIndex < 0 || choiceIndex >= choices.size) return;

        Reward reward = choices.get(choiceIndex);

        // Retirer les choix en attente pour ce joueur
        pendingChoices.remove(player);
        votes.put(player, choiceIndex);

        // Appliquer la récompense
        reward.apply(session, player, target);

        // Notifier tous les clients
        RoguePackets.RewardAppliedPacket packet = new RoguePackets.RewardAppliedPacket();
        packet.playerName = player.name;
        packet.rewardName = reward.displayName;
        packet.rewardType = reward.type.name();
        packet.targetName = target != null ? target.name : null;
        boolean sent = false;
        for(Player p : Groups.player){
            if(p.con != null){
                p.con.send(packet, true);
                sent = true;
            }
        }
        if(!sent){
            Log.info("[RogueIndustry] Solo: Core.app.post pour RewardAppliedPacket");
            Core.app.post(() -> packet.handleClient());
        }

        Log.info("[RogueIndustry] " + player.name + " a choisi: " + reward.displayName +
                 (target != null ? " (cible: " + target.name + ")" : ""));

        // Si tous les joueurs ont voté, passer à la vague suivante
        checkAllVotesReceived();
    }

    /** Vérifie si tous les joueurs ont voté. Si oui, passe à la vague suivante. */
    private void checkAllVotesReceived(){
        int alivePlayers = 0;
        for(Player p : Groups.player){
            if(!p.dead()) alivePlayers++;
        }

        if(votes.size >= alivePlayers){
            // Tous ont voté, reprendre le jeu et lancer la vague suivante
            waitingForVotes = false;
            session.nextWave();
            state.wave = session.wave;

            // Réactiver le timer de vague
            state.rules.waveTimer = true;

            // Réactiver tous les bâtiments
            for(Building b : Groups.build){
                if(b.team == state.rules.defaultTeam){
                    b.enabled = true;
                }
            }

            Log.info("[RogueIndustry] Tous les votes reçus. Vague " + session.wave + " lancée.");
        }
    }

    /** Force le passage à la vague suivante (commande admin / debug). */
    public void skipToNextWave(){
        if(!session.active) return;

        // Tuer tous les ennemis restants
        Groups.unit.each(u -> {
            if(u.team == state.rules.waveTeam){
                u.kill();
            }
        });

        lastProcessedWave = session.wave;
        onWaveComplete();
    }

    /** Arrête la run en cours. */
    public void stopRun(){
        session.stop();
        waitingForVotes = false;
        waitingForCommanderVotes = false;
        pendingChoices.clear();
        votes.clear();
        pendingCommanderChoices.clear();
        commanderVotes.clear();
        state.rules.waveTimer = true;
        // Réactiver tous les bâtiments
        for(Building b : Groups.build){
            if(b.team == state.rules.defaultTeam){
                b.enabled = true;
            }
        }
    }

    // --- Joueur actif par entité ---

    /** Applique un changement de joueur actif sur une ou plusieurs entités. */
    public void handleSetActivePlayer(int[] entityIds, boolean isBuilding, int playerId){
        Player player = playerId >= 0 ? Groups.player.find(p -> p.id == playerId) : null;

        for(int entityId : entityIds){
            if(isBuilding){
                Building building = Groups.build.getByID(entityId);
                if(building != null){
                    session.setActivePlayer(building, player);
                }
            }else{
                Unit unit = Groups.unit.getByID(entityId);
                if(unit != null){
                    session.setActivePlayer(unit, player);
                }
            }
        }
    }

    /** Assigne le propriétaire par défaut quand une entité est créée. */
    public void setupDefaultOwner(){
        // Bâtiment construit → propriétaire = constructeur
        Events.on(BlockBuildEndEvent.class, e -> {
            if(!session.active) return;
            if(e.unit != null && e.unit.isPlayer()){
                Player builder = e.unit.getPlayer();
                if(builder != null && e.tile.build != null){
                    // Ne pas écraser si un joueur actif est déjà assigné
                    if(session.getActivePlayer(e.tile.build) == null){
                        session.setActivePlayer(e.tile.build, builder);
                    }
                }
            }
        });

        // Unité créée → propriétaire = joueur qui la contrôle
        Events.on(UnitSpawnEvent.class, e -> {
            if(!session.active) return;
            if(e.unit.isPlayer()){
                Player owner = e.unit.getPlayer();
                if(owner != null && session.getActivePlayer(e.unit) == null){
                    session.setActivePlayer(e.unit, owner);
                }
            }
        });
    }

    // --- Sélection commandant (début de run) ---
    // Les méthodes handleCommanderVote, startCommanderSelection, etc. sont définies plus haut.

    /** Nettoie les joueurs actifs quand une entité est détruite. */
    public void setupEntityCleanup(){
        Events.on(mindustry.game.EventType.BlockDestroyEvent.class, e -> {
            if(session.active && e.tile.build != null){
                session.clearActivePlayer(e.tile.build);
            }
        });
        Events.on(mindustry.game.EventType.UnitDestroyEvent.class, e -> {
            if(session.active){
                session.clearActivePlayer(e.unit);
            }
        });
    }
}
