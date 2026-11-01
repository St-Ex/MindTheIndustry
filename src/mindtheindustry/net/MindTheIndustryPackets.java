package mindtheindustry.net;

import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindtheindustry.*;
import mindtheindustry.client.*;
import mindtheindustry.rewards.*;

import static mindustry.Vars.*;

/**
 * Custom network packets for MindTheIndustry.
 *
 * - {@link RewardChoicesPacket} (S→C): the 3 proposals of a player after a wave
 *   end (wave choice) or at boss window open (relic choice; empty list = hide
 *   the choice box).
 * - {@link RewardVotePacket} (C→S): the index of the proposal the player took,
 *   or -1 for the explicit "take none" decision (conversion).
 * - {@link BossWindowPacket} (S→C): boss window opened/closed (+ remaining time).
 * - {@link ReadyPacket} (C→S): the player is ready to leave the boss window.
 * - {@link TradeOfferPacket} (C→S): offer one already-taken reward to another player.
 * - {@link TradeConfirmPacket} (C→S): confirm the pending trade.
 * - {@link SwitchPacket} (C→S): switch the active player of an entity (boss window only).
 *
 * Run control (start/stop/skip/status) still goes through the server command
 * `/mindtheindustry`, which works identically in solo and multiplayer.
 */
public class MindTheIndustryPackets{

    /** Registers all the mod's packets. Call at mod load. */
    public static void register(){
        Net.registerPacket(RewardChoicesPacket::new);
        Net.registerPacket(RewardVotePacket::new);
        Net.registerPacket(BossWindowPacket::new);
        Net.registerPacket(ReadyPacket::new);
        Net.registerPacket(TradeOfferPacket::new);
        Net.registerPacket(TradeConfirmPacket::new);
        Net.registerPacket(SwitchPacket::new);
    }

    /** S→C: sends a player their proposals for the current wave (wave choice or boss-window relic choice). */
    public static class RewardChoicesPacket extends Packet{
        public int wave;
        public boolean relic;
        public Seq<Reward> choices = new Seq<>();

        public RewardChoicesPacket(){}

        public RewardChoicesPacket(int wave, Seq<Reward> choices, boolean relic){
            this.wave = wave;
            this.choices = choices;
            this.relic = relic;
        }

        @Override
        public void write(Writes write){
            write.i(wave);
            write.bool(relic);
            write.i(choices.size);
            for(Reward r : choices){
                r.write(write);
            }
        }

        @Override
        public void read(Reads read){
            wave = read.i();
            relic = read.bool();
            int n = read.i();
            choices = new Seq<>(n);
            for(int i = 0; i < n; i++){
                choices.add(Reward.read(read));
            }
        }

        @Override
        public void handleClient(){
            ClientRewardState.instance.set(wave, choices, relic);
        }
    }

    /** C→S: the player took proposal {@link #index} of their open choice; -1 = take none (conversion). */
    public static class RewardVotePacket extends Packet{
        public int index;

        public RewardVotePacket(){}

        public RewardVotePacket(int index){
            this.index = index;
        }

        @Override
        public void write(Writes write){
            write.i(index);
        }

        @Override
        public void read(Reads read){
            index = read.i();
        }

        @Override
        public void handleServer(NetConnection con){
            if(MindTheIndustryMod.serverController != null && con.player != null){
                MindTheIndustryMod.serverController.handleRewardVote(con.player, index, con.player);
            }
        }
    }

    /** S→C: boss window opened ({@link #open}) or closed, with the remaining time in seconds. */
    public static class BossWindowPacket extends Packet{
        public boolean open;
        public float timeLeft;

        public BossWindowPacket(){}

        public BossWindowPacket(boolean open, float timeLeft){
            this.open = open;
            this.timeLeft = timeLeft;
        }

        @Override
        public void write(Writes write){
            write.bool(open);
            write.f(timeLeft);
        }

        @Override
        public void read(Reads read){
            open = read.bool();
            timeLeft = read.f();
        }

        @Override
        public void handleClient(){
            ClientRewardState.instance.setBossWindow(open, timeLeft);
        }
    }

    /** C→S: the player is ready — closes the boss window early when everyone is ready. */
    public static class ReadyPacket extends Packet{
        public ReadyPacket(){}

        @Override
        public void write(Writes write){}

        @Override
        public void read(Reads read){}

        @Override
        public void handleServer(NetConnection con){
            if(MindTheIndustryMod.serverController != null && con.player != null){
                MindTheIndustryMod.serverController.handleReady(con.player);
            }
        }
    }

    /** C→S: offer one already-taken reward ({@link #rewardId}) to another player ({@link #targetName}). */
    public static class TradeOfferPacket extends Packet{
        public String targetName;
        public String rewardId;

        public TradeOfferPacket(){}

        public TradeOfferPacket(String targetName, String rewardId){
            this.targetName = targetName;
            this.rewardId = rewardId;
        }

        @Override
        public void write(Writes write){
            write.str(targetName);
            write.str(rewardId);
        }

        @Override
        public void read(Reads read){
            targetName = read.str();
            rewardId = read.str();
        }

        @Override
        public void handleServer(NetConnection con){
            if(MindTheIndustryMod.serverController == null || con.player == null) return;
            Player target = Groups.player.find(p -> p.name != null && p.name.equalsIgnoreCase(targetName));
            if(target != null){
                MindTheIndustryMod.serverController.offerTrade(con.player, target, rewardId);
            }
        }
    }

    /** C→S: confirm the pending trade (both sides must confirm). */
    public static class TradeConfirmPacket extends Packet{
        public TradeConfirmPacket(){}

        @Override
        public void write(Writes write){}

        @Override
        public void read(Reads read){}

        @Override
        public void handleServer(NetConnection con){
            if(MindTheIndustryMod.serverController != null && con.player != null){
                MindTheIndustryMod.serverController.confirmTrade(con.player);
            }
        }
    }

    /** C→S: switch the active player of an entity (boss window only). */
    public static class SwitchPacket extends Packet{
        public int entityId;
        public boolean isBuilding;
        public String targetName; // "none" = clear

        public SwitchPacket(){}

        public SwitchPacket(int entityId, boolean isBuilding, String targetName){
            this.entityId = entityId;
            this.isBuilding = isBuilding;
            this.targetName = targetName;
        }

        @Override
        public void write(Writes write){
            write.i(entityId);
            write.bool(isBuilding);
            write.str(targetName);
        }

        @Override
        public void read(Reads read){
            entityId = read.i();
            isBuilding = read.bool();
            targetName = read.str();
        }

        @Override
        public void handleServer(NetConnection con){
            if(MindTheIndustryMod.serverController == null || con.player == null) return;
            Player target = null;
            if(!targetName.equalsIgnoreCase("none")){
                target = Groups.player.find(p -> p.name != null && p.name.equalsIgnoreCase(targetName));
            }
            MindTheIndustryMod.serverController.setActivePlayer(entityId, isBuilding, target, con.player);
        }
    }
}
