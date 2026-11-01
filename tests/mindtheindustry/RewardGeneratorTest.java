package mindtheindustry;

import arc.struct.*;
import mindustry.ctype.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import mindtheindustry.core.*;
import mindtheindustry.rewards.*;
import mindtheindustry.rewards.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for the reward generator (categories, rarity scaling, determinism). */
class RewardGeneratorTest extends HeadlessMindTheIndustryTest{

    private MindTheIndustrySession newSession(long seed){
        state.rules = new Rules();
        resetUnlockFlags();
        MindTheIndustrySession session = new MindTheIndustrySession();
        session.start(seed);
        return session;
    }

    @Test
    void generatesExactlyThreeChoices(){
        MindTheIndustrySession session = newSession(42);
        RewardGenerator gen = new RewardGenerator(session.rand);

        Seq<Reward> choices = gen.generateChoices(session, null, 1);

        assertEquals(3, choices.size);
        for(Reward r : choices){
            assertNotNull(r);
            assertNotNull(r.id);
            assertNotNull(r.type);
            assertNotNull(r.rarity);
            assertNotNull(r.displayName);
            assertFalse(r.description.isEmpty());
        }
    }

    @Test
    void deterministicForSameSeed(){
        MindTheIndustrySession s1 = newSession(1234);
        MindTheIndustrySession s2 = newSession(1234);
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
                assertEquals(c1.get(i).rarity, c2.get(i).rarity);
            }
        }
    }

    @Test
    void differentSeedsGiveDifferentRuns(){
        MindTheIndustrySession s1 = newSession(1);
        MindTheIndustrySession s2 = newSession(2);
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
    void alwaysOneOfEachCategory(){
        MindTheIndustrySession session = newSession(42);
        RewardGenerator gen = new RewardGenerator(session.rand);

        for(int wave = 1; wave <= 20; wave++){
            Seq<Reward> choices = gen.generateChoices(session, null, wave);
            assertEquals(3, choices.size);
            int tradeoffs = 0, plans = 0, resources = 0;
            for(Reward r : choices){
                switch(r.type){
                    case TRADEOFF -> tradeoffs++;
                    case PLAN -> plans++;
                    case RESOURCE -> resources++;
                }
            }
            assertEquals(1, tradeoffs, "exactly one trade-off per wave (wave " + wave + ")");
            assertEquals(1, plans, "exactly one plan per wave (wave " + wave + ")");
            assertEquals(1, resources, "exactly one resource gift per wave (wave " + wave + ")");
        }
    }

    @Test
    void allCategoriesAndRaritiesEventuallyAppear(){
        MindTheIndustrySession session = newSession(42);
        RewardGenerator gen = new RewardGenerator(session.rand);

        ObjectSet<RewardType> types = new ObjectSet<>();
        ObjectSet<Rarity> rarities = new ObjectSet<>();
        for(int i = 0; i < 300; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                types.add(r.type);
                rarities.add(r.rarity);
            }
        }

        assertEquals(RewardType.values().length, types.size,
            "all categories must be able to appear, seen: " + types);
        assertEquals(Rarity.values().length, rarities.size,
            "all rarities must be able to appear, seen: " + rarities);
    }

    @Test
    void tradeoffsHaveDistinctOppositeStatsScaledByRarity(){
        MindTheIndustrySession session = newSession(7);
        RewardGenerator gen = new RewardGenerator(session.rand);

        int seen = 0;
        for(int i = 0; i < 300; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof TradeoffReward t){
                    seen++;
                    assertNotEquals(t.bonusId, t.malusId, "the two stats must be distinct");
                    assertTrue(t.bonusValue > 0, "bonus must be positive");
                    assertEquals(t.bonusValue, -t.malusValue, 0.0001f, "malus must mirror the bonus");
                    assertEquals(RewardGenerator.tradeoffBase * t.rarity.multiplier, t.bonusValue, 0.0001f,
                        "magnitude must be base x rarity multiplier");
                }
            }
        }
        assertTrue(seen > 0, "trade-offs must appear over 300 generations");
    }

    @Test
    void plansNeverOfferAlreadyUnlockedContent(){
        MindTheIndustrySession session = newSession(7);
        RewardGenerator gen = new RewardGenerator(session.rand);

        for(int i = 0; i < 50; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof PlanReward p){
                    assertFalse(p.contentNames.isEmpty());
                    for(String name : p.contentNames){
                        UnlockableContent content = PlanReward.resolve(name);
                        assertNotNull(content, "unknown content: " + name);
                        assertFalse(session.isUnlockedForGroup(content),
                            "must not offer an already unlocked plan: " + name);
                    }
                    r.apply(session, null, null); // apply → unlocks for the group
                    for(String name : p.contentNames){
                        assertTrue(session.isUnlockedForGroup(PlanReward.resolve(name)));
                    }
                }
            }
        }
    }

    @Test
    void plansNeverOfferStarterPlans(){
        MindTheIndustrySession session = newSession(11);
        RewardGenerator gen = new RewardGenerator(session.rand);

        // floor plans are unlocked at run start → must never be offered as a reward
        ObjectSet<String> starters = ObjectSet.with(
            "unit:dagger", "unit:mace", "unit:nova", "unit:flare",
            "block:core-shard", "block:container", "block:vault",
            "block:conveyor", "block:junction", "block:router", "block:titanium-conveyor",
            "block:mechanical-drill", "block:pneumatic-drill",
            "block:power-node", "block:combustion-generator", "block:solar-panel",
            "block:copper-wall", "block:copper-wall-large", "block:duo", "block:scatter", "block:hail", "block:mender");

        for(int i = 0; i < 200; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof PlanReward p){
                    for(String name : p.contentNames){
                        assertFalse(starters.contains(name), "starter plan must not be offered: " + name);
                    }
                }
            }
        }
    }

    @Test
    void planCountScalesWithRarity(){
        MindTheIndustrySession session = newSession(99);
        RewardGenerator gen = new RewardGenerator(session.rand);

        int seen = 0;
        for(int i = 0; i < 300; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof PlanReward p){
                    seen++;
                    // fresh session: the pool is far larger than 4, so count == multiplier
                    assertEquals(p.rarity.multiplier, p.contentNames.size,
                        "plan count must equal the rarity multiplier");
                    // all picked plans must be distinct
                    ObjectSet<String> distinctPlans = new ObjectSet<>();
                    for(String n : p.contentNames) distinctPlans.add(n);
                    assertEquals(p.contentNames.size, distinctPlans.size);
                }
            }
        }
        assertTrue(seen > 0, "plans must appear over 300 generations");
    }

    @Test
    void resourceGiftsScaleWithRarity(){
        MindTheIndustrySession session = newSession(3);
        RewardGenerator gen = new RewardGenerator(session.rand);

        int seen = 0;
        for(int i = 0; i < 300; i++){
            for(Reward r : gen.generateChoices(session, null, 1)){
                if(r instanceof ResourceReward res){
                    seen++;
                    int expectedAmount = RewardGenerator.resourceBaseAmount * res.rarity.multiplier;
                    assertEquals(Math.min(RewardGenerator.resourceMaxTypes, 1 + res.rarity.ordinal()),
                        res.itemNames.size, "item type count grows with rarity");
                    ObjectSet<String> distinctItems = new ObjectSet<>();
                    for(String n : res.itemNames) distinctItems.add(n);
                    assertEquals(res.itemNames.size, distinctItems.size, "items must be distinct");
                    for(int amount : res.amounts){
                        assertEquals(expectedAmount, amount, "amount must be base x rarity multiplier");
                    }
                }
            }
        }
        assertTrue(seen > 0, "resource gifts must appear over 300 generations");
    }

    @Test
    void highWaveDoesNotCrash(){
        MindTheIndustrySession session = newSession(99);
        RewardGenerator gen = new RewardGenerator(session.rand);
        Seq<Reward> choices = gen.generateChoices(session, null, 500);
        assertEquals(3, choices.size);
    }

    @Test
    void relicChoicesAreAlwaysRareOrEpic(){
        MindTheIndustrySession session = newSession(7);
        RewardGenerator gen = new RewardGenerator(session.rand);

        int seen = 0;
        for(int i = 0; i < 100; i++){
            Seq<Reward> relics = gen.generateRelicChoices(session, null, 10);
            assertEquals(3, relics.size);
            for(Reward r : relics){
                seen++;
                assertTrue(r.rarity == Rarity.RARE || r.rarity == Rarity.EPIC,
                    "relic pool must be rare/boosted only, got " + r.rarity);
            }
        }
        assertEquals(300, seen);
    }

    @Test
    void relicChoicesAreDeterministicPerSeed(){
        MindTheIndustrySession s1 = newSession(555);
        MindTheIndustrySession s2 = newSession(555);
        RewardGenerator g1 = new RewardGenerator(s1.rand);
        RewardGenerator g2 = new RewardGenerator(s2.rand);

        Seq<Reward> r1 = g1.generateRelicChoices(s1, null, 10);
        Seq<Reward> r2 = g2.generateRelicChoices(s2, null, 10);
        for(int i = 0; i < 3; i++){
            assertEquals(r1.get(i).id, r2.get(i).id);
            assertEquals(r1.get(i).rarity, r2.get(i).rarity);
        }
    }
}
