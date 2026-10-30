# OpenSpec — Project Context

MindTheIndustry: a Mindustry mod (Java 17) adding a cooperative roguelike mode.
Waves get harder each round; after each cleared wave every player picks 1 of 3
random rewards (buildings, schematics, unit bonuses, global modifiers, synergies).
Commanders with unique abilities are picked at run start and unlock permanently
through meta-progression.

## Conventions

- Read `AGENTS.md` (rules) and `DECISIONS.md` (active decisions) first.
- Architecture & mechanics: `DEVELOPMENT.md`. Build: `mise build`. Tests: `mise exec -- ./gradlew test` (headless harness only — never launch the game manually).
- English only. Work on `main`. Never push without explicit owner approval.

## How we use OpenSpec here

- `openspec/specs/<capability>/spec.md` — the **current truth** of a capability (purpose, requirements, scenarios).
- `openspec/changes/<slug>/` — an **active proposal**:
  - `proposal.md` — the intent: why & what
  - `design.md` — how (only for non-trivial changes)
  - `tasks.md` — implementation checklist (agents execute this)
  - `specs/` deltas — ADDED / MODIFIED / REMOVED requirement deltas targeting `openspec/specs/`
- When the change lands (merged PR): apply the deltas to `openspec/specs/`, then move the change directory to `openspec/changes/archive/<slug>/`.
- Small fixes and refactors don't need a change entry — GitHub issue + PR is enough.
- Backlog (ideas for someday) lives in GitHub Issues, not here.
