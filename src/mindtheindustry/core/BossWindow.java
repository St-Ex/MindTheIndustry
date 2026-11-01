package mindtheindustry.core;

import arc.struct.*;
import mindustry.gen.*;

/**
 * Boss window (ubiquitous language): the chapter start break opened by a boss
 * kill — a ceasefire (wave countdown frozen, no spawns; the world keeps
 * simulating), hosting relic choices per player, exactly one trade of an
 * already-taken reward, and entity assignment switches.
 *
 * The break is a cap, not a wait: it closes early when every connected player
 * is ready, or at {@link RunConfig#bossWindowTimeout}.
 */
public class BossWindow{

    /** Relic choices per player (rare/boosted pool), revealed at window open. */
    public final ObjectMap<Player, ChoiceState> relicChoices = new ObjectMap<>();

    /** Players who signalled ready. */
    public final ObjectSet<Player> readyPlayers = new ObjectSet<>();

    /** Whether the single trade of this window was used. */
    public boolean tradeUsed = false;

    /** Pending trade (mutual confirmation). */
    public TradeRequest pendingTrade = null;

    /** Remaining time before the duration cap closes the window (engine ticks, 60/s). */
    public float timeLeft;

    public BossWindow(RunConfig config){
        this.timeLeft = config.ticks(config.bossWindowTimeout);
    }
}
