# RogueIndustry — Mode Rogue-like pour Mindustry

Un mod qui transforme Mindustry en roguelike coopératif : à chaque vague réussie,
chaque joueur choisit des récompenses aléatoires (bâtiments, plans, bonus d'unités).

## Environnement de développement

### Prérequis
- [mise](https://mise.jdx.dev/) installé (`~/.local/bin/mise`)
- Java 17 (géré automatiquement par mise)

### Commandes

```bash
# Se placer dans le projet
cd /home/stex/projects/mindtheindustry

# Build le mod (génère build/libs/mindtheindustryDesktop.jar)
mise exec -- ./gradlew jar

# Compiler seulement
mise exec -- ./gradlew compileJava

# Nettoyer
mise exec -- ./gradlew clean
```

### Tester le mod

1. Build le JAR : `mise exec -- ./gradlew jar`
2. Copier `build/libs/mindtheindustryDesktop.jar` dans le dossier `mods/` de Mindustry
   - Linux Steam : `~/.local/share/Steam/steamapps/compatdata/451480/pfx/drive/c/users/steamuser/AppData/Roaming/Mindustry/mods/`
   - Ou lancer Mindustry avec le JAR en argument
3. Lancer Mindustry, activer le mod dans le menu Mods
4. Héberger une partie en solo ou multijoueur
5. Dans le chat, taper `/rogue start` pour démarrer une run

### Commandes en jeu

| Commande | Description |
|----------|-------------|
| `/rogue start` | Démarre une run rogue-like |
| `/rogue stop` | Arrête la run en cours |
| `/rogue skip` | Force le passage à la vague suivante (tue tous les ennemis) |
| `/rogue status` | Affiche l'état de la run (vague, seed, actif/inactif) |

## Architecture

```
src/rogueindustry/
├── RogueIndustryMod.java          # Point d'entrée, commandes, init
├── core/
│   ├── RogueSession.java          # État serveur: unlocks, bonus, modifiers
│   ├── ServerRogueController.java # Orchestration serveur (vagues, votes)
│   ├── ClientRogueController.java # Orchestration client (choix, votes)
│   └── RewardGenerator.java       # Génération des 3 choix par joueur
├── rewards/
│   ├── Reward.java                # Classe abstraite
│   ├── RewardType.java            # BUILDING, SCHEMATIC, UNIT_BONUS, GLOBAL_MOD, SYNERGY
│   ├── RewardSerializer.java      # Sérialisation réseau
│   └── rewards/
│       ├── BuildingReward.java    # Débloque bâtiment pour TOUS
│       ├── SchematicReward.java   # Débloque plan pour UN joueur
│       ├── UnitBonusReward.java   # Bonus unités d'un joueur CIBLE
│       ├── GlobalModifierReward.java # Bonus pour TOUS
│       └── SynergyReward.java     # Bonus conditionnel
├── modifiers/
│   ├── UnitBonus.java             # Bonus d'unité (dégâts, vie, vitesse...)
│   ├── GlobalModifier.java        # Modificateur global
│   └── GlobalModifierRegistry.java # Registre des modificateurs
├── net/
│   └── RoguePackets.java          # Tous les packets réseau
└── ui/
    └── RewardDialog.java          # Dialogue de choix (3 cartes)
```

## Mécaniques de récompense

| Type | Portée | Description |
|------|--------|-------------|
| **Building** | Tout le groupe | Débloque un bâtiment pour tous les joueurs |
| **Schematic** | Joueur individuel | Débloque un plan utilisable par un seul joueur |
| **Unit Bonus** | Joueur cible | Bonus permanent sur les unités d'un joueur (au choix) |
| **Global Mod** | Tout le groupe | Bonus de run (vitesse prod, dégâts, etc.) |
| **Synergy** | Tout le groupe | Bonus conditionnel si prérequis débloqué |

## Prochaines étapes (TODO)

- [ ] Phase 2: Implémenter SchematicReward (génération de plans aléatoires)
- [ ] Phase 2: Implémenter UnitBonusReward (application sur unités existantes + futures)
- [ ] Phase 3: RogueHUD (affichage vague, modifiers, bonus)
- [ ] Phase 3: Meta-progression entre les runs
- [ ] Phase 3: Équilibrage de la courbe de difficulté
- [ ] Phase 3: Contenu custom (bâtiments/unités spécifiques au mode)

## Notes & Ressources

- **Server UI Builder System** (arc.scene.ui) — riche en possibilités pour construire des interfaces custom (Table, Button, Tex, Styles). À explorer pour améliorer les dialogues du mod (sélection commandant, récompenses, HUD).
- **Wiki modding** : https://mindustrygame.github.io/wiki/modding/1-modding/
- **Mindustry v160.7** — build 160, Java 17
