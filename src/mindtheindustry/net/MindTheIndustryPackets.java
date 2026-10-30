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
 * - {@link RewardChoicesPacket} (S→C): the 3 reward choices of a player after a
 *   wave (empty list = hide the choice box).
 * - {@link RewardVotePacket} (C→S): the index of the reward the player clicked.
 *
 * Run control (start/stop/skip/status) still goes through the server command
 * `/mindtheindustry`, which works identically in solo and multiplayer.
 */
public class MindTheIndustryPackets{

    /** Registers all the mod's packets. Call at mod load. */
    public static void register(){
        Net.registerPacket(RewardChoicesPacket::new);
        Net.registerPacket(RewardVotePacket::new);
    }

    /** S→C: sends a player their reward choices for the current wave. */
    public static class RewardChoicesPacket extends Packet{
        public int wave;
        public Seq<Reward> choices = new Seq<>();

        public RewardChoicesPacket(){}

        public RewardChoicesPacket(int wave, Seq<Reward> choices){
            this.wave = wave;
            this.choices = choices;
        }

        @Override
        public void write(Writes write){
            write.i(wave);
            write.i(choices.size);
            for(Reward r : choices){
                r.write(write);
            }
        }

        @Override
        public void read(Reads read){
            wave = read.i();
            int n = read.i();
            choices = new Seq<>(n);
            for(int i = 0; i < n; i++){
                choices.add(Reward.read(read));
            }
        }

        @Override
        public void handleClient(){
            ClientRewardState.instance.set(wave, choices);
        }
    }

    /** C→S: the player clicked reward choice {@link #index}. */
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
                MindTheIndustryMod.serverController.handleVote(con.player, index, con.player);
            }
        }
    }
}
