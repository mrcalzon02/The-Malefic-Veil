# The Malefic Veil — ground vehicles, vehicle economy and endgame air power

**Status:** Design contract adopted 2026-10-21; implementation tracked by gates below. Existing city-sector drivable truck physics, host-authoritative seats, fuel, collisions, one bulk cargo load and persistent car state are real. Continental spawning is planning data until G2 streaming exists. Air combat is design only.

## Design laws

1. Vehicles are **world objects**, not decorative scenery. A found car can become a player asset only by taking possession and maintaining it through server-authorized gameplay.
2. Ground transport extends survival, inventory, scavenging, construction and faction logistics. Do not invalidate walking, stealth, urban cover or scarcity.
3. **Three playable factions:** The Unraveled (survivors/civilians), The Penitent (human killers/cult), The Begotten (monsters). All groups belonging to the *same faction* are allied regardless of local group membership. Vehicle ownership, vehicle AI, gunners, turret targeting and air AI inherit this hard rule. A stolen vehicle never secretly changes the occupant's affiliation.
4. **Power of three:** each faction ultimately fields exactly three baseline aircraft roles: LIGHT, MEDIUM and HEAVY (nine original aircraft). Upgrades may diversify loads, but not erase role counterplay.
5. Aircraft are extremely late-game **earned production goals**: no early lucky air spawns, shortcuts via a simple purchase, or indestructible aerial domination. Ground anti-air, cover, landing/logistics cost, limited fuel, maintenance, weather and pilot exposure remain meaningful.
6. All mutation of position, inventory, ammo, fuel, wear, damage, component condition, crafting completion and affiliation is **server-owned**. Clients send bounded intentions. No arbitrary client health, cargo or asset-spawn events.
7. The whole generated city footprint, roads, world structures and immutable world objects remain **unbuildable for player structures**. Vehicle parking/salvaging is not a loophole for constructing garages inside generated protected sites. Player workshops, fuel depots, pads and hangars are built on eligible wilderness parcels only.

## City miasma: immovable boundary on aerial power

All procedural cities have permanent high-altitude **purple-black billowing smoke**, an authoritative no-aircraft **vertical exclusion column** sized from each city's geometry with an additional safety standoff. `CityMiasma` holds the deterministic macro exclusion geometry and swept-flight crossing checks; actual aircraft physics, cloud visuals, ordnance interception, map markers and cross-chunk collision are still future gates. **Flying above/below the cloud is never a bypass**, and neither is firing/dropping bombs, drones or cargo into city resource plots from the perimeter. Same rule applies to all nine faction aircraft even at level 50, without faction exemptions. Only airborne movement and airborne attacks are denied; ground vehicles, foot scavenging, NPCs and city freight yards remain entirely accessible. The purpose is to prevent endgame air supremacy from shutting players out of resources needed to advance. The full rules and rendering plan are in [City Miasma / Settlement Economy and Trade](CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md).

## Source material: existing cars

**Verified Drive archive:** [PSX_Style_Cars_by_GGBot_(August2023).zip](https://drive.google.com/file/d/1TeD9Nwo_79ZxL_7mSTZ6euz1M7SjqsX-/view). **Publisher:** [GGBotNet — PSX Style Cars](https://ggbot.itch.io/psx-style-cars). Publisher lists **CC0 1.0** for this pack (not an inherited right for other assets) and `.blend`, `.obj`, `.png`, `.ogg`. The vendor explicitly notes mismatched vehicle scale units; import requires common meter-to-game-unit scale, wheel pivots, collision bounds, normal/material/UV validation, triangle and texture budgets, and side-by-side proof at PSX resolution.

Pack entries **as described by the publisher**, pending per-entry archive extraction/hash:
- Car 01: five-door station wagon — good survivor hauler.
- Car 02: four-door saloon — modest speed/cargo.
- Car 03: three-door hatchback — agile scouting.
- Car 04: minivan — passengers plus medium cargo.
- Car 05: sedan with police/taxi variants — patrol or civilian look.
- Car 06: burned-out wreck — static initial salvage or repair candidate, not an instantly drivable model.
- Car 07: 1920s car — rare collector / characterful find.
- Car 08: step van — heavy cargo, slow acceleration.
- Source OGG includes engine start/loop/off, acceleration, horn, parking brake and door/hood/trunk events.

**Asset gate:** inventory exact archive paths, extract only needed models and sounds, record SHA-256, license, conversion recipe and resulting hashes, produce working GLB runtime assets and verify actual meshes are rendered. Do not equate a catalog ID with an imported model. The original Kenney trucks remain the fallback live runtime meshes until replacement models are imported and verified.

## Procedural generation: discoverable vehicles

### Placement

Vehicles have seeded, stable world IDs and are generated as a **separate overlay** on the immutable map. Position candidates are:
- **City parking:** empty/open, non-developed roadside plots; avoid actual street carriageways and solid world buildings. Because the entire city remains protected for construction, these are *world-spawned objects*, not player-made structures.
- **Highway shoulders:** suitable adjacent wilderness parcel near an intercity road, never in water, marshes, steep mountain, a natural immutable prop, or on the road corridor itself.
- **Later hand-placed sites:** fuel stations, garages, police lots, garages behind ruined houses, repair shops, faction depots, abandoned convoys and scrapyards, with authored collision/access tests.
- **Excluded zones:** protected player spawn clearance, impassable terrain, blocked vehicle door/dismount zones, tight junctions, emergency fire lanes, active player buildings, hangars and runway approaches.

Seeded generation is deterministic and order independent across chunk streaming; use `worldSeed + generatorVersion + siteId` as identity. Parked/wreck/repairable condition, fuel, model, salvage parts, visual wear, possible locked/trapped state, and modest loot are rolled from a *separate deterministic stream* from terrain, NPCs and player spawns. Ensure early regions offer at least one reachable repairable ground vehicle while retaining genuine scarcity. At world start, choose from:
- **Working:** uncommon, modest fuel/condition, can be driven after any access requirement.
- **Disabled:** more common, one or more missing or severely damaged components; must repair before starting.
- **Wrecked:** useful salvage, no free resurrection from zero HP.

No vehicles should spontaneously respawn on top of claimed/repaired/occupied/salvaged vehicles. On the first chunk activation, write a durable spawn-consumed tombstone or a materialized vehicle ID. Subsequent reloads restore that vehicle or its destroyed/salvaged state, not a fresh one. A configurable server option can enable **long-interval unclaimed-world-vehicle replenishment**, but only in unloaded/unobserved, unoccupied eligible locations and never as a repair/loot duplication exploit.

### Ground vehicle mechanics

A catalog defines chassis ID, faction-compatible styling/parts, collision bounds, dry mass, maximum HP, engine torque, gear/speed limits, wheel/tire condition, fuel capacity and burn rate, seats, dedicated cargo slot capacity and weight allowance. Damage has a **hull** and **named subcomponents**:
`ENGINE`, `TRANSMISSION`, `TIRES`, `FUEL_SYSTEM`, `ELECTRICAL`, `STEERING`, `BODY`, and optional `CARGO_BAY`/`MOUNT`.

- HP affects collision integrity and overall wreck state; damaged components independently change drivability. Blown tires reduce grip; failed engine stalls; fuel-line damage leaks fuel; ruined cargo bay spills cargo; destroyed steering can trap the vehicle even with an intact engine.
- Server-verified crashes apply severity-dependent hull and part damage; attacks have projectile/area-of-effect hit regions and component exposure. Occupants are not secretly invulnerable inside disabled or burning cars; apply explicit cover/seat/ejection rules.
- Vehicle inventory is a true capacity-checked server container: bulk freight, small-slot loot, equipment, fuel and spare parts have size and mass. Distinguish existing **single-pallet truck cargo** (`CargoHaulage`) from future independent personal/loot trunk slots; do not discard or duplicate pallet state.
- Installation/repair uses real items, tools, workstation access, time and skills, with an authoritative atomic inventory debit. Salvaging returns a bounded worn-part yield and leaves a persisted wreck/tombstone. Rebuilt chassis require a crafted frame, engine, drivetrain, fuel system, tires, electrical set and assembled body; rarity is controlled by machine/facility tier.
- Ground vehicle progress: **find → inspect → access → load → drive → recover → repair → maintain → upgrade → construct**. Relevant upgrades include reinforced panels, puncture-resistant tires, racks/roof storage, spare fuel, winch, headlights, quiet exhaust, limited armor and faction-specialist modules.
- VFX/audio react to actual state: start failure and sputtering, engine RPM pitch/load, skid, crunch, tire blowout, part damage, horn, trunk/door/hood, fuel leak and failure. Random small pitch/speed shift for repeated impact/sound events prevents robotic repetition.

### Ownership / multiplayer

Vehicle ID is permanent per world, with owner account/faction optional; faction allied groups may share authorized access. Locked does not mean invulnerable; theft/contestation is a real server event. A player disconnect releases transient steering but not ownership. No instant vanishing into inventory. Vehicle state transfers on shard/chunk boundaries by stable ID and versioned snapshots. Vehicles block nearby building footprints (world-owned structures already block construction independently); vehicle movement cannot overwrite generated roads or lots. Server clamps speed, acceleration, steering, interaction distance, item deltas, seat transitions, fuel and repair costs. Dismount checks must find a valid location.

## Static aircraft mesh asset slice — nine actual GLBs (2026-10-21)

**Source accepted and committed:** exactly nine independently modeled, original low-poly glTF 2.0 binary models are in [assets/game/aircraft](../assets/game/aircraft/). Their mesh IDs are `unraveled_needlewing`, `unraveled_harrier`, `unraveled_leviathan`, `penitent_shrike`, `penitent_censer`, `penitent_cathedral`, `begotten_gnat`, `begotten_mantis` and `begotten_behemoth`. The name `penitent_cathedral` remains a stable **working asset ID**; its design is emphatically **an airborne industrial slaughter/harvester machine**, not a gothic cathedral. Every aircraft is distinct real baked 3D geometry with multiple material primitives, colors, embedded 128x128 PSX-style pixel-noise texture, and an authoring palette. Triangle counts respectively: 732 / 796 / 1104, 1056 / 1114 / 2312, and 1626 / 1524 / 3544. Model directions: nose negative glTF Z, Y up, geometry pre-transformed.

The [manifest](../assets/game/aircraft/aircraft_manifest.json) records exact SHA-256, triangle counts and AABB per GLB, plus the [reproducible Python generator](../tools/build_malefic_aircraft.py) and an [inspection contact sheet](../assets/game/aircraft/aircraft_contact_sheet.png). `AirPowerMeshes.load(Path.of("."))` rejects missing/corrupt/mismatched binary models and decodes them with the **existing actual `GlbMesh` importer**. `GameplayMeshes` now exposes these under asset IDs in the ordinary triangle rendering model index. `AirPowerMeshesTest` in the quality gate checks all nine model hashes, real triangle counts, roles, bounds and material colors; Python also validated GLB chunks/embedded texture size and `trimesh` mesh decoding. **Commit presence does not imply a flyable, instantiated aircraft, in-game screenshot, animation or human-tested experience**; those are separate gates.

Design distinctions are required for subsequent mesh detail passes: Unraveled = junkyard civilian fuselages cut to accept military noses, pusher propellers, riveted patch armor, mixed aircraft scrap; Penitent = ugly flying tractor/harvester/dozer cabs, gigantic agricultural tires, feed augers, pipes and **serrated woodchipper-style thruster intakes on the front, flame exhaust behind**, never wingtip deathwheels or temple ornamentation; Begotten = light hunchback mange bat with machine-gun legs and crude harness, medium four-legged vulture/lion with a hooked beak and talons, heavy exposed red whale with gaping shark teeth and **exactly four** ragged dragon wings. No generic common fighter geometry masquerading as three factions.

**Remaining A0 asset engineering:** physically rig wings/legs/maw and control surfaces as independently pivoted submeshes, add actual collision/hit regions, hardpoints, LOD, animated engines/flesh movement, sounds and in-engine interactive previews. Flight roles, turret damage, aerodynamic behavior, refueling, build recipes, AI, server movement, faction targeting and city miasma are not implemented by static GLBs.

## The late-game aircraft: exactly three roles per faction

**Faction roster (original custom art; names are working labels, not source-model claims)**

| Role | The Unraveled | The Penitent | The Begotten |
|---|---|---|---|
| **Light** — scout / skirmisher | **Needlewing**: scavenged ultralight scout, fragile and fast | **Shrike**: shrieking blade-wing raider, burst and evasion | **Gnat**: chitinous flier, dizzying turns and ambush |
| **Medium** — multirole interceptor | **Harrier**: armored field-built attack craft, flexible hardpoints | **Censer**: ritual gunship/interceptor, suppression and balanced handling | **Mantis**: living hunter, pursuit and precise attacks |
| **Heavy** — assault / transport fortress | **Leviathan**: slow armored lift-gunship, squad/cargo support | **Cathedral**: imposing shrine-bomber, area denial and siege | **Behemoth**: living armored terror, brutal close air support |

Names are provisional pending faction-lore approval; the three distinct silhouettes, functional loadouts and roles are binding. Begotten vehicles are appropriately biological and must follow the same fuel/energy/ammunition scarcity principles using their own organ and nutrient equivalents, not a free infinite-energy exemption. Penitent construction blends salvaged mechanics with forbidden rites; Unraveled manufacturing remains industrial/salvaged. Each faction gets comparable strategic opportunity, not symmetric art.

### Rock–paper–scissors combat

The triangle is by **engagement style, not raw HP/DPS**:

- **Light counters Heavy:** high turn rate, climb/evade, attacks blind spots and exposed control surfaces; heavy turret traverse and lock time are ineffective when unescorted. Small weapons must be capable of subsystem mission-kills instead of simply grinding enormous hull HP.
- **Medium counters Light:** reliable short-lock intercept weapons, tracking and speed to deny unlimited hit-and-run; can catch a light craft that commits to an attack.
- **Heavy counters Medium:** armor and sustained fire outlast medium aircraft in a straight attack, while long-range suppression, flak and payload pressure its approach.
- The counter is an *advantage* in equal technology and pilot skill conditions, **never hard immunity**. Terrain, altitude, numbers, skill, weather, component condition, loadout, fuel, ammunition and positioning can overturn outcomes. Matchups must stay interesting in mixed squads.

| Baseline attribute (relative index) | Light | Medium | Heavy |
|---|---:|---:|---:|
| Speed | 5 | 3 | 1 |
| Agility / avoidance | 5 | 3 | 1 |
| Armor | 1 | 3 | 5 |
| Firepower / payload | 1 | 3 | 5 |
| Cargo / seats | 1 | 2 | 5 |
| Manufacturing upkeep | 1 | 3 | 5 |
| Anti-heavy subsystem threat | 5 | 3 | 1 |
| Anti-light tracking | 1 | 5 | 2 |
| Sustained anti-medium pressure | 1 | 2 | 5 |

These are **design indices**, not fake live engine stats. Final numbers require simulated engagements and flight playtests; faction variants modify mechanics while respecting class identity.

### Air access and economy

**Hard endgame prerequisites:** faction research and manufacturing milestones; wilderness-built hangar/launch infrastructure; rare components, refined fuel/power source, specialist tools and multi-stage frame construction; licensed/learned pilot qualification; defensive logistics. A heavy craft should represent a serious coordinated faction achievement, not one afternoon of looting. Each class has a distinct build and sustain cost; losses consume scarce inventory, but salvage recovery creates a meaningful counterattack and rebuild loop. Aircraft cannot be captured at spawn as ready-to-fly early-game trophies.

Flight model: server-owned six-degree motion with clear arcade-readable controls; stall/ceiling/ground-effect rules appropriate to airframe; collision, landing, takeoff, fuel and altitude bounds; ground-target LOS; air-to-air aim and lead cues; optional transport seats; component-level damage. Ground combat retains cover, concealment, anti-air turrets, man-portable deterrents, repair/rearm positions, and ways to move under radar or weather interference. Aircraft cannot eliminate the entire game from outside practical return-fire or visibility distance.

**Support facilities:** aircraft assembly plant, hangars, repair stands, refuel/rearm depot, radar/lookout and anti-air defenses. Placeable only on validated wilderness plots, not generated city rooftops or roads. Building prerequisites cross-link existing industry/crafting era and faction facility systems.

## Two specialist anti-air towers per faction — deliberate medium-aircraft gap

These are **six custom faction models**, not a reskin-only set; each faction gets exactly two equivalent mechanical roles. The defenses use wilderness-only settlement facility rules, character-owner permissions, owner-level capacity and faction friend-or-foe; they never spawn player-owned towers on immutable world roads or inside city lots.

| Role | Unraveled | Penitent | Begotten |
|---|---|---|---|
| **FAST_TRACKER** (rapid precision, small strikes) | **Needle Array** — radar/servo rotary cannon, feed belts, spinning barrel array | **Confessor's Choir** — ring of chanting warders and repeated directed bolts | **Spineweaver** — swiveling living stalk with high-rate barbed spines |
| **HEAVY_BREAKER** (slow tracking, devastating projectile) | **Siegebreaker** — costly slow heavy guided missile rack with weak turn authority | **Judgment Bell** — deliberate arcing consecrated siege shell/bolt, loud warning | **Maw Spire** — huge organic piercing pod, slow flight, visible pressure buildup |

**Targeting contract:** only genuinely hostile aircraft may be targeted. Every crew and individual of the same playable faction is always allied; settlements within that faction do not fire on each other even with different owners or access roles. Neutral or unverified aircraft follow a server policy (never client-reported faction). AA ammunition/fuel/nutrient, power/ritual upkeep, range, unobstructed firing arcs, cooldown, projectile speed and rotation speed all matter. The guns are destructible, repairable, salvageable and disable on supply/power loss.

**Desired engagement envelope:** fast tracker has excellent acquisition, traverse, hit chance and anti-light damage but low penetration/low per-shot damage. Medium hull/armor reduces its effective damage; heavy armor renders it mostly harassment. Heavy breaker deals large chunks of high-penetration damage to large, durable and slow airframes, but telegraphs firing, takes time to rotate/lock, has slow projectiles with limited midflight corrections and poor hit chance on moving light/medium airframes. No universal homing that erases medium evasion.

**Attackers' decision:** light flyers die rapidly if exposed to tracker coverage; heavy bombers can be disabled quickly by a few heavy direct hits; medium flyers have the **longest expected survival against both standard AA systems** and are the preferred assault force. This is a deliberate additional incentive to take medium craft, without changing the existing air-to-air triangle of LIGHT > HEAVY > MEDIUM > LIGHT. Mixed tower grids, concentrated fire, bomber escorts, weather, elevation, line-of-sight breaks, ground attackers and aircraft maneuvering retain meaningful counterplay. Medium aircraft are **not invulnerable** and still burn fuel/ammunition or risk subsystem disablement.

**Illustrative balancing target (non-runtime):** tracker projectile 10–12 raw damage with ~0.35s fire interval, high anti-light tracking and poor armor penetration; breaker projectile ~300 raw damage with ~9s cycle and highly limited turn rate / evadable warning. Test target classes in identical skill/equipment situations for (a) fast-tracker defeats light faster than medium; (b) heavy-breaker defeats heavy faster than medium; (c) medium endures a standard one-of-each two-tower array longer than both light and heavy. Require actual flight-combat simulation and human tests before tuning is accepted. Counterplay must remain possible with skilled pilots and targeted base suppression.

**Progression:** these are high-tier **DEFENSE** structures after power/command/security infrastructure and anti-air research, not starter improvised turrets. Each consumes one settlement automated-defense slot from its owner's level-derived cap; its construction, ammunition, utility hookups and repair remain paid, staged and raid-vulnerable. Faction-specific production inputs (metal/machine parts, consecrated reagents, nutrients) require equivalent economic effort.

**Release gates AA0–AA3:** AA0 six distinct PSX silhouettes and component/recipe data; AA1 trusted target/faction selection and owner-authorized wilderness placement with defensive-cap accounting; AA2 host-authoritative rotation, projectiles, damage, ammunition and utility outages; AA3 reproducible fast-vs-light, heavy-vs-heavy and two-tower medium-survivability tests under latency, line-of-sight, rain/fog and multi-faction conditions. No functional tower claim before all three implemented behavior slices are tested.

## Implementation and release gates

| Gate | Deliverable | Verification |
|---|---|---|
| **V0 — catalog/provenance** | Exact GGBot entries, CC0 evidence, normalized scale/mesh/texture pipeline; select 3 first live road cars | Archive hashes and visual screenshots; real OBJ→GLB conversion, loading and collision align |
| **V1 — seeded discovery blueprint** | Deterministic parking and roadside shoulder spawn registry, model/condition/affiliation metadata, no overlaps with protected spawns | Multi-seed identical output; site adjacency and no water/road carriageway; IDs stable |
| **V2 — ground playable** | Spawn-into-loaded-sector, actual GGBot car models, multiple chassis physics, visible damage states, usable multi-slot cargo | Solo and LAN vehicle/loot tests, no client-authoritative state; unload/reload no respawn dupes |
| **V3 — workshop economy** | Parts damage, parts harvesting, fuel/repair, true constructed ground vehicles, craft/repair transactions | No free repairs; wreck stays wreck; atomic inventory debit; backup/restore; crash/part simulation |
| **V4 — factions & raids** | Faction-compatible cars, group access and neutral salvage, combat target filtering, vehicle-mounted equipment | Same-faction group alliance is never broken; no PVP permission bypass; stolen vehicle identity is correct |
| **A0 — nine aircraft art contracts** | Three distinct source-owned airframes per faction, readable PSX silhouettes, render/collision LOD | 9 distinct valid models, matched hit volumes and audio, no recolor-only faction pass |
| **A1 — air physics/industry** | Host flight authority, crafting gate, hangars, pads, transport and supply chain | No early acquisition; valid takeoff/landing; fuel/ammo/repair cannot be spoofed |
| **A2 — mixed arms combat** | Light/medium/heavy role specializations, air-air and ground-air weapon systems | 3-way counter advantage in controlled encounters, with meaningful skill counterplay |
| **A3 — operational safety** | Chunk handoff, reconnect, pilots/crew, crash/salvage, performance and anti-cheat | 4–16 player soak, speed/teleport abuse, high-latency tests, scene-stream stress and real playtest |

**Dependencies:** Continent G2 streaming must precede V2 worldwide ground vehicle deployment. Full wilderness construction G3 must precede manufactured hangars/workshops. Air combat depends on the final client renderer, authoritative flight physics and integrated real factions, not just nine catalog rows. No aircraft release until both ground anti-air and its counterplay are working.

**Non-goals / explicit blockers now:** This document does not claim the GGBot car archive has been converted, that the 224×224 continent is traversable, that multi-slot cargo/repair/build systems currently exist, or that nine custom aircraft models are completed.
