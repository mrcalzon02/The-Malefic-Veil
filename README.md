# The Malefic Veil

A Java 17, PSX-inspired urban horror arena shooter: fast Doom/Unreal Tournament-style combat overlaid with health, sanity, stealth, resources, equipment, character abilities and deployable infrastructure.

**Phase 0/1:** Java Swing software-rendered prototype and an authoritative inventory of upstream Google Drive assets. No proprietary archived asset is bundled automatically.

## Quick start

JDK 17+ (no dependencies, no GitHub Actions):

```sh
mkdir -p out
javac --release 17 -d out src/main/java/veil/*.java
java -cp out veil.MaleficVeil
```

Run `java -cp out veil.WorldSelfTest` for headless city checks and `java -cp out veil.LanSmokeTest` for localhost network checks. A local experimental server can also be run with `java -cp out veil.LanServer 7777`; the launcher offers SOLO, HOST LAN or JOIN LAN. LAN currently relays player positions and checks PvP hits only. It does **not** yet authoritatively synchronize NPCs, inventories, pickups or devices and is not ready for public servers. For Windows double-click `play.bat` with JDK 17+ installed.

## Play concept

- PSX-resolution software renderer in a Swing window; compact keyboard/mouse interface
- Seed-driven modular city plan, streets, building lots, alleys, props, traversal lanes
- Spawn selection, handgun and shotgun, ammo and equipment collection
- Independent health and sanity; sanity hazards, darkness/visibility and Thief-inspired visibility gem
- Characters supply distinct abilities, not merely cosmetic skins
- Arena economy: cash and supplies fund defenses, turrets, healing and revival stations
- Roaming neutral/hostile NPCs and expandable LAN deathmatch

## Asset policy

Google Drive remains the archival source of truth. See [assets/SOURCES.md](assets/SOURCES.md) and [assets/manifest.csv](assets/manifest.csv) for VERIFIED FOLDER/FILE LOCATORS. Archive presence is NOT game integration, a rigged character or a runtime-tested scene. Never commit complete purchased packs to this public repository. Ship only curated permitted derived game runtime assets after license and functionality checks.

## Scope / priorities

[Design contract](docs/DESIGN.md) · [Development and QA gates](docs/ROADMAP.md)

No GitHub Actions by default. A remote commit is only claimed after its acceptance and read-back. Existing shared Beckenburg library and its original archives must not be deleted or reorganized.
