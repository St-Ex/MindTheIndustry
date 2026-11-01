# AGENTS.md — MindTheIndustry

Rules for any agent working on this repository. Read `DECISIONS.md` (active decisions) before making any change.

## Context

Mindustry mod (Java 17): a cooperative roguelike mode. Game mechanics are described in `DEVELOPMENT.md`.

## Environment

- Java 17, managed by [mise](https://mise.jdx.dev) (`mise.toml`).
- All commands go through mise: `mise <task>` (tasks defined in `mise.toml`) or `mise exec --`.
- Never launch the game, a server or a long-running watcher from an agent.

## Commands

```bash
# Compile
mise exec -- ./gradlew compileJava

# Build the mod (build/libs/mindtheindustryDesktop.jar)
mise build

# Tests — headless harness, use systematically
mise exec -- ./gradlew test
```

## Test harness (mandatory)

- Tests live in `tests/` (Gradle `test` sourceSet), JUnit 5.
- `tests/mindtheindustry/HeadlessMindTheIndustryTest.java` boots Mindustry **headless** (no UI, no rendering), once per test JVM. Every game-logic test class extends it.
- **Do not validate a change by launching the game manually**: write a headless test.
- Test rules:
  - Deterministic: fixed seed (`new Rand(42)`, `session.start(42)`).
  - Independent: if a test touches `Vars.state.rules`, do `state.rules = new Rules()` at the start of the test (global state shared between tests).
  - Cover game logic: `core/`, `rewards/`, `modifiers/`, `meta/`, `client/`, `net/` serialization. UI (`ui/`) is not headless-testable.

## Architecture rules

- **Game logic must not depend on `arc.scene`.** The `ui/` layer only renders client state (`ClientRewardState`) and sends intents (vote packets); every mechanic (reward, modifier, wave) lives in `core/` and must be testable without a UI.
- No purely visual content without a gameplay counterpart (no "just a render").
- Run control is the server-side `/mindtheindustry` command (works in solo and multi); reward picking is the HUD box (`ui/RewardChoicePanel`) fed by `RewardChoicesPacket`.
- **User-facing strings are localized**: all in-game text goes through Mindustry bundle keys (`bundles/bundle.properties` as the canonical EN reference, plus locale variants matching Mindustry's own bundle locales, e.g. `bundle_fr.properties`), never hardcoded Java literals. Mindustry falls back to the default bundle for missing keys/locales, so partial coverage is safe. Code, docs, openspec and commits remain English-only.

## Ubiquitous language (DDD, November 2026)

Agents and code must use these terms consistently:

- **Wave**: the spawn of a set of enemies; it ends when its last enemy dies. Not the Mindustry wave counter.
- **Chapter**: 5-minute start break + 9 waves + 1 boss wave (10 waves, configurable). The start break doubles as the post-boss window.
- **Choice**: the 3 proposals offered to a player at wave end. At most 1 may be taken; taking none converts. One choice at a time per player: a choice remains available until the next one appears — the arrival of a new choice triggers the fallback conversion of the previous, unchosen one. No timers.
- **Boss window**: the chapter start break after a boss kill (ceasefire: relic choice, one trade of an already-taken reward, assignment switches). The final chapter's boss death goes straight to victory — no break.
- **Strategy**: the run "class" picked at the draft (Kanban, Lean, Stock zero, Six Sigma, Agile).

## Design direction (decided, October 2026)

- This section is the **target design** agreed in the October 2026 session. Until the M1-M3 changes land, the current truth remains `DECISIONS.md` and `openspec/specs/`; when they conflict, the specs win for what is built next, `DECISIONS.md` wins for what the mod does today.
- **Blocking principle (owner, Nov 2026)**: the only hard block in the whole game is the strategy draft lobby (no builders spawned, no countdown). After the draft, nothing blocks: wave countdowns and chapter-end boss windows are simply **longer waits** during which the world keeps simulating (build, mine, produce). Never gate wave progression on player input.
- Bring the roguelike **through Mindustry's own mechanics** (supply chains, logistics, territory, tech trees). Altering core behaviours positively or negatively is in-scope; bolting a generic roguelike on top is not.
- Build identity comes from **typed bonus-malus and entity assignment**, never from lock-outs: unlocks are group-wide, anyone can build anything unlocked (slower/costlier without bonuses), and forced multi-player construction is rejected.
- Reward pools: **weighted bias is allowed, hard exclusions are not** (target ~70/30, see strategies change). Unit lifespans/timers are measured in **waves, not seconds**. Player builder-unit destruction keeps vanilla respawn behaviour — no roguelike penalty on it.
- Enemy waves: **plug onto the native Mindustry wave system** (intervals, stat multipliers, boss flags). No parallel wave engine.
- Numeric balance values (fallback amounts, scaling curves, tech prices, unit caps) are deliberately **frozen**: do not invent them while implementing — mark them "balance pass" and defer (issue tracker).
- **Flavor & tone (owner, Nov 2026)**: the mod's voice is corporate satire (assignments, bench time, PTO, certifications, Man-Hours). Flavor text is welcome on screens and events (first instance: the GG victory screen) but never gates a mechanic or widens a milestone's scope.

## Roadmap (playtest-gated)

Implement **one change at a time**; each milestone must be validated by a real co-op playtest before stacking the next:

1. **M1** — `openspec/changes/reward-pacing-and-boss-window/` — loop pacing, boss cadence, boss window.
2. **M2** — `openspec/changes/strategies-certification-draft/` — identities, draft, lobby.
3. **M3** — `openspec/changes/tech-tree-and-unit-lifespan/` — tech currency, group tree, unit lifespans.

Backlog (4-square UI, zone branching, elite waves, balance pass) lives in GitHub Issues. The 4-square UI deliberately waits until the M1-M3 session states exist (relics, trade, tech currency) to avoid permanent catch-up.

## Intent tracking (openspec)

- Backlog (ideas for someday): GitHub Issues.
- Non-trivial features: create `openspec/changes/<slug>/` with `proposal.md` (why/what), `design.md` (how), `tasks.md` (checklist). `openspec/specs/` holds the current truth; apply deltas there when the change lands, then archive the change dir.
- Small fixes and refactors: straight to a PR, no openspec entry.
- Format and lifecycle: `openspec/project.md`.

## References

- **Game sources**: `../Mindustry` — clone of tag `v160` (read-only), next to this project. Engine architecture: `NetServer`, `Waves`, `Events`, `arc.scene.ui`.
- Headless boot pattern: `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- Modding wiki: https://mindustrygame.github.io/wiki/modding/1-modding/
- License: GPL-3.0-or-later, derivative of Mindustry (GPL-3.0). See `LICENSE`.
- **Full custom campaign example**: [NewHorizonMod](https://github.com/Yuria-Shikibe/NewHorizonMod) — custom planet, sectors, tech tree. Reference for campaign content; clone next to the project if needed: `git clone --depth 1 https://github.com/Yuria-Shikibe/NewHorizonMod ../NewHorizonMod`.

## Git

- Commits in English, concise messages, one topic per commit.
- **PR-first workflow** (see `DECISIONS.md`): one branch per change, one **worktree per PR** (`git worktree add ../MindTheIndustry-<slug> -b <branch>`) so agents/devs never share a dirty checkout; `main` receives changes only via merged PRs. Do not push without explicit approval from the owner.
- If a branch diverges from its origin with duplicate commits, rebase onto origin (duplicates drop automatically). Never force-push.
