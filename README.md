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

## Play concept

- PSX-resolution software renderer in a Swing window; compact keyboard/mouse interface
- Seed-driven modular city plan, streets, building lots, alleys, props, traversal lanes
- Spawn selection, handgun and shotgun, ammo and equipment collection
- Independent health and sanity; sanity hazards, darkness/visibility and Thief-inspired visibility gem
- Characters supply distinct abilities, not merely cosmetic skins
- Arena economy: cash and supplies fund defenses, turrets, healing and revival stations
- Roaming neutral/hostile NPCs and expandable LAN deathmatch

Read the [asset implementation audit and release gates](docs/ASSET_PIPELINE_AUDIT.md) for source rights, importer capabilities and rigged-character blockers.

## Asset policy

Google Drive remains the archival source of truth. See [assets/SOURCES.md](assets/SOURCES.md) and [assets/manifest.csv](assets/manifest.csv) for VERIFIED FOLDER/FILE LOCATORS. Archive presence is NOT game integration, a rigged character or a runtime-tested scene. Never commit complete purchased packs to this public repository. Ship only curated permitted derived game runtime assets after license and functionality checks.

## Scope / priorities

[Design contract](docs/DESIGN.md) · [Development and QA gates](docs/ROADMAP.md) · **[Player QoL must-haves and external shooter research](docs/PLAYER_QOL_MUST_HAVES.md)**

Project-authorized GitHub Actions compile and test on Windows/Linux; other repositories retain their separate workflow policies. A remote commit is only claimed after acceptance and read-back. Existing shared Beckenburg library and its original archives must not be deleted or reorganized.
