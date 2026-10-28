# AGENTS.md — RogueIndustry

Rules for any agent working on this repository. Read before making any change.

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
- `tests/rogueindustry/HeadlessRogueTest.java` boots Mindustry **headless** (no UI, no rendering), once per test JVM. Every game-logic test class extends it.
- **Do not validate a change by launching the game manually**: write a headless test.
- Test rules:
  - Deterministic: fixed seed (`new Rand(42)`, `session.start(42)`).
  - Independent: if a test touches `Vars.state.rules`, do `state.rules = new Rules()` at the start of the test (global state shared between tests).
  - Cover game logic: `core/`, `rewards/`, `modifiers/`, `meta/`, `net/` serialization.

## Architecture rules

- **Game logic must not depend on `arc.scene`.** The `ui/` folder consumes game state (`RogueSession`, controllers); it does not contain it. Any mechanic (reward, modifier, synergy, wave) must exist and be testable without a UI.
- No purely visual content without a gameplay counterpart (no "just a render").
- The UI (`ui/`) is not testable headless: test what it displays (state, schematics, serialized content), not pixels.

## References

- **Game sources**: `../Mindustry` — clone of tag `v160` (read-only), next to this project. Engine architecture: `NetServer`, `Waves`, `Events`, `arc.scene.ui`.
- Headless boot pattern: `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- Modding wiki: https://mindustrygame.github.io/wiki/modding/1-modding/

## Git

- Commits in English, concise messages, one topic per commit.
- Development happens on `dev`; `main` is the main branch. Merge `dev` into `main` via pull request. Do not push without explicit approval from the owner.
