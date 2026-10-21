# FROM CAMPFIRE TO INCURSION — PUNISHING CRAFTING AND INDUSTRY
**The Malefic Veil • canonical economy blueprint v1.1 • 21 October 2026**

**DESIGN AND DATA CONTRACT.** The runnable city sector does not yet support walkable, server-owned wilderness bases, working production grids, automated industrial plants, or reality-incursion construction. The Java model and recipe gates implemented alongside this specification are only foundations. Never market planned assets, effects, automated crafting, or abandoned concept pictures as actual in-game systems.

**Core pacing rule:** *Surviving your first night should be achievable; constructing self-sufficient industry should be a massive cooperative undertaking.* The player should feel the transformation from scavenger to builder to industrialist to territorial nightmare. No intentionally inescapable soft locks. Recovering after complete inventory loss must be feasible naked, **but recovering an industrial economy is not easy**.

## New v2 farming, equipment and three-faction cooperation dependencies

The [city miasma / settlement economy / upgrade and trade specification](CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md) is the binding companion to this era progression. `CraftingEraRegistry.SCHEMA_VERSION=2` adds early facilities for shelter, plots, seed nursery, compost, herbal propagation, roots/cellar storage, smoking, weaving, watch posts and palisades, with later grain mills, irrigation, animal pens, recyclers, controlled greenhouses, fuel refineries and vehicle depots. The initial recipes consume finite ingredients and time; **none currently instantiates growing plants, animals or usable equipment**.

`CrudeStructureIndex` is a full blueprint-to-primitive-geometry plan and may later be replaced with faction-native PSX models without changing collision and save IDs. `EquipmentUpgrades` describes paid single-rank upgrades through **level 50** across facilities, vehicles and defenses, increasing both benefit and upkeep. `SettlementSupplyBalance` asserts that a single founder's maxed **28-normal-facility** site cannot alone sustain ten level-50 defenses indefinitely; regional trade is nonoptional at maximum fortification. `FactionTradeNetworks` requires different collaboration shapes: bulky stable Unraveled serial factory shipping, Penitent parallel rites with actual concurrent performers and expiring commitments, and Begotten living stocks with maturation and decay. Do not claim a trade transaction happened because a data graph contains a recipe.

## Two layered progression tracks

The **Era** measures economic capability; the **Facility Tier** (T0–T4 in `FACTION_FACILITIES_BASE_CONSTRUCTION.md`) measures base complexity and footprint. They are not interchangeable. A temporary camp can precede the Claim Heart, while expensive T4 graveyards may coexist with late-Era infrastructure.

| Era | Player fantasy | Primary crafts and real facilities | Gating bottleneck | Automation ceiling |
|---|---|---|---|---|
| E0 — Stranded | cold and hungry, a few gathered branches | hand-built campfire, tent, crude crate, woven fence sections, simple warning/noisemaker and sharpened-stake game trap | wild timber, fiber, stone, safe wilderness ground | **none**; hand gathering and direct player interaction |
| E1 — Encampment | safe-ish camp, skilled subsistence | fixed Claim Heart, manual bench, drying rack, water boiling/filter, rain catcher, garden, handcart, improvised storage and defenses | wilderness travel, crew labor, water and food | one manual recipe at a time; containers cannot auto-produce |
| E2 — Salvage Age | leave base to scavenge cities and return | hand forge, bloomery/smelter, charcoal kiln, brick maker, recycler, freight bay, crude truck servicing, anvils | **first METAL_STOCK and MACHINE_PARTS cargo deliveries**; ore/fuel/heat | mechanical assist only; batch is manually queued, inefficient |
| E3 — Steam & Works | towns begin to sound like factories | boiler, steam engine, mechanical shafts/belts, pumping station, powered crusher, stamping press, foundry, depot/warehouse rails | repeat urban freight, coal/charcoal supply, real water/pressure/cooling | limited conveyor/belt lines, steam power, scheduled machine cycle |
| E4 — Electrification | a recognizable Industrial Revolution | generator, fuel distillery, electrical bus, load shedder, workshop lathe, precision tooling, refinery, automated sorters | manufactured parts, energy balance, power/fuel, maintenance technicians | multi-machine queues, bounded electricity-based automation |
| E5 — Complex Industry | a city-state base, not a camp | assembly line, bulk logistics dock, secure vehicle factory, automated quarry/forestry with environmental impact, repair bays and electronics | massive infrastructure, land, worker/electricity throughput, hostile raids and supply-chain complexity | closed loops where inputs physically arrive; **never** material or energy creation from nothing |
| E6 — Endgame Divergence | industrial fortress or invading physical nightmare | Unraveled resilience grid and advanced fabricators; Penitent Terror Works and dread-powered industry; Begotten membrane reactors, assimilating foundries and **reality-incursion anchors** | T3/T4 base, stable E5 infrastructure, scarce city materials, faction reagents, expensive exposed landmarks and prolonged commissioning | faction-specific service *parity* with radically different visual/operating methods; no cheap shortcuts |

**Progression can be staggered** between crew members and parallel subprojects. The very first survival shelter is deliberately cheap. Costs increase through logistics, multi-stage transformations, nonreturnable input consumption, service upkeep, site selection and time—not simply multiplying every starting recipe by 100. Durability and scarcity are severe; grind should be meaningful and transparently signposted.

## E0 — a real survival start (low cost, no prerequisite heart)

Players with zero gear can harvest limited, honestly spawned hand resources. The following **portable building kits** are hand-crafted from nearby materials. They are **not** full permanent city-style structures. Using the kit requires server-valid wilderness placement and leaves a temporary physical object. A kit in inventory is lost on death; a placed camp remains world-bound and is subject to normal damage/decay.

| Kit | Initial recipe | Actual in-world function | Limits |
|---|---|---|---|
| Campfire | **3 timber + 2 field stone** | heat, crude cooking and boil dirty water using gathered fuel | light/smoke attract Lost; fuel 1 timber / proposed 12 in-game minutes; extinguishes in rain without shelter |
| Cloth / scavenged tent | **4 fiber + 2 timber** | weather protection and rest, temporary wilderness respawn eligibility only after binding and safe-state check | 1 per character before charter; no inventory retention, no item-granting respawn; 2×3 footprint |
| Wooden crate | **5 timber + 2 fiber** | **12 slots**, 80 mass capacity, stationary storage ledger and physical interaction | 1 per unclaimed camp; destructible, subject to loot/raids; cannot remotely withdraw |
| Fence section (2 pieces) | **3 timber + 1 fiber** | short cover/animal warning, collision/LOS only when physically justified | 1×2 per piece; vulnerable to damage and fire, cannot cover roads/world structures |
| Sharp-stake ground trap (game object) | **2 timber + 1 fiber** | a visible, destructible PvE/PvP close-contact trap; audible cue and meaningful short-lived status | 1×1, no construction details for real-world devices; deploy in wilderness only; 3 active per player pre-charter |
| Tripwire bell/noise alarm | **1 timber + 2 fiber** | signals nearby approaching entity, no damaging effect | audible, limited radius, false triggers; 2 active pre-charter |
| Crude bandage | 2 fiber + 1 medicinal herbs | treats a finite status severity or small amount of damage | no miraculous instant resurrection |
| Improvised water filter | 3 fiber + 2 field stone | enables a finite number of dirty-water cleaning operations | wears out and needs fresh media |

**No zero-item death loophole:** A naked player can gather E0 materials by hand. Safe item retrieval requires returning to actual stationary crate/supply stock; death never automatically transfers backpack gear into that crate. Graveyard recovery is the one separate monumental exception.

**E0 placement caps:** unclaimed temporary camp 1 tent + 1 crate + 1 fire + 8 fence pieces + 3 damage traps + 2 alarms per character, 24 m temporary camp boundary, lasts 12 real hours after final active use if the owner never founds a base. Such camps do **not** project territory, create protection hours, prevent raiding or evade city masks. To avoid camping spawn corridors, use 50 m default protection around world spawns (tunable after surveying actual world distances) and mandatory route-clearance check; no snap-placement inside any world structure. All these defaults are pending in-engine balancing, not a current live feature.

## E1 — found a settlement rather than multiply tents forever

Claim Heart remains **12 timber + 12 field stone + 6 fiber** (design cost from canonical facilities registry). It formalizes a wilderness crew claim but **does not automatically refund or upgrade a temporary camp**. E0 kit salvage is limited and requires actual dismantling/time. The crew may erect workshop, dry storage, a rain catcher, cooking hearth and food garden at T0/T1, but must supply physical water and heat. No free permanent room/bed respawn gear. Workbench craft uses **6 timber + 4 stone + 2 fiber**, not the previous inaccessible recipe that demanded pre-industry `METAL_STOCK` before a player could build their first bench.

New starter buildables gradually introduce truthful maintenance: tent wear, crate slots, fence repair, smoke/fire attention, herb/food spoilage, purification filter usage and exposed-world risk. Early survival timers allow a player to understand a problem before dying; initial desolation is hard because of weather/threats and transportation, not because recipes are impossible.

## E2 — salvaged metals are a serious expedition

**Cargo is sacred:** the city's two sources `METAL_STOCK` and `MACHINE_PARTS` are truckloads, not freely conjured pickaxes. A heavy load has its own receipt ID and delivery/claim ledger. Existing loaded Kenney truck GLBs are 3D **loaded-truck variants**, not independent cargo modules. The player finds and repairs a truck, drives into hostile city districts, stops, unloads by foot, survives the return trip and delivers the load to a real **wilderness freight bay**. Until the freight bay is built, early industry may hand-process rare small salvage in limited quantities but cannot produce heavy bulk from pocket recipes. Freight bay costs remain 4 METAL_STOCK, 3 MACHINE_PARTS, 24 stone, 16 timber from the facility plan—this implies a commissioning bridge is required: an E1 **temporary cargo laydown** may secure **up to three truckloads** into a guarded base construction stockpile with on-foot unloading, not process them, and carries serious theft/decay risk. This prevents a progression deadlock where the permanent freight bay needs freight to exist.

**Explicit first industrial path:**
1. Build bench and a temporary cargo laydown.
2. Harvest fuel, ore, and forge stone; secure food/water.
3. Repair/acquire truck and haul first `METAL_STOCK` / `MACHINE_PARTS`.
4. Commission charcoal kiln, hand forge and basic smelter with correct staged costs.
5. Cast fasteners, simple metal plates and repair components; these use smaller refined goods derived from processed ore or **verified fractions** of unloaded bulk stock, with lossy conversion.
6. Expand stable storage, build the full freight bay, then establish E3 machines.

No hand recipe can turn a pallet into hundreds of light portable copies without a machine and consumption receipt.

## E3/E4 — physically modeled industrial revolution

### Actual processing graph (example baseline)

`TIMBER + LABOR -> FIREWOOD -> CHARCOAL` (kiln, consumable fuel); `RAW_ORE + CHARCOAL -> IMPURE_METAL -> IRON_BLOOM -> METAL_BILLETS` (smelter, forging and waste); `METAL_BILLETS + ENERGY -> PLATES / FASTENERS` (press and precision bench); `METAL_BILLETS + MACHINE_PARTS + ENERGY -> SHAFTS / GEAR_ASSEMBLIES` (lathe, workshop); `DIRTY_WATER + FIREWOOD/FILTER -> CLEAN_WATER` (finite purifier); `WATER + FUEL + INSPECTED BOILER -> STEAM` (lossy conversion); `STEAM + SHAFTS -> MECHANICAL_WORK`; `GENERATOR + FUEL + SERVICE PARTS -> ELECTRICITY`; `ELECTRICITY + WATER + ORE + REAGENTS -> REFINED_MATERIALS`. Generic **power**, **steam**, **dread**, **metabolites** and **ritual service** are operational *rates* or utility buffers, **not freely tradeable material items**.

Default target machine physics and simulation contracts:
- Input buffer **<=64 item stacks / machine**; output buffer <=64; bounded in-flight belt packages <=512 / active zone. All transfers are server-clock deterministic and idempotent.
- Every machine has finite **power input, operating temperature/pressure, cycle duration, wear, service interval, waste/output and a hard hourly throughput**. Under-supplied runs do not invent product or progress.
- Mechanical shafts can move work only through an actual powered topology graph; electric distribution has per-circuit capacity and priority/load shedding. Robots/AI workers are **not** an excuse to skip input, worker safety, or fuel costs.
- Conveyors connect physical input/output ports. Belts cannot pass through fixed city walls, world structures, other crews' claims, or server-only hazards. Cargo loading jobs are bounded: 1 truck pallet at a time with visible motion/state; no instant transcontinental shipping.
- Cropped garden, lumber operation, quarry and ore extraction degrade resources or require replenishment and permitted wilderness territory. Resource regenerates only according to explicit node clocks, not each time a region unloads.
- **Production batch transactional rule:** reserve inputs; record job with unique operation ID; debit exact inputs as work is committed; hold intermediate/outputs in machine-owned storage; produce a single output instance after final validated cycle. A crash replays journaled work safely and cannot mint multiple outputs. Stop/start, role changes, rollback and chunk eviction must not duplicate production.
- Automated structures simulate **when zone is unloaded** through bounded elapsed-time catch-up from persistent UTC, using available supplies and service history; never extrapolate infinite fuel, items or production from offline time. Cap catch-up to an explicit stable audited window (e.g. 24h) and place excess in safe paused state.

**Abuse/performance gates:** ~0.5–1 Hz facility economic ticks, fixed event queue budgets, rate-limited input network updates, instanced rendering with 3-stage LOD and aggregate factory animation at distance. No 60 Hz rebuilding of production chains, no one JVM thread per conveyor or crop, no unbounded debris or machine entities. Destroying power before a cycle is complete pauses/fails work and accounts for scrap, not always free refunds.

## E5 — expensive automation still has supply chains

Automated warehouse/freight unloading, sorting belts, industrial assemblers, security/alarm control, motor pool and foundries require real physical facilities, external access, storage and utility networks. Building an automated metal plant is an **expedition chain**: scouting ruins, risking life for pallets, importing them by truck, supplying wood/coal/water, processing ore, maintaining tools, and defending the plant from Lost incursions and PvP sieges. Individual upgrade chains should have 3–5 meaningful manufacturing intermediates, not fifty cosmetically distinct rivet recipes. Automation unlocks throughput, safety and scheduling, **not unlimited resources**.

Default automation ability ramps:
- E2 manual: one queued recipe per workstation, 1 worker, costly input waste.
- E3 shafts/steam: one source powers 2–4 connected workstations, small visible conveyance, heat/wear.
- E4 electric: 4–12 connected machines in actual circuit topology, limited sorting/autonomous queues.
- E5 industrial: 12–32 machines under multi-utility constrained dispatch, alternate power/fuel sources and dedicated crew maintenance; hardware/zone budgets are configurable.

## E6 — radically different endgames, equivalent production accounting

### The Unraveled — survival industry taken to extremes

A fortress-city's fuel depot, rugged steam/electric grid, precision fabrication works, repair trains and defensive platforms are recognizably human: a skyline of repurposed steel and grime. Their endgame output is **resilient industry and convoy support**. Machines fail visibly, need real replacement parts and rely on risky city deliveries. They may receive capped maintenance efficiency and easier manual intervention, not free fuel, invulnerable walls or automated body retrieval.

### The Penitent — machinery that harvests terror

The Penitent are **cultists and slasher killers**, not a generic pious civilization. Their bases look like murderous summer-camp lodges, furnace slaughterhouses, motel corridors, false-exit mazes and heavy black industrial smelters. Chains, boiler houses, numbered prisoner-like rooms and Weeping Eye apparatus feed **Dread Works**. This facility requires authentic **server-verified hostile encounters** to produce finite `DREAD_IMPRINT`, plus heat, power, reagents and a capped staffed work cycle. Dread fuel can power terror wards, warning systems and unstable machine variants with physical risk. No real-player suffering/farming, corpse farming, AFK pressure loops or unsignaled unavoidable PvP kill zones. Their late game may *bend perception* and twist approaches architecturally through authored labyrinth pieces, but must keep actual walkable/navmesh geometry authoritative and visually honest.

### The Begotten — industrial otherworldly incursions

The Begotten are **monsters**. Their E6 expansion is not just a flesh shader or differently labeled generator. Industrial girders and towering arch supports must remain visibly engineered **beneath draped flesh and incorporated bodies**, with beating connective tissues, membrane collectors and large articulated machinery. Their advanced workstations can **reshape wilderness**, but only through a deliberate, expensive, bounded **Incursion Anchor** that consumes stored `METAL_STOCK`, `MACHINE_PARTS`, manufactured `NUTRIENT_STOCK`, water and locally acquired reagents while emitting obvious visual/audio warnings.

**Incursion phases (world, not UI fiction):**
1. **Seeding:** claim-owned bounded nucleus is built from real stock, projects a construction footprint and maps valid wilderness parcels. No effect reaches any generated city, road, coastline-protected ecological feature, another claim or active spawn/safe route.
2. **Incubation:** resource- and time-consuming conduit network establishes environmental field inside the approved region; steel ribs/biological membranes grow along authored collision-aware anchors. Anyone can see progress from outside. Cannot be accelerated infinitely by relogging.
3. **Breach:** the area becomes visually and mechanically abnormal: limited terrain material changes, biome tint and fog pockets, new authored body-metal structures, flickering local lighting, unsettling gravity/air effects **only if explicitly validated for player controls**, finite NPC response rate and fertile organic growth. No automatic terrain/structure destruction or teleport of strangers.
4. **Assimilation:** powered/staffed industry may craft reinforced body-metal modules and special faction consumables using physical material conservation, nutrient feed and power equivalents. Anchors suffer wear/raid damage and expose weak points.
5. **Collapse/reclamation:** on disable or destruction, stop new changes and NPC spawns, then schedule bounded site reclamation to a source-continent biome snapshot; maintain abandoned debris as finite salvage, not an unbounded persistent mesh shower. Must preserve terrain navmesh, heightfields and property ledger and avoid trapping players underground.

**Territorial limits (provisional):** one E6 incursion per base, center constrained by claim; maximum 36-meter influence radius at initial level, upgradeable once to 60 meters at high additional material cost, never overlapping world-owned cities or protected public roads. Streaming is chunk-scoped, NPC spawns have own area caps and countdowns, no infinite cloud of particles. Dread and nutrient are specialty inputs, not ways to mint resources from zero. Incursion can be countered through accessible, clearly marked anchors, power/fuel/nutrient disruption, declared PvP windows and reverse reclamation; it cannot expand invasively through player claims while their owners are offline.

**Visual gates:** actual OpenGL 320×200 in-game daytime/nighttime/high overhead captures from GitHub Actions, model/license provenance, rendering LOD/triangle budgets, walkable interior and destructible state. Do not claim the metal/body arch meshes or terrain-transmutation shaders exist until created and verified.

## Building, traps and PvP consequences

- **Before base charter:** only E0 temporary kit placement in valid wilderness; pre-charter 3 damaging traps maximum, ordinary destructible fence and alarm. No aggressive auto turret.
- **After charter:** upgrades unlocked by explicitly constructed facilities. The *same cheap trap* can be produced in bulk only with real timber/fiber stock and labor; placement budget caps and raid restrictions apply to avoid spawn trapping. More elaborate alarm / gas / ritual hazards require their actual faction tech station and reagents.
- **Trap gameplay contract:** `ownerCrewId`, placement footprint, visible telegraph, arm delay, target permission rules, finite durability/charges, max damage/status/LOS, immunity to owner/friendly by configured party rules, 1-s cooldown on repeated triggers, disable/repair path, despawn under siege policy. No one-hit lethal traps on commonly traveled routes or inside public road buffer. All trap triggers are decided by server physics, never a client's reported proximity/damage.
- **Otherworldly territory and industrial machinery** must never bypass the existing total carried-item loss on death, graveyard requirement, wilderness-only construction, or crew permissions. No occult "keep everything" charm.

## Economy cost tuning and acceptance

Use baseline **time-to-first** metrics rather than only cookbook counts: E0 fire within ~5–10 minutes, tent+crate+fence within first ~15–30 minutes in ordinary weather; E1 settled food/water within 1–3 cooperative hours; E2 first sourced cargo in a hazardous city trip; E3 steam age ~8–15 focused crew hours; E4 electricity ~20–40 crew hours; E5 resilient automation ~50–100 crew hours; E6 incursion ~100+ **coordinated** crew-hours *in addition to nontrivial staged cargo hauling*. These are balance **targets for testing**, not simulated or verified timing claims. Solo players may be slower; no mandatory impossible multiplayer-only input dependencies.

Test the minimum complete reachable dependency path to E0, E1, E2, E3, E4, E5, E6. At every required era, list exact obtainable sources, staging costs, survival maintenance, danger and exit materials. Fail CI on circular recipes, unresolvable station requirements, negative/zero yields, illegal city resources, bypass of heavy cargo provenance, machine input/output duplication, infinitely positive lossless loops or compact body-recovery exploit.

**Milestone policy:** ship E0 genuinely playable before advanced features. E1 and E2 bring the real wilderness+city freight loop. E3–E5 require genuine production simulations, electrical/mechanical graph authority and 24/7 persistent facilities. E6 requires source-verified PSX horror architecture. Green unit tests for a registry are not evidence that an incursion has changed terrain or that conveyors are visible.
