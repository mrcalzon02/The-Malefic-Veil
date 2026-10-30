# The Malefic Veil — physical runways, rail freight and aircraft carriers

**Manufacturing update:** The original direct-raw-material prices quoted below are historical first-slice assumptions. The new authoritative [Transport Manufacturing and Systems Requirements](TRANSPORT_MANUFACTURING_REQUIREMENTS.md) specifies live `CraftingEraRegistry` production chains: runway and rail sections are now made from faction-specific manufactured panels, locomotives and carriers from finished factory/shipyard tokens, fuels from separate industrial processes, and structural repairs from manufactured kits. **Always use the newer canonical registry and requirements for actual bills, not historical examples in this first-slice document.**

**Implementation date:** 29 October 2026. Source contract for the shared six-metre transport system. The Java **host-core physics, bills, construction state, movement and tests** are implemented; live UI, multi-client persistence, terrain-proxy mesh art and continent chunk streaming remain release blockers. These are **not yet player-buildable in the current 132×132 city-only game session**.

## Core design: one map, three different industries

Airfields, railways and carrier ships are owned infrastructure overlays on the **immutable** `ContinentLayout`, never player edits to city or road generation. One six-metre macro parcel becomes one construction segment with its own material debit, owner, faction and HP. An absent or critically damaged segment does not count. Every mutation must eventually be exposed only through `LanServer` requests with real character authorization, regional ownership, material reservations, server clocks and durable world/entity IDs.

The Unraveled build alloy/timber/metal infrastructure from machinery, recovered stock and engineered fuel; the Penitent use separate consecrated components, rites and dread-sourced material; the Begotten use grown chitin, membranes, nutrients and organs. A common track gauge or runway footprint is a *physical interface*, not a shared universal manufacturing civilization.

## Runway system implemented: `TransportInfrastructure`

A runway begins with a cardinal orientation, a site, an exact length of **12–60 six-metre slabs**, and a unique host-owned ID. `planRunway` checks the entire future strip and shoulders for water, protected cities/roads, miasma, collisions with other planned runways, and dangerous elevation variation. It also verifies the original `AircraftFlight.validAirfield` geometry. Planning pays nothing and grants no launch right. `buildNextRunwaySection` consumes a faction-specific bill plus **2 metered construction-energy units** for each physical six-metre tile, creating a 100-HP segment. The completed strip only becomes usable when **all** tiles exist, each is above 50 HP, the length meets the selected aircraft's rule, and the original geometry remains valid.

**Per segment preliminary material bill:**

| Raw material | Unraveled runway | Penitent runway | Begotten runway |
|---|---:|---:|---:|
| `METAL_STOCK` | 2 | 2 | 2 |
| `MACHINE_PARTS` | 1 | 1 | 1 |
| `FIELD_STONE` | 4 | 4 | 4 |
| Faction-specific addition | 1 `HARDENED_ALLOY` | 1 `VOTIVE_INCENSE` | 1 `GROWN_CHITIN` |

A 78-metre Needlewing strip has **13 individual paid slabs**, consuming 26 metal stock, 13 machine parts, 52 field stone, 13 faction supplements and 26 metered energy units, **in addition** to the existing airfield/site-wide facility construction. Repairing a damaged tile consumes 1 machine part and 1 energy unit. Cross-owner construction/repair, duplicate placement, free slabbing and runway deployment with damage are rejected in code.

The new public `AircraftFlight.commission(...,TransportInfrastructure,runwayId,...)` requires that actually completed runway before transferring a paid aircraft from its workshop. The old abstract-pad commissioning method is package-private, preserved only for legacy headless tests, and is not an approved released-game route.

## Train system implemented: track and `RailTransport`

Rails are **independent six-metre segments** (not paved runways), aligned east-west or north-south, each costing 1 metal stock, 4 timber, 2 fasteners, 1 relevant faction material and 1 metered construction-energy unit. Connections require physically continuous adjacent, correctly oriented, above-50-HP track. Ground placement rejects city/world structures, roads, occupied construction segments and invalid terrain. A future route editor will provide curves/switches, crossing blocks, bridge spans, signals, sidings and depot geometry; these are *not* silently simulated by the straight-segment model.

A paid freight locomotive requires three continuous rail segments and consumes a faction-specific locomotive bill from a controlled material inventory: 12 metal stock, 9 machine parts, 4 gear assemblies and 1 assembly controller plus **1 Unraveled resilience core / 1 Penitent dread ward / 2 Begotten organ cores**. The server ledger gives each locomotive a stable operational ID and keeps its owner/faction, hp, speed, direction, brake, fuel and up to one 40-unit bulk cargo type. Loading debits real depot stock and unloading returns that exact cargo once. `CHARCOAL` powers the current initial prototype at one fuel unit per purchased unit; it consumes finite fuel in motion (base 0.03 plus 0.012 × metres/sec, per second). Unladen max speed is 12 m/s, loaded max speed 8 m/s, acceleration 1.8 m/s², braking 3.5 m/s². Missing/destroyed rails stop the train with bounded damage rather than allowing travel over gaps. These are initial numbers, not a final locomotive suspension, grade, collision, signal or freight-waybill system.

## Aircraft carrier implemented: `AircraftCarrier`

An aircraft carrier is a **real paid hull with an 18-parcel (108-metre) flight deck, five parcels wide**, placed entirely over water outside permanent miasma. Ship construction debits 80 metal stock, 40 machine parts, 20 hardened alloys, 8 precision modules and 5 assembly controllers, plus a separate faction addition: 4 resilience cores / 4 dread wards and 12 reliquary plates / 8 organ cores, 25 grown chitin and 16 living membranes. The 600-HP hull has deck damage, paid repair, finite sea transit fuel, and one-at-a-time deck-flight reservation.

The deck is 8 metres above the base water datum and supports **light aircraft only** under the current 108-metre length. Medium and heavy aircraft must not be permitted without new tested arrestor/catapult technology, larger vessels, or longer deck geometries. The ship's `clearSea` checks all occupied water parcels and city miasma, and a bounded one-parcel host movement consumes 1 marine fuel unit, blocks coast collisions and refuses departure while a deck flight is active. Refuelling consumes real charcoal from the vessel store. Carrier hull damage below 50% prevents launch; repairing a ship consumes 3 machine parts and 2 hardened alloy.

`AircraftFlight.commissionCarrier` now accepts a paid, finished light airframe, reserves the functional deck, checks world/owner/faction, and initializes the existing aircraft physics at deck level. Fuel is **not gifted** to embarked aircraft; players must supply their faction's paid flight canisters. The deck is treated as ground only inside its real footprint; leaving it creates genuine over-water flight. Unauthorized carrier repositioning, double deck occupation and material duplication are denied. Air-to-sea collision, carrier recovery, long-lived ship/plane cross-session placement, deck elevators, launch equipment, crew, naval weapons, wakes and source-backed ship models remain **unimplemented** release features, not mock-completed by this model.

## How this becomes fully playable

1. **Immutable-to-mutable continent integration:** map six-metre cells to fine 3D terrain, actual colliders and `ContinentSectors`; never allow placement inside city A–G or on intercity highways. Expose the overlay as a durable, versioned host snapshot with collision and collision-damage replication and owner ACLs.
2. **Construction pipeline:** stage foundations, a full 6m runway/rail geometry family, cross/curved rails, switches, signals, level grade/soil preparation, taxiways and aprons, and shipyard/drydock with finite costs, actual labor time, power and construction inventory. Tie to the existing factory/settlement limits and distinct three-faction logistics. Provide build ghost, rotate/length selection, per-segment bill, paid progress, damaged repair UI and station labels.
3. **Source-backed rendered assets:** original PSX runway/rail/ties/lights/signal/switch prefabs, faction materials, a ship hull and deck with LOD and moving visual parts, rail cars/locomotives with wheels/bogies, GPU instancing, authoritative audio emitters and world lighting.
4. **Operations:** aircraft cockpit, controls, camera, a real landing/deck service area, physical aircraft parking and cargo, actual rail switches/crossings and train pathfinding/signals, carrier movement and water buoyancy, deck crew, taxiways, automated intercity travel and freight stations.
5. **Authority/persistence:** LAN messages and bounded user intentions, persistent aircraft/rail/ship IDs and owner changes, fuel/energy stock ledgers, per-chunk ownership, conflict/grade collisions, crash/repair/abandon/restart and two-client anti-duplication tests.
6. **Air superiority safety:** city miasma pushes aircraft tangentially and denies aerial attacks/cargo; land/sea/air AA counters, flight corridors, crew and mission capability are real, not standalone balancing tables.
7. **Release gates:** verified 20+ seeds and grade/runway/signal edge cases, 4–16 player multiplayer and restart soak, drawn-in-world runway construction and first light-plane delivery from factory to field/deck, point-to-point rail cargo haul and shipment receipt, and repeatable ship-launched flights.

## Verifiability

`TransportInfrastructureTest` validates paid 6m construction, incomplete and short strips, damaged runway shutdown and paid repair, ownership isolation, rail continuity, duplicate engine suppression, freight debit/unload, bounded train movement and fuel, paid ship hull/repair, deck size, deck reservation and physical aircraft launch. Both the host and GPU Java 17 CI invoke it.

**Important boundary:** A passing headless test validates deterministic logic, NOT live build menu use, a rendered track mesh, actual GPU ship, streamed world travel or multiplayer deployment. Those must be implemented before the user should see a claim that the full transport system is released.

## A4: Head-on locomotive block protection (October 2026)

`RailTransport.advance()` now makes a server-owned six-metre locomotive-block clearance check before each move. A train approaching another live or wrecked engine on the same single track brakes instead of passing through its position; this is deterministic under the existing bounded 250 ms simulation ticks. `RailTransport.restore()` also rejects physically overlapping train coordinates before any persistent state changes. `RailTrafficSafetyTest` drives two opposing, paid and fueled locomotives together across eighteen constructed parcels, checks every tick for crossing/overlap, verifies collision-free safety stops and rejects a forged overlapping snapshot; host and GPU-CI Java suites execute this objective behavior test.

**Still not implemented:** wagon-length block occupation, signals, switches and junction dispatch, full scheduling/depot logistics, live hosted world actors, or player-visible 3D trains. This safety check does not authorize multiplayer rail operation or represent a complete train signalling system.
