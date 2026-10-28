package rogueindustry;

import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for applying rewards to a session (gameplay effect of each type). */
class RewardApplyTest extends HeadlessRogueTest{

    private RogueSession newSession(long seed){
        state.rules = new Rules();
        RogueSession session = new RogueSession();
        session.start(seed);
        return session;
    }

    @Test
    void buildingRewardUnlocksForGroup(){
        RogueSession session = newSession(1);
        BuildingReward reward = new BuildingReward("spectre", "Spectre", "desc");

        reward.apply(session, null, null);

        Block spectre = content.block("spectre");
        assertTrue(session.isUnlockedForGroup(spectre));
        assertTrue(state.rules.researched.contains(spectre));
    }

    @Test
    void unitBonusRewardAppliesToTargetOrChooser(){
        RogueSession session = newSession(1);
        Player chooser = Player.create();
        chooser.name = "chooser";
        Player target = Player.create();
        target.name = "target";

        UnitBonusReward reward = new UnitBonusReward(UnitBonusReward.BonusType.DAMAGE, 0.5f, "n", "d");

        // explicit target
        reward.apply(session, chooser, target);
        assertEquals(0, session.getUnitBonuses(chooser).size);
        assertEquals(1, session.getUnitBonuses(target).size);
        assertEquals(1.5f, session.getDamageMultiplier(target), 0.001f);

        // no target → applies to the voter
        reward.apply(session, chooser, null);
        assertEquals(1, session.getUnitBonuses(chooser).size);
    }

    @Test
    void buildingBonusRewardAppliesToTarget(){
        RogueSession session = newSession(1);
        Player chooser = Player.create();
        chooser.name = "chooser";
        Player target = Player.create();
        target.name = "target";

        BuildingBonusReward reward = new BuildingBonusReward(BuildingBonus.BonusType.RELOAD, 0.5f, "duo", "n", "d");
        reward.apply(session, chooser, target);

        assertEquals(1.5f, session.getBuildingReloadMultiplier(target, "duo"), 0.001f);
        assertEquals(1f, session.getBuildingReloadMultiplier(chooser, "duo"), 0.001f);
    }

    @Test
    void globalModifierRewardAppliesThroughRegistry(){
        RogueSession session = newSession(1);
        GlobalModifierReward reward = new GlobalModifierReward("build_speed", 2f, "n", "d");

        reward.apply(session, null, null);

        // registry: build_speed base 0.15 × multiplier 2 = 0.30
        assertEquals(1.3f, state.rules.buildSpeedMultiplier, 0.001f);
        assertEquals(1, session.getGlobalModifiers().size);
        assertEquals("build_speed", session.getGlobalModifiers().get(0).id);
    }

    @Test
    void globalModifierRewardWithUnknownIdIsIgnored(){
        RogueSession session = newSession(1);
        GlobalModifierReward reward = new GlobalModifierReward("unknown", 1f, "n", "d");

        reward.apply(session, null, null);

        assertEquals(0, session.getGlobalModifiers().size);
    }

    @Test
    void schematicRewardUnlocksForChooserOnly(){
        RogueSession session = newSession(1);
        Player alice = Player.create();
        alice.name = "alice";
        Player bob = Player.create();
        bob.name = "bob";

        SchematicReward reward = new SchematicReward("", "Test Schematic", "n", "d");
        reward.apply(session, alice, null);

        assertTrue(session.hasSchematic(alice, "Test Schematic"));
        assertFalse(session.hasSchematic(bob, "Test Schematic"));
    }

    @Test
    void synergyRewardRegistersWithoutCrash(){
        RogueSession session = newSession(1);
        SynergyReward reward = new SynergyReward("duo", "+10% duo damage", "n", "d");
        // no public getter for synergies: mainly checking for no error
        reward.apply(session, null, null);
    }
}
