# Proposal — tech-tree-and-unit-lifespan

## Why

Two systems that make production chains the heart of the roguelike:

1. **Random building/unit unlocks are unbalanceable**: bonus-malus that bet on a
   building "we might unlock someday" cannot be calibrated. A purchasable tech
   tree gives a deterministic, priceable backbone — and it is native Mindustry
   (research trees). The tree unlocks **for the whole group**: identity comes
   from bonus-malus and entity assignment, never from lock-outs. Deliberately
   rejected: forced multi-player construction ("we must be three to build this")
   — frustrating as soon as someone is absent.
2. **Units stack infinitely** in vanilla survival: army snowball erases tower
   defense tension. Giving units a **lifespan measured in waves** creates a new
   metric (reward effects "units live longer"), keeps production chains
   running, and opens entire build archetypes: alpha-strike waves, disposable
   JIT swarms, factories-as-turrets on the battlefield.

## What

- A dedicated **tech currency** — event-based, not mineable, not transportable
  (a session counter, not a map resource). Sources: forfeited rewards (the main
  sink for non-aligned proposals) and kills (units/bosses) — the latter opens
  the door to "seek tougher waves" risk-reward, later.
- A **group tech tree** (buildings and units). Unlocked plans are shared by all
  players. Units require the unit plan **and** a built producer building.
- Anyone can build anything unlocked; a player without construction bonuses
  builds slower/costlier but is **never blocked**.
- **Unit lifespan**: default units are persistent with a **cap** (some builds
  raise the cap). Run modifiers may offer fast-produced units that last a
  number of waves. **Excess units** above the cap are still produced but carry
  an expiry timer. Lifespan is measured in **waves, not seconds** (predictable,
  readable, compatible with the boss window).
