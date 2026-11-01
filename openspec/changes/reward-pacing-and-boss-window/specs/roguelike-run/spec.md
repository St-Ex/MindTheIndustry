# Delta — roguelike-run (MODIFIED + ADDED)

## MODIFIED Requirements

### Requirement: Wave rewards
The run SHALL reveal 3 proposals per player when a wave is cleared. The choice is
individual (even for group-benefit proposals), revealed after the wave clear,
subject to a configurable timer (default 30s) after which unchosen proposals
convert to a small resource grant.

## ADDED Requirements

### Requirement: Boss cadence
The run SHALL trigger a boss wave every `bossInterval` waves (default 10), using
the native Mindustry wave system with a boss-flagged unit.

### Requirement: Boss window
Killing the boss SHALL open a ceasefire window (wave countdown frozen, no
spawns). During the window the run SHALL offer rare relic proposals per player,
allow exactly one reward trade between players, allow entity active-player
assignment switches, and expire all pending wave proposals. The window SHALL
close when all players are ready or after a safety timeout.
