# Design — reward-pacing-and-boss-window

## Ubiquitous language

- **Wave** = one spawn of a set of enemies; ends at the last enemy's death.
- **Chapter** = 10 waves (configurable `chapterLength = 10`); last wave features a boss.
- **Choice** = 3 proposals per player at wave end; ≤1 taken; none → conversion.
- **Boss window** = ceasefire between chapters.

The codebase names must match (e.g. `WaveState`, `ChapterTracker`, `ChoiceState`,
`BossWindow`); the Mindustry wave counter is referenced as "wave number" only
where unavoidable (vanilla interop).

## Choice flow

- Trigger: wave end (last enemy of the wave set dies) → choice revealed for each
  connected player. The discovery happens right after the stress peak; it is
  never announced in advance.
- A choice stays open through the inter-wave countdown AND through the following
  wave's combat, until the next choice appears (next wave end, or boss window
  open). At that moment an unchosen choice converts automatically (fallback).
- Taking none of the 3 proposals is a valid, always-available decision →
  conversion (small resource grant, group cores).
- Disconnect → pending choice converts. Late joiner → first choice at the next
  wave end. AFK → no block: the choice simply converts when replaced.
- Effect scope:
  - **Bonus-malus**: applied to the **taking player**, consumed via the
    entity-assignment system (an entity only benefits from its active player's
    bundle — no cherry-picking across players).
  - **Plans / resources**: applied to the **group**.
- Relic choice (boss window) follows the same rules with a rarer pool;
  arrival of the relic choice converts any pending wave choice.

## Chapter structure (5 min — 9 waves — boss wave)

- A **chapter** = **5-minute start break + 9 waves + 1 boss wave** (10 waves).
- The start break IS the post-boss window — one notion, not two:
  - Chapter 1: pure initial build time (5 min, `chapterBreakDuration = 300s`).
  - Chapters 2+: the break opens on the previous chapter's boss death — a
    ceasefire (`waveTimer = false`, no spawns; the world keeps simulating,
    multiplayer Mindustry has no true pause) — and hosts the boss-window
    features:
    - **Relic choice** per player (rare/boosted pool), one-at-a-time rules.
    - **Trade**: exactly one exchange between two players of an
      **already-taken reward** (effects transfer); mutual confirmation.
    - **Assignment switch**: entity active-player reassignment allowed *only*
      during a break.
    - Pending choices convert at break open.
  - The break is a **cap, not a wait**: all-ready closes it early; the 5-minute
    cap only bounds AFK players.
- **Victory (GG)**: the FINAL chapter's boss death triggers the victory screen
  directly — no break, no next chapter. Fixed chapter count
  (`runChapters`, default 3) → `winWave = runChapters * 10` (default 30).
  The screen shows the GG title plus a corporate-satire flavor subtitle
  (owner, Nov 2026): "GG — Mission accomplished. Next assignment under
  negotiation. Don't forget to take your PTO during your bench time."

## Vanilla interop (verified against Mindustry v160 source)

- Vanilla has **no post-boss window**: a boss is just a unit with `StatusEffects.boss`
  (native boss health bar), and after the campaign `winWave` waves are simply
  disabled (`sectorCapture()` sets `state.rules.waves = false`).
- Our window is therefore built from existing knobs:
  - Ceasefire = `state.rules.waveTimer = false` (freezes the countdown while the
    world keeps simulating — the same mechanism the M0 vote gate uses; we demolish
    the gate, keep the trick).
  - Window duration = own timer while `waveTimer = false` (decoupled from
    `waveSpacing`, which the run overrides to 30s).
- `Logic.skipWave()` is native and public (reusable for the wave-skip backlog item).
- `state.rules.waitEnemies` already pauses the countdown while enemies remain
  (already enabled on MTI maps).

## Run pacing targets (owner, Nov 2026)

- Vanilla survival default `waveSpacing` = **120s** (`Rules.java`:
  `waveSpacing = 2 * Time.toMinutes`) — but the mod overrides it to **30s**
  (owner, Nov 2026: combats already take long; the countdown must not add dead
  time). Config knob: `runWaveSpacing = 30s`.
- `initialWaveSpacing` is set explicitly to **300s** (5 min before wave 1):
  vanilla's `<= 0 → waveSpacing * 2` fallback would collapse to 60s with the
  30s spacing — the room for initial base building is preserved instead.
- Chapter = 5-min break + 9 waves + boss wave: ~4.5 min of countdown + combat
  durations (`waitEnemies = true` freezes the countdown while enemies live, so
  combat time stacks on top and **dominates** the chapter length) → ≈ 20-30 min
  for typical fights.
- Target run length: **3 chapters ≈ 1h-1h30** (combat-dominant). Targets, not
  hard rules: the balance pass owns the numbers.
- Victory (decided, Nov 2026): fixed chapter count (`runChapters`, default 3);
  the final boss death goes straight to the victory screen (GG) — no break, no
  endless mode.

## Playable start (protected floor)

- A fresh run SHALL start with a **floor of pre-unlocked techs** — enough to
  build basic supply and defense from wave 0 (extraction, transport, basic
  turret class). Owner observation (Nov 2026): the current build starts with
  the bastion only — the 5-minute initial break has nothing to build.
- The floor is the **protected core of the standard pack** (tech-tree change,
  M3): it never varies between runs and cannot be removed by certifications.
- Contents: a provisional list marked "balance pass" — category coverage
  (extraction, transport, storage, basic defense) is structural; exact nodes
  are not to be tuned while implementing.
- M3 formalizes the floor as data inside the named standard pack; M1 ships a
  provisional unlock list.

## Flavor text (corporate satire tone)

- The mod's voice is **corporate satire** (consulting/ESN humor: assignments,
  bench time, PTO, certifications, Man-Hours). Flavor text on screens and
  events is welcome; the victory screen is the first instance.
- Rules: never a hardcoded Java literal — always a Mindustry bundle key
  (first instance: `mti.victory.flavor`). The **default EN bundle is the
  canonical reference**; end-user text is localized into the locales
  Mindustry ships bundles for (e.g. `bundle_fr.properties`), with automatic
  fallback to the default bundle for missing keys/locales — partial
  coverage is safe. English-only remains the rule for code, docs, openspec
  and commits.
- FR variant of the victory subtitle (owner's original wording):
  « GG — Mission accomplie. Prochaine affectation en cours de négociation.
  N'oubliez pas de poser vos congés pendant votre inter-contrat. »
- Flavor never gates a mechanic and never widens a milestone's scope.

## Enemy scaling

- Reuse the vanilla Waves table as backbone; the mod adjusts spawn interval and
  unit stat multipliers via a calibration hook (values deferred to the balance
  pass). No custom wave engine, no elite content in this change.

## Architecture

- Session state machine per run: `WAVE → CHOICE(open) → WAVE → … → BOSS →
  BOSS_WINDOW → WAVE`; the choice is not a blocking phase — it overlays the
  countdown and the next wave. All state tick-driven, headless-testable, no
  `arc.scene` dependency.
- Per-player `ChoiceState`: current proposals, pending pick, conversion on
  replacement.
- Net: packets for choice reveal/pick, relic, trade confirmation, ready signal,
  assignment switch (extend `net/`, seed-synced).

## Acceptance criteria (headless-testable)

1. Seeded run: at each wave end (last enemy death), each player receives 3
   proposals; deterministic per seed.
2. Picking none, or being replaced by the next choice, converts to a small
   resource grant; taking 2 of 3 is rejected.
3. A choice opened at wave N is still openable during wave N+1 combat; it
   converts exactly when the next choice appears.
4. Bonus-malus taken by player A apply via A's entity assignment only; plans
   and resources apply to the group.
5. Boss wave at wave `k * chapterLength`; boss flagged unit present.
6. Boss killed → countdown frozen, no spawns until window closes.
7. Trade: one already-taken reward transfer per window; second trade rejected.
8. Assignment switch succeeds during the window, rejected outside.
9. Pending choices convert at window open.
10. Victory: final chapter boss death (`runChapters` default 3 → wave 30)
    triggers the victory state directly — no break, no wave 31 — and the
    flavor subtitle resolves from the bundle key `mti.victory.flavor`
    (canonical EN copy, no hardcoded literal in game-logic code).
11. Playable start: a fresh run's floor of basic techs is unlocked and
    buildable from wave 0 (deterministic; does not vary between runs).
