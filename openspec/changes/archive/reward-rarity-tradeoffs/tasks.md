# Tasks

- [ ] openspec change created (proposal/design/tasks)
- [ ] `rewards/Rarity.java` (NORMAL/RARE/EPIC, color, multiplier, weighted roll)
- [ ] `RewardType` → TRADEOFF/PLAN/RESOURCE; `Reward` gains `rarity` (write/read);
      `RewardSerializer` rewritten for the 3 types
- [ ] New `TradeoffReward`, `PlanReward`, `ResourceReward`
- [ ] Delete old reward subclasses, `SchematicGenerator`, `net/` package,
      `MindTheIndustryPackets.register()` call, session schematic machinery
- [ ] New `modifiers/UnitBonusType`; update `UnitBonus` + session references
- [ ] Rewrite `core/RewardGenerator` (category x rarity, magnitude scaling)
- [ ] Rewrite tests: RewardGeneratorTest, RewardSerializerTest, RewardApplyTest;
      update MindTheIndustrySessionTest; delete SchematicGeneratorTest,
      MindTheIndustryPacketsTest
- [ ] `./gradlew test` and `./gradlew jar` green
- [ ] Apply spec delta to `openspec/specs/roguelike-run/spec.md`; archive change
      under `openspec/changes/archive/reward-rarity-tradeoffs/`
- [ ] Update README features + DECISIONS.md reward bullet
- [ ] Commit on main (no push without owner approval)
