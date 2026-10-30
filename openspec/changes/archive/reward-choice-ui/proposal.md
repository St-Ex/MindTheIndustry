# Proposal: clickable reward choices in the HUD instead of the chat command

## Why

Picking a reward with `/mindtheindustry reward <index>` is bad UX: the player
must read chat, remember an index, and type a command mid-strategy-phase. The
choices should be visible as icons in a HUD box anchored near the wave display,
with a hover tooltip describing each reward, and a click to select.

## What

1. **Packets** (`net/MindTheIndustryPackets`, recreated):
   - `RewardChoicesPacket` (S→C): wave + the player's 3 serialized rewards
     (`Reward.write`/`read` already exist). An empty list means "hide the box".
   - `RewardVotePacket` (C→S): the clicked index; the server runs the existing
     `ServerMindTheIndustryController.handleVote`.
2. **Client state** (`client/ClientRewardState`): pure data holder (wave +
   choices + visible flag) — no `arc.scene`, so it stays headless-testable.
3. **HUD panel** (`ui/RewardChoicePanel`): a table added to `ui.hudGroup` on
   `ClientLoadEvent`, visible only while the server says choices are pending.
   3 icon buttons (trade-off → `Icon.chartBar`, plan → the unlocked content's
   `uiIcon`, resource → the item's `uiIcon`), a rarity-colored bar under each
   icon, hover shows displayName + description, click sends `RewardVotePacket`.
   The `ui/` layer only renders state and sends intents — all game logic stays
   server-side in `core/`.
4. **Server authority**: `onWaveComplete` still generates choices and now also
   sends each player their `RewardChoicesPacket`; when the wave starts (all
   voted) or the run stops, an empty packet clears the box. The chat
   announcement stays as a text fallback/log.
5. **Server running in solo AND multi**: the server controller is initialized
   on `ServerLoadEvent` (fires in solo too — Mindustry solo runs an integrated
   server) with `PlayEvent`/`WorldLoadEvent` fallbacks, so the mod works
   identically in solo, hosted and dedicated games.
6. The `/mindtheindustry reward` subcommand is removed (the HUD replaces it);
   the other subcommands (`start|stop|skip|status|...`) stay for run control.

## Out of scope

- Commander selection at run start stays command-based for now (separate change).
- No modal dialog; the box floats above the HUD and does not block input.
