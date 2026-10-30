# Proposal: procedural run planet (MindTheIndustry menu entry)

## Why

The current entry points are broken in practice:

- HUD Start/Stop buttons do nothing in solo and only send a chat line in
  multiplayer — the `/mindtheindustry` command was registered **client-side
  only**, so the server never handles it.
- The commander selection dialog cannot be interacted with (votes go through
  the same unreachable command / through packets that don't loop back in solo).

Starting a run must be one click, identical in solo and in multiplayer.

## What

1. Add a **MindTheIndustry planet** to the New Game planet menu (`alwaysUnlocked =
   true` so it shows without campaign progress). Its start sector launches a map
   **based on the vanilla `labyrinth` map** via a `SectorPreset` (`default/labyrinth`).
   The planet also carries a procedural generator (`SerpuloPlanetGenerator`
   subclass, randomized `seedOffset` per launch) for any sector without a preset.
   Later: replace labyrinth with a dedicated map built in the map editor (backlog).
2. Launching that sector **auto-starts the roguelike run** (commander choice
   via chat + `/mindtheindustry vote`).
3. Remove the broken in-game UI (HUD buttons, commander/reward dialogs,
   countdown overlay) and all client→server control packets. Control flow goes
   exclusively through the **server-registered** `/mindtheindustry` command,
   which works identically in solo (integrated server) and multiplayer (host).
   The only packet kept is `SchematicUnlockPacket` (S→C): schematic data cannot
   travel through chat.
4. Reward/commander choices are presented as **chat messages** with indices;
   players answer with `/mindtheindustry reward <i> [target]` /
   `/mindtheindustry vote <i>`.
