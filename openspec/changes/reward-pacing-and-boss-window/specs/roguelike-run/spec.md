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

### Requirement: Chapter structure
A chapter SHALL consist of a 5-minute start break (configurable) followed by 9
waves and 1 boss wave (10 waves per chapter). The start break of chapters after
the first SHALL open on the previous chapter's boss death, as a ceasefire (wave
countdown frozen, no spawns), and SHALL offer rare relic proposals per player,
allow exactly one trade of an already-taken reward between players, allow entity
active-player assignment switches, and convert all pending choices. The break
SHALL close early when all connected players are ready, or at the duration cap
(default 5 min). The run SHALL override the inter-wave countdown to 30s
(configurable) and keep a 5-minute break before wave 1.

### Requirement: Victory
The final chapter's boss death SHALL trigger the victory screen directly, with
no break and no next chapter. The chapter count SHALL be run-configurable
(default 3), giving winWave = chapters x 10.
