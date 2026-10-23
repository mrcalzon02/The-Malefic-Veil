# Gameplay Systems

[Home](Home.md) · [Classes](Classes.md) · [Recipes](Recipes.md)

## The live city prototype

A Java 17 PSX-style urban horror FPS. The Java2D launcher supports solo and experimental LAN host/join; the GPU/libGDX executable is primarily a **visual environment walkthrough**, not yet feature parity with the shooter. Headless server authority covers shots, the playable city, inventory pickups, deployable gadgets, vehicles and some persistent state. Check [source README](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/README.md) before assuming release readiness.

**Combat.** Player weapons presently use a handgun and shotgun with finite magazines, host-run fire cooldowns, line-of-sight, damage and ammunition debit. Health and sanity are independent resources. Visibility/stealth matters; taking hostile action breaks respawn protection. The new Lost weapons registry is [designed, not wired into NPC attacks](Lost-Arsenal.md).

**Pickup and inventory.** Cash, scrap, ammunition and healing resources are subject to host validation. An advanced survival inventory has weight 200 and hunger/hydration rules as domain code; full authenticated inventory integration remains gated. Death removes carried resources; fully designed corpse recovery requires a preexisting built graveyard, **not** a free death chest.

**Construction.** Arena players can place four combat devices with authoritative cost and clearance checks. These are **not** unrestricted city buildings. Permanent facilities belong on legal wilderness claims only once that system is streamed and active.

**Survival/crafting.** Field recipes have server-compatible recipe/domain validation with ingredient debit; industry is staged from STRANDED → ENCAMPMENT → SALVAGE → STEAM → ELECTRIC → AUTOMATION → INCURSION. The [recipe index](Recipes.md) includes station, faction and costs. Recipe UI integration remains unfinished.

**Vehicles/cargo.** City cars and trucks have host-mediated driving/occupancy. `METAL_STOCK` and `MACHINE_PARTS` are distinct truck-only pallets at limited city depots; yard replenishment, damaged-truck cargo ejection and recovery exist. Wilderness-base delivery/industrial offload are not playable yet.

**PvP, PvE and respawn.** Three faction allegiances preserve same-faction alliance. Lost attack non-Begotten characters in the existing host loop. Server-owned spawn safety, death timer and short *nonoffensive* protection support reentry; hard-core hosting can reset relevant character progression on death. Read [hardcore design](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/HARDCORE_FACTION_ALLIANCES_RPG.md) and [death policy](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/GRAVEYARD_DEATH_AND_ITEM_RECOVERY.md) for the distinction between code and target behavior.

**AI.** Dedicated-host settlers have decision policies and faction affiliations. The Lost have separate site-owned combat and spawning; their currently live attacks are simple pursuit and short-range health/sanity damage, not the full multi-attack design roster.

Sources: [ArenaSystems](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArenaSystems.java) · [SurvivalCrafting](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/SurvivalCrafting.java) · [CargoHaulage](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CargoHaulage.java) · [FactionGroups](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionGroups.java) · [world creation](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/WORLD_CREATION_AND_LOADING.md).

## Character levels versus equipment upgrade levels

[CharacterProgression.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/CharacterProgression.java) records five earned disciplines (MARKSMANSHIP, ATHLETICS, ENGINEERING, SCAVENGING, SURVIVAL), with character cap **50** and an alive-time reward multiplier that resets on death. Hardcore can reset the underlying earned ranks as well, according to the host's persistent rules. A Lost NPC's encounter difficulty is not the same thing as a player character's level.

[EquipmentUpgrades.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/EquipmentUpgrades.java) contains **one-level-at-a-time** design plans for facilities, defenses and vehicles, with an independent cap of **50**. The visible stages are CRUDE (levels 1–9), REINFORCED (10–19), MECHANIZED (20–29), ELECTRIFIED (30–39), INDUSTRIAL (40–49) and ENDGAME_SPECIALIZED (50). They are upgrade planning data, **not** an operational in-world upgrade transaction or functional turret/vehicle stat upgrade system. Applying them requires a host-owned work order, materials, power or fuel and durable save-state change.

## Working Arsenal

Eight [player weapons](Weapons.md) are map loot; the other 24 have faction-owned tiered fabrication. The older pistol/shotgun command remains a compatibility path. The 32-weapon host-authoritative player arsenal now defines equipment selection, ammunition, reloads, alternate modes, projectiles, environmental effects and original animated textured models. The Lost's NPC weapons are an unrelated, design-only combat expansion.
