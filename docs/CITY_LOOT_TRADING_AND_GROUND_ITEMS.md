# City loot, aimed trading and temporary ground items

## Scope and existing constraints

This increment extends [PLAYER_QOL_MUST_HAVES.md](PLAYER_QOL_MUST_HAVES.md), [PLOT_GENERATION.md](PLOT_GENERATION.md), [CRAFTING_INDUSTRY_INCURSION_PROGRESSION.md](CRAFTING_INDUSTRY_INCURSION_PROGRESSION.md) and [FACTION_FACILITIES_BASE_CONSTRUCTION.md](FACTION_FACILITIES_BASE_CONSTRUCTION.md). Server-owned quantities, atomic debits, build-route clearance, nonblocking previews, clear rejection feedback and compact inventory controls remain non-negotiable.

## Implemented host primitives

- `UrbanDressing` deterministically places source-licensed `detail-dumpster-closed` physical assets in RESOURCE, HAZARD and OPEN plots. SAFE plot categories are excluded. The original truck/parked-derelict assets remain.
- `CityLootEconomy` derives deterministic one-use loot containers from dumpsters and derelict trucks. The host owns item yield and container depletion; failed full-inventory acquisition cannot consume contents.
- `LanServer` announces containers to joining clients and accepts `SCAVENGE|id` with character-position/LOS validation.
- `DROP_ITEM|item|quantity` debits physical inventory and creates host-owned ground items at the player's authoritative position. `GET_DROP|id` is short-range, LOS-validated and weight-checked. Host expiry is **20 minutes** of wall time.
- `TRADE_ITEM|recipientId|item|quantity` requires a living, connected recipient within three metres and within a 60-degree aim cone (30 degrees each side), unobstructed line of sight, available inventory, and recipient capacity.
- Client receives result packets and refreshed authoritative inventories after successful transfers. No client may provide loot yield or recipient inventory quantities.

## Not yet complete / release gates

- Client inventory interaction and look-at-player trade affordance (including explicit receiving-player consent if required by game mode), world prop focus/highlights, drop meshes and visual despawn synchronization.
- Unique crate and car loot meshes, beyond source-licensed dumpsters and truck derelicts, must be checked in the asset library before usage. No fabricated asset references.
- Persistence for already-looted containers and temporary drops across host restart, plus save compatibility tests.
- Distinct factions' loot tables, weighted biome/district drop distributions and container restocking policy.
- Mandatory enclosed two-exit SAFE spawn buildings, starting equipment, no enemy spawn inside and city-by-city safety validation.
- End-to-end Java 17 compilation, host packet tests, two-client live gameplay, duplication/race and performance tests. Repository writes alone are **not test evidence**.

## QoL follow-through

Construction still requires an unobstructed placement preview, atomic host debit and exact failure reason; industrial crafting remains recipe/era/faction-driven and cannot be replaced by a universal handcraft menu. Automation must respect facility levels, power, production queue capacity, resource provenance and permissions. During testing, verify that trading/drop actions are reachable via compact, remappable client controls, not merely raw protocol messages.
