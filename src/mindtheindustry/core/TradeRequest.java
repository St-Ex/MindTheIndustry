package mindtheindustry.core;

import mindustry.gen.*;

/**
 * A pending trade during a boss window: transfer of one already-taken reward
 * from one player to another (effects transfer). Both sides must confirm.
 * At most one trade per boss window.
 */
public class TradeRequest{

    public final Player from;
    public final Player to;

    /** Id of the already-taken reward offered by {@code from}. */
    public final String rewardId;

    public boolean confirmedFrom;
    public boolean confirmedTo;

    public TradeRequest(Player from, Player to, String rewardId){
        this.from = from;
        this.to = to;
        this.rewardId = rewardId;
    }

    public boolean involves(Player player){
        return player == from || player == to;
    }

    public boolean bothConfirmed(){
        return confirmedFrom && confirmedTo;
    }
}
