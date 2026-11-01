# Tasks — reward-pacing-and-boss-window

- [ ] Add run config: choiceTimer (default 30s), bossInterval (default 10),
      windowTimeout (default 60s), fallback resource amounts.
- [ ] Session state machine (WAVE / CHOICE / BOSS / BOSS_WINDOW) with tick-driven
      timers, headless-testable.
- [ ] Proposal reveal on wave clear (per player, seeded).
- [ ] Timer expiry → fallback resource conversion (auto-convert on leave too).
- [ ] Proposal expiry at boss window open.
- [ ] Boss wave injection via native Waves (flag boss unit every bossInterval).
- [ ] Spawn interval / stat multiplier calibration hook (values deferred to
      balance pass).
- [ ] Ceasefire: freeze wave countdown, suppress spawns during window.
- [ ] Relic proposal pool (rare/boosted) generated per player at window open.
- [ ] Trade mechanic: one per window, mutual confirmation.
- [ ] Assignment switch restricted to boss window (rejection outside).
- [ ] Ready-signal + safety timeout to close the window.
- [ ] Net packets + serialization tests (relic, trade, ready, switch).
- [ ] Headless tests for every acceptance criterion (fixed seeds).
