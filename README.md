# The Malefic Veil

## Repository wiki: systems, inventories, recipes and the Lost

**Progression and release integration:** [October 26 end-to-end gameplay audit](docs/PROGRESSION_END_TO_END_AUDIT_20261026.md) records evidence, unresolved gaps and functional acceptance paths from initial spawn to level-50 base. Do not equate registry entries or a passing unit test with a completed playthrough.

Browse the [source-driven wiki](wiki/Home.md) for a navigable reference covering 157 exported items, 22 field recipes, 62 industrial processes, 69 facility blueprints, three factions, nine class definitions, current gameplay systems and world generation. [The Lost](wiki/The-Lost.md) now have a detailed five-family bestiary and [25 proposed weapon profiles](wiki/Lost-Arsenal.md) with distinct tactics, windups, counterplay and encounter roles, backed by the [LostArsenal design registry](src/main/java/veil/LostArsenal.java) and a self-test. **Only the Lost Drifter's opening bone-shiv is now wired to host combat**: a replicated 300 ms windup, committed avoidable 12 HP / 1 sanity stab and 650 ms recovery, with Begotten protection and existing death rules. The other **24** registry weapons remain descriptive; non-Drifter host monsters retain basic attacks. Solo-client attack parity, actual installed client visuals and full encounter balance remain unverified.


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

## Crafting economy placeholder icons (150 verified sprites)

The five consistent inventory sheets—shared materials, shared equipment, Unraveled manufacturing, Penitent ritual supplies and Begotten biological goods—are now checked in as **five archives of 30 individually sliced transparent PNGs**, together with five matching WebP visual references. Every asset has a stable uppercase item ID and SHA-256 recorded in [the crafting icon index](assets/game/crafting/icons_index.csv). [Crafting art readme](assets/game/crafting/README.md) explains the exact archive paths and naming rules.

`CraftingIconPacks.load(Path.of("."))` reads the committed ZIPs directly and returns a real 128×128 RGBA sprite via `icon("TIMBER")`, `icon("RITUAL_CLOTH")`, etc. The dedicated headless `CraftingIconPacksTest` checks all **150** actual sprites, with no remote art service or external runtime dependency. The sprites are placeholder art for the planned economies; the base survival resource subset can now be physically harvested in the continuous world, but not every indexed specialty item is craftable or tradeable yet.

## Opt-in GPU Kenney client (libGDX / OpenGL 3.3)

The separate `gpu-client/` Maven project produces an actual **hardware-instanced** PSX-resolution desktop renderer from the same generated Kenney city tile placements. Identical GLB meshes are uploaded once; visible transforms draw with real `glDrawArraysInstanced` calls. It renders via a 320×200 framebuffer, nearest-neighbor upscale, hardware depth and fog, with a separate executable shaded JAR.

Build with JDK 17 and Maven:

```sh
mvn -B -ntp -f gpu-client/pom.xml -DskipTests package
java -jar gpu-client/target/MaleficVeil-GPU.jar
```

WASD moves, arrow keys turn, Left Shift sprints, plus/minus changes view distance. This is currently a **real GPU Kenney environment walkthrough**, **not yet** a port of the existing shooter client, authentication, LAN combat or UI. The original Java2D launchers and headless server remain unchanged. The [GPU CI workflow](.github/workflows/gpu-client.yml) builds on Linux and Windows, executes a Mesa/Xvfb OpenGL smoke screenshot on Linux, and publishes a downloadable JAR and screenshot in its build artifacts. [Implementation contract and evidence gates](docs/GPU_CLIENT_IMPLEMENTATION.md).

### GPU environment, night cycle and PSX UI

The GPU walkthrough now has a procedural retro skybox with sun, moon, stars, dawn/dusk fog and a 24-minute preview day/night cycle. Actual Kenney streetlight meshes create local point-light pools (up to eight nearest per fragment). The seeded city assembles connected streets from modular Kenney asphalt/pavement and builds category-sensitive SAFE, RESOURCE, HAZARD and OPEN plots with licensed walls, roofs and dressing.

The **main menu uses the original CC BY 4.0 PSX Horror UI Free sprites and Ribbon bitmap font**, with keyboard/mouse menu navigation, graphics controls and an attribution screen. Controls: main-menu arrows/Enter; in-world Escape for menu, F10 for graphics, `[ ]` to scrub the clock, WASD/arrows for movement, +/- for visual range. Current graphics controls adjust view range, local light count and visual clock rate. This is still a **local GPU world preview**; server-synchronized time and multiplayer functionality are not yet ported.

**Required UI credit:** PSX Horror UI Free by heyheythere — https://heyheythere.itch.io/psx-horror-ui-free — CC BY 4.0. The commercial full pack has a separate license and is not used here. [Detailed asset attribution, screenshots and pending gates](docs/PSX_UI_SKY_LIGHTING_IMPLEMENTATION.md).

## Continental population, vehicles and dedicated host (development slices)

A deterministic continent contains seven city sites, biomes, roads, categorized plots, NPC encounter and resource sites. **City A's detailed 132×132-metre district and the 224×224×6-metre continental terrain now share one authoritative world coordinate system**. Walkers cross the City A boundary with ordinary movement; there is no scene portal, wilderness dimension, or separated harvesting session. Simple resources have Beckenburg-style E aim prompts, host-validated gathering, common survival inventory, and durable depletion. **The rest of the cities still use coarse procedural terrain and incomplete 3D buildings/collision**; fully authored city-to-city roads, NPC placement, complete settlements and driving routes remain gated. See [the continuous-world correction](docs/ONE_CONTINUOUS_WORLD_AND_HARVESTING.md).

Cars are **drivable in the playable city sector** in solo and LAN: press `Q` at a car to enter/exit; use `W/S` to accelerate/reverse and `A/D` to steer. Host authority controls occupancy, car collision, fuel, and car pose. Host-controlled drivable cargo vehicles deliberately retain their licensed **Kenney truck** GLB geometry. Separately, generated city parking lots, rare police/taxi parking and ruined hazard lots now use the **GGBot CC0 PSX car pack** with original OBJ/PNG UV material mapping (39 appearances across 10 meshes). GGBot cars are static, physical scenery rather than new drivable car classes. Release builds include the source archive by verified SHA-256; local source checkouts without the archive omit those decorative cars. Tuning, refueling, passengers, repairs and intercity streaming remain subsequent gates. See [GGBot car source and material contract](assets/game/ggbot/README.md).

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

## Asset policy

Google Drive remains the archival source of truth. See [assets/SOURCES.md](assets/SOURCES.md) and [assets/manifest.csv](assets/manifest.csv) for VERIFIED FOLDER/FILE LOCATORS. Archive presence is NOT game integration, a rigged character or a runtime-tested scene. Never commit complete purchased packs to this public repository. Ship only curated permitted derived game runtime assets after license and functionality checks.

## Scope / priorities

[Design contract](docs/DESIGN.md) · [Development and QA gates](docs/ROADMAP.md) · **[Player QoL must-haves and external shooter research](docs/PLAYER_QOL_MUST_HAVES.md)**

Project-authorized GitHub Actions compile and test on Windows/Linux; other repositories retain their separate workflow policies. A remote commit is only claimed after acceptance and read-back. Existing shared Beckenburg library and its original archives must not be deleted or reorganized.

## Original 32-weapon arsenal — integration

Restored from [arsenal-prototypes](https://github.com/mrcalzon02/The-Malefic-Veil/tree/arsenal-prototypes): eight shared loot guns (Vesper through Furnace Organ), 24 tiered faction-fabricated weapons (Hailstorm through Veilbreaker), complete authored working meshes/GLBs, timed primary and alternate host combat, component/bench progression, and runtime faction material palettes. Manufacturers use the game's permanent Unraveled/Penitent/Begotten enrollment rather than the old Foundry/Lumen/Veil fabrication picker. [Full weapon wiki](wiki/Weapons.md). Model reshaping and final sound remain future work.
