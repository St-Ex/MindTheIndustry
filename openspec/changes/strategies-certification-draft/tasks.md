# Tasks — strategies-certification-draft

- [ ] Data model for Strategy (id, name, theme, pool weights, ability) replacing
      commander registration; migration/mapping of Vanguard, Architect,
      Industrialist.
- [ ] Draft sequence: random order (seeded), announcement, pick + lock.
- [ ] Lobby state: hold builder spawn and wave countdown until all picked.
- [ ] AFK auto-pick timeout (default 60s).
- [ ] Late-join handling: random remaining Strategy, announced.
- [ ] Reward generator: strategy weights compose with typed weights; no hard
      exclusions.
- [ ] Strategy card content: 5 initial strategies (Kanban, Lean, Stock zéro,
      Six Sigma, Agile) with displayed bias.
- [ ] Meta-progression: reuse commander unlock conditions for Strategy unlocks.
- [ ] Net packets: draft order, pick, lock, lobby-ready.
- [ ] Headless tests for every acceptance criterion.
