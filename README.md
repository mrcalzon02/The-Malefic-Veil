# The Malefic Veil

A Java 17, PSX-inspired urban horror arena shooter: fast Doom/Unreal Tournament-style combat overlaid with health, sanity, stealth, resources, equipment, character abilities and deployable infrastructure.

**Current build:** Java 17 Swing prototype with the licensed Kenney urban kit, **32 working primitive weapons** with shared textures and rigid animation, and 19 authored legacy gameplay models including the fabrication bench. The first eight weapons spawn as map loot; the other 24 require base fabrication, materials, faction recipes and bench upgrades. Both solo and LAN use host-owned combat/inventory rules. Full UV/pixel rasterization and skinned characters remain separate phases. No purchased source packs are published.

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

The headless **persistent dedicated-host development mode** adds verified world snapshots with a last-good backup, autosaves, console moderation, stable account-based build ownership, and offline base protection. Snapshot v2 also saves arsenal ownership, ammunition, crafting materials, faction research, bench tiers and collected arsenal loot, and reads older v1 world saves. Character-stat recovery, clans, world streaming and exposed-Internet hardening remain incomplete.

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

## Working arsenal and fabrication

The [32-weapon roster](design/ARSENAL.md) uses the approved primitive models as the current working art. [Implementation and controls](docs/ARSENAL_IMPLEMENTATION.md) describe the prototype behavior and progression. The working [Blender master](assets/game/arsenal/working-arsenal.blend) and 32 textured animated GLBs are in `assets/game/arsenal/`; matching component meshes drive the Java first-person animations. The [original blockout review](docs/ARSENAL_STATUS.md) remains as design history.

Hold left mouse / Space for primary fire, right mouse for alternate fire; `R` reloads, `1–8` selects basic weapons, and `[` / `]` cycles owned guns. `K` opens inventory/fabrication; `J` places a bench for $150 and 12 scrap. Select a faction in the fabrication menu; specialist crafting locks that choice. Stand within 2.6 m of your own bench to fabricate or upgrade it. The menu lists component costs, prerequisites and bench tiers. Advanced weapons never appear in random weapon loot.

Detailed kitbash replacement and [power-up art/mechanics](design/PICKUPS.md) are deferred. Run `ArsenalSystemsTest`, `ArsenalAssetTest` and `ArsenalNetworkTest` after compiling for the functional weapon checks.

## Asset policy

Google Drive remains the archival source of truth. See [assets/SOURCES.md](assets/SOURCES.md) and [assets/manifest.csv](assets/manifest.csv) for VERIFIED FOLDER/FILE LOCATORS. Archive presence is NOT game integration, a rigged character or a runtime-tested scene. Never commit complete purchased packs to this public repository. Ship only curated permitted derived game runtime assets after license and functionality checks.

## Scope / priorities

[Design contract](docs/DESIGN.md) · [Development and QA gates](docs/ROADMAP.md) · **[Player QoL must-haves and external shooter research](docs/PLAYER_QOL_MUST_HAVES.md)**

Project-authorized GitHub Actions compile and test on Windows/Linux; other repositories retain their separate workflow policies. A remote commit is only claimed after acceptance and read-back. Existing shared Beckenburg library and its original archives must not be deleted or reorganized.
