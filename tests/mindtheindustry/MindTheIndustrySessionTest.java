package mindtheindustry;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;
import mindtheindustry.core.*;
import mindtheindustry.modifiers.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for the server session (state of the rogue-like run). */
class MindTheIndustrySessionTest extends HeadlessMindTheIndustryTest{

    /** New session with fresh game rules and pristine unlock flags (global state shared between tests). */
    private MindTheIndustrySession newSession(long seed){
        state.rules = new Rules();
        resetUnlockFlags();
        MindTheIndustrySession session = new MindTheIndustrySession();
        session.start(seed);
        return session;
    }

    @Test
    void startUnlocksStarterBlocks(){
        MindTheIndustrySession session = newSession(42);

        assertTrue(session.active);
        assertEquals(0, session.wave);
        assertEquals(42, session.seed);

        Block duo = content.block("duo");
        assertNotNull(duo);
        assertTrue(session.isUnlockedForGroup(duo));
        assertTrue(duo.unlocked(), "host-side flag must be set for the build menu");
        assertTrue(state.rules.researched.contains(duo), "starter blocks must be propagated to the game rules");
        assertTrue(session.getGroupUnlocks().size >= 20, "the playable-start floor must be unlocked (blocks + units)");
    }

    @Test
    void startUnlocksBasicCategoryBlocks(){
        MindTheIndustrySession session = newSession(42);

        // the playable-start floor: core/storage, transport, extraction, power, basic defense (Serpulo)
        for(String name : new String[]{
            "core-shard", "container", "vault",
            "conveyor", "junction", "router", "titanium-conveyor",
            "mechanical-drill", "pneumatic-drill",
            "power-node", "combustion-generator", "solar-panel",
            "copper-wall", "copper-wall-large", "duo", "scatter", "hail", "mender"
        }){
            Block block = content.block(name);
            assertNotNull(block, "unknown block: " + name);
            assertTrue(session.isUnlockedForGroup(block), "floor block must be unlocked at start: " + name);
            assertTrue(state.rules.researched.contains(block), "floor block must be in game rules: " + name);
            assertTrue(block.unlocked(), "host-side flag must be set, the build menu reads it in campaign: " + name);
        }
    }

    @Test
    void floorIsSerpuloOnlyAndStaysSmall(){
        MindTheIndustrySession session = newSession(42);

        // Erekir content (beryllium ducts etc.) must NOT be in the floor —
        // the run map is Serpulo-based; Erekir comes via PLAN rewards
        for(String name : new String[]{"duct", "beryllium-wall", "pulse-conduit", "phase-conveyor", "thruster"}){
            Block block = content.block(name);
            assertNotNull(block, "unknown block: " + name);
            assertFalse(session.isUnlockedForGroup(block), "Erekir/advanced block must NOT be in the floor: " + name);
        }
        // advanced serpulo content stays locked too
        for(String name : new String[]{"lancer", "parallax", "meltdown", "mass-driver", "naval-factory", "logic-processor"}){
            Block block = content.block(name);
            assertNotNull(block, "unknown block: " + name);
            assertFalse(session.isUnlockedForGroup(block), "advanced block must NOT be in the floor: " + name);
        }
    }

    @Test
    void startDoesNotUnlockBatteriesOrAdvancedGenerators(){
        MindTheIndustrySession session = newSession(42);

        for(String name : new String[]{"battery", "battery-large", "thorium-reactor", "impact-reactor"}){
            Block block = content.block(name);
            assertNotNull(block, "unknown block: " + name);
            assertFalse(session.isUnlockedForGroup(block), "must NOT be unlocked at start: " + name);
            assertFalse(state.rules.researched.contains(block), "must NOT be in game rules: " + name);
            assertFalse(block.unlocked(), "host-side flag must NOT be set: " + name);
        }
    }

    @Test
    void startUnlocksBasicUnitPlans(){
        MindTheIndustrySession session = newSession(42);

        for(String name : new String[]{"dagger", "mace", "nova", "flare"}){
            UnitType unit = content.unit(name);
            assertNotNull(unit, "unknown starter unit: " + name);
            assertTrue(session.isUnlockedForGroup(unit), "basic unit must be unlocked at start: " + name);
            assertTrue(state.rules.researched.contains(unit), "starter units must be propagated to the game rules: " + name);
        }
    }

    @Test
    void nextWaveAppliesDifficulty(){
        MindTheIndustrySession session = newSession(1);

        session.nextWave();
        assertEquals(1, session.wave);
        assertEquals(1.05f, state.rules.unitHealthMultiplier, 0.001f);

        session.nextWave();
        assertEquals(2, session.wave);
        assertEquals(1.10f, state.rules.unitHealthMultiplier, 0.001f);
    }

    @Test
    void globalModifierAppliesToRules(){
        MindTheIndustrySession session = newSession(1);

        session.addGlobalModifier(new GlobalModifier("build_speed", 0.5f, "Build speed"));
        assertEquals(1.5f, state.rules.buildSpeedMultiplier, 0.001f);

        session.addGlobalModifier(new GlobalModifier("unit_damage", 0.2f, "Unit damage"));
        assertEquals(1.2f, state.rules.unitDamageMultiplier, 0.001f);

        assertEquals(2, session.getGlobalModifiers().size);
    }

    @Test
    void unitBonusMultipliersStack(){
        MindTheIndustrySession session = newSession(1);
        Player player = Player.create();
        player.name = "tester";

        session.addUnitBonus(player, new UnitBonus(UnitBonusType.DAMAGE, 0.5f));
        session.addUnitBonus(player, new UnitBonus(UnitBonusType.DAMAGE, 0.5f));
        session.addUnitBonus(player, new UnitBonus(UnitBonusType.RELOAD, 0.25f));

        assertEquals(2.25f, session.getDamageMultiplier(player), 0.001f);
        assertEquals(1.25f, session.getReloadMultiplier(player), 0.001f);
        assertEquals(3, session.getUnitBonuses(player).size);
    }

    @Test
    void buildingBonusMultipliersAreScopedByBlock(){
        MindTheIndustrySession session = newSession(1);
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
        MindTheIndustrySession session = newSession(1);
        Unit unit = UnitTypes.dagger.create(Team.sharded);
        unit.maxHealth = 100f;
        unit.health = 50f;
        unit.speedMultiplier = 1f;
        unit.armor = 2f;
        unit.armorOverride = -1f;

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusType.HEALTH, 0.5f));
        assertEquals(150f, unit.maxHealth, 0.001f);
        assertEquals(75f, unit.health, 0.001f, "health ratio must be preserved");

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusType.SPEED, 0.5f));
        assertEquals(1.5f, unit.speedMultiplier, 0.001f);

        session.applyBonusToUnit(unit, new UnitBonus(UnitBonusType.ARMOR, 3f));
        assertEquals(5f, unit.armorOverride, 0.001f);
    }

    @Test
    void stopDeactivatesRun(){
        MindTheIndustrySession session = newSession(1);
        session.stop();
        assertFalse(session.active);
    }
}
