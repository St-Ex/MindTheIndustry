# Proposal — reward-pacing-and-boss-window

## Why

Mindustry survival waves chain fast. The current reward flow does not define *when*
players must choose, and nothing structures the run: waves are an endless stream
with no macro rhythm. Two problems:

1. **Pacing**: reward choices must not block the game, but unconstrained choice
   deferral lets rational players bank options and pick later with full
   information — the "choice" becomes a spreadsheet decision.
2. **Macro structure**: a roguelike run needs a heartbeat (Slay the Spire's
   elites/relics, FTL's sector jumps). Mindustry has none beyond wave numbers.

## What

- Each cleared wave reveals **3 proposals per player** (solo choices, including
  group-benefit ones). Reveal happens **right after the wave-clear stress peak**;
  the discovery is part of the fun and is not announced in advance.
- A **30-second timer** (run-configurable) applies. Unchosen proposals convert
  automatically to a **small resource grant** (the deliberately mediocre
  fallback). Choices also **expire at the next boss**.
- Waves come in **groups of ~10** (configurable) ending with a **boss wave**,
  reusing Mindustry's native wave system (spawn budget, unit types, boss flag)
  with a thin calibration layer (intervals, stat multipliers) instead of a
  custom wave engine.
- Killing the boss opens a **boss window**: a ceasefire period (next wave timer
  frozen, no spawns) used for rebuilding, relic picks (bigger/rarer rewards,
  per player with time to deliberate), **one reward trade between players**, and
  the only moment where **entity active-player assignment may be switched**.
- Enemy difficulty scaling stays native Mindustry; the mod plugs onto it.
