# The Lost, Resources, Cargo Hauling and Survival — First Authoritative Slice

**Status: implemented mechanics with explicit integration gates (21 October 2026).**

This document is a development contract. The current playable map is a fixed, immutable **132×132 city sector**; the larger continent and wilderness exist as deterministic data and render-only terrain, **not** as fully streamed walkable server-authoritative world. Player construction remains forbidden in every city plot, including empty plots. Do not quietly permit urban building because wilderness simulation has not yet shipped.

## 1. The Lost: horror originates at places

Each HAZARD plot (tenement, cursed chapel, burned clinic, wrecked flats, contaminated laboratory, etc.) is a persistent **Lost source building**. `LostSpawnSites` allocates 45 stable NPC network/save slots across up to 36 eligible named ruins, rather than sprinkling monsters into unrelated random positions. Every NPC records its source site ID and assigned Lost archetype; each building owns a bounded set of those slots.

- **Host simulation:** `ArenaSystems.advance` ticks all sites using the authoritative match clock. A site only fills a vacant owned slot after its cooldown and if no player stands within ten world units.
- **Limits:** 45 active NPC maximum across the city sector; five active monsters within a 12-unit site neighborhood; each building's own limited reserve (normally 1–2 slots, seed-adapted).
- **Lifetime:** killing an NPC releases that *site's* capacity and schedules a **45-second cooldown**. Respawning uses the same actor/network slot rather than minting new IDs or appending unlimited NPC entities.
- **Source variety:** Lost drifters, stalkers, wailers, brutes, and the previously audited PSX candidate catalog. Current AI and shot/health simulation remain functional. The final PSX animated actor meshes are a **separate art/renderer gate**; do not claim that source FBX models are already live characters.
- **Persistence caveat:** persistent NPC position/health and 45 slot count are saved by `DurableRealm`; the site's transient per-entity respawn cooldowns are not yet snapshotted with full offline-clock semantics. This is an open 24/7 host hardening gate.

## 2. Two distinct urban bulk resources

The approved Kenney Retro Urban Kit 2.0 contains `truck-green.glb`, `truck-grey.glb`, `truck-green-cargo.glb`, `truck-grey-cargo.glb`, `pallet.glb` and `pallet-small.glb`. The two truck cargo GLBs are **complete loaded-truck meshes, not standalone detachable cargo modules**. Do not claim otherwise.

`CargoHaulage` seeds up to **24 RESOURCE-plot roadside loading yards** per city sector, including salvage yards, depots, workshops, markets and vehicle scrapyards. Each has a stable ID, site name, source-kit pallet ground asset, one load in stock and a **three-minute** replenishment clock. Two distinct hauling resources:

| Load | Gameplay identity | Source ground asset | Loaded vehicle appearance |
|---|---|---|---|
| METAL_STOCK | structural metal, reinforced components, construction and repairs | `pallet.glb` | `truck-green-cargo.glb` or `truck-grey-cargo.glb`, matching chosen truck |
| MACHINE_PARTS | machinery, engine components, crafting stations and vehicle repairs | `pallet-small.glb` | same two loaded truck meshes, matching chosen truck |

Truck color is visual, never the server-authoritative resource identity. Neither resource may be stuffed into a normal player backpack. Distinct small cargo-specific mesh modules may be designed later from licensed sources.

### The intended gameplay loop

1. Find/restore a functional truck, then **drive to a RESOURCE city yard**. Today the host spawns six drivable vehicles. Truck crafting and ownership entitlement are future gates.
2. Stop and **exit the cab**. Stand within 2.5 world units of the yard, within 4.5 of the parked truck, and keep the truck within 4.2 of the loading bay. Movement, seat occupancy, remaining load, damage and clear line of sight are checked **on the server**, never trusted from client coordinates.
3. Press **L** to load one unit of bulk cargo. A stocked yard is immediately depleted. A truck cannot load twice, cannot accept cargo while occupied/moving, and cannot load below 81 HP. The server broadcasts yard stock and truck cargo type; the desktop truck switches to an actual Kenney `-cargo` model.
4. Transport to your wilderness base. **This final delivery is not available yet**, because wilderness building and player bases are not live. The data-side `SurvivalCrafting` inventory accepts bulk cargo only when an authoritative future base-unloading gate marks it delivered.
5. Heavy damage to the truck (HP at or below **80/200**) automatically ejects exactly one recoverable ground load. It cannot remain secretly in the truck or duplicate on repeated physics ticks. Park another functional truck nearby, exit and press **G** to recover it; the host authorizes proximity and clears the picked-up drop from other clients.
6. Loose cargo persists for **30 world-clock minutes**, with up to 128 outstanding dropped loads per city sector; stale objects expire to protect memory and network budget. A damaged truck must be repaired before accepting a new load.

`DurableRealm` now saves the resource-yard stock, refill deadlines, truck cargo and dropped cargo inside **version-2 CRC-checked atomic realm snapshots**, with version-1 read compatibility. A host restart does not mint a fresh cargo load from an already depleted yard. The existing game save backup/recovery mechanism remains in force.

The current game client has L / G controls and server-verified success/failure responses. Resource marker visuals currently come from real pallets, but live removal of every static yard marker after depletion and full GPU-side multiplayer haul animation are not yet complete.

## 3. Wilderness resources, vegetation and survival

`ContinentVista.foliage` uses seed-jittered cluster centers with independently offset trees/shrubs, rather than a conspicuous regular 7-unit planting lattice. Biome-dependent density produces forest clusters, plains copses, wetland brush, hill trees and minimal beach vegetation. All rendered trees use authentic source-kit GLBs; vegetation outside the current city remains **render-only** until authoritative continent streaming arrives.

`WildernessResources` independently plans irregular seed-jittered wilderness harvesting sites (never inside city masks, built structures, highways or sea). Actual harvesting is modeled with proximity checks, stock depletion and 10-minute replenishment. Types: timber, field stone, fiber, edible forage, medicinal herbs, raw ore and **dirty, unsafe wetland water**. Do not label seawater as drinkable. Generated resources use 224×224 continental macro coordinates and need conversion to streamed game coordinates.

`SurvivalCrafting` provides an item economy with a bounded inventory (weight limit 200), hunger and hydration decay, consumables, hand/fire/workbench crafting gates and validated debit-before-mint of ingredients. Recipes currently include bandages, campfires, water filters, boiled water, workbenches, shelters and vehicle-repair kits. Bulk `METAL_STOCK` and `MACHINE_PARTS` are required for advanced crafting. **This is tested domain logic, not yet wired into authenticated player saves or the GPU crafting interface.**

## 4. Acceptance evidence and remaining implementation

Source: `LostSpawnSites.java`, `CargoHaulage.java`, `Vehicles.java`, `ArenaSystems.java`, `LanServer.java`, `NetLink.java`, `DurableRealm.java`, `WildernessResources.java`, `SurvivalCrafting.java`, `ContinentVista.java`.

Tests: `LostSpawnSitesTest`, `CargoHaulageTest`, `CargoRealmPersistenceTest`, `V1RealmCompatibilityTest`, `WildernessResourcesTest`, `SurvivalCraftingTest`. Linux/Windows Java 17/21 execution is in `.github/workflows/game-verification.yml`; verify the conclusion of the run at the current HEAD before making a release claim.

**Open release gates**: (a) streaming real walkable terrain with server-owned wilderness actors; (b) base location/build/save authority and base delivery of heavy cargo; (c) authenticated persisted personal survival inventory and character needs; (d) site-local 24/7 cooldown persistence and anti-farm restart protections; (e) genuine distinct detachable cargo/vehicle damage and repair visuals; (f) dynamic resource and NPC models in the OpenGL client; (g) in-game crafting UI/interaction using attributed PSX assets; (h) full 2+ client soak, exact restart, drop recovery and resource protection under disconnect and crash timing.

**Visual proof rule**: game screenshots must be actual GitHub Actions / native OpenGL framebuffer captures, downloaded from their matching successful workflow run—never synthesized artwork.
