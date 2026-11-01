# Delta — roguelike-run (ADDED)

## ADDED Requirements

### Requirement: Tech currency
The run SHALL maintain a progression currency per session, obtainable from
forfeited proposals and kills, not producible, storable or transportable on the
map.

### Requirement: Standard pack
The run SHALL start with a named, curated baseline of unlocked techs (the
standard pack), identical across runs without certification mutations, defined
as data (declarable and diffable). The pack SHALL include a protected floor of
survival-critical basics that never varies. Certification mutations (M2) MAY
add or remove techs above the floor, group-wide, at the draft.

### Requirement: Group tech tree
The run SHALL offer a purchasable tech tree unlocking building and unit plans
for the entire group. Unit plans additionally require a built producer. Any
player may construct any unlocked plan (without bonuses if they have none);
no constructible plan SHALL be exclusive to one player.

### Requirement: Unit lifespan
Units SHALL be persistent up to a configurable per-player cap. Production above
the cap SHALL still spawn units carrying a wave-based expiry. Expiring units
SHALL be removed at the end of their expiry wave. Run modifiers MAY produce
fast, short-lived units. Lifespan SHALL be measured in waves, not seconds.
Player builder unit destruction SHALL keep vanilla respawn behaviour without
roguelike penalty.
