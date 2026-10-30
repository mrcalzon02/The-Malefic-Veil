# The Malefic Veil — Transport Manufacturing and Systems Requirements

**Authoritative late-game implementation contract (2026-10-29).** Real recipes and facility prerequisites are defined in `CraftingEraRegistry.java`, indexed in `CraftingRecipeIndex`, and exported to the canonical `economy/*.csv` files. Live manufacturing uses `IndustrialWorkstation` on authorized settled facilities. The headless transport overlays currently accept trusted, already-delivered inventory; **they are not yet wired to live multiplayer inventory/streamed world persistence or GPU construction panels.**

## Engineering invariants

These are **three distinct manufacturing civilizations**, not a single railway/airfield factory with recolored inventory. Shared physical dimensions (six-metre ground parcel, standard rail gauge, runway approach envelope, ship footprint) exist for geometry and server rules only. Unraveled metalwork burns recovered industrial fuel; Penitent sanctified harvest machinery consumes witness/dread supplies; Begotten grew chitin, membrane and living organs that need nutrient stock. All items and station rights are faction-scoped by server-trusted identity.

Every completed manufacturing order must pass: research era and station existence; faction ownership and station proximity; power supply for *every working tick*; labor/specialist skill; full material debit exactly once at start; sufficient storage space at completion; bounded wear/maintenance; persistent transaction receipts and world object ID. `IndustrialWorkstation` already enforces input debit, power/service, bounded elapsed ticks, wear, no-room pause and idempotent request receipts; user/settlement access, generated plant placement, skill gates and durable journal integration remain outstanding. The planning index correctly labels industrial production **INDUSTRIAL_PLANNED**, not fully playable.

### Facilities: construction costs, size, and utility

| Facility / faction | Era | Size (m) | Build time | Operating power | Prerequisites | Construction bill |
|---|---|---:|---:|---:|---|---|
| `rail-survey-office` (ALL) | STEAM | 10×12 | 0.67 h | 2 | `mechanical-shaft`, `freight-bay` | 3 METAL_STOCK + 2 MACHINE_PARTS + 16 FIELD_STONE |
| `rail-freight-depot` (ALL) | AUTOMATION | 18×24 | 2.50 h | 14 | `rail-survey-office`, `motor-pool` | 10 METAL_STOCK + 9 MACHINE_PARTS + 36 FIELD_STONE + 3 GEAR_ASSEMBLY |
| `harbor-drydock` (ALL) | INCURSION | 30×36 | 6.00 h | 24 | `rail-freight-depot`, `fuel-refinery`, `assembly-line` | 28 METAL_STOCK + 20 MACHINE_PARTS + 100 FIELD_STONE + 5 PRECISION_MODULE |
| `unraveled-transport-forge` (UNRAVELED) | INCURSION | 24×26 | 4.00 h | 26 | `rail-freight-depot`, `resilience-grid`, `unraveled-defense-works` | 18 METAL_STOCK + 14 MACHINE_PARTS + 8 HARDENED_ALLOY + 2 ASSEMBLY_CONTROLLER |
| `penitent-procession-works` (PENITENT) | INCURSION | 24×26 | 4.17 h | 26 | `rail-freight-depot`, `dread-works`, `penitent-ward-sanctum` | 15 METAL_STOCK + 12 MACHINE_PARTS + 8 RELIQUARY_PLATE + 8 DREAD_IMPRINT |
| `begotten-transit-vat` (BEGOTTEN) | INCURSION | 24×26 | 4.50 h | 26 | `rail-freight-depot`, `incursion-anchor`, `begotten-organ-lattice` | 12 METAL_STOCK + 10 MACHINE_PARTS + 12 GROWN_CHITIN + 12 NUTRIENT_STOCK |
| `unraveled-naval-yard` (UNRAVELED) | INCURSION | 32×42 | 6.00 h | 40 | `harbor-drydock`, `unraveled-transport-forge`, `unraveled-flight-works` | 28 METAL_STOCK + 20 MACHINE_PARTS + 14 HARDENED_ALLOY + 2 RESILIENCE_CORE |
| `penitent-slaughter-dock` (PENITENT) | INCURSION | 32×42 | 6.67 h | 40 | `harbor-drydock`, `penitent-procession-works`, `penitent-aerial-abattoir` | 24 METAL_STOCK + 18 MACHINE_PARTS + 14 RELIQUARY_PLATE + 16 DREAD_IMPRINT |
| `begotten-tidal-brood` (BEGOTTEN) | INCURSION | 32×42 | 7.00 h | 40 | `harbor-drydock`, `begotten-transit-vat`, `begotten-sky-brood` | 20 METAL_STOCK + 15 MACHINE_PARTS + 20 GROWN_CHITIN + 12 LIVING_MEMBRANE |

`rail-survey-office` begins after the Steam era. A powered freight depot is available in Automation. The communal drydock provides construction infrastructure only. **Actual locomotive and carrier manufacturing happens in the three faction-specific works and naval yards**, which themselves depend on their respective Incursion endgame institutions. No shipyard may be erected within a city/immutable world structure; the future harbor construction pass must validate shoreline access and seawater depth before breaking ground.

### Manufactured components, vehicles, maintenance and fuels

`Utility` below is **required power capacity during production**, not a free output. The energy budget for one batch is `required utility × duration(hours)`; the runtime's power-grid service must supply that capacity on every paid work tick. Final carrier hulls are single non-stackable tokens; locomotive and freight-wagon tokens likewise represent physical world objects once commissioned. Each finished runway/rail panel token installs exactly one six-metre physical section; panels are produced two per batch. Each signal and light has distinct maintenance and action requirements for future powered rail routing and airfield navigation.

| Recipe ID / Faction | Station | Output (per batch) | Exact consumed ingredients | Duration | Utility | Energy/batch |
|---|---|---|---|---:|---:|---:|
| `unraveled-runway-panels` / UNRAVELED | `unraveled-transport-forge` | 2 × `RUNWAY_PANEL_UNRAVELED` | 2 HARDENED_ALLOY + 6 FIRED_BRICKS + 2 FASTENERS | 0.17 h | 14 | 2.33 |
| `unraveled-rail-sections` / UNRAVELED | `unraveled-transport-forge` | 2 × `RAIL_SECTION_UNRAVELED` | 2 HARDENED_ALLOY + 4 FASTENERS + 6 TIMBER | 0.20 h | 14 | 2.80 |
| `unraveled-rail-fuel` / UNRAVELED | `unraveled-transport-forge` | 10 × `RAIL_FUEL_UNRAVELED` | 10 CHARCOAL + 2 CLEAN_WATER | 0.15 h | 12 | 1.80 |
| `unraveled-marine-fuel` / UNRAVELED | `unraveled-naval-yard` | 8 × `MARINE_FUEL_UNRAVELED` | 12 CHARCOAL + 4 CLEAN_WATER | 0.25 h | 26 | 6.50 |
| `unraveled-transport-repair` / UNRAVELED | `unraveled-transport-forge` | 2 × `TRANSPORT_REPAIR_UNRAVELED` | 1 HARDENED_ALLOY + 2 MACHINE_PARTS + 3 FASTENERS | 0.20 h | 16 | 3.20 |
| `unraveled-runway-light` / UNRAVELED | `unraveled-transport-forge` | 4 × `RUNWAY_LIGHT_UNRAVELED` | 2 CONTROL_BOARD + 4 CONDUCTOR_WIRE + 1 TRACKING_CIRCUIT | 0.25 h | 18 | 4.50 |
| `unraveled-rail-signal` / UNRAVELED | `unraveled-transport-forge` | 2 × `RAIL_SIGNAL_UNRAVELED` | 1 SENSOR_HEAD + 1 CONTROL_BOARD + 4 CONDUCTOR_WIRE | 0.23 h | 18 | 4.20 |
| `unraveled-locomotive` / UNRAVELED | `unraveled-transport-forge` | 1 × `LOCOMOTIVE_UNRAVELED` | 4 DRIVE_SHAFT + 8 HARDENED_ALLOY + 5 GEAR_ASSEMBLY + 1 RESILIENCE_CORE + 2 ASSEMBLY_CONTROLLER | 1.50 h | 30 | 45.00 |
| `unraveled-freight-wagon` / UNRAVELED | `unraveled-transport-forge` | 1 × `FREIGHT_WAGON_UNRAVELED` | 4 HARDENED_ALLOY + 2 GEAR_ASSEMBLY + 3 MACHINE_PARTS | 0.75 h | 26 | 19.50 |
| `unraveled-carrier-keel` / UNRAVELED | `unraveled-naval-yard` | 1 × `CARRIER_KEEL_UNRAVELED` | 40 METAL_STOCK + 24 HARDENED_ALLOY + 2 RESILIENCE_CORE + 6 PRECISION_MODULE | 3.33 h | 42 | 140.00 |
| `unraveled-carrier` / UNRAVELED | `unraveled-naval-yard` | 1 × `CARRIER_UNRAVELED` | 1 CARRIER_KEEL_UNRAVELED + 5 ASSEMBLY_CONTROLLER + 6 TRACKING_CIRCUIT + 18 HARDENED_ALLOY | 6.00 h | 55 | 330.00 |
| `penitent-runway-panels` / PENITENT | `penitent-procession-works` | 2 × `RUNWAY_PANEL_PENITENT` | 1 RELIQUARY_PLATE + 5 FIRED_BRICKS + 3 VOTIVE_INCENSE | 0.17 h | 14 | 2.33 |
| `penitent-rail-sections` / PENITENT | `penitent-procession-works` | 2 × `RAIL_SECTION_PENITENT` | 2 RELIQUARY_PLATE + 1 WITNESS_SEAL + 3 METAL_BILLET | 0.20 h | 14 | 2.80 |
| `penitent-rail-fuel` / PENITENT | `penitent-procession-works` | 10 × `RAIL_FUEL_PENITENT` | 3 VOTIVE_INCENSE + 1 DREAD_IMPRINT + 4 CHARCOAL | 0.15 h | 12 | 1.80 |
| `penitent-marine-fuel` / PENITENT | `penitent-slaughter-dock` | 8 × `MARINE_FUEL_PENITENT` | 3 DREAD_IMPRINT + 10 CHARCOAL + 6 VOTIVE_INCENSE | 0.25 h | 26 | 6.50 |
| `penitent-transport-repair` / PENITENT | `penitent-procession-works` | 2 × `TRANSPORT_REPAIR_PENITENT` | 2 RELIQUARY_PLATE + 1 MACHINE_PARTS + 4 VOTIVE_INCENSE | 0.20 h | 16 | 3.20 |
| `penitent-runway-light` / PENITENT | `penitent-procession-works` | 4 × `RUNWAY_LIGHT_PENITENT` | 1 CHORUS_RESONANCE + 3 CONDUCTOR_WIRE + 2 WITNESS_SEAL | 0.25 h | 18 | 4.50 |
| `penitent-rail-signal` / PENITENT | `penitent-procession-works` | 2 × `RAIL_SIGNAL_PENITENT` | 1 CHOIR_SCORE + 2 WITNESS_SEAL + 1 CONTROL_BOARD | 0.23 h | 18 | 4.20 |
| `penitent-locomotive` / PENITENT | `penitent-procession-works` | 1 × `LOCOMOTIVE_PENITENT` | 8 RELIQUARY_PLATE + 3 BRONZE_CENSER + 1 DREAD_WARD + 8 MACHINE_PARTS + 3 GEAR_ASSEMBLY | 1.50 h | 30 | 45.00 |
| `penitent-freight-wagon` / PENITENT | `penitent-procession-works` | 1 × `FREIGHT_WAGON_PENITENT` | 5 RELIQUARY_PLATE + 2 GEAR_ASSEMBLY + 4 WITNESS_SEAL | 0.75 h | 26 | 19.50 |
| `penitent-carrier-keel` / PENITENT | `penitent-slaughter-dock` | 1 × `CARRIER_KEEL_PENITENT` | 30 METAL_STOCK + 20 RELIQUARY_PLATE + 3 DREAD_WARD + 20 MACHINE_PARTS | 3.33 h | 42 | 140.00 |
| `penitent-carrier` / PENITENT | `penitent-slaughter-dock` | 1 × `CARRIER_PENITENT` | 1 CARRIER_KEEL_PENITENT + 8 BOUND_LITURGY + 6 SACRED_SUPPLY + 16 RELIQUARY_PLATE | 6.00 h | 55 | 330.00 |
| `begotten-runway-panels` / BEGOTTEN | `begotten-transit-vat` | 2 × `RUNWAY_PANEL_BEGOTTEN` | 2 LIVING_MEMBRANE + 1 BINDING_RESIN + 2 NUTRIENT_STOCK | 0.17 h | 14 | 2.33 |
| `begotten-rail-sections` / BEGOTTEN | `begotten-transit-vat` | 2 × `RAIL_SECTION_BEGOTTEN` | 3 GROWN_CHITIN + 1 BINDING_RESIN + 3 GROWTH_FEED | 0.20 h | 14 | 2.80 |
| `begotten-rail-fuel` / BEGOTTEN | `begotten-transit-vat` | 10 × `RAIL_FUEL_BEGOTTEN` | 6 GROWTH_FEED + 2 NUTRIENT_STOCK + 5 EDIBLES | 0.15 h | 12 | 1.80 |
| `begotten-marine-fuel` / BEGOTTEN | `begotten-tidal-brood` | 8 × `MARINE_FUEL_BEGOTTEN` | 8 NUTRIENT_STOCK + 8 GROWTH_FEED + 6 CLEAN_WATER | 0.25 h | 26 | 6.50 |
| `begotten-transport-repair` / BEGOTTEN | `begotten-transit-vat` | 2 × `TRANSPORT_REPAIR_BEGOTTEN` | 2 BINDING_RESIN + 2 NUTRIENT_STOCK + 2 GROWN_CHITIN | 0.20 h | 16 | 3.20 |
| `begotten-runway-light` / BEGOTTEN | `begotten-transit-vat` | 4 × `RUNWAY_LIGHT_BEGOTTEN` | 1 ORGAN_CORE + 3 LIVING_MEMBRANE + 2 NUTRIENT_STOCK | 0.25 h | 18 | 4.50 |
| `begotten-rail-signal` / BEGOTTEN | `begotten-transit-vat` | 2 × `RAIL_SIGNAL_BEGOTTEN` | 4 SPORE_POD + 1 ORGAN_CORE + 2 GROWN_CHITIN | 0.23 h | 18 | 4.20 |
| `begotten-locomotive` / BEGOTTEN | `begotten-transit-vat` | 1 × `LOCOMOTIVE_BEGOTTEN` | 5 ORGAN_CORE + 12 GROWN_CHITIN + 6 BINDING_RESIN + 1 SPINE_SUPPLY + 8 LIVING_MEMBRANE | 1.50 h | 30 | 45.00 |
| `begotten-freight-wagon` / BEGOTTEN | `begotten-transit-vat` | 1 × `FREIGHT_WAGON_BEGOTTEN` | 8 GROWN_CHITIN + 4 LIVING_MEMBRANE + 3 BINDING_RESIN | 0.75 h | 26 | 19.50 |
| `begotten-carrier-keel` / BEGOTTEN | `begotten-tidal-brood` | 1 × `CARRIER_KEEL_BEGOTTEN` | 35 GROWN_CHITIN + 20 LIVING_MEMBRANE + 12 ORGAN_CORE + 10 BINDING_RESIN | 3.33 h | 42 | 140.00 |
| `begotten-carrier` / BEGOTTEN | `begotten-tidal-brood` | 1 × `CARRIER_BEGOTTEN` | 1 CARRIER_KEEL_BEGOTTEN + 8 SPINE_SUPPLY + 2 BREACH_SEED + 30 NUTRIENT_STOCK | 6.00 h | 55 | 330.00 |

**Cost discipline:** The final carrier assembly recipe consumes one already-manufactured `CARRIER_KEEL_*` produced in the same faction's naval yard; this intermediate alone has substantial materials, time and power. Never allow player-side spawning directly from raw metal, purchase-only aircraft, or a generic craft recipe that sidesteps the keel, faction works, and shipyard.

## Site placement and completion requirements

**Land airfields.** A server-owned site plan reserves a cardinal strip of 12–60 six-metre units and shoulders across suitable wilderness land. The authoritative validator checks all cells, slope, water, other infrastructure, world structures, roads and city miasma. Installation is **one paid `RUNWAY_PANEL_<FACTION>` plus its faction-specific bedding material per slab and two metered construction energy units**, producing one 100-HP segment. Until the strip is fully built with all segments above 50 HP, takeoff and landing are denied. Unraveled Light needs 13 sections (78 m); its Medium 21 sections (126 m), Heavy 30 (180 m). Aircraft commission only when a finished airframe token has been removed from its factory, a real completed runway is present, and valid takeoff rules are satisfied. Field lights use their own four-per-batch production chains; operational runway-light electricity/cabling and taxiway topology are a distinct further integration gate.

**Railways.** Installing each six-metre rail block debits **one `RAIL_SECTION_<FACTION>`**, faction-appropriate support material, and one metered construction energy unit. Host collision forbids placement in city spaces and on generated roads. The initial physical topology handles straight cardinal tracks and only links adjacent, matched orientation, sufficiently healthy sections; it intentionally does **not** invent curves, switches, grade crossings, bridges, signals or rail pathfinding. Paid `RAIL_SIGNAL_*` parts are included for the later authority-controlled signaling system. A locomotive cannot be commissioned without one completed `LOCOMOTIVE_<FACTION>` and three contiguous safe siding blocks. It may attach up to three individually manufactured `FREIGHT_WAGON_*` tokens at rest; each increases real freight capacity and reduces allowed speed. The actual train consumes `RAIL_FUEL_<FACTION>` canisters in 10 traction-unit increments, burns fuel with movement, and cannot cross broken rail. Passenger coaches, wagon cargo compartments, route reservations and station inventory journals remain planned extensions.

**Naval dock and carrier.** A completed `CARRIER_<FACTION>` is a manufactured shipyard output, not an automatic boat from a flat ocean tile. Placement consumes that item after checking an 18-by-5 parcel deep-water exclusion footprint, no world overlap and no city miasma. Initial 108-metre deck supports **Light aircraft only** at an 8-metre water-relative deck elevation, with a deck reservation that blocks another departure and ship travel until released. Carrier travel consumes `MARINE_FUEL_<FACTION>` charges (25 marine-energy units per canister); two `TRANSPORT_REPAIR_<FACTION>` items restore a damaged hull. `AircraftFlight.commissionCarrier` still needs a paid, distinct airframe and faction aviation fuel. Sea docking/launch geometry, collision, water depth and stationary-vs-moving-deck reconciliation must be demonstrated in streamed-world tests. The 108-metre flight deck will not launch Medium or Heavy airframes until new hulls/launch equipment are designed and tested.

**Maintenance.** Rail and runways stop operating when segment health drops to 50% or below. A finished transport-repair kit and metered energy restore a runway/track segment. A carrier consumes two kits to restore structural HP. Maintain a record of owners, materials, exact HP, train wagons and cargo, operating fuel, collision and repairs; reloading must not mint duplicate sections, locomotives, wagons, carrier hulls, flight decks or aviation resources. No arbitrary client-reported ownership, HP, cargo or instant construction.

## Missing operational gates — not passed off as complete

1. Real physical asset models for faction runways (paving, thresholds, lights, markings), railway tracks (curves, crossovers, switches, signals, sleepers), locomotive and freight wagons (bogies/couplers), and carrier hull/flight deck (elevators, island, weapons) with PSX LOD, collider parity, and asset provenance.
2. Actual wilderness/continental sector streaming, grade-preserving site construction, rail line path planning with slopes and junctions, shoreline surveys, shipyard mooring, and moving platform/cross-sector physics.
3. A single authoritative player-facing build UI that shows time, ownership, ingredient chains, power, workforce, preview collisions, progress and damaged-section repair. It must never permit protected city plots or full-carrier teleportation.
4. Production service integration between physical facilities, stockpiles, work crews, research qualification, power grid, inventory/cargo transfer, scheduled work orders and server persistence.
5. Live train controls, occupancy and collision authority, station scheduling, route reservations and signals, actual naval piloting/deck recovery, and GPU aircraft/cockpit flight, plus anti-air and city miasma projectile exclusions.
6. Multiplayer 4–16 player restart/reconnect/ownership/transport handoff soak, no-duplication after abort, chunk unloading and PVP faction safeguards; real screenshot-based geometry and sound validation.

## Automated evidence

`TransportProductionTest` checks **33 actual canonical recipes**, **nine prerequisite facilities**, separated faction manufacturing, no cross-faction production, no-power pauses, idempotent receipts, carrier keel production and atomic final-hull withdrawal. `TransportInfrastructureTest` checks that raw-stock-only players cannot create runway panels, and that paid assembled components build flight surfaces, rail freight trains and a paid naval deck. Both are run by GPU and host CI. Canonical economy CSV/Java consistency is checked by `CraftingEconomyCsvTest` and Python `tools/validate_economy_csv.py`. A passing headless test is **not** evidence of a playable GPU-powered continent.
