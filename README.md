# The Malefic Veil

A Java 17, PSX-inspired urban horror arena shooter: fast Doom/Unreal Tournament-style combat overlaid with health, sanity, stealth, resources, equipment, character abilities and deployable infrastructure.

**Current build:** Java 17 Swing prototype with the **entire licensed 124-GLB / 22-texture Kenney Retro Urban CC0 source kit**, 42 selected procedural city model types, and **18 original low-poly gameplay models** covering classes, hostiles, both weapons, seven pickups and four deployables. Models use original mesh triangles and per-triangle source PNG color sampling; full UV/pixel rasterization, skinned characters and animation are separate gated phases. No purchased PSX Mega Pack originals are published.

## Quick start

JDK 17+ (no external dependencies; GitHub Actions compile, run combat tests and package the source with licensed models):

```sh
mkdir -p out
javac --release 17 -d out src/main/java/veil/*.java
java -cp out veil.MaleficVeil
```

Run `java -cp out veil.WorldSelfTest` for headless city checks and `java -cp out veil.LanSmokeTest` for localhost network checks. A local experimental server can also be run with `java -cp out veil.LanServer 7777`; the launcher offers SOLO, HOST LAN or JOIN LAN. Protocol v3 hosts PvP damage, visibility, NPCs, inventory, pickups and construction. Internet security, robust movement reconciliation and real gameplay loads are **not** accepted yet. For Windows double-click `play.bat` with JDK 17+ installed.

## Continental population, vehicles and dedicated host (development slices)

A deterministic continental **macro blueprint** now includes seven connected city sites, biome/wilderness areas, categorized plots, 3 player-spawn candidates per city, extra wilderness rally points, NPC encounter sites, and multicell/rotated parcel-level construction denial over world-owned features. **The actual playable game remains the original 132×132 city sector**: continental chunk rendering, collision, movement, NPC actors, and seamless city-to-city driving are not ready yet.

Cars are **drivable in the playable city sector** in solo and LAN: press `Q` at a car to enter/exit; use `W/S` to accelerate/reverse and `A/D` to steer. Host authority controls occupancy, car collision, fuel, and car pose. Cars currently use licensed truck GLB geometry; tuning, refueling, passengers, repairs and intercity streaming are subsequent gates.

The headless **persistent dedicated-host development mode** adds verified world snapshots with a last-good backup, autosaves, console moderation (players/status/save/kick/ban/allowlist), stable hashed account-based build ownership, and offline base protection (default 30 minutes **of vulnerability after logout**, then shielded while offline). A local LAN connection is not safe for exposed internet hosting; the authentication token traverses plaintext sockets and encrypted transport is a release blocker. Server recovery of character inventory, clan claims, world-streaming persistence and long-load Internet scalability remain unimplemented.

```sh
javac --release 17 -encoding UTF-8 -d out src/main/java/veil/*.java
java -Dveil.bind=127.0.0.1 -Dveil.offlineGraceMinutes=30 \
  -cp out veil.LanServer 7777 20261020 server-data
```

The dedicated server binds loopback by default; launch on trusted networks only during development. Full operator procedures, limitations and an example Linux service are in **[Dedicated hosting and security gates](docs/DEDICATED_HOSTING.md)**. Run `java -cp out veil.QualityGateTest` to exercise combat, drivable car physics, saves/backups, operator actions and deterministic continental spawn candidates.

## Play concept

- PSX-resolution software renderer in a Swing window; compact keyboard/mouse interface
- Seed-driven modular city plan, streets, building lots, alleys, props, traversal lanes
- Spawn selection, handgun and shotgun, ammo and equipment collection
- Independent health and sanity; sanity hazards, darkness/visibility and Thief-inspired visibility gem
- Characters supply distinct abilities, not merely cosmetic skins
- Arena economy: cash and supplies fund defenses, turrets, healing and revival stations
- Roaming neutral/hostile NPCs and expandable LAN deathmatch

Read the [asset implementation audit and release gates](docs/ASSET_PIPELINE_AUDIT.md) for source rights, importer capabilities and rigged-character blockers.

## Arsenal design and primitive review

The [32-weapon proposal](design/ARSENAL.md) now has **32 original primitive blockouts**, separate receiver/mechanism/ammo groups, movement pivots and muzzle/hand sockets. Open [the Blender master](assets/game/arsenal-blockouts/arsenal-blockouts.blend) or review the [four contact sheets](docs/ARSENAL_STATUS.md). These are silhouettes for proportion and attachment review, not finished kitbash weapons or additional playable combat behaviors. The current game still uses its original handgun and shotgun rules.

The [component replacement plan](design/COMPONENT_REPLACEMENT.md) maps functional blocks to the local supplied mech parts. Detailed source prototypes remain local and retain their full geometry. See [power-ups and inventory proposals](design/PICKUPS.md) for the later health/armor/ammo/resource and timed-buff work; food/water mechanics remain deferred. Validate the committed blockout exports with `java -cp build/classes veil.ArsenalBlockoutTest` after compilation.

## Asset policy

Google Drive remains the archival source of truth. See [assets/SOURCES.md](assets/SOURCES.md) and [assets/manifest.csv](assets/manifest.csv) for VERIFIED FOLDER/FILE LOCATORS. Archive presence is NOT game integration, a rigged character or a runtime-tested scene. Never commit complete purchased packs to this public repository. Ship only curated permitted derived game runtime assets after license and functionality checks.

## Scope / priorities

[Design contract](docs/DESIGN.md) · [Development and QA gates](docs/ROADMAP.md) · **[Player QoL must-haves and external shooter research](docs/PLAYER_QOL_MUST_HAVES.md)**

Project-authorized GitHub Actions compile and test on Windows/Linux; other repositories retain their separate workflow policies. A remote commit is only claimed after acceptance and read-back. Existing shared Beckenburg library and its original archives must not be deleted or reorganized.
