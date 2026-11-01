# Proposal — strategies-certification-draft

## Why

With a fully open reward pool, any player can drift into any build: identities
blur, the group homogenizes, and success stops feeling earned. Roguelikes solve
this with classes (Slay the Spire characters, FTL ships): an **upfront, visible
constraint** on the reward RNG. MTI has commanders, but they add an ability, not
an identity. We rename and repurpose them as **Strategies** (working display
name: "Certifications") — the class system of the run.

## What

- Commanders become **Strategies**, themed after real-world industry/logistics
  methods, half-jokingly: Kanban, Lean, Stock zéro / Just-in-time, Six Sigma,
  Agile/Scrum.
- A Strategy **biases the player's reward pool by type** (logistics, buildings,
  units, liquids, quality) — a **weighted bias, never an exclusive constraint**:
  runs can still fail, and failure must remain possible ("you earn your win").
- Strategies are picked at run start through a **draft**: a randomly announced
  player order; **first taker locks a Strategy for the rest of the group**.
- While any player has not picked, the run stays in a **lobby state**: no builder
  units spawned, wave countdown frozen. Safety timeout auto-picks a remaining
  Strategy for AFK players (default 60s).
- Existing meta-progression (commander unlocks) carries over to Strategies.
