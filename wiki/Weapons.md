# Weapons — Scavenging, Manufacturing, and Combat Gates

[Home](Home.md) · [Items](Items.md) · [Recipes](Recipes.md) · [Gameplay](Gameplay.md) · [Lost Arsenal](Lost-Arsenal.md)

**Current status: 8 seeded city weapon types and 12 craftable physical weapon items; the added weapons cannot yet be equipped or fired.** The older handgun/shotgun firing code remains active as an independent legacy system. World pickups grant real weighted inventory items through host-authoritative acquisition; the twelve recipes use the existing atomic ingredient-debit crafting engine. No client can create weapon inventory by claiming its own pickup position or a fabricated recipe success.

## Eight spawned city weapon types

Each world seed allocates one instance of every weapon below to navigable RESOURCE city plots. Legacy pickup slots 0–169 are preserved; kinds 7–14 are appended rather than reshuffling existing pickup IDs. An item already taken is not granted twice. If the backpack is full, the pickup stays in the world.

| World weapon | Item ID | Carry weight | Battlefield intent | Pickup kind |
|---|---|---:|---|---:|
| **Rusted Crowbar** | `RUSTED_CROWBAR` | 4 | Melee control | 7 |
| **Fire Axe** | `FIRE_AXE` | 5 | Heavy melee | 8 |
| **Nail Bat** | `NAIL_BAT` | 4 | Improvised impact | 9 |
| **Survival Knife** | `SURVIVAL_KNIFE` | 2 | Fast blade | 10 |
| **Old Revolver** | `OLD_REVOLVER` | 5 | Sidearm | 11 |
| **Pump Shotgun** | `PUMP_SHOTGUN` | 9 | Scattergun | 12 |
| **Hunting Rifle** | `HUNTING_RIFLE` | 8 | Precision firearm | 13 |
| **Compact SMG** | `COMPACT_SMG` | 7 | Automatic firearm | 14 |

## Twelve player-crafted weapons

| Weapon | Manufacturer | Recipe | Station | Ingredients | Mass |
|---|---|---|---|---|---:|
| **Wooden Club** | **UNRAVELED** | [weapon-wooden-club](Recipes.md#handweapon-wooden-club) | HAND | 2× `TIMBER`, 1× `FIBER` | 3 |
| **Stone Shiv** | **PENITENT** | [weapon-stone-shiv](Recipes.md#handweapon-stone-shiv) | HAND | 2× `FIELD_STONE`, 1× `FIBER` | 2 |
| **Stone Hatchet** | **BEGOTTEN** | [weapon-stone-hatchet](Recipes.md#handweapon-stone-hatchet) | HAND | 3× `FIELD_STONE`, 2× `TIMBER`, 2× `FIBER` | 4 |
| **Wooden Spear** | **BEGOTTEN** | [weapon-wooden-spear](Recipes.md#handweapon-wooden-spear) | HAND | 3× `TIMBER`, 2× `FIBER`, 1× `FIELD_STONE` | 4 |
| **Short Bow** | **BEGOTTEN** | [weapon-short-bow](Recipes.md#handweapon-short-bow) | HAND | 3× `TIMBER`, 4× `FIBER` | 4 |
| **Spiked Club** | **PENITENT** | [weapon-spiked-club](Recipes.md#handweapon-spiked-club) | WORKBENCH | 3× `TIMBER`, 2× `RAW_ORE`, 2× `FIBER` | 5 |
| **Reinforced Spear** | **PENITENT** | [weapon-reinforced-spear](Recipes.md#handweapon-reinforced-spear) | WORKBENCH | 5× `TIMBER`, 3× `RAW_ORE`, 3× `FIBER` | 5 |
| **Salvage Machete** | **BEGOTTEN** | [weapon-salvage-machete](Recipes.md#handweapon-salvage-machete) | WORKBENCH | 5× `RAW_ORE`, 2× `TIMBER`, 1× `FIBER` | 5 |
| **Hand Crossbow** | **PENITENT** | [weapon-hand-crossbow](Recipes.md#handweapon-hand-crossbow) | WORKBENCH | 4× `TIMBER`, 4× `FIBER`, 2× `RAW_ORE` | 5 |
| **Pipe Pistol** | **UNRAVELED** | [weapon-pipe-pistol](Recipes.md#handweapon-pipe-pistol) | WORKBENCH | 6× `RAW_ORE`, 1× `MACHINE_PARTS` | 5 |
| **Bolt Rifle** | **UNRAVELED** | [weapon-bolt-rifle](Recipes.md#handweapon-bolt-rifle) | WORKBENCH | 8× `RAW_ORE`, 2× `MACHINE_PARTS` | 8 |
| **Scrap Shotgun** | **UNRAVELED** | [weapon-scrap-shotgun](Recipes.md#handweapon-scrap-shotgun) | WORKBENCH | 7× `RAW_ORE`, 1× `METAL_STOCK` | 9 |

## Exclusive production, shared scavenging and trading

**The Unraveled:** Wooden Club, Pipe Pistol, Bolt Rifle and Scrap Shotgun. **The Penitent:** Stone Shiv, Spiked Club, Reinforced Spear and Hand Crossbow. **The Begotten:** Stone Hatchet, Wooden Spear, Short Bow and Salvage Machete. This allocation preserves current stable item IDs; names and component formulas remain provisional pending dedicated faction-themed equipment and materials.

The permanent **host-owned faction enrollment** determines which weapons players can manufacture. The host validates ownership before consuming ingredients; an unenrolled player or another faction cannot forge these items, even using raw craft packets. The GPU recipe book and field-craft list filter other factions' formulas, and the index/CSV show owners. **Found weapons, transfers and trading remain faction-neutral:** exclusive *manufacturing* does not mean faction-exclusive possession. Separate specialist material supply chains remain an open development gate.

## Current operating behavior

Eight physical weapons are seeded and rendered as distinct **original low-poly interim proxy meshes**. Host pickup validates range, unconsumed slot and the 200-weight inventory limit, then broadcasts depletion and refreshes the collector's inventory. The 12 recipes are registered directly in `SurvivalCrafting`; `hand` recipes use immediate resources, while workbench recipes need legitimate station access and advanced firearms may require delivered truck-only `METAL_STOCK` or `MACHINE_PARTS`. The latter cannot be manufactured by bypassing the base freight economy.

Carried weapons use existing item-trade/drop and death-clearing rules. Added pickup flags are append-only so older realm snapshots with 170 pickup flags can be read and expanded safely. However, **survival inventory restoration through account/realm restart is still an open gate**, so obtaining a weapon is not yet a durable equipment commitment.

## What still must be implemented

Real server-owned **equip/unequip, weapon slots, weapon-specific ammunition, magazine/loading, melee collision, firearm shot execution, weapon recoil, sound, first-person model rigs, damage feedback and NPC equip/loadouts**. These are required before the player may use the new weapon entries in a live fight. The original handgun and shotgun remain the only live firing modes. Distinct verified weapon icons and final PSX asset conversion remain pending. Workbench placement in the current city is illegal, so the advanced craft chain also depends on functional wilderness stations.

These twenty names are the **new source-backed initial roster** created after the existing repository was found to lack the previously discussed eight-weapon implementation. They are *not* represented as a recovered, previously verified canonical naming list. Earlier intended weapon identities must be reconciled before content lock.

Source: [WeaponEconomy.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/WeaponEconomy.java) · [SurvivalCrafting.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/SurvivalCrafting.java) · [World.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/World.java) · [ArenaSystems.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArenaSystems.java) · [WeaponEconomyTest.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/WeaponEconomyTest.java).
