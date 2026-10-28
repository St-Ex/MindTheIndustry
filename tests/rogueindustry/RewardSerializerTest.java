package rogueindustry;

import arc.util.io.*;
import org.junit.jupiter.api.*;
import rogueindustry.modifiers.*;
import rogueindustry.rewards.*;
import rogueindustry.rewards.rewards.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for reward network serialization (round-trip write → read). */
class RewardSerializerTest extends HeadlessRogueTest{

    /** Serializes then deserializes a reward. */
    private static Reward roundTrip(Reward reward){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reward.write(new Writes(new DataOutputStream(baos)));
        return Reward.read(new Reads(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))));
    }

    private static void assertBaseFields(Reward original, Reward back){
        assertEquals(original.id, back.id);
        assertEquals(original.type, back.type);
        assertEquals(original.displayName, back.displayName);
        assertEquals(original.description, back.description);
    }

    @Test
    void buildingRewardRoundTrip(){
        BuildingReward original = new BuildingReward("salvo", "Salvo", "Unlocks Salvo for everyone");
        Reward back = roundTrip(original);

        assertInstanceOf(BuildingReward.class, back);
        assertBaseFields(original, back);
        BuildingReward r = (BuildingReward)back;
        assertEquals("salvo", r.blockName);
        assertNotNull(r.block, "the block must be resolved from content");
        assertEquals("salvo", r.block.name);
    }

    @Test
    void buildingBonusRewardRoundTrip(){
        BuildingBonusReward original = new BuildingBonusReward(BuildingBonus.BonusType.DAMAGE, 0.42f, "duo", "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(BuildingBonusReward.class, back);
        assertBaseFields(original, back);
        BuildingBonusReward r = (BuildingBonusReward)back;
        assertEquals(BuildingBonus.BonusType.DAMAGE, r.bonusType);
        assertEquals(0.42f, r.value, 0.0001f);
        assertEquals("duo", r.blockName);
    }

    @Test
    void buildingBonusRewardRoundTripNullBlock(){
        BuildingBonusReward original = new BuildingBonusReward(BuildingBonus.BonusType.PRODUCTION, 0.2f, null, "n", "d");
        BuildingBonusReward r = (BuildingBonusReward)roundTrip(original);
        assertNull(r.blockName, "a null blockName must survive serialization (encoded as empty string)");
    }

    @Test
    void unitBonusRewardRoundTrip(){
        UnitBonusReward original = new UnitBonusReward(UnitBonusReward.BonusType.SPEED, 0.33f, "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(UnitBonusReward.class, back);
        assertBaseFields(original, back);
        UnitBonusReward r = (UnitBonusReward)back;
        assertEquals(UnitBonusReward.BonusType.SPEED, r.bonusType);
        assertEquals(0.33f, r.value, 0.0001f);
    }

    @Test
    void globalModifierRewardRoundTrip(){
        GlobalModifierReward original = new GlobalModifierReward("turret_damage", 0.15f, "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(GlobalModifierReward.class, back);
        assertBaseFields(original, back);
        GlobalModifierReward r = (GlobalModifierReward)back;
        assertEquals("turret_damage", r.modifierId);
        assertEquals(0.15f, r.value, 0.0001f);
    }

    @Test
    void schematicRewardRoundTrip(){
        SchematicReward original = new SchematicReward("data-base64", "My Schematic", "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(SchematicReward.class, back);
        assertBaseFields(original, back);
        SchematicReward r = (SchematicReward)back;
        assertEquals("data-base64", r.schematicData);
        assertEquals("My Schematic", r.schematicName);
    }

    @Test
    void synergyRewardRoundTrip(){
        SynergyReward original = new SynergyReward("duo", "+10% duo damage", "n", "d");
        Reward back = roundTrip(original);

        assertInstanceOf(SynergyReward.class, back);
        assertBaseFields(original, back);
        SynergyReward r = (SynergyReward)back;
        assertEquals("duo", r.requiredUnlockId);
        assertEquals("+10% duo damage", r.bonusDescription);
    }
}
