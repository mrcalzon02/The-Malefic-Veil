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

The hand-drawn island/cities A–G layout now has a deterministic 224×224 parcel-scale `ContinentLayout` with coast/water, seven biome types, irregular city masks (square City C), intercity network, connected city arteries and categorized local plots. World-owned roads/sites have a separate protected construction mask, with undeveloped vacant lots left buildable. The `N` preview exposes macro topology beside the existing `M` city map. **The live combat world is still the existing 132×132 city sector**; continental chunk streaming, road meshes, fine elevation, adjacent city traversal and terrain-specific 3D assets are unimplemented gates. See [CONTINENT_WORLD_GENERATION.md](CONTINENT_WORLD_GENERATION.md).

Next gates: verified Java 17/21 topology and host-build regressions, macro-to-fine sector conversion, terrain transition artistry, uninterrupted intercity traversal and large building-footprint enforcement.

## Phase P3 — Character, arsenal and animations [PLANNED]

**Deliver:** Inventory and rig audit of `Characters_psx_1.2.zip`, reusable asset importer (GLB/FBX only after source conversion), character select with real approved models, idle/run/crouch/shoot/reload/death/revive animations, weapon models, animation replication and compact loadout UI.

**Release gate R3:** Each playable class has inspected source mesh and valid rig/animation path, visual screenshots, movement/ability tests, synchronized remote hitboxes, authoritative damage and no placeholder labels passed off as finished characters. Target `v0.3.0-alpha`.

## Phase P4 — Multiplayer correctness / internet hardening [DEDICATED-HOST AND VEHICLE FOUNDATION IMPLEMENTED; RELEASE GATE OPEN]

**Deliver:** Socket lifetime management, reconnect, join-in-progress, server-owned state and object IDs, snapshots, reconciliation/interpolation, anti-teleport and interaction validation, spawn selection, friendly/hostile turret team logic, inventory transactions, packet budgeting, authenticated identities and disconnect cleanup. Dedicated server configuration, game modes, admin and lobby.

**Implemented incremental slices:** Authoritative enter/drive/exit server commands, deterministic road-front car spawn and mesh rendering, vehicle state replication and network test; separate headless dedicated mode with stable LAN account keys, administrative status/kick/ban/allowlisting and audit, 60s autosave to CRC-checked world snapshots and recovery backup, reconnect-safe structure ownership IDs, player-built device PvP damage and configurable offline raid grace. Host shutdown saves, restores vehicles/NPC state/pickups/devices, and fails closed for incompatible or corrupt worlds. Generated continental NPC/player spawn sites and macro construction masks exist, but **do not yet instantiate actual continental game sectors**. See [Dedicated host runbook](DEDICATED_HOSTING.md).

**Unfinished blockers:** internet authentication/TLS, connection flood defense, authoritative full-continent movement and chunk streaming, per-character inventory/economic persistence, true multiplayer teams/base claims, collision/height-aware multibuilding validation, client raid timer indicators, staged disaster recovery testing, long-duration real dedicated server soak, balancing and operator signoff.

**Release gate R4:** Deterministic + adversarial multiplayer tests including duplicate pickup/build commands, disconnect mid-revive, 4–16 clients, 150ms latency/packet loss, 2h soak, resource and security bounds. Localhost protocol tests **do not** satisfy this gate. Target `v0.4.0-alpha`.

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
