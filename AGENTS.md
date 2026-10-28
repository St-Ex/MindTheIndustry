# AGENTS.md — RogueIndustry

Règles pour tout agent travaillant sur ce dépôt. À lire avant toute modification.

## Contexte

Mod Mindustry (Java 17) : mode rogue-like coopératif. Les mécaniques de jeu sont décrites dans `DEVELOPMENT.md`.

## Environnement

- Java 17, géré par [mise](https://mise.jdx.dev) (`mise.toml`).
- Toutes les commandes passent par `mise exec --`.
- Ne jamais lancer le jeu, un serveur ou un watcher longue durée depuis un agent.

## Commandes

```bash
# Compiler
mise exec -- ./gradlew compileJava

# Build le mod (build/libs/mindtheindustryDesktop.jar)
mise exec -- ./gradlew jar

# Tests — harness headless, à utiliser systématiquement
mise exec -- ./gradlew test
```

## Harness de test (obligatoire)

- Les tests vivent dans `tests/` (sourceSet Gradle `test`), JUnit 5.
- `tests/rogueindustry/HeadlessRogueTest.java` boot Mindustry en **headless** (sans UI ni rendu), une seule fois par JVM de test. Toute classe de test métier étend cette classe.
- **Ne pas valider une évolution en lançant le jeu à la main** : écrire un test headless.
- Règles pour les tests :
  - Déterministes : seed fixe (`new Rand(42)`, `session.start(42)`).
  - Indépendants : si le test touche à `Vars.state.rules`, faire `state.rules = new Rules()` en début de test (état global partagé entre tests).
  - Couvrir la logique métier : `core/`, `rewards/`, `modifiers/`, `meta/`, sérialisation `net/`.

## Règles d'architecture

- **La logique métier ne dépend pas de `arc.scene`.** Le dossier `ui/` consomme l'état du jeu (`RogueSession`, contrôleurs) ; elle ne le contient pas. Toute mécanique (reward, modifier, synergie, vague) doit exister et être testable sans UI.
- Pas de contenu purement visuel sans contrepartie métier (pas de "juste un rendu").
- L'UI (`ui/`) n'est pas testable headless : on teste ce qu'elle affiche (état, schematics, contenu sérialisé), pas les pixels.

## Références

- **Sources du jeu** : `../Mindustry` — clone du tag `v160` (lecture seule), à côté de ce projet. Architecture moteur : `NetServer`, `Waves`, `Events`, `arc.scene.ui`.
- Pattern de boot headless : `../Mindustry/tests/src/test/java/ApplicationTests.java`.
- Wiki modding : https://mindustrygame.github.io/wiki/modding/1-modding/

## Git

- Commits en français, messages concis, un sujet par commit.
- Branche principale : `master`. Ne pas push sans validation explicite du propriétaire.
