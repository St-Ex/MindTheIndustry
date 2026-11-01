# Active changes

One directory per active proposal: `openspec/changes/<slug>/` containing
`proposal.md` (why/what), `design.md` (if non-trivial), `tasks.md` (checklist)
and spec deltas under its own `specs/` folder.

Lifecycle: propose → review (PR) → implement `tasks.md` → apply spec deltas to
`openspec/specs/` → move this directory to `changes/archive/<slug>/`.

Active proposals (from the October 2026 design session):

- `reward-pacing-and-boss-window` — wave choice pacing (30s timer, resource
  fallback), boss every ~10 waves, boss ceasefire window (relics, one trade,
  assignment switch), native wave system reuse.
- `strategies-certification-draft` — commanders become Strategies
  ("Certifications"): visible pool bias (Kanban, Lean, Stock zéro, Six Sigma,
  Agile), draft with lock, lobby until all picked.
- `tech-tree-and-unit-lifespan` — event-based tech currency, group-wide tech
  tree, unit cap with wave-based expiry for excess units.
