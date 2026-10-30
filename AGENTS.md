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
- `tests/mindtheindustryindustry/HeadlessMindTheIndustryTest.java` boots Mindustry **headless** (no UI, no rendering), once per test JVM. Every game-logic test class extends it.
- **Do not validate a change by launching the game manually**: write a headless test.
- Test rules:
  - Deterministic: fixed seed (`new Rand(42)`, `session.start(42)`).
  - Independent: if a test touches `Vars.state.rules`, do `state.rules = new Rules()` at the start of the test (global state shared between tests).
  - Cover game logic: `core/`, `rewards/`, `modifiers/`, `meta/`, `net/` serialization.

## Architecture rules

- **Game logic must not depend on `arc.scene`.** The `ui/` folder consumes game state (`MindTheIndustrySession`, controllers); it does not contain it. Any mechanic (reward, modifier, synergy, wave) must exist and be testable without a UI.
- No purely visual content without a gameplay counterpart (no "just a render").
- The UI (`ui/`) is not testable headless: test what it displays (state, schematics, serialized content), not pixels.

## Intent tracking (openspec)

- Backlog (ideas for someday): GitHub Issues.
- Non-trivial features: create `openspec/changes/<slug>/` with `proposal.md` (why/what), `design.md` (how), `tasks.md` (checklist). `openspec/specs/` holds the current truth; apply deltas there when the change lands, then archive the change dir.
- Small fixes and refactors: straight to a PR, no openspec entry.
- Format and lifecycle: `openspec/project.md`.

## References

- **Game sources**: `../Mindustry` — clone of tag `v160` (read-only), next to this project. Engine architecture: `NetServer`, `Waves`, `Events`, `arc.scene.ui`.
- Headless boot pattern: `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- Modding wiki: https://mindustrygame.github.io/wiki/modding/1-modding/
- **Full custom campaign example**: [NewHorizonMod](https://github.com/Yuria-Shikibe/NewHorizonMod) — custom planet, sectors, tech tree. Reference for campaign content; clone next to the project if needed: `git clone --depth 1 https://github.com/Yuria-Shikibe/NewHorizonMod ../NewHorizonMod`.

## Git

- Commits in English, concise messages, one topic per commit.
- Development happens directly on `main` (owner decision, see `DECISIONS.md`). Do not push without explicit approval from the owner.
- If a branch diverges from its origin with duplicate commits, rebase onto origin (duplicates drop automatically). Never force-push.
