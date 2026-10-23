# Source of Truth and Integration Status

[Home](Home.md)

| Topic | Authoritative implementation or catalog | Integration statement |
|---|---|---|
| Items and icons | [economy/items.csv](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/items.csv), [icon index](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/assets/game/crafting/icons_index.csv) | 177 exported item records; much of specialty crafting not live |
| Field recipes | [SurvivalCrafting.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/SurvivalCrafting.java), [CSV](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/hand_recipes.csv) | 34 real domain formulas; player/GPU connection incomplete |
| Industrial recipes | [CraftingEraRegistry.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CraftingEraRegistry.java), [CSV](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/industrial_recipes.csv) | 62 registered processes; no complete physical factory runtime |
| Facilities | [CraftingEraRegistry.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CraftingEraRegistry.java), [CSV](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/facilities.csv) | 69 registered facilities; placement/power/production integration pending |
| Factions / classes | [FactionGroups.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionGroups.java), [FactionClasses.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionClasses.java) | Three persistent factions; nine class records, only 4 legacy ability IDs live |
| Weapon economy | [WeaponEconomy.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/WeaponEconomy.java), [weapons guide](Weapons.md) | Eight shared map pickups + four exclusive host-gated weapon recipes per faction; equip/fire/save restoration still gated |
| Lost spawn | [LostSpawnSites.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostSpawnSites.java) | Site-owned live NPC populations in city arena |
| Lost appearance | [LostNpcCatalog.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostNpcCatalog.java) | 15 source-audition variants are encounter metadata, not fully rigged live assets |
| Lost weapons | [LostArsenal.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostArsenal.java) | 25 detailed **design descriptors only**; not host-executed attacks |
| Combat | [ArenaSystems.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArenaSystems.java) | Live basic handgun/shotgun and Lost close attack; new arsenal not integrated |
| Trucks / hauling | [CargoHaulage.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CargoHaulage.java) | City resource loading/transport and cargo recovery; base delivery missing |
| City/continent | [CityLayout.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CityLayout.java), [ContinentLayout.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ContinentLayout.java) | Playable city and larger planning/render preview, not complete contiguous gameplay |
| World creation | [WorldCreationConfig.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/WorldCreationConfig.java) | Follow [end-to-end world tests](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/WorldCreationEndToEndTest.java) |

## Editorial rules

Every table must link to a code/CSV source. The wiki may describe a *planned* mechanic, but must name it as such. Do not infer complete assets from a manifest, completed gameplay from an enum or a released build from a pending CI job. Current release gates live in [ROADMAP](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/ROADMAP.md), [STATUS](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/STATUS.md), and individual system contracts. **No GitHub Actions workflow is added for this wiki**; use existing tests when source changes.
