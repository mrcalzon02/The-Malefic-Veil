# Classes — Nine Exclusive Roles

[Home](Home.md) · [Factions](Factions.md) · [Gameplay](Gameplay.md)

There are **three classes per faction**. Classes are not transferable between factions, and appearance selection cannot change an ability loadout. Four legacy gameplay IDs (0–3) have actual prototype effects; the remaining five are registered concepts only. The complete passive / primary / secondary / signature design, costs, cooldowns and counters is in the [ability codex](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/FACTION_LORE_AND_ABILITY_CODEX.md); those ability values are *provisional*, not live balance.

| Faction | Class | ID | Battlefield identity | Current integration |
|---|---|---|---|---|
| The Unraveled | **Warden** | `unraveled-warden` | Anchor / defensive devices | Legacy Warden ability ID 0; wider catalog registered |
| The Unraveled | **Stitcher** | `unraveled-stitcher` | Field rescue / improvised medicine | Legacy Medic ability ID 1; wider catalog registered |
| The Unraveled | **Hollow** | `unraveled-hollow` | Concealment / infiltration | Legacy Shade ability ID 2; wider catalog registered |
| The Penitent | **Confessor** | `penitent-confessor` | Sanity / repulsion | Legacy Occultist ability ID 3; future suite registered |
| The Penitent | **Flagellant** | `penitent-flagellant` | Risk-based melee pressure | Registered design; not playable |
| The Penitent | **Votary** | `penitent-votary` | Group rites / protection | Registered design; not playable |
| The Begotten | **Harrower** | `begotten-harrower` | Stalk / isolate prey | Registered design; not playable |
| The Begotten | **Devourer** | `begotten-devourer` | Brutal endurance / hunger | Registered design; not playable |
| The Begotten | **Mourner** | `begotten-mourner` | Dread / area corruption | Registered design; not playable |

**Progression.** Persistent progression tracks marks­manship, athletics, engineering, scavenging and survival in `CharacterProgression`. Character class unlocks and skills must be server-owned. The launcher may preview locked classes, but must not silently substitute an ability or treat cosmetic PSX casting as a playable class.

Source: [FactionClasses.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionClasses.java), [ClassAbilityCatalog.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ClassAbilityCatalog.java), [class matrix](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/FACTION_CLASS_MATRIX.md) and [character roster](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CharacterRoster.java).

## Complete ability metadata

All 36 abilities below are stable design descriptors from [ClassAbilityCatalog.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ClassAbilityCatalog.java). **The cooldown/duration/meter values are design goals, not proof of a working ability.** The four playable legacy IDs are implemented in their earlier form; their full four-slot designs remain unimplemented. Read [faction ability codex](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/FACTION_LORE_AND_ABILITY_CODEX.md) for counterplay and balance intent.

### Warden (`unraveled-warden`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Brace** | Reduced stagger while stationary | — | — | — |
| PRIMARY | **Interlock** | Temporary ballistic screen | 24 | 15 | — |
| SECONDARY | **Holdfast** | Defensive stagger-resistance circle | 28 | 8 | — |
| SIGNATURE | **Last Redoubt** | Visible allied damage-resistance field | — | 12 | 80 |

### Stitcher (`unraveled-stitcher`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Triage** | Injury awareness and faster medical use | — | — | — |
| PRIMARY | **Suture Shot** | Interrupted heal over time | 22 | 3 | — |
| SECONDARY | **Field Tourniquet** | Status resistance and wound relief | 30 | 8 | — |
| SIGNATURE | **Keep Breathing** | Interruptible stabilization and healing pulses | — | 8 | 80 |

### Hollow (`unraveled-hollow`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Stillness** | Reduced visibility while motionless | — | — | — |
| PRIMARY | **Thinning** | Semi-transparent concealment | 28 | 7 | — |
| SECONDARY | **False Footsteps** | Audible decoy | 25 | 5 | — |
| SIGNATURE | **Between Seconds** | Brief stealth mobility window | — | 6 | 80 |

### Confessor (`penitent-confessor`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Litany** | Ambient sanity-drain mitigation | — | — | — |
| PRIMARY | **Rebuke** | Telegraphed psychic displacement | 24 | — | — |
| SECONDARY | **Forced Truth** | Line-of-sight target mark | 26 | 6 | — |
| SIGNATURE | **The Great Confession** | Sanity-pressure ritual zone | — | 8 | 80 |

### Flagellant (`penitent-flagellant`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Testament** | Enemy-damage-triggered speed | — | 4 | — |
| PRIMARY | **Blood Price** | Health-for-melee-power | 22 | 7 | — |
| SECONDARY | **Thorned Vow** | Capped melee damage reflection | 30 | 5 | — |
| SIGNATURE | **Ecstasy of Ruin** | High-risk melee and speed enhancement | — | 10 | 80 |

### Votary (`penitent-votary`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Shared Burden** | Nearby ally sanity resilience | — | — | — |
| PRIMARY | **Vigil Flame** | Visible ritual recovery circle | 28 | 10 | — |
| SECONDARY | **Psalm of Binding** | Short telegraphed slow | 30 | 3 | — |
| SIGNATURE | **Ninefold Vigil** | Choir field support and warning | — | 12 | 80 |

### Harrower (`begotten-harrower`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Scent of Fear** | Directional awareness of low-sanity prey | — | — | — |
| PRIMARY | **Skitter** | Collision-swept predator dash | 20 | — | — |
| SECONDARY | **Listening Horror** | Movement-listening pulses | 26 | 5 | — |
| SIGNATURE | **The Long Hunt** | LOS-bound pursuit mark | — | 10 | 80 |

### Devourer (`begotten-devourer`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Carrion Memory** | Capped heal on valid kill participation | — | — | — |
| PRIMARY | **Rending Rush** | Telegraphed collision charge | 26 | — | — |
| SECONDARY | **Hardened Husk** | Slower but tougher | 32 | 5 | — |
| SIGNATURE | **Hunger Made Flesh** | Capped enemy-melee lifesteal | — | 10 | 80 |

### Mourner (`begotten-mourner`)

| Slot | Ability | Description | Cooldown | Duration | Meter |
|---|---|---|---:|---:|---:|
| PASSIVE | **Funeral Hush** | Localized auditory suppression | — | — | — |
| PRIMARY | **Grief Pool** | Visible slowing sanity pool | 28 | 7 | — |
| SECONDARY | **Echo of Loss** | Short sound-indicator disruption | 30 | 4 | — |
| SIGNATURE | **Black Lament** | Telegraphed dread field | — | 10 | 80 |

