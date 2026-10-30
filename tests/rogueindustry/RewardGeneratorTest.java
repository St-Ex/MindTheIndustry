package rogueindustry;

import arc.struct.*;
import mindustry.game.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;
import rogueindustry.core.*;
import rogueindustry.rewards.*;
import rogueindustry.rewards.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for the reward generator (determinism, unlock filtering). */
class RewardGeneratorTest extends HeadlessRogueTest{

    private RogueSession newSession(long seed){
        state.rules = new Rules();
        RogueSession session = new RogueSession();
        session.start(seed);
        return session;
    }

    @Test
    void generatesExactlyThreeChoices(){
        RogueSession session = newSession(42);
        RewardGenerator gen = new RewardGenerator(session.rand);

        Seq<Reward> choices = gen.generateChoices(session, null, 1);

        assertEquals(3, choices.size);
        for(Reward r : choices){
            assertNotNull(r);
            assertNotNull(r.id);
            assertNotNull(r.type);
            assertNotNull(r.displayName);
            assertFalse(r.description.isEmpty());
        }
    }

    @Test
    void deterministicForSameSeed(){
        RogueSession s1 = newSession(1234);
        RogueSession s2 = newSession(1234);
        RewardGenerator g1 = new RewardGenerator(s1.rand);
        RewardGenerator g2 = new RewardGenerator(s2.rand);

        for(int wave = 1; wave <= 10; wave++){
            Seq<Reward> c1 = g1.generateChoices(s1, null, wave);
            Seq<Reward> c2 = g2.generateChoices(s2, null, wave);
            assertEquals(c1.size, c2.size);
            for(int i = 0; i < c1.size; i++){
                assertEquals(c1.get(i).id, c2.get(i).id,
                    "same seed → same choices (wave " + wave + ", choice " + i + ")");
                assertEquals(c1.get(i).type, c2.get(i).type);
            }
        }
    }

    @Test
    void differentSeedsGiveDifferentRuns(){
        RogueSession s1 = newSession(1);
        RogueSession s2 = newSession(2);
        RewardGenerator g1 = new RewardGenerator(s1.rand);
        RewardGenerator g2 = new RewardGenerator(s2.rand);

        boolean different = false;
        for(int wave = 1; wave <= 10 && !different; wave++){
            Seq<Reward> c1 = g1.generateChoices(s1, null, wave);
            Seq<Reward> c2 = g2.generateChoices(s2, null, wave);
            for(int i = 0; i < c1.size; i++){
                if(!c1.get(i).id.equals(c2.get(i).id)){
                    different = true;
                    break;
                }
            }
        }
        assertTrue(different, "two different seeds must produce different runs");
    }

    @Test
    void neverProposesAlreadyUnlockedBuilding(){
        RogueSession session = newSession(7);
        RewardGenerator gen = new RewardGenerator(session.rand);

        for(int i = 0; i < 50; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof BuildingReward br){
                    Block block = content.block(br.blockName);
                    assertNotNull(block, "unknown block: " + br.blockName);
                    assertFalse(session.isUnlockedForGroup(block),
                        "must not offer an already unlocked building: " + br.blockName);
                    r.apply(session, null, null); // apply → unlocks for the group
                }
            }
        }
    }

    @Test
    void allRewardTypesEventuallyAppear(){
        RogueSession session = newSession(42);
        RewardGenerator gen = new RewardGenerator(session.rand);

        ObjectSet<RewardType> seen = new ObjectSet<>();
        for(int i = 0; i < 300; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                seen.add(r.type);
            }
        }

        assertEquals(RewardType.values().length, seen.size,
            "all reward types must be able to appear, seen: " + seen);
    }

    @Test
    void highWaveDoesNotCrash(){
        RogueSession session = newSession(99);
        RewardGenerator gen = new RewardGenerator(session.rand);
        Seq<Reward> choices = gen.generateChoices(session, null, 500);
        assertEquals(3, choices.size);
    }
}
