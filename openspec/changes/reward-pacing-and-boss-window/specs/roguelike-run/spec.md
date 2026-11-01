# Delta — roguelike-run (MODIFIED + ADDED)

## MODIFIED Requirements

### Requirement: Wave rewards
The run SHALL reveal a choice of 3 proposals to each player when a wave ends
(last enemy of the wave set dies). At most 1 proposal may be taken; taking none
converts to a small resource grant. A choice remains available until the next
choice appears, at which point an unchosen choice converts. Bonus-malus effects
apply to the taking player (via entity assignment); plans and resources apply to
the group.

## ADDED Requirements

### Requirement: Chapters
The run SHALL group waves into chapters of `chapterLength` waves (default 10);
the last wave of each chapter SHALL feature a boss, using the native Mindustry
wave system with a boss-flagged unit.

### Requirement: Boss window
Killing the chapter boss SHALL open a ceasefire window (wave countdown frozen,
no spawns). During the window the run SHALL offer rare relic proposals per
player, allow exactly one trade of an already-taken reward between players, allow
entity active-player assignment switches, and convert all pending choices. The
window duration SHALL be a fixed configurable cap (default 5 min), not tied to
waveSpacing. The window SHALL close early when all connected players are ready,
or when the duration cap is reached. The run SHALL override the inter-wave
countdown to 30s (configurable) while preserving a long initial spacing (default
4 min) before wave 1.
