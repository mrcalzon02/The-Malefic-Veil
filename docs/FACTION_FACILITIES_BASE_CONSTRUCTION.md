# FACILITIES, FACTION BASES AND WILDERNESS CONSTRUCTION
**The Malefic Veil — canonical design specification v1.0 — 21 October 2026**

**Status: DESIGN APPROVED FOR IMPLEMENTATION PLANNING, NOT IMPLEMENTED FACILITIES.** Current city gameplay rejects ALL player structures, and the wider continent has no live walkable wilderness sector. The existing `SurvivalCrafting`, `CargoHaulage`, `ContinentLayout`, `ContinentZones`, `RaidProtection`, `FactionClasses` and identity architecture are useful foundations, **not** evidence that any facility listed here is functioning. Do not release placeholder buttons, fake construction placements, client-authoritative structures, or invented screenshots.

**Progression companion:** [FROM CAMPFIRE TO INCURSION](CRAFTING_INDUSTRY_INCURSION_PROGRESSION.md) is the authoritative era/recipe logistics sequence and [THREE ARCHITECTURAL LANGUAGES](FACTION_HORROR_ARCHITECTURE.md) defines the monster/body-metal and Penitent terror-industrial architectural requirements. These complement, rather than replace, the facility tiers below. **E0 temporary survival kits** (campfire, tent, crate, primitive fences/traps) are cheap and may be deployed on suitable wilderness **before** a permanent Claim Heart; they do not constitute a base claim or protect death inventory. `CraftingEraRegistry.java` is versioned **data**, and `IndustrialWorkstation.java` is an isolated server-side process component; the wilderness placement, actual power topology and faction world transformations are not yet playable.

## 0. Hard invariants

1. **Player facilities may be constructed only in wilderness.** All seven generated cities, every empty city lot, plazas, ruined buildings, rooftops, generated roads, bridges, outposts and all preexisting fixed-world structures are **immutable world-owned objects**. No replacement or salvage of city geometry. A whole rotated 3D structure footprint, foundation, obstruction clearance, expansion, overhang, underground layer and construction path must pass the world authority check. Walking on terrain never implies permission to build.
2. **Death means total carried-inventory loss.** Equipped weapons, armor, tools, hotbar, backpack, ammo in clips and magazines, currency physically carried, consumables, crafting materials, carried keys/items, food/water containers and any other transferable possessed object are lost on death. No free gravestone loot pile, dropped PvP backpack, default kept weapon, insurance fee, protected inventory pocket or class-specific exemption. Only a *preexisting operational* monumental graveyard can establish a single recoverable corpse escrow. Knowledge, appearance ownership, class, faction and character identity are not physical items and remain.
3. **Never delete items silently.** Items affected by death are either (a) consumed in an auditable, irreversible death event or (b) moved to one uniquely keyed graveyard escrow as part of that same atomic event. On respawn living inventory is empty in both cases. A corpse escrow can never coexist with duplicated inventory items, player-lootable corpses or cargo ownership.
4. **The three playable factions are The Unraveled, The Penitent and The Begotten.** The Lost are world NPCs/monsters, not a fourth player construction faction. A player character has one immutable-at-ordinary-play faction; a **crew** is a separate organized ownership/permission unit, and its base allegiance must be coherent with member affiliation.
5. **Facility functions are genuinely earned.** No free power, repair, unlimited water/food, invisible background refinement, teleported truck cargo, instant build completion or immortal unattended defenses. Server owns inventories, site allocation, crafting, construction progress, fuel, upkeep, capture/destruction, raid eligibility and corpse recovery.
6. **Parity of access, not identical recipes.** Every faction can shelter, feed, cleanse water, craft, defend, repair vehicles, unload freight and establish a graveyard. Their appearances, naming, utility resources, distinctive production methods and specialized bonuses differ. No faction can avoid the price/footprint of corpse recovery.

## 1. Structure of an actual base

**Hierarchy:** realm → continent sector/zone authority → wilderness claim → crew/base → constituent facilities → bounded interactive/storage/power/production components → authenticated characters and their permissions. Do not make every faction a single globally shared construction owner: each faction contains multiple independent crews that can cooperate or feud according to server mode.

**Founding:** A player creates a same-faction crew, names it, selects an eligible wilderness site and builds a physical **Claim Anchor / Heart**. Anchor must exist and be completed before any permanent facility. Crew charter, base UUID and owner character/account IDs persist; the anchor is a physical destructible facility rather than an abstract invisible radius. Initial claim footprint target: **48-meter radius** with extensions via costly expansion pylons up to 96 meters in approved configurations. Final limits are balancing values; collision and zone geometry must derive from measured world meters, not from the current 224×224 macro blueprint index.

**Site rejection:** CITY_PROTECTED, WORLD_STRUCTURE, ROAD_BUFFER, WATER, CLIFF_SLOPE, COLLIDER, RESOURCE_NODE, SPAWN_CLEARANCE, SAFE_ROUTE, CLAIM_OVERLAP, NO_CHARTER, FACTION_DENIED, OUT_OF_ZONE, HEIGHT_CONFLICT, CAPACITY, CLAIM_BUDGET. Return the exact reason to the client and show projected bounds before resource debit. No snap exception, camera exploit, overhang, temporary foundation or subterranean door may cross protected land. Natural trees/resources in build footprint require explicit manual wilderness clearing, logging/loot and terrain modification transactions, never hidden deletion of world structures.

**Ownership and crew roles:** Founder (charter changes, disband); Steward (roles, alliances, defense policies); Architect (place, upgrade, remove buildings); Quartermaster (stock and production priorities); Member (approved stations and storage); Guest (explicit bounded access). A single player may have access to multiple crew bases, but may found only one active crew charter per character under default realm rules. Guests cannot access the morgue/corpse ledger. Server administrators have auditable recovery/rollback capabilities, not hidden personal resurrection privileges.

**Expansion:** 1 claim anchor, up to 2 extension pylons at starting balance; default hard cap 28 normal facilities plus 1 monument graveyard per base, 10 automated defense units, 6 production queues and 1 large freight yard. Tier upgrades expand service capacities rather than multiplying thousands of decorative entities. Housing/storage/wooden walls can be modular assemblies inside these limits. Ruined/abandoned bases decay only under declared upkeep rules with visible 72h+ notice and recovery grace; no silent removal when a player disconnects.

**Faction conflict:** bases are PvP objectives. Building destruction/loot is governed by server-defined vulnerability windows, the existing raid-protection direction, and explicit online/offline raid notices. Never apply a "logoff gives immediate invulnerability" rule. Construction, repair and evacuation actions during siege have capped rates and cooldowns. No raid loophole allows building walls inside city geometry or trapping public access roads.

## 1A. Character-founded settlement authority and level-based budget (2026-10-21 amendment)

**Binding decision:** A settlement belongs to the **founding persistent character ID**, not the currently connected player/socket, the entire faction, the builder who helps at a worksite or implicitly to the founder's account. Store both `founderCharacterId` and an authenticated `founderAccountKey` for identity verification and recovery. The current arena stores only one progression/profile per account; true multi-character slots, unique persistent character IDs and live wilderness claim materialization are **still separate release gates**. Never silently substitute a transient connection/player ID or treat mere faction membership as parcel ownership.

**Founder controls the buildable set.** The default charter has no delegated builders. The founder may explicitly grant per-character permissions, with an approved **blueprint/category whitelist**, to Architect (place/upgrade/remove), Defender (configure defenses), Quartermaster (access resource production/storage) or Guest (narrow interactions). Only founder authorization may add/remove builders, edit whitelists and change charter-wide limits or hand off ownership; crew roles do not automatically override a settlement's charter. A Steward may receive a **limited, revocable** role but can never grant themselves authority, transfer the claim or exceed the owner's building budget. Permissions are distinct from faction friendship. Same-faction characters must **always remain allied for targeting** even when they cannot build; they cannot use allied status to place, dismantle, steal resources or reassign structures in a private base.

**Level binding:** settlement capacity is computed from the **current live/persisted CharacterProgression.overallLevel() of the founder** (1–50), looked up by server identity; not the highest level among crew, not the connected character's account-total, and not the installer/visitor rank. Era and actual facility research/crafting prerequisites remain separate requirements. Suggested starting capacities:

| Founder level | Normal facilities | Automated defense slots (including either AA tower) | Production queues | Expansion pylons |
|---:|---:|---:|---:|---:|
| 1–4 | 4 | 1 | 1 | 0 |
| 5–9 | 6 | 1 | 1 | 0 |
| 10–19 | 10 | 2 | 2 | 0 |
| 20–29 | 16 | 4 | 3 | 1 |
| 30–39 | 22 | 7 | 4 | 1 |
| 40–49 | 26 | 9 | 5 | 2 |
| 50 | **28** | **10** | **6** | **2** |

The Claim Heart and one monumental graveyard (after all normal requirements) are special allocations and do not occupy the normal facility pool. Automated defenses and production queues have separate counters; do not create one independent defense slot for every turret type or faction. Normal facility count covers all completed or reserved normal facilities; damage, unplugging or hiding a structure does not free capacity. **One active charter per founding character** by default. Additional personal outposts or a second core require explicit future realm rules, never a second copy of a disconnected claim.

**Owner rank decrease:** normal death does not erase permanent earned skill levels; hardcore reset can sharply shrink eligible construction capacity. Existing physical buildings and stocked inventory are **never silently deleted or confiscated**. Excess structures remain owned and visible; new placement, upgrades and territory extensions are denied until under budget, and over-cap automated defenses/queues are gracefully suspended deterministically (prefer newest-first with clear UI reason), never allowed to bypass the limits through rebuild/relog. If hardcore truly retires the character identity, freeze charter changes and use explicit successor transfer or auditable server recovery; do not assign the settlement to any fresh character merely sharing the account. Changes to owner identity require both explicit authorization and a durable audit/revision, not client-supplied IDs.

**Placement transaction order:** authenticated session → server-derived stable character ID and faction → live claim heart/footprint and founder's level → settlement ACL + approved template → versioned capacity budget → exact rotated 3D wilderness/collider/world-structure checks → durable cost reservation → atomic placement and counter update → replicated structure. Commit must be revision-checked and idempotent; a retry after timeout must not duplicate a building or debit supplies twice. Dismantle/destruction frees the right budget only after a server-confirmed persisted state transition. A low-level owner cannot defeat their limit by asking a high-level ally to perform construction. When a charter is not materialized, all permanent construction remains denied, not redirected into the current all-city sector.

**Defense specifics:** two late-game anti-air types per faction — fast accurate light-fighter tracker, and slow high-impact heavy-airframe breaker. Six distinct PSX designs; both use owner-approved defensive slots, appropriate industrial facilities, ammunition and operating energy. Their joint targeting must preserve the medium-aircraft survival window, and both honor universal same-faction alliance. See [FACTION_VEHICLES_AND_AIR_POWER.md](FACTION_VEHICLES_AND_AIR_POWER.md).

## 2. Physical building and construction workflow

At a wilderness site, a player uses a **survey stake** then a compact 3D placement ghost with dimensions, height, slope, road/water/claims overlay and a single explicit Confirm button. Construction creates a foundation **worksite object** in the persistent overlay only after a host-authorized reservation transaction; it does not conjure a completed structure. Resources must arrive in a base ledger from hand-carried deliveries or unloaded trucks, then be reserved/debited exactly once. Worksite has state PLANNED → RESERVED → FOUNDATION → FRAME → COMMISSIONING → OPERATIONAL → DAMAGED/UNDER_RAID → DISABLED/DESTROYED. A cancelled job refunds its **undelivered** resources; materials already poured into a destroyed site become salvage with bounded yields, not full duplication refunds.

All completed structures use modular foundations, doors/windows, walls and roof modules selected from approved source assets, with grounded colliders, ray/LOS blockers, floor/support points, entrance clearance, snapping grid and actual walkable access. The Kenney urban kit is appropriate for salvaged metal/walls/industrial pieces, not a license to fabricate photoreal monsters; missing Begotten/Penitent forms must be source-checked original compatible PSX assets. Structural grade and construction phase are separate from shader LOD.

**Workmanship:** building progress depends on station operation, real delivered materials and player/crew work contributions. No unconstrained instantaneous crafting. Default construction durations below are **suggested active work targets**, balanced around 1–4 people and constrained by the appropriate station/utility. Offline real-time tasks progress only when inputs, upkeep and server power/biological metabolism/ritual maintenance exist; no phantom production through crashes.

**Support integrity:** foundations bear modules, walls may be repaired/replaced, connected doors block LOS legitimately. No infinitely floating structures, recursive support updates every frame, disconnected device ownership, or grief tunnels into cities. Structural state changes are tick-budgeted and persistence-transactional.

## 3. The facility network

Facilities are a meaningful interdependent *settlement*, not a giant crafting menu. Each has a footprint in approximate real-world meters, cost and dependency. Counts and recipes below are initial economic tuning targets; the authoritative schema must track them as versioned registry data. `M` = METAL_STOCK truck load, `P` = MACHINE_PARTS truck load, `T` timber units, `S` field stone, `F` fiber, `O` raw ore, `H` medicinal herbs. **M/P are heavy truck loads, never normal backpack ingredients.** More recipes will be introduced from world loot with explicit, obtainable origins.

| ID / facility function | Tier, approx. footprint | Prerequisites | Initial delivered material target | Gameplay service |
|---|---|---|---|---|
| `claim-heart` / Wilderness Heart | T0 · 8×8 | approved survey site | T12 S12 F6 | owns base UUID, claims land, marks authorized build zone |
| `field-shelter` / Refuge | T0 · 6×8 | heart | T10 F8 S4 | weather shelter, rest; no protection for carried equipment on death |
| `hand-workshop` / Maker's Bench | T0 · 6×6 | heart | T10 S6 F4 | early recipes and tool repair |
| `dry-store` / Locked Store | T0 · 8×8 | heart | T12 S8 F6 | access-controlled **stationary** inventory, never moved with dying player |
| `rain-catcher` / Water Gatherer | T0 · 6×6 | heart | T8 F8 S6 | collects dirty water; does not magically purify it |
| `cooking-hearth` / Heat & Cookfire | T0 · 6×6 | heart | T10 S12 F3 | heat, cooked food, water boiling; fuel required |
| `forager-garden` / Field Garden | T1 · 12×12 | rain-catcher, shelter | T16 F12 S8 | planted edible crops/herbs, water/season/soil dependent |
| `purification-works` / Clean Water | T1 · 10×10 | rain-catcher, workshop | M2 P1 S14 F8 | filtered potable water; filter media/energy cost |
| `infirmary` / Field Hospital | T1 · 10×12 | purification, shelter | M2 P2 T16 H14 | heals injuries/sanity with drugs, bandages, clean water; not death inventory insurance |
| `forge` / Smelting Yard | T1 · 12×12 | workshop, hearth | M3 P1 S20 O8 | converts wild ore into smaller refined materials; heat and fuel |
| `materials-recycler` / Salvage Line | T1 · 12×10 | workshop, forge | M2 P3 T10 S10 | process urban scrap, repair salvage, limited inefficient material yield |
| `generator` / Power Source | T1 · 10×10 | workshop | M2 P3 T12 O6 | finite power generation; fuel, noise and maintenance |
| `freight-bay` / Truck Cargo Receiving | T1 · 16×18 | workshop, dry-store | M4 P3 S24 T16 | actual parked-truck unload, transfer to anchored bulk stock; never remotely teleports loads |
| `watch-post` / Lookout | T1 · 6×8 | shelter | T14 S8 M1 | threat markers/LOS alerts, not radar omniscience |
| `barracks` / Sleeping Ward | T2 · 12×16 | infirmary, dry-store | M3 P2 T24 F14 | beds, crew spawn-eligibility and logistics rest; death leaves inventory empty |
| `industrial-workshop` / Machine Hall | T2 · 14×14 | forge, generator, freight-bay | M6 P6 S28 | advanced craft, machinery and repairs |
| `motor-pool` / Vehicle Depot | T2 · 16×18 | industrial-workshop, freight-bay | M6 P5 S30 | vehicle maintenance, fuel storage, repair queue, truck configuration |
| `fuel-refinery` / Chemical Plant | T2 · 14×14 | purification, generator | M5 P6 S24 O12 | makes/refines fuel/industrial reagents with contamination risks |
| `wall-gate-system` / Defensive Works | T2 · modular | barracks, industrial workshop | M3 P2 S30 | fence/armor/gate health, enforced access without sealing global public roads |
| `signal-relay` / Long-range Relay | T2 · 8×12 | generator, watch-post | M3 P5 O8 | alerts, maps/convoy signals, limited-range intelligence |
| `cold-store` / Preservation Hall | T2 · 10×10 | generator, purification | M2 P3 T14 | slows food spoilage, requires actual power |
| `command-hall` / Crew Seat | T3 · 14×16 | barracks, signal-relay | M8 P6 S34 T24 | role permissions, base management, limited territory upgrades |
| `research-archive` / Veil Laboratory | T3 · 14×16 | industrial-workshop, infirmary | M8 P8 H20 O20 | advanced material research, faction specialization |
| `security-grid` / Defense Control | T3 · 12×14 | command-hall, generator | M6 P8 O12 | bounded powered turrets/sensors; hostile LOS and public route rules |
| `monument-graveyard` / Recovery Cemetery | T4 · **24×24 minimum** | command-hall, research-archive, infirmary, signal-relay, powered utilities | **M24 P16 S140 T90 F60 H30 O24** | **sole mechanism to recover all possessions from a previous death; detailed contract below** |

**Staged uses:** T0 survival is achievable on foot without city cargo. T1 requires the city truck/salvage loop for clean water, advanced healing and production. T2 turns a camp into a defendable industrial settlement. T3 permits powered specialization and politics. T4 graveyard is a monumental, expensive collective project requiring **40 full heavy cargo deliveries**, scarce wild resources and an operating staffed/powered base. The graveyard must not be achievable by combining several pocket kits while standing outside a city or by exploiting faction abilities that conjure resources.

### Dependencies and outputs have real physical meaning

- **Power:** maximum generated load, per-consumer draw, circuit priority and degraded state. During outage, freezers, security systems, powered machinery and graveyard recovery **stop**; ownership/storage does not disappear. Generator fuel is stocked, logged and consumable; no decorative generator that provides infinite energy.
- **Water and sanitation:** dirty collection → finite filtration/boiling → potable distribution. Infirmary and food production have explicit water throughput; dirty water isn't automatically safe.
- **Food and health:** biome-appropriate farming, disease/contamination, storage spoilage; infirmary requires gathered herbs and manufactured supplies. Hunger/hydration use server time and persist with character.
- **Bulk logistics:** truck drives from fixed urban resource yard to wilderness `freight-bay`, stops/driver exits, host checks cargo manifest and cargo-bay bounding volume, deducts the single truck load and credits secured crew storage in ONE durable transaction. Power/forklift or manual unload work time applies. Truck damage before unload can eject cargo; no fractional or duplicated load.
- **Material craft:** stone/timber/fiber may be carried by hand under weight cap. Metal/machinery require physical hauling, with intermediate recipes that consume stocked quantities and produce smaller items only through an actual facility.
- **Human/Begotten/ritual production:** mechanical equivalents share resource and throughput envelopes for fairness; a Begotten nutrient vat isn't allowed unlimited potable-water creation just because it resembles a pool.
- **Operating upkeep:** active utility/facility consumption has per-hour bounded costs, with publicly visible reserve forecasts. Disconnected crews do not become invisible/unraidable but have explicit timers and decay warnings.

## 4. How all three factions build

The shared **functional** registry above is the balance baseline. A completed faction's facility must have a **faction-specific, identifiable physical form and service animation** at 320×200. Names, rituals and distinctive sidegrades are faction-authored; the underlying minimum storage, revival, food, healing and progression remain equivalent.

| Core function | The Unraveled (improvised engineering) | The Penitent (ritual industry) | The Begotten (organic architecture) |
|---|---|---|---|
| Claim Heart | **Ledger Beacon** — radio mast, names etched in plates | **Oath Obelisk** — stone votive and candle ring | **Rootmind Node** — branching living claim core |
| Shelter | **Patchwork Refuge** — welded cabins/tents | **Vigil Cells** — cloister tents/chambers | **Warm Husk** — sealed membrane burrow |
| Storage | **Quartermaster Lockup** — metal crates and cages | **Offering Crypt** — sealed reliquary shelving | **Gullet Vault** — layered hard-shell pods |
| Heat/cooking | **Salvage Stove** | **Censer Hearth** | **Digestive Pit** |
| Clean water | **Filter Pump** — tanks and pipes | **Lustration Font** — mechanical filtration under rite | **Membrane Cistern** — biological filtration with reagents |
| Medical | **Triage Ward** | **House of Absolution** | **Regrowth Nest** |
| Forge/recycle | **Scrap Foundry** | **Ash Smeltery** | **Bone-Metal Digestor** |
| Power | **Jury-Rigged Dynamo** | **Bell-Driven Array** | **Metabolic Conduit** |
| Freight bay | **Salvage Freight Dock** | **Processional Goods Yard** | **Hauler Maw** |
| Workshop/motor pool | **Machine Shed / Service Garage** | **Consecrated Smithy / Reliquary Garage** | **Fleshforge / Vehicle Symbiote Pen** |
| Defense | **Watchtower / Hardpoint Grid** | **Bell Tower / Vigil Wards** | **Sentinel Stalks / Spore Wards** |
| Research/command | **Ledger Archive / Council Bunker** | **Scriptorium / Choir Hall** | **Memory Cyst / Brood Throne** |
| Monument graveyard | **Cemetery of Names** | **Reliquary of Last Confession** | **Ossuary of the Unmade** |

**Faction-specific sidegrades** (one service channel, mutually exclusive at matching tiers; costs and player counterplay documented before coding):
- **Unraveled:** improved generator repair predictability / convoy radio notice; noisy/visible machines and higher fuel consumption. No bonus that insures death inventory.
- **Penitent:** ritual sanitation/condition resistance and public pilgrimage rally signal; rites take deliberate service time and emit audible bells. No free souls-based immortality or instant item recoveries.
- **Begotten:** biological material recycling and night-sensitive perimeter warnings; needs water, nutrients, metabolite upkeep and visible growth vulnerabilities. Does not gain permission to build on city structures or bypass heavy vehicle cargo.

**Character class never determines base ownership.** Warden can help fortify an Unraveled camp, Votary can tend Penitent rites, Harrower can help Begotten scout, but class cannot independently conjure permanent facilities, change storage ACLs or exempt upkeep. Some class/crew combos may receive modest, capped *labor efficiency* boosts (provisional <=10%), not free items or fundamentally different corpse insurance.

## 5. Death, bodies and the monumental graveyard

The monument is the only exception to irreversible loss of **possessed physical items**, and it is a *recovery service*, not a "keep inventory on death" toggle. Full authoritative protocol is in [GRAVEYARD_DEATH_AND_ITEM_RECOVERY.md](GRAVEYARD_DEATH_AND_ITEM_RECOVERY.md). All three graveyard variants have identical recovery rules, minimum footprint, service cost, capacity and PvP vulnerability.

**Construction:** requires an isolated **24×24-meter** wilderness plot with perimeter access, full foundation/collider, memorial/morgue chamber, access doors, power/ritual/nutrient service and corpse alcoves. One operational monument per base; crew must have T3 command, research, infirmary, live signal service and sufficient basic utilities. Price is deliberately enormous: 24 METAL_STOCK, 16 MACHINE_PARTS, 140 stone, 90 timber, 60 fiber, 30 herbs, 24 ore. A build order reserves and consumes inputs by stages; breaking it does not produce a complete refund.

**Service contract:** only linked crew character deaths *while the finished graveyard was operational* generate a sealed body ledger there. Immediately on death the character has **zero carried inventory and zero equipped items**. On respawn at an eligible spawn, the player must physically travel to the graveyard, satisfy authorization, power and brief retrieval time, then claim their sealed body into inventory or a dedicated limited access morgue locker. No remote button, faction ability, teleport, retroactive construction or fee bypass. Crew member may have only **one outstanding corpse escrow**: a further death destroys the older deposit and replaces it with the newer event if eligible. If the graveyard did not qualify at the time of death, those lost possessions are permanently gone.

**Risk:** a graveyard is a valuable, raid-vulnerable facility inside declared windows. On disabling it, recoveries stop; on destruction, sealed bodies enter a defined grace for repair before final loss. All limits, timing, conflict resolution and no-duplication transactions are documented in the graveyard contract.

## 6. Explicit server data model

Persistent tables/records are authoritative, not reconstructed from client snapshots: `Crew(crewId,factionId,founderCharacterId,members/roles,policy,revision)`; `Base(baseId,crewId,zoneId,claimGeometry,anchorId,upkeep,vulnerabilityWindow,revision)`; `Facility(facilityId,baseId,registryId,variantId,transform/rotatedFootprint,foundationHeight,phase,health,utilityPorts,ACL,revision)`; `WorkOrder(jobId,facilityId,inputReservations,deliveredInputs,contributors,progress,phase,revision)`; `CargoReceipt(receiptId,sourceCarId,loadId,freightBayId,resourceType,creditedQuantity,consumed,revision)`; `StorageAccount(storageId,baseId,slots,itemInstances,permissions,revision)`; `Graveyard(graveId,facilityId,operationalEpoch,serviceCapacity,activeBodyIds,revision)`; `CorpseEscrow(deathEventId,characterId,graveId,itemInstanceSnapshot,status,expiry,revision)`; `CharacterInventory(characterId,version,equippedSlots,carriedItems,currencyAsPhysicalItems,survivalNeeds,revision)`.

Each mutable object is single-writer under its zone authority epoch; cross-zone transfers (cargo/base/character/death) use deterministic command IDs, idempotency tables and transaction journals. On crash/reconnect, replay committed transfers; never replay "take from inventory" without corresponding "credit storage" or "destroy on death". Snapshot and journal belong to the same atomic durability boundary. Zone migration must fence prior worker epochs, never resurrect inventories from stale saves.

**Build command:** `PLACE_FACILITY(characterId,baseId,facilityRegistryId,position,rotation,requestId,expectedRevision)` → validate authentication, faction/crew permission, appropriate tier, 3D world masks, future road clearance, claim budget, unique facility cap, inputs in crew stock, siege lock, foundation, collision and route access → reserve and emit durable construction worksite. **No accepted response until journal append has been durably committed.** Repeated request ID returns the original answer, not a second worksite.

**Death command:** `DIE(characterId,eventId,hostClock)` processes in one serializable transaction from latest inventory revision. The client cannot specify what was carried, which items get restored or where the body escrow should appear.

## 7. UI: compact but truthful

Building UI is a compact sortable facility list filtered by **buildable now / missing requirements**, an expandable material line with truck-load counts, footprint ghost, power/water/claim indicators, one Build/Place control and exact failure reason. Facility details show service, inputs, storage, upkeep, output, damage, labor and defense. One unified crew overview exposes power/water deficits and job queue rather than dozens of one-button tabs. Graveyard shows **unclaimed bodies, time remaining, current service availability and destination locker**; never hide whether a particular death is insured. Avoid excessively spaced faction exposition or tutorial screens covering the game viewport.

## 8. Incremental execution sequence and release gates

| Gate | Implement and test | Do not claim until |
|---|---|---|
| **B0 — registry/schema** | versioned shared facility templates, per-faction names/meshes, costs, utilities and caps; validate DAG and existing source model IDs | complete registry no phantom assets/dependency cycles |
| **B1 — real wilderness** | server-authoritative streamed terrain, 3D claim protection, walking/vehicle transit, zone entities | two players travel city → wilderness and see one matching claim site |
| **B2 — charter and claims** | persisted crew/faction ACLs, anchor and site rejection with rotated vertical footprint | all city/road/other-base placements rejected, even under race/lag |
| **B3 — starter construction** | 6 core T0 facilities with worksite stages, forestry, material deposits and raid/decay | restart mid-build and dismantle/refund without dupes |
| **B4 — utilities and logistics** | power/water/heat network, city cargo to freight-bay unloading, material receipts | two connected players haul M/P through hostile city to real base and can't mint extras |
| **B5 — production and defense** | T1–T3 facility chains, grow/forge/repair, contested PvP raiding and crew access | actual sustained survival, power-loss behavior and repeatable siege abuse tests |
| **B6 — death inventory law** | unified item ledger; ALL death modes atomically clear carry/equipment; spawn empty | no PvE/PvP/fall/crash/disconnect/class exceptions; legacy saves migrate correctly |
| **B7 — three monument graveyards** | enormous graveyard construction; single corpse escrow, timed/physical claim, siege loss and restart | real 2-client death → empty spawn → travel → body claim exactly once; 2nd-death override |
| **B8 — polish and load** | genuine PSX faction geometry/assets, graphics & facility LOD, 24/7 persistence metrics, 4–16-client soak | no fake renders, mass runaway NPCs, orphan build tiles or massive frame spikes |

Prioritize B6 death rules **before** any publicly available death-recovery service. During B6 without B7, death always destroys the carried inventory; do not quietly enable legacy item retention to mask missing graveyard support. Balance all prices/throughput after comparing observed travel time, actual combat deaths and 1–4-person crew progression.

## 9. Change management

Any modification to the graveyard's protection status, inventory destruction, cost, capacity, expiry or whether PvP can take a body is **mechanically material**: update canonical design, registry schema, migration tests, user-readable UI and changelog before merging. Do not "fix" complaints by quietly lowering costs, granting free starters, allowing city construction or permitting client-owned inventories. If balance testing reveals unrecoverable soft locks, improve safe **itemless** forage opportunities and clearly located accessible world resources rather than negating inventory loss.
