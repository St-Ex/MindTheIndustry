# Tasks

- [ ] openspec change created (proposal/design/tasks)
- [ ] `net/MindTheIndustryPackets.java`: RewardChoicesPacket + RewardVotePacket
- [ ] `Rarity` gains a `Color hex` field (alongside chat markup)
- [ ] `client/ClientRewardState.java` (pure data holder)
- [ ] `ui/RewardChoicePanel.java` (3 icon buttons, rarity bar, hover tooltip, click → vote packet)
- [ ] Controller: send choices packet in onWaveComplete; empty packet on wave start + stopRun
- [ ] Mod: register packets, add panel on ClientLoadEvent, remove `reward` subcommand,
      add WorldLoadEvent fallback for serverController init (solo + multi guarantee)
- [ ] Tests: MindTheIndustryPacketsTest, ClientRewardStateTest; full suite green
- [ ] `./gradlew test` and `./gradlew jar` green
- [ ] Spec delta in openspec/specs/roguelike-run/spec.md; archive change; README/DECISIONS/AGENTS update
- [ ] Commit on main (no push without owner approval)
