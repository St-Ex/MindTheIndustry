package rogueindustry;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;
import rogueindustry.core.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.rewards.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for the server session (state of the rogue-like run). */
class RogueSessionTest extends HeadlessRogueTest{

    /** New session with fresh game rules (global state shared between tests). */
    private RogueSession newSession(long seed){
        state.rules = new Rules();
        RogueSession session = new RogueSession();
        session.start(seed);
        return session;
    }

    @Test
    void startUnlocksStarterBlocks(){
        RogueSession session = newSession(42);

        assertTrue(session.active);
        assertEquals(0, session.wave);
        assertEquals(42, session.seed);

        Block duo = content.block("duo");
        assertNotNull(duo);
        assertTrue(session.isUnlockedForGroup(duo));
        assertTrue(state.rules.researched.contains(duo), "starter blocks must be propagated to the game rules");
        assertTrue(session.getGroupUnlocks().size > 50, "many base blocks must be unlocked");
    }

    @Test
    void nextWaveAppliesDifficulty(){
        RogueSession session = newSession(1);

        session.nextWave();
        assertEquals(1, session.wave);
        assertEquals(1.05f, state.rules.unitHealthMultiplier, 0.001f);

        session.nextWave();
        assertEquals(2, session.wave);
        assertEquals(1.10f, state.rules.unitHealthMultiplier, 0.001f);
    }

    @Test
    void globalModifierAppliesToRules(){
        RogueSession session = newSession(1);

        session.addGlobalModifier(new GlobalModifier("build_speed", 0.5f, "Build speed"));
        assertEquals(1.5f, state.rules.buildSpeedMultiplier, 0.001f);

        session.addGlobalModifier(new GlobalModifier("unit_damage", 0.2f, "Unit damage"));
        assertEquals(1.2f, state.rules.unitDamageMultiplier, 0.001f);

        assertEquals(2, session.getGlobalModifiers().size);
    }

    @Test
    void unitBonusMultipliersStack(){
        RogueSession session = newSession(1);
        Player player = Player.create();
        player.name = "tester";

        session.addUnitBonus(player, new UnitBonus(UnitBonusReward.BonusType.DAMAGE, 0.5f));
        session.addUnitBonus(player, new UnitBonus(UnitBonusReward.BonusType.DAMAGE, 0.5f));
        session.addUnitBonus(player, new UnitBonus(UnitBonusReward.BonusType.RELOAD, 0.25f));

        assertEquals(2.25f, session.getDamageMultiplier(player), 0.001f);
        assertEquals(1.25f, session.getReloadMultiplier(player), 0.001f);
        assertEquals(3, session.getUnitBonuses(player).size);
    }

    @Test
    void buildingBonusMultipliersAreScopedByBlock(){
        RogueSession session = newSession(1);
        Player player = Player.create();
        player.name = "tester";

        session.addBuildingBonus(player, new BuildingBonus(BuildingBonus.BonusType.DAMAGE, 0.5f, "duo"));
        session.addBuildingBonus(player, new BuildingBonus(BuildingBonus.BonusType.DAMAGE, 0.5f, "hail"));

        assertEquals(1.5f, session.getBuildingDamageMultiplier(player, "duo"), 0.001f);
        assertEquals(1.5f, session.getBuildingDamageMultiplier(player, "hail"), 0.001f);
        assertEquals(1f, session.getBuildingDamageMultiplier(player, "scorch"), 0.001f);
        assertEquals(1, session.getApplicableBuildingBonuses(player, "duo").size);
    }

    @Test
    void applyBonusToUnitScalesStats(){
        RogueSession session = newSession(1);
        Unit unit = UnitTypes.dagger.create(Team.sharded);
        unit.maxHealth = 100f;
        unit.health = 50f;
        unit.speedMultiplier = 1f;
        unit.armor = 2f;
        unit.armorOverride = -1f;

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusReward.BonusType.HEALTH, 0.5f));
        assertEquals(150f, unit.maxHealth, 0.001f);
        assertEquals(75f, unit.health, 0.001f, "health ratio must be preserved");

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusReward.BonusType.SPEED, 0.5f));
        assertEquals(1.5f, unit.speedMultiplier, 0.001f);

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusReward.BonusType.ARMOR, 3f));
        assertEquals(5f, unit.armorOverride, 0.001f);
    }

    @Test
    void stopDeactivatesRun(){
        RogueSession session = newSession(1);
        session.stop();
        assertFalse(session.active);
    }
}
