# The Malefic Veil — release-gated development plan

Each phase has **entry criteria**, concrete deliverables, automated acceptance evidence and explicit **exit gates**. The license/provenance/renderer/animation implementation gates are tracked in **[ASSET_PIPELINE_AUDIT.md](ASSET_PIPELINE_AUDIT.md)**. Release-blocking player experience requirements are specified in **[PLAYER_QOL_MUST_HAVES.md](PLAYER_QOL_MUST_HAVES.md)**; P0 issues are non-waivable for public multiplayer. A green unit test is never proof of real user playability. Work stays on `main`; GitHub Actions are authorized for build/testing on this project, not for unsupervised release of purchased archives.

## Phase P0 — Foundation / repository / licensing [BASELINE VERIFIED]

**Delivered:** Java 17 Swing shell, seeded city, PvP protocol v2, handgun and shotgun, inventories, visibility jewel with remote-player alpha, NPCs, item pickup, health/sanity and deployable gameplay. Asset catalog has 633 observed Drive references, which is not an exhaustive file count.

**Acceptance:** Windows + Linux JDK17/JDK21 build; city invariant test; headless Swing screenshot; real localhost socket combat tests and repeated host soak. Last verified baseline: [39532421213](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532421213), soak [39532421377](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532421377).

**Exit:** Foundation suitable for development — **passed for those automated checks**; human playability is NOT implied.

## Phase P1 — First playable urban asset slice [IN PROGRESS]

**Deliver:** Actual archived urban geometry (Kenney CC0 archive: source license read, hash-proven source model), deterministic in-world mesh instances, model parsing and raster projection, material handling, destructible/solid barriers, sample road, streetlight, bench and dumpster. Follow with real texture sampling, occlusion and collision alignment.

**Release gate R1:**
- Five selected GLBs byte-verified against the original Kenney Retro Urban Kit 2.0 and license evidence stored.
- Java 17 parser actually extracts triangle vertices/indices from each GLB; malformed/unsupported assets fail loudly.
- In-world projected geometry visibly changes a screenshot, not just a model-file existence check.
- Both source launch and runnable JAR load all selected assets; no dependency on private Drive at runtime.
- 7+ seeded maps, combat checks and LAN tests remain green on Windows/Linux.
- Frame time, accurate world collision, texture/material correctness, actual desktop movement and input acceptance before tagging `v0.1.0-alpha`.

**Status:** First five GLBs committed; Java 17 static mesh geometry importer and renderer now in integration testing. UV textures, collision integration and manual desktop sign-off remain open. No R1 release tag.

## Phase P2 — Fully usable PSX city [FOUNDATION IN PROGRESS]

**Deliver:** Expanded tile grammar from URBAN/Retro Urban with intersecting roads, interiors, multilevel buildings, roof access, cover, destructible props, map streaming/chunks, deterministic spawn-safe navigation. Texture atlas, low-res PSX shading, dynamic lights/fog and occlusion.

**Completed plot-foundation slice:** 22x22 seeded road/parcel topology; connected road graph with intersections and cul-de-sacs; seven footprint sizes; 104 categorized plot definitions (32 standard 2x2); safe/resource/hostile spawn placement; collision accessibility repairs; category-colored city map; Java 17 layout/world regression tests. See [PLOT_GENERATION.md](PLOT_GENERATION.md). Named templates currently share procedural geometric motifs and are **not** yet finished source-verified 3D levels.

**Release gate R2:** 20 deterministic map seeds with connected spawn routes; traversal and geometry/collision matching; no visual wall-through; 30-minute Windows desktop tour; memory/frame profiling at small, medium and large city sizes; licensed pack references/hashes and authored conversion evidence. Target `v0.2.0-alpha`.

### Continental world-generation expansion [BLUEPRINT IMPLEMENTED; GAME STREAMING PENDING]

The hand-drawn island/cities A–G layout now has a deterministic 224×224 parcel-scale `ContinentLayout` with coast/water, seven biome types, irregular city masks (square City C), intercity network, connected city arteries and categorized local plots. World-owned roads/sites have a separate protected construction mask, with undeveloped vacant lots left buildable. The `N` preview exposes macro topology beside the existing `M` city map. **The playable City A district is now embedded in the same world coordinates as the continent**, with outdoor world-movement and harvest nodes sharing player positions; the other cities still lack finished physical site geometry, road meshes, elevation, NPC instantiation and streamed 3D urban art. See [CONTINENT_WORLD_GENERATION.md](CONTINENT_WORLD_GENERATION.md).

Next gates: verified Java 17/21 topology and host-build regressions, macro-to-fine sector conversion, terrain transition artistry, uninterrupted intercity traversal and large building-footprint enforcement.

### Continuous world correction and Beckenburg harvesting port [CODE COMMITTED; RUNTIME QA OPEN]

The erroneous `WILD_ENTER/WILD_EXIT/WILD_MOVE` session model was removed entirely. City A is aligned to the actual macro continent in world-metres; ordinary `ArenaSystems.move` handles all walking through the edge crossings and validates swept movement across tile/terrain obstacles. Host combat, pickups, vehicles, crafting and resource nodes all use ONE `ArenaSystems.Player.x/y/angle` and there is no special dimension flag. `WorldHarvesting` authoritatively ray-validates `HARVEST|nodeId`, awards material to the same character's inventory, syncs `RESOURCE_NODE` to nearby players and durably saves depletion. The client preserves the Beckenburg center-aim hand/E prompt and draws CC0 Beckenburg tree GLBs plus authored resource proxy meshes.

`WorldHarvestingTest` and `InteractionFocusTest` are registered in `QualityGateTest`, **but current branch compilation and actual Windows/Linux traversal/visual tests require verification**. Gate next: continuous graphical roads/terrain blending, real physical City B–G lots/interiors, forestry node collision/animations, combat/vehicle convoys across old City A boundary and multiplayer load/carry/deplete soak. Full [contract and exact limits](ONE_CONTINUOUS_WORLD_AND_HARVESTING.md).

## Phase P3 — Character, arsenal and animations [PLANNED]

**Deliver:** Inventory and rig audit of `Characters_psx_1.2.zip`, reusable asset importer (GLB/FBX only after source conversion), character select with real approved models, idle/run/crouch/shoot/reload/death/revive animations, weapon models, animation replication and compact loadout UI.

**Release gate R3:** Each playable class has inspected source mesh and valid rig/animation path, visual screenshots, movement/ability tests, synchronized remote hitboxes, authoritative damage and no placeholder labels passed off as finished characters. Target `v0.3.0-alpha`.

## Phase P4 — Multiplayer correctness / internet hardening [DEDICATED-HOST AND VEHICLE FOUNDATION IMPLEMENTED; RELEASE GATE OPEN]

**Deliver:** Socket lifetime management, reconnect, join-in-progress, server-owned state and object IDs, snapshots, reconciliation/interpolation, anti-teleport and interaction validation, spawn selection, friendly/hostile turret team logic, inventory transactions, packet budgeting, authenticated identities and disconnect cleanup. Dedicated server configuration, game modes, admin and lobby.

**Implemented incremental slices:** Authoritative enter/drive/exit server commands, deterministic road-front car spawn and mesh rendering, vehicle state replication and network test; separate headless dedicated mode with stable LAN account keys, administrative status/kick/ban/allowlisting and audit, 60s autosave to CRC-checked world snapshots and recovery backup, reconnect-safe structure ownership IDs, player-built device PvP damage and configurable offline raid grace. Host shutdown saves, restores vehicles/NPC state/pickups/devices, and fails closed for incompatible or corrupt worlds. Generated continental NPC/player spawn sites and macro construction masks exist, but **do not yet instantiate actual continental game sectors**. See [Dedicated host runbook](DEDICATED_HOSTING.md).

**Unfinished blockers:** internet authentication/TLS, connection flood defense, authoritative full-continent movement and chunk streaming, per-character inventory/economic persistence, true multiplayer teams/base claims, collision/height-aware multibuilding validation, client raid timer indicators, staged disaster recovery testing, long-duration real dedicated server soak, balancing and operator signoff.

**Release gate R4:** Deterministic + adversarial multiplayer tests including duplicate pickup/build commands, disconnect mid-revive, 4–16 clients, 150ms latency/packet loss, 2h soak, resource and security bounds. Localhost protocol tests **do not** satisfy this gate. Target `v0.4.0-alpha`.

### Vehicles and endgame aviation [V1 discovery blueprint integrated; release gates open]

The ground/air contract is documented in **[Faction vehicles and air power](FACTION_VEHICLES_AND_AIR_POWER.md)**. The current city-sector vehicles still support host-authoritative driving, collision damage, fuel, single truck-cargo loads and persistence. New `ContinentVehicleSites` and `ContinentPopulation.vehicleSpawns()` define off-road, seed-stable city parking and highway-shoulder discovery points with stable IDs, source-car variants, initial damage/fuel and capacity/marking metadata. **They are not spawned or driven in the unstreamed continent.**

V2 requires actually converted GGBot CC0 runtime cars, multiple chassis, server materialization/tombstones and physical collision/access checks. V3 adds component repairs, multi-slot cargo and chassis manufacturing; V4 adds faction access and ally-safe vehicle combat. Aircraft are gated behind late-game industry, nine original airframes (three distinct classes per faction), actual host flight and mixed-arms counterplay; no aircraft are implemented yet.

### Character-owned settlements and dual-role faction anti-air [POLICY CODE ADDED; LIVE WORLD GATED]

The [facility authority contract](FACTION_FACILITIES_BASE_CONSTRUCTION.md) now gives **a specific persistent founder character** settlement control, rather than automatic faction/crew/global ownership. The new host-side `SettlementAuthority` is a policy-only module: founder-managed blueprint-specific builder ACLs, fixed versioned charters, one founding charter per character, dynamic level-1–50 budgets, separate defense/production/extension caps, idempotent structure IDs and cold-start snapshots. `SettlementAuthorityTest` exercises grant/deny, foreign faction, high-level helper cap bypass, revocation after ownership transfer, duplicate placements and hardcore level reset.

**Not yet shipped:** character-slot identity and offline directory backed by real saved characters; wilderness Claim Heart materialization and claim geometry; transactional inventory/material debit + placement ledger in the streamed world; zone persistence storage and replication; suspend excess defenses after hardcore rank rollback. The new policy API must not be exposed to client packets or mistaken for placed settlement structures.

The [vehicle and aviation contract](FACTION_VEHICLES_AND_AIR_POWER.md) adds exactly **two specialist AA towers per faction** (six distinct custom art contracts): precise, rapid, low-caliber light anti-air versus slow, armor-piercing heavy-breaker artillery. The intentional **medium-aircraft survival window** is represented by `AntiAirBalance`'s isolated illustrative matchup curve and `AntiAirBalanceTest`. These are balance-specification data and tests, **not** runtime turret entities, projectiles, visual assets or real multiplayer combat. Power/ammunition, line of sight, persistent damage and faction-friendly-fire constraints gate a playable AA release.

### City miasma, level-50 settlements and three incompatible industrial supply architectures [SOURCE DATA AND SPEC]

The [city miasma, crude structure, full economy and trade spec](CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md) adds:
- `CityMiasma`: deterministic city-wide aircraft exclusion columns at all altitudes, swept-crossing validation and test data; **not yet an integrated flight server or rendered purple billows**.
- `CrudeStructureIndex` and `CraftingEraRegistry` **v2**: full proxy-art index, primitive farming/survival structures and ordered processing recipes; **not instantiated farms or model replacements**.
- `EquipmentUpgrades`: single-stage level-2→50 upgrade plans for structures, vehicles and defenses with cost/time/era and upkeep escalation; **not world-entity upgrades or stored recipes**.
- `FactionTradeNetworks`: different five-stage+ Unraveled freight metallurgy, Penitent multi-performer timed rites and Begotten living maturation products, all requiring independent base contributions for final endgame lots; **not transactional or authenticated trade**.
- `SettlementSupplyBalance`: exhaustive 28-facility / ten-tower modeled inequality proving even optimistic local power and ammo plants cannot self-supply maximum defenses; **not an active power-grid simulation**.

Next gates: consistent fine world/air coordinates, real miasma particle/cloud art and air-launched projectile blocking, verified character ownership and wilderness construction, actual planted crop states and industrial machines, founder-level upgrade transactions, physical convoy/lot receipts across streamed sectors, and full production/defense two-client tests.

### Icon atlas integration: five committed economy sheets [ASSET FOUNDATION VERIFIED LOCALLY]

All **five** October 2026 placeholder icon sheets have been sliced into **150 unique 128×128 RGBA PNGs**, archived in five tracked `assets/game/crafting/packs/*_sprites_only.zip` files, with matching `*_source.webp` visual sheet references. Committed `icons_index.csv` maps each item ID to its ZIP entry and SHA-256; `icons_manifest.json` adds pack integrity and grouped labels. `CraftingIconPacks` loads images by stable item ID, verifies SHA-256 and dimensions, and provides them directly for inventory/recipe/trade UIs; `tools/extract_crafting_icon_packs.py` expands verified individual PNGs for renderers that need loose files. Local **JDK 17-targeted** headless test passed for all 150; end-to-end integrated UI renders and game release packaging are **not yet verified**. The WebP sheets are reference previews; the full-resolution originals and labeled full-card slices are in the separately retained authoring package.

Next UI slice: bind inventory, recipe selection, faction workstations and multi-base trade listings to `CraftingIconPacks.icon(id)`, with a readable separately drawn label. Next simulation slice: make new generic Unraveled/Penitent/Begotten materials physically obtainable through their real faction-specific guarded production chains, not by assuming that an indexed image grants an item.

## Phase P5 — Horror, traversal and production gameplay [PLANNED]

**Deliver:** Full health/sanity effects, darkness and visibility gem affecting actual remote alpha, stealth/detection AI, combat feedback, sound staging, classes, team objectives, extraction/holdout, health regen pacing, repair/assembly and supply/cash economy, dynamic environments.

**Release gate R5:** Reproducible dedicated-host and real desktop gameplay tests, PvP visibility fairness and legibility at every alpha threshold, balancing simulations and complete 30-minute play sessions. Target `v0.5.0-beta`.

## Phase P6 — Packaging, release and sustainment [PLANNED]

**Deliver:** Self-contained Java 17 Windows distributable with launcher and complete allowed assets, Linux portable build, content manifest/versioning, settings, saves/backup, host setup, install/uninstall path, error reporting and changelog.

**Release gate R6:** Clean-machine install, asset licenses and source hashes, test/build parity, Windows and Linux input/audio/GPU/software-render checks, human sign-off, 60-minute 2+ player session, no critical movement/host exceptions, release archive hash+readback. Tag `v1.0` only after all gates.

## No-bypass enforcement

States per actual asset: `ARCHIVED → INVENTORIED → LICENSED → CONVERTED → COMMITTED → RUNTIME_RENDERED → REGRESSION_TESTED → DESKTOP_PLAYTESTED → RELEASED`. A catalog link is not an import. A screenshot is not a movement test. An Actions JAR is not a signed installer. Record blocker, reproduce, fix at source, rerun gate. Never call a GitHub write "pushed" without remote branch readback.

## Next execution slices

1. Verify R1 loader and five GLB model rendering in GitHub Actions and exercise the JAR from a clean working directory.
2. Implement correct material PNG lookup, UV sampling, pixel-depth occlusion, prop colliders and mesh scaling; add golden/behavioral tests.
3. Add automated virtual-desktop interaction smoke with keyboard/mouse and screen capture, then require actual user Windows acceptance before R1.
4. Analyze Characters PSX archive mesh and animation inventory, implement first rigged character, then proceed through P3.
