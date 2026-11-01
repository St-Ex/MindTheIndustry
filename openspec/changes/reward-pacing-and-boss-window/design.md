# Design — reward-pacing-and-boss-window

## Reward choice (per wave)

- Trigger: wave cleared → proposals revealed for each connected player.
- Scope: each player gets **3 solo proposals**; some benefit the whole group but
  the *choice* is always individual. No group voting during waves (too slow,
  too heavy while defending).
- Timer: `30s` default, part of run config. Expiry → proposals convert to a
  small resource grant (fallback, intentionally weak). Proposals not consumed
  also expire when the boss window opens.
- Auto-convert rules: disconnect/leave → auto-convert; AFK → auto-convert on
  timer (no block).

## Boss cadence and boss window

- One boss wave every `bossInterval = 10` waves (run config, not a constant).
- Boss = vanilla enemy unit flagged as boss via the native Waves system; the
  mod may thicken the wave budget around it but must not build a parallel wave
  generator.
- Boss window = **ceasefire, not a pause**: the next wave countdown freezes and
  no spawns happen; the world keeps simulating (players may build/mine freely).
  This is required because multiplayer Mindustry has no true pause.
- During the window:
  - **Relic choice**: each player gets a rare/boosted proposal (rarer pool);
    individual picks, but there is time to discuss build rotations.
  - **Trade**: exactly **one** reward exchange between two players per window
    (both must confirm). Costs the window slot — a deliberate cap so trades stay
    meaningful.
  - **Assignment switch**: entity active-player reassignment is possible *only*
    during boss windows (see acceptance criteria). This turns optimization into
    a bet on the next wave group rather than mid-fight micro.
  - **Expiry cleanup**: all pending wave proposals convert/expire.
- Window ends when all players confirm "ready" (or a safety timeout, default
  `60s` after the last relic pick) → countdown resumes.

## Enemy scaling

- Reuse the vanilla Waves table of the run map as the backbone; the mod adjusts:
  spawn interval (faster), unit stat multipliers (calibration layer driven by
  group power — exact curve deferred to the balance pass, see tasks).
- No custom wave content in this change; elite/branching content is out of scope.

## Edge cases

- Solo run: the window works identically (trade and switch simply unused).
- Player joins mid-run: gets proposals from the next wave clear only.
- Disconnect during the window: pending relic converts to resources.
- Boss killed while players still choosing: window still opens; pending choices
  expire-convert.

## Architecture

- Session gains a small state machine: `WAVE → CHOICE(timer) → … → BOSS →
  BOSS_WINDOW → WAVE`. Timers live in the session (tick-driven), fully
  headless-testable; no `arc.scene` dependency.
- Net: new packets needed for relic proposals, trade confirmation, ready signal
  and assignment switch (extend the existing `net/` serialization; deterministic,
  seed-synced).
- UI (later, separate change): 4-square click UI consumes the same session state;
  the chat commands remain the fallback.

## Acceptance criteria (headless-testable)

1. Seeded run: proposals appear on wave clear for each player; deterministic
   content for a fixed seed.
2. Unchosen proposal converts to resources after the configured timer.
3. Boss wave triggers at wave `k*10`; boss flagged unit present.
4. Boss killed → wave countdown frozen, no spawns until window closes.
5. Entity assignment switch succeeds during the window and is rejected outside.
6. Exactly one trade per window; second trade rejected.
7. Pending proposals expire-convert when the window opens.
