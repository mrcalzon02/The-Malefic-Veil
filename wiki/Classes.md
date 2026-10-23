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
