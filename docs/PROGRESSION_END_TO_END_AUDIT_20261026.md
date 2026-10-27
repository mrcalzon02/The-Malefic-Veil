# The Malefic Veil — end-to-end gameplay progression audit
**Assessment date:** 2026-10-26  
**Code baseline audited:** `main` at `2705b3bd620dff90f2d177120a4b3516686b266b`  
**Scope:** Read-only implementation/CI evidence reviewed before this ledger and first corrective patch. No claim of a physical Windows start-to-finish playtest.

## Working definition of “playable to max level”
One newly created character on a genuinely persisted server must be able to progress through: world creation → safe arrival → scavenging/harvesting → physical survival → a temporary camp → first owned wilderness claim → truck-based city salvage → first working faction industry → advanced construction/upgrades → multiple allied settlements and trade → a level-50 character and functioning advanced base. **Every step must use real host-validated inputs, physical world objects, usable outputs, persistence/restart, correct faction access and player-visible failure feedback**. Tests of one model or registry never establish this complete path.

**Current conclusion:** The game has substantial host-authoritative combat, vehicles, survival/crafting foundations, three faction identities, 32 player weapon profiles and 21 separate throwable kits. The fully joined progression into permanent settlement, productive industry, advanced upgrades, inter-settlement logistics, aircraft and level-50 fortress operation is **not yet playable end to end**. The latest baseline full game verification, GPU and desktop release were blocked by an export-count mismatch after the 21 throw recipes were indexed. See [workflow](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/40132895709) and [release workflow](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/40132895708). Re-evaluate these results after any fix; do not assume they became green merely because code was committed.

## Incremental implementation log

- **2026-10-26 / catalog blocker (P0):** Export entrypoint now checks the 22 survival recipes **plus 21 independent thrown-field recipes**, and the regression checks every throw row. [Full game verification](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/40403051799), GPU build, input and multiplayer soak completed successfully for commit `98c69b0`. Packaging runs have their own conclusions and must be checked separately.
- **2026-10-26 / inventory integrity (P1, validation pending):** In the next gameplay correction, fatal damage clears the separate carried arsenal ledger and invalidates actions against an unowned weapon, including post-death pickup of loose ammunition. A functional normal/hardcore death → respawn → save/load → rearm test is added to release/game verification. **This does not reconcile starting loadouts, the unified item mass model or graveyard recovery.** Verify remote CI before promoting this slice to accepted.

- **2026-10-26 / survival supply actions (P2 initial slice; validation pending):** Handcrafted rations and smoked rations can restore hunger through the host, clean water restores hydration, and an actual `CONSUME` command sends an authoritative result and updated inventory/RPG status. The native OpenGL pause menu exposes food/water actions. This is **not yet** the full dehydration/starvation damage or shelter/environment system; survival consequences remain a separate release gate.

## Evidence labels
- **Runtime foundation:** executable host/client code or specific functional checks exist, but no full player-journey sign-off is implied.
- **Partial:** some game transactions work; essential UI, economic transfer, persistence, world geometry, or balance remains disconnected.
- **Registry/design:** a specification, independent policy class, catalog, blueprint, or isolated test exists; does not constitute usable in-world gameplay.
- **Verified gate:** new functional test reaches the entire named journey, with readback and CI result pinned to its actual commit.

## Phase-by-phase audit

| Phase | Actual source-backed status | Missing journey dependency / completion gate |
| --- | --- | --- |
| Level 1 arrival / world creation | **Partial.** `WorldCreationConfig`, `GpuFrontEnd` and `ArenaSystems.Player` support world settings, faction selection and a host-connected start. Fresh players still begin with 150 cash, four scrap, two medkits and legacy ammo; `ArsenalSystems.State` also begins owning a loaded Vesper. | Choose and enforce one real stranded-survival starting inventory policy; verify every faction can enter as an intended class, move safely, interact and save/rejoin. World enemy-density and loot selectors are presently reserved settings, not functional modifiers. |
| Primitive scavenging, needs and camp (E0–E1) | **Partial.** `WorldHarvesting`, `SurvivalCrafting`, `CityLootEconomy` and `ArenaSystems` support harvest, finite mass, hand recipes, camp placement portions, hunger and hydration meters. | Implement host-authoritative hunger/thirst consequences, recovery, usable crafted rations, portable object functions, and no-soft-lock recovery from zero items. Verify directly through client/host, not only crafting class tests. |
| Early PvE and PvP | **Runtime foundation.** `ArsenalCatalog` and `ArsenalSystems` have 8 map-loot and 24 faction-gated weapons, fired through host combat; `LostSpawnSites` runs host enemies. | Verify progression of actual encounter difficulty, player hit/replication, durability of ownership and inventory loss. `LostArsenal` describes 25 proposed NPC attacks; most are **not** live attacks. `FactionClasses` has nine entries, but only four implemented ability IDs, and no currently selectable Begotten ability ID. |
| Vehicle salvage → settlement (E1–E2) | **Partial.** `Vehicles`, `CargoHaulage`, `LanServer` provide driving, bulk city pickup, ejection and recovery. | A server-owned wilderness cargo laydown/offload into base stock is missing; no debug minting or backpack shortcut. Test acquisition of both `METAL_STOCK` and `MACHINE_PARTS`, delivery, stolen/destroyed cargo, restart, and then legal expenditure. |
| Permanent base / first industry (E2–E3) | **Registry/design plus partial camp.** `SettlementAuthority`, `CraftingEraRegistry`, `CrudeStructureIndex`, `IndustrialWorkstation` contain policy, facilities and isolated production mechanics. | Actual claim identity and legal placement, constructed production objects, inputs/outputs, repair/power/water/storage, time, server ledger and durable state must operate together. The early temporary camp is **not** a claim. |
| Electricity / complex manufacturing (E4–E5) | **Primarily registry/design.** 62 registered industrial processes and 69 facilities; `FactionTradeNetworks` holds planning descriptors. | Real utility topology, queues, resource logistics, authenticated lots, maintenance, automation, usable faction output, settlement-to-settlement freight and live upgrades. |
| Level 50 / faction endgame (E6) | **Design and isolated systems.** `CharacterProgression` caps skills at 50; `EquipmentUpgrades` computes proposed one-level plans through 50; `SettlementSupplyBalance` and `SettlementSustenance` model dependencies. Nine aircraft meshes are static showcase assets. | Actual upgrade payments/jobs/benefits, level-50 economy, regional trade/raids, AI industrial operation, late-game aircraft flight, faction incursions, graveyard recovery and sustainable specialization. Confirm full world/client/server/restart path. |

## Critical cross-system contradictions to resolve

1. **Duplicate starting economies.** `ArenaSystems.Player` and `ArsenalSystems.State` grant overlapping starting gear/resources. The stripped-down E0 design is not yet reconciled with the shooter-era loadout. **Do not silently choose a controversial starter loadout**: define the starting weapon policy and normal/hardcore semantics, then make one authoritative inventory.
2. **Death inventory integrity.** `ArenaSystems.damage` clears cash, scrap, medkits, legacy clips/reserves and `SurvivalCrafting` contents; separate `ArsenalSystems.State` ownership/clip/materials must be examined in an actual death → respawn → reconnect test to ensure the specified total loss. Graveyard escrow is DESIGN ONLY; never recover items without a commissioned monument.
3. **Survival consequences.** `SurvivalCrafting.advance` drains meters but currently does not itself impose physiological loss. New penalties must be evaluated against passive host health regeneration and respawn balance.
4. **Industrial deadlock.** `CargoHaulage` supports truck loading/recovery but not permanent offload at a functional wilderness base. Recipes that require bulk metal/parts must not be declared fully obtainable before a legitimate transfer path exists.
5. **Faction choice/playability.** `FactionGroups` enforces permanent same-faction alliances; `FactionClasses` only maps four of nine abilities to playable class IDs. Host NPC roster and player character capability are different things.
6. **Progression balance.** `CharacterProgression.levelThreshold` uses `400(n^3)+120(n^2)` with `n=level-1`. Level 50 requires 47,347,720 XP in a skill. Current passive Survival award is base 20 XP / five minutes (50 XP / five minutes after the 2.5× alive-streak cap), which would require about 78,913 hours to reach that skill cap if this were the only source. That is not playable pacing. Preserve slow earned progression while adding real milestones and testing target hours.
7. **Rendered world versus fully playable world.** A single world coordinate system and authored City A operate; Cities B–G remain significantly less complete. GPU menu/gameplay integration has progressed beyond README's original walkthrough description, but current CI/release gates have not been accepted.
8. **Separate material ledgers.** The 32-weapon arsenal fabrication inventory is distinct from the broad `SurvivalCrafting` ledger and the industry registry. Reconcile deliberately, without manufacturing shortcuts and without merging faction economies.
9. **Developer-facing catalog must follow real registration.** `CraftingRecipeIndex` now emits base hand recipes plus 21 independent `throw:` FIELD recipes, 62 industrial plans and 69 permanent facilities. Export completeness tests must count both field families separately and confirm 21 throw rows; `ThrownDeployables` is never a permanent `World.Device` facility.

## Non-negotiable system boundaries

- Unraveled machinery/fuel/freight/electronics; Penitent ritual/relic/parallel rites; Begotten organic/maturation/perishability remain **three independently operated manufacturing civilizations**. Shared primitive wilderness inputs do not authorize a shared factory backend. See [industrial progression](CRAFTING_INDUSTRY_INCURSION_PROGRESSION.md), [separate facilities](FACTION_FACILITIES_BASE_CONSTRUCTION.md), [trade](CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md).
- [Thrown field equipment](THROWN_FIELD_DEPLOYABLES.md): 21 distinct faction variants, craft a portable item → throw → fly → land → last up to one hour → lose HP/expire harmlessly. Do **not** convert them into claim construction or normal permanent structures.
- Generated cities/world roads are not valid permanent player building plots. Cargo transport must be physical; no item minting when the player crosses a map boundary.
- Same-faction individuals/crews are always allied. The Lost are hostile NPC population, not a fourth player faction.
- Normal death loses carried physical items while retaining permanent skill progress; hardcore also resets earned skills. Graveyard recovery cannot be enabled as a fake free death chest.
- Release-blocking diagnostics must test whether real gameplay works; do not install unrequested cosmetic gates, and do not confuse render snapshots with actual end-to-end mechanics.

## Implementation update — 26 October, ongoing

Fixed-world camp crates now have an original low-poly model, require a physically crafted crate kit and wilderness camp proximity, carry a capped 128-unit ledger with 32 stack types, can receive actual truck freight, and expose host-authorized deposit/withdrawal plus native in-game controls. Realm snapshot format V4 adds validated crate stock; older realm versions remain readable. This is an **E1 field storage milestone**, **not** a claim deed, faction factory, or completed utility grid. The existing two-file realm/account save sequence is still not crash-atomic across distinct file replacements; a cross-domain journal remains essential before unrestricted player trade.

## First functional faction workshops — 26 October 2026

The first three **permanent**, faction-locked structures are no longer catalog-only: Unraveled scrap outposts, Penitent votive altars and Begotten brood nests each consume their registered construction cost from an owned nearby camp crate. Their authored geometry and world identities persist.

Each site now starts its own real E1 work order by reserving verified inputs from the same camp-storage economy. Unraveled works unattended after fuel payment; Penitent requires physically present allied ritual witnesses and receives participant speed bonuses; Begotten gestates independently but loses maturity progress when harmed. Completed products are respectively `SCRAP_METAL`, `RITUAL_CLOTH` and `NUTRIENT_SLURRY`; outputs must fit in authentic nearby camp storage or remain pending. Active production and completion counters are in realm V5, never client-authored.

The paired host-ledger checkpoint commits `realm.dat` with `accounts.dat` under a recoverable pointer, retains the preceding verified generation, and must not silently mix newer backpack contents with older crate stock. Its scope does not yet include every source of harvest or AI state; do not describe it as a global all-file transaction journal. E1 worksite registration and production do **not** constitute full E2–E6 manufacturing, separate factory facility networks, power grids, advanced skill perks or a finished faction class roster.

## E2 physical support and second-era faction branches — 26 October 2026

The second-tier Unraveled machinist bay, Penitent reliquary, and Begotten spore basin are now implemented as distinct permanent mesh-backed world devices. Unlike legacy E1 placement, **new E2 support and faction facilities check every registered real, living, owner-controlled prerequisite in the local settlement**. Fifteen upstream shared/basic facilities are modeled, including claim heart, hand workshop, dry store, hearth, rain catcher, garden, compost, herb plot, charcoal kiln, stone furnace, forge, freight bay, cargo laydown and material recycler. Their definitions and material costs come from the canonical registry, not invented substitutes.

Host operations now accept a selected registered process instead of always choosing a single default. Shared live work orders include charcoal processing, ore sorting, cooking, crops and forge preproducts; the three faction E2 processes manufacture distinct `BEARING_SET`, `RELIQUARY_PLATE`, and `SPORE_POD`. Penitent ceremonies require witnesses at either altar or reliquary; Begotten gestation at either nest or spore basin is vulnerable to damage. V6 snapshots persist active `processId` alongside remaining time, while earlier V1–V5 records remain parsable. Native GPU in-world menus page through only world-buildable facility types and physically runnable registered recipes, with host-side ownership, proximity, stock and faction checks.

**Scope boundaries:** This does not implement complete machinery/power networks for electric or automation eras, nor does it make registry-only facilities usable automatically. Some common physical building meshes are authored interim low-poly infrastructure rather than final environment art. The E1 first factories retain migration-compatible placement behavior; imposing all newer historical E1 prerequisites requires a dedicated save migration and must not silently delete existing bases. The E2 functional test supplies externally harvested/raw resources via a trusted test fixture while verifying actual host construction and the real E1-to-E2 crafting transactions.

## E3 true steam utility and faction manufacture — October 26 2026

Permanent steam boilers, mechanical-shaft distributions, metal presses, and three additional faction-only production buildings now exist as actual world meshes and registry-priced blueprints. The Unraveled pressure shop manufactures `PRESSURE_TANK`; the Penitent cantor dais performs participant-required rituals for `BRONZE_CENSER`; the Begotten chitin vat cultures `CHITIN_SHARDS` and suffers maturity setbacks when attacked. Manufacturing remains three distinct civilizations, not a uniform recipe-factory abstraction or field deployable clone.

Steam machines with `utilityDemand` now require a living same-owner mechanical shaft connected to a real charged boiler, with per-tick boiler capacity capped at eight service units. An authenticated owner must manually fire the boiler with actual `FIREWOOD` and `DIRTY_WATER` from a nearby persistent storage crate. A charged boiler has at most thirty minutes of operating reserve, depleted only when work draws steam. Destroying shafts or boilers immediately interrupts powered work without refunding inputs. Realm format V7 adds bounded persisted boiler reserves, retaining read support for earlier versions. Host requests and the in-game industry menu expose manual fuel loading. No infinite production from nominal facilities, no magic utility from a registry line.

**Scope remaining:** Shared steam utility does not unify independent faction economies. It is not yet a graphical pipe-placement utility network, nor does it cover E4 electricity, E5 industrial automation or E6 incursion. Work-order scheduling remains single-batch per physical machine; full construction build timers and repair/fuel diagnostics are later work.

## E4 electrical civilization milestone — 27 October 2026

Generator, electric-bus, precision-lathe, water-works, and the Unraveled sensor lab, Penitent choir relay, and Begotten membrane lab now exist as seven independent mesh-backed permanent structures. New-world placement pays the canonical facility materials from a genuine nearby owned camp crate and enforces each physical prerequisite. Old save device kinds and item ordinals remain stable.

The host can now charge a real generator using a same-owner live steam boiler with at least one minute of paid pressure, plus three charcoal and two clean water from nearby persistent storage. An electric bus physically routes service within eight world units to the supplied generator and nearby machines; its per-tick allocation is capped at sixteen shared load units. **E4 electrical recipes explicitly use the electric grid rather than mistakenly consuming E3 shaft service.** Lost bus or generator connectivity stalls work without refunding reserved recipe materials. Generator charge and exact running recipes persist in realm V8, with V1–V7 backwards load compatibility.

E4 factory behavior remains three separate systems: Unraveled produces mechanical SENSOR_HEAD at an actual sensor lab, Penitent CHOIR_SCORE at a witness-dependent ritual relay, and Begotten LIVING_MEMBRANE at its vulnerable organic membrane lab. The shared precision lathe makes CONDUCTOR_WIRE and CONTROL_BOARD through its own registered recipes; it does not craft faction-specific products. Existing thrown field equipment remains a completely separate transient construction category.

**Still not delivered:** freely placeable cables and generator assemblies, full electrical failure effects/audio, tier-E5 automated factory complexes, automated hauling or timed multi-stage construction. E4 production is one real paid work order per machine, not yet a power-grid visual editor.

## E5 powered material-routing and distinct automation facilities — 27 October 2026

Permanent E5 construction now includes conveyor lines, automatic sorters, industrial workshops, assembly lines, command halls, research archives, signal relays, security grids, fuel refineries, Penitent terror collectors, Begotten nutrient digestors, and a distinct final E5 manufacturing site for each faction. All fourteen have original low-poly 3D world geometry and actual dependency/cost-based build eligibility. Faction E5 work orders remain in the registered separate economies: Unraveled builds `RELAY_BOX`, Penitent manufactures `CONSECRATED_SHELL` with witnesses and terror-derived materials, Begotten grows `ORGAN_CORE` from membranes and nutrient stock. One physically installed machine handles one paid batch. E5 jobs draw electricity, not the prior E3 shaft utility.

There is now real warehouse-to-warehouse E5 automation: at an owned living conveyor the player selects one stocked resource, and an electrically powered physical west-to-east route carries **one unit per five simulated seconds** from a nearby west-side owned crate to an east-side owned crate. This is an actual resource debit and credit within the same host world snapshot; a route cannot mint items, access another player's inventory, traverse remote sectors, overload a target crate, or work without a functioning bus and charged generator. Conveyor identity, material choice and partial timing persist in realm V9 and are covered by restart tests. Crates remain permanent; **the separate one-hour throwable equipment line remains untouched**.

Boundaries: E5 is an initial fixed-site automatic logistics implementation, not yet a freeform drawable conveyor spline or AI truck-routing scheduler. Existing catalog-only E5 recipe outputs that need other unimplemented factories are not misrepresented as functioning. Steam and electrical utility services are shared, but the three faction-owned manufacturing civilizations are not unified.

## Host-controlled E5 next-job scheduling — October 2026

Owner-authorized permanent AUTOMATION-era facilities can retain exactly one
`queuedRecipeId` while another batch is running or while awaiting real
materials. Queueing neither spends stock nor grants output. The authoritative
host advances to the next batch only after the physical machine is idle,
a same-owner electrical grid is live, and a nearby same-owner crate contains
the complete canonical recipe input. All ingredients are debited atomically
*at work-order launch*, not at queue time, and the active recipe always runs
through existing timed output, capacity, faction, and damage rules.

The native industry UI offers separate next-job selection and cancellation;
LAN hosts validate the same account ownership, exact station and E5 recipe.
A saved V10 realm preserves the pending recipe separately from the paid active
job, validates faction/station/era mapping, and retains V1–V9 loading.
Destroyed equipment clears pending receipts. This is deliberately one
pending order per machine, not a forged inventory escrow or unrestricted
remote crafting. Fully configurable priority lists, multi-station batch
queues, source-to-destination conveyor graphs and automatic generator fueling
remain later work.

## Ordered development backlog and measurable acceptance

| Priority | Work item | Acceptance proving actual gameplay |
| --- | --- | --- |
| **P0** | Repair catalog exporter/CI after 21 throwable recipes were indexed | Source-derived expected count = base recipes + independent throw kits; all unique `throw:` rows exported; compile and full workflows run; inspect status for the pushed SHA. |
| **P1** | Settle level-one equipment policy and single inventory/death transaction | From new world, correct one-time starter grants; all weapons, ammo, materials and money respect capacity/loss rules. Death/respawn/restart cannot preserve unintended carried equipment or create free items. |
| **P2** | Complete basic survival consequences and real food use | Test real time-driven need loss, status effects, recovery from obtainable food/water, no duplicate consume, death outcome and UI feedback. |
| **P3** | Deliver a first legal cargo load into a functional base stockpile | On-foot yard loading → actual truck transport → authorized legal wilderness unloading → durable receipt/stock → a consuming recipe. Verify stolen/missing/duplicate delivery and restart. |
| **P4** | Found first persistent wilderness base and working independent factory | Claim/placement → actual facility mesh/collision → inputs → power/water/work timer → usable output; verify the chain independently once for each faction. |
| **P5** | Close continent and player class/AI content gaps | Reach every city by continuous traversal, join each playable faction with real abilities, exercise appropriate Lost enemy encounter variety. |
| **P6** | Usable E3–E5 production, upgrading, repairs, defense and physical trade | Run power/machine queues and real faction logistics; preserve supply ledgers and correct same-faction relations across host restart. |
| **P7** | Tune level 1–50 and build real E6 endgame | Measure active-play XP time-to-level; operate a level-50 fortified settlement with external supply; verify PvE/PvP counters, costs, faction assets, upkeep and aircraft when live. |

### Whole-game functional checkpoints (required in this order)

1. **First hour:** New save → correct character and safe spawn → gather and consume supplies → make/place camp → save/reload.
2. **First expedition:** Leave camp → engage/evade Lost → collect weapon and resources → return alive; repeat after death to verify true loss.
3. **First bulk delivery:** Find usable truck → take physical cargo → drive to wilderness → unload to authorized stock → spend it → host restart preserves debits.
4. **First production:** Construct a real facility and produce one useful faction-specific item, with authentic ingredients, labor/utility and UI.
5. **First defended stronghold:** Claim, build, power, defend, repair and resupply permanent structures, including a hostile attack.
6. **Faction divergence:** One complete independent Unraveled factory, Penitent ritual and Begotten biological growth journey, each with same-faction cooperative access.
7. **Level-50 fortress:** Attain levels through measured play, build/upgrade specialized assets, use inter-settlement trade and survive a credible endgame threat.

**Verification discipline:** inspect actual run logs and acceptance captures; never claim CI success from source inspection, commit creation, or a different SHA. Once a write is made, verify the remote branch HEAD and read back changed files. Retry only safe/idempotent operations; never force-push. Do not disable scheduled work when a write fails.
