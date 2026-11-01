# Proposal — reward-pacing-and-boss-window

## Why

Mindustry survival waves chain fast. The current reward flow does not structure the
run and lets players defer choices without consequence. Two problems:

1. **Pacing**: reward choices must not block the game, but must also not linger
   forever as open decisions. The natural metronome is the wave rhythm itself.
2. **Macro structure**: a roguelike run needs a heartbeat (Slay the Spire's
   elites/relics, FTL's sector jumps). Mindustry has none beyond wave numbers.

## What

- **Ubiquitous language (DDD)**: a **wave** is the spawn of a set of enemies; it
  ends when its last enemy dies. A **chapter** is a set of 10 waves
  (configurable), the last one featuring a boss. Waves arrive after a standard
  Mindustry countdown.
- At each wave end, every player gets a **choice**: 3 proposals, of which at most
  1 may be taken — or none, which converts (small resource grant in M1; the
  fallback is deliberately mediocre).
- **One choice at a time per player**: a choice remains available until the next
  one appears (typically the next wave end); the arrival of a new choice
  triggers the fallback conversion of the previous, unchosen one. **No timers**:
  the pacing is the wave rhythm.
- Effect scope: **bonus-malus apply to the player who took them** (consuming the
  entity-assignment system); **plans and resources apply to the group**.
- Killing a chapter boss opens a **boss window**: ceasefire (countdown frozen,
  no spawns), relic choice per player, exactly **one trade of an already-taken
  reward** between two players, entity assignment switches, and conversion of
  pending choices.
- Enemy scaling stays native Mindustry (Waves system) with a thin calibration
  layer (intervals, stat multipliers).
