# Tasks — reward-pacing-and-boss-window

- [x] Run config: chapterLength (default 10), windowTimeout (default 60s),
      fallback resource amounts, relic pool weights.
- [x] Playable start: pre-unlocked floor at run start (basic
      extraction/transport/storage/defense; provisional contents marked
      balance pass).
- [x] Ubiquitous language in code: Wave/Chapter/Choice/BossWindow types named
      accordingly (rename existing wave-* concepts to match).
- [x] Wave-end detection per wave set (last enemy of the set dies) — extend the
      existing `checkWaveComplete` polling.
- [x] Per-player ChoiceState: 3 proposals on wave end (seeded), ≤1 pick,
      explicit "take none" decision.
- [x] Conversion on replacement: arrival of a new choice converts the previous
      unchosen one (fallback resources to group cores).
- [x] Choice persistence across countdown and next-wave combat (no blocking).
- [x] Demolish the M0 vote gate: `waitingForVotes`, wave-timer freeze, building
      disable during vote, `checkAllVotesReceived` — replaced by the overlay model.
- [x] Effect scoping: bonus-malus per player (via entity assignment), plans and
      resources for the group.
- [x] Chapter tracker: boss wave at chapter end via native Waves (boss flag).
- [x] Calibration hook for spawn interval / enemy stat multipliers (values
      deferred to balance pass).
- [x] Ceasefire during boss window (countdown frozen, spawns suppressed).
- [x] Relic pool (rare/boosted) per player at window open; pending choices
      convert.
- [x] Trade: one already-taken reward transfer per window, mutual confirmation.
- [x] Assignment switch restricted to the boss window.
- [x] Ready signal + safety timeout to close the window.
- [x] Victory: `runChapters` run config (default 3, `winWave = chapters x 10`);
      final boss death shows the victory screen directly (no break, no next
      chapter).
- [x] Victory screen copy: GG title + corporate-satire flavor subtitle via
      bundle key `mti.victory.flavor` in `bundles/bundle.properties`
      (canonical EN, no hardcoded literals).
- [x] Localization setup: mod bundle files (`bundles/bundle.properties`
      canonical EN + locale variants matching Mindustry's bundle locales,
      e.g. `bundles/bundle_fr.properties` with the FR victory subtitle);
      verify automatic fallback to the default bundle for missing keys.
- [x] Net packets + serialization tests (choice, relic, trade, ready, switch).
- [x] Headless tests for every acceptance criterion (fixed seeds).
