# Delta — roguelike-run (MODIFIED + ADDED)

## MODIFIED Requirements

### Requirement: Commanders
Renamed to **Strategies** (display name "Certifications"): picked at run start
through an announced random-order draft. A Strategy taken by one player is
locked for the rest of the group. Strategies unlock permanently through
meta-progression with the same conditions as commanders.

## ADDED Requirements

### Requirement: Draft lobby
Until every connected player has picked a Strategy, the run SHALL remain in a
lobby state with no builder units spawned and no wave countdown. A safety
timeout SHALL auto-pick a remaining Strategy for players who have not chosen.

### Requirement: Strategy pool bias
Each Strategy SHALL bias the player's reward proposals toward its theme via
weights (target ~70/30), displayed at draft time. The bias SHALL never exclude
reward types outright.
