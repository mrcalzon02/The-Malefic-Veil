# The Lost — Complete Arsenal (Design Registry)

[Home](Home.md) · [The Lost](The-Lost.md) · [Gameplay](Gameplay.md)

**State: REGISTERED DESIGN, NOT ACTIVE NPC ATTACKS.** These 25 profiles are traceable to [LostArsenal.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostArsenal.java). The host currently performs basic close-range 8 HP / 8 sanity attacks at a 1.2-second cooldown; it does **not** cast these 25 weapon behaviors. Damage, sanity, range, timing and telegraphs below are **starting balance targets**, not shipping statistics. Each family gets five attack tools, but a single NPC should have a constrained encounter loadout rather than simultaneously spamming its whole catalog.

**Roster philosophy:** progression unlocks new behavior, perception, coordination, environmental exploitation and counterplay; it does **not** simply multiply level-scaled health and damage. The same-family elite profile is a rare hard encounter, not an automatic upgrade of every spawn. A player's escape/counter action should reliably cancel or mitigate dangerous attacks.

## Drifters

Ruined civilians using scavenged objects. They move between cover and punish careless close-quarters looting; irregular aim and scarce ammunition distinguish them from trained soldiers.

| Role / weapon | Delivery | Reach | HP + sanity | Telegraph (windup) | Counter / recovery | Distinct behavior |
|---|---|---:|---|---|---|---|
| **bone shiv** <a id="lost-drifter-bone-shiv"></a> · OPENING | MELEE | 1.4m | 12 HP + 1 sanity | arm low; rusted point catches light (0.3s) | step sideways and force a miss (0.65s) | quick two-step approach; single close stab |
| **pipe hook** <a id="lost-drifter-pipe-hook"></a> · PRESSURE | MELEE | 2.1m | 15 HP + 2 sanity | pipe scrapes asphalt before a broad hook (0.52s) | back away beyond arc or interrupt windup (0.88s) | wide stagger arc pushes prey into chokepoints |
| **pocket revolver** <a id="lost-drifter-pocket-revolver"></a> · CONTROL | PROJECTILE | 11m | 11 HP + 0 sanity | distinct cylinder click and extended arm (0.95s) | break sight; punish lengthy reload (1.85s) | rare inaccurate shot; finite rounds per encounter |
| **shard toss** <a id="lost-drifter-shard-toss"></a> · AREA | PROJECTILE | 6m | 7 HP + 2 sanity | glass clinks inside a raised fist (0.7s) | leave marked scatter cone (1.4s) | short cone of glass fragments; limited ground persistence |
| **salvage cleaver** <a id="lost-drifter-salvage-cleaver"></a> · ELITE | MELEE | 2m | 23 HP + 4 sanity | overhead two-hand cleaver pose (1s) | dodge lateral recovery and counterattack (1.35s) | armor-splitting committed blow; never silent one-hit kill |

## Zealots

Human remnants organized around broken rites. They punish clustered squads and drain sanity; powerful casts are noisy and readable. Zealots are present in the source-candidate catalog, but current city site spawns do not select them.

| Role / weapon | Delivery | Reach | HP + sanity | Telegraph (windup) | Counter / recovery | Distinct behavior |
|---|---|---:|---|---|---|---|
| **censer mace** <a id="lost-zealot-censer-mace"></a> · OPENING | MELEE | 1.9m | 12 HP + 5 sanity | chain rattles and ember flare (0.45s) | avoid short chained swing; punish reset (0.88s) | physical impact plus modest sanity burn |
| **ash lash** <a id="lost-zealot-ash-lash"></a> · PRESSURE | MELEE | 4m | 9 HP + 7 sanity | corded lash cracks before strike (0.73s) | move into cover; cut diagonally inside reach (1.15s) | mid-range displacement and mark for pursuit |
| **confession bell** <a id="lost-zealot-confession-bell"></a> · CONTROL | WAVE | 7m | 2 HP + 16 sanity | clear bell lift and pulsing circle (1.25s) | break line of sight or exit warning ring (2.1s) | sanity-shock wave; brief sound distortion, capped duration |
| **votive embers** <a id="lost-zealot-votive-embers"></a> · AREA | HAZARD | 5.5m | 6 HP + 8 sanity | two-handed bowl held aloft; ash falls (1.1s) | avoid outlined consecration patch (2.25s) | slow-burning ground strip discourages camping |
| **reliquary nailgun** <a id="lost-zealot-reliquary-nailgun"></a> · ELITE | PROJECTILE | 10m | 17 HP + 4 sanity | ratcheting reliquary charge (1.15s) | duck hard cover then close during reload (1.9s) | three separately simulated, avoidable spike shots |

## Stalkers

Predators exploiting occlusion and movement. Flank paths, pursuit marks and physical leaps matter more than raw HP. Their attacks must respect the same geometry and sound readability as player movement.

| Role / weapon | Delivery | Reach | HP + sanity | Telegraph (windup) | Counter / recovery | Distinct behavior |
|---|---|---:|---|---|---|---|
| **talon rake** <a id="lost-stalker-talon-rake"></a> · OPENING | MELEE | 1.5m | 13 HP + 3 sanity | shoulders bunch; claws turn outward (0.25s) | keep spacing and listen for scrape (0.74s) | fast lateral swipe at exposed flanks |
| **hook leap** <a id="lost-stalker-hook-leap"></a> · PRESSURE | CHARGE | 5.5m | 17 HP + 5 sanity | creature compresses and hisses at target (0.95s) | sidestep landing corridor; exploit miss recovery (1.65s) | telegraphed pounce ending in finite collision |
| **spine dart** <a id="lost-stalker-spine-dart"></a> · CONTROL | PROJECTILE | 9m | 8 HP + 5 sanity | spines flare along back (0.68s) | use cover; do not sprint straight back (1.35s) | low damage dart briefly impairs sprint, not paralysis |
| **tendon snare** <a id="lost-stalker-tendon-snare"></a> · AREA | HAZARD | 4m | 3 HP + 8 sanity | ground scratching and pale cord growth (1.05s) | spot and destroy marked cord (2s) | temporary visible foot trap; per-player immunity after escape |
| **mirror backstab** <a id="lost-stalker-mirror-backstab"></a> · ELITE | MELEE | 1.3m | 26 HP + 9 sanity | distorted reflection and rear-foot scrape (1.3s) | turn toward warning audio; back near solid cover (2.1s) | rear-flank bonus only after actual verified positioning |

## Brutes

Heavy obstacles that control doors and roads through displacement. Strong impacts require long tells, collision checks and large punish windows.

| Role / weapon | Delivery | Reach | HP + sanity | Telegraph (windup) | Counter / recovery | Distinct behavior |
|---|---|---:|---|---|---|---|
| **hammerfist** <a id="lost-brute-hammerfist"></a> · OPENING | MELEE | 2.2m | 20 HP + 2 sanity | fist rises high with deep exhale (0.65s) | sidestep the marked impact point (1.1s) | heavy single-target slam with manageable knockback |
| **girder sweep** <a id="lost-brute-girder-sweep"></a> · PRESSURE | MELEE | 3.5m | 15 HP + 4 sanity | metal grinds across rubble (1.15s) | retreat behind sturdy geometry (1.75s) | very broad physical arc with extended recovery |
| **rubble heave** <a id="lost-brute-rubble-heave"></a> · CONTROL | PROJECTILE | 9m | 18 HP + 1 sanity | both hands pry up a visible chunk (1.4s) | move behind solid cover during lift (2.2s) | slow ballistic rock, interceptable collision |
| **ground rupture** <a id="lost-brute-ground-rupture"></a> · AREA | WAVE | 4.5m | 13 HP + 5 sanity | both feet plant; cracked ground pulses (1.55s) | jump or leave indicated fracture zone (2.7s) | ground-bound short ring that interrupts stationary aims |
| **wrecking charge** <a id="lost-brute-wrecking-charge"></a> · ELITE | CHARGE | 8m | 29 HP + 6 sanity | long roar and straight-line stomp buildup (1.6s) | take lateral cover; exploit hard stop on impact (3s) | high commitment straight-line collision, no corner teleport |

## Wailers

Fragile sanity specialists. Attacks are announced through visual/sound cues, with clear exit routes. A Wailer should not stack unrecoverable sanity drains.

| Role / weapon | Delivery | Reach | HP + sanity | Telegraph (windup) | Counter / recovery | Distinct behavior |
|---|---|---:|---|---|---|---|
| **glass shriek** <a id="lost-wailer-glass-shriek"></a> · OPENING | WAVE | 6m | 3 HP + 13 sanity | inhalation and visible throat glow (0.62s) | break line of sight; seek acoustic shelter (1.2s) | short-direction sanity cone rather than silent damage |
| **breath siphon** <a id="lost-wailer-breath-siphon"></a> · PRESSURE | WAVE | 4m | 5 HP + 11 sanity | head tracks and surrounding sound dulls (1s) | leave narrow tether range (1.55s) | channeled attrition ends on occlusion or distance |
| **resonance lance** <a id="lost-wailer-resonance-lance"></a> · CONTROL | PROJECTILE | 12m | 8 HP + 14 sanity | straight purple filament charges visibly (1.25s) | dodge perpendicular to the beam (2.05s) | narrow aimed sonic bolt; no through-wall hit |
| **grief chorus** <a id="lost-wailer-grief-chorus"></a> · AREA | HAZARD | 6m | 2 HP + 10 sanity | multiple spectral mouths flare in an expanding circle (1.6s) | exit slow pulse boundary (2.4s) | pulsing sanity field; no stacking duplicate effects |
| **silence bloom** <a id="lost-wailer-silence-bloom"></a> · ELITE | HAZARD | 5.5m | 3 HP + 20 sanity | all ambience drops then violet rings appear (2s) | withdraw before final pulse; watch floor ring (3.2s) | large telegraphed finale that disrupts sound cues briefly |

## Attack execution contract

When implemented, each attack needs host-side line of sight, geometry/collision, stamina or charges where appropriate, target eligibility (including monster/Begotten allegiance), active windup, capped active effect, post-attack recovery, networking, particle/audio cues, difficulty tests and interrupted-action cleanup. State IDs must be stable, not client-chosen. **Projectile** means a real trace or simulated collision path; **HAZARD** means a visible bounded zone with expiry; **WAVE** means a correctly obstructed directional/radial pulse; **CHARGE** means physical traversed space, never teleport damage.

Minimum viable enemy loadouts: one OPENING + one context-selected PRESSURE or CONTROL; add AREA only to an elite squad coordinator; ELITE only after clear rarity and site cues. Specialist attacks cannot silently chain stun a protected respawn, kill behind solid walls or apply unlimited debuff stacks.

## Weapon progression beyond level scaling

- **Tier A — Unsettled:** opening attacks, loud pursuit, telegraphed individual decision-making.
- **Tier B — Learned:** conditional pressure option, line-of-sight checking, smarter cover and target selection.
- **Tier C — Coordinated:** control or area tool, synchronized approach with *bounded* nearby allies; no infinite summons.
- **Tier D — Ruin Guardian:** one elite technique and clearer visual/aural threat signature, adaptive response to repeated player tactics, protect a source ruin or site objective.

Levels may modify bounded durability and impact, but tier, site role, weapon availability and behavior are **separate dimensions**. Loadouts are decided from stable site seed + archetype + encounter role; no loot reroll on every frame or inconsistent save reload. Recovered weapons and drops require their own legal economy, asset and ownership gate, not automatic material creation.

Source for current lives/slots: [LostSpawnSites.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostSpawnSites.java) and [ArenaSystems.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArenaSystems.java).
