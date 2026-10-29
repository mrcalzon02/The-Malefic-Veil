# Crafting and construction — implementation ledger

This report is a **live-state classification**, not a promise that every
indexed blueprint is buildable. The current generated catalog has **303 rows** (22 survival + 62 industry + 69 facilities + 150 original icon entries). The machine-readable index is generated from
the game code, via `veil.CraftingCatalogExport`, during the host, GPU and
release tests. Download `build/test-visuals/crafting-completion-catalog.csv`
from the latest successful test artifact to inspect every producer, input,
output, prerequisite, station, faction, era, dependency and icon provenance.

## Single source of truth

- `SurvivalCrafting.recipes()`: **22 gameplay recipes**; input/output debit
  occurs on host. The hand, fire, and now **workbench** station paths are
  intentionally distinct. Station access is not a clickable client override.
- `CraftingEraRegistry.processes()`: **62 industrial processes**; each has
  positive required input quantities, output yield, processing time,
  electricity/mechanical power demand, and faction-compatible facility.
  `IndustrialWorkstation` actually supports reserving/debiting materials,
  bounded progress, power/service interruption, wear/repair, exactly-once
  completion, replay protection, and cold snapshot restoration. This engine
  component is **not yet attached to physical host-owned base machines**.
- `CraftingEraRegistry.facilities()`: **69 buildings** with real construction
  material requirements, predecessor chains, faction locks, dimensions, time
  and utility demand. `SettlementAuthority` uses these IDs for blueprint
  permissions and capacity. A blueprint's existence **does not create**
  terrain foundations, durable build worksites or working machines.
- `CraftingIconPacks`: **150 separately verified original source icons**
  spanning five distinct art packs. The GPU crafting book renders actual
  icon PNGs, not arbitrary borrowed art. `BANDAGE`, `WORKBENCH`,
  `PRESERVED_RATIONS`, `GUARD_PROVISIONS`, `STERILE_WATER`,
  `DEFENSE_CACHE` and `FIELD_MRE` still need their OWN sprites;
  current index labels these `ICON_REQUIRED`.
- `CraftingRecipeIndex` computes the interactive recipe/item book's direct
  predecessors and downstream users. It separates `PLAYABLE_HAND`,
  `PLAYABLE_FIRE`, `PLAYABLE_WORKBENCH`,
  `INDUSTRIAL_PLANNED` and `FACILITY_PLANNED`. A decorative
  image-only item is never implicitly craftable.

## Faction-specific chains stay sovereign

The shared catalogue is a **read-only index**, not shared manufacturing rights. The Unraveled, Penitent and Begotten each have five consecutive faction-owned sites across Encampment, Salvage, Steam, Electric and Automation, plus their own intermediate recipes and Incursion facilities. A faction-only intermediate may not enter a competing faction process unless an explicit future trade/contract capability authorizes it. The neutral `ALL` branch contains ordinary survival and shared infrastructure, not pooled faction-owned production. `FactionIndustrySeparationTest` checks faction production visibility and intermediate ownership before a release can pass.

## Newly completed playable workbench chain

A player with wilderness TIMBER, FIBER and FIELD_STONE can craft the actual
`WORKBENCH` kit by hand. The *host* accepts placement of the new
`FIELD_BENCH` world device only in unprotected wilderness, in an unobstructed
position near the player's own camp, once per owner, after checking an
unconsumed workbench kit. The kit is charged exactly once, the physical model
uses its own authored 3D mesh and the device is saved in `DurableRealm`.
For `CRAFT_BENCH` the host checks a live owned/allied nearby undamaged device,
line of sight, item costs, weight cap, crafting cooldown and player status.
The GPU crafting UI shows its proximity/ingredient requirements and offers
physical placement; the server remains authoritative. The end-to-end
`FieldWorkbenchRuntimeTest` verifies placement, debit, no replay, proximity
and durable realm reload.

**Do not mistake this for** an operational industrial hand-workshop or a
settlement charter. These require physically claimed foundations and their own
facility service implementation.

## Previously orphaned structures now fully costed *in the registry*

| Structure | Tier | Existing dependencies | Prototype target |
|---|---|---|---|
| wall-gate-system | Encampment | Claim Heart, palisade | Gatehouse |
| cold-store | Salvage | Root Cellar, Freight Bay | Refrigerated storage |
| barracks | Salvage | Watch Post, Dry Store | Guard hall |
| purification-works | Electric | Water Works, Charcoal Kiln | Filter plant |
| security-grid | Automation | Signal Relay, Command Hall | Security array |

They no longer exist merely as additional cost-free names inside the build
permissions map. The underlying tiered construction CSV, prerequisites, utility
needs, footprint and `CrudeStructureIndex` geometry classification now match
their actual recipes. Their five corresponding processes produce
`PRESERVED_RATIONS`, `GUARD_PROVISIONS`, `STERILE_WATER`,
`DEFENSE_CACHE` and `FIELD_MRE` in the indexed progression. These outputs
are **planned factory goods**, not currently spawned in personal inventories.

The entire `monument-graveyard` cost and prerequisites now appear in
`economy/facilities.csv` and `economy/facility_costs.csv`, ending the
previous omission from exported construction tables.

## Remaining release gates — do not mark finished without proof

1. **Physical bases:** Survey and validate real wilderness footprint,
   collision, slope, existing world geometry, seed-protected roads/cities,
   flood zones, claim identity and faction capacity. Persist reserved
   foundation, paid deliveries, construction progress, destruction/repair and
   building upgrades. A costed registry record alone is not a building.
2. **Logistics and power:** Link real truck unloading and owned stores to
   machine input buffers; consume generator fuel; physically validate supply
   lines, power load, interruption and service. No client may invoke
   `IndustrialWorkstation.deposit` directly.
3. **Production:** Bind a built/faction-authorized, active facility to a
   workstation, store its snapshot atomically with the realm and journal
   material transfers; verify power shortages, process completion, restarts,
   contested ownership and no duplication across reconnect/restart.
4. **Actual outputs/uses:** Make preserved food, sterilized water, defense
   supplies, faction reagents and level-50 upgrade materials work with real
   survival, building, weapons, convoy and trading transactions. Icons alone
   do not establish game mechanics.
5. **Missing distinct art:** Finish icon identities for the seven
   unillustrated item products listed above and build source-verified PSX
   facility meshes/colliders. The primitive index is a target catalog,
   not fake evidence of deployed geometry.

## Contracts and automated evidence

`CraftingEconomyCsvTest` checks *all* field/industrial recipes and **every**
facility export against canonical code: identical quantities, station, faction,
footprint, tier, seconds, utility and materials. `CraftingRecipeIndexTest`
checks both forward and reverse dependencies.
`CraftingCatalogExportTest` checks that all craft, facility and original
icon rows appear, and unfinished manufacture does not masquerade as playable.
`FieldWorkbenchRuntimeTest` checks authoritative production through an
actual physical station and save/reload. All tests are release gates; a
screenshot is not a substitute for gameplay evidence.
