# PSX Horror UI Free — The Malefic Veil attribution and integration

**Original artist:** heyheythere — https://heyheythere.itch.io/psx-horror-ui-free  
**Asset:** PSX Horror UI Free, acquired in the approved shared Drive intake folder `PSX Horror UI Free — heyheythere (source intake)`; source archive: `psx-horror-ui-free-assets.zip` (135,407 bytes).  
**License:** Creative Commons Attribution 4.0 International (CC BY 4.0); license file included inside original ZIP at `psx-horror-ui-free/LICENSE.txt`.  
**Required credit:** `PSX Horror UI Free by heyheythere - https://heyheythere.itch.io/psx-horror-ui-free - CC BY 4.0`

The **free pack** is available for modification and commercial distribution under CC BY 4.0 with attribution. This grant is **specific to the free pack** and must **not** be incorrectly applied to heyheythere's separately sold full PSX Horror UI product or other purchased assets.

Original asset bytes are stored intact in `gpu-client/src/main/resources/psx-ui/psx-horror-ui-free-assets.zip`. They are loaded from the JAR using Java `ZipFile` (with a temporary read-only ZIP copy to handle source STORE method descriptors) and decoded into nearest-neighbor GPU textures; they are not replaced with generated stand-ins.

## GPU UI coverage

`PsxHorrorMenu` implements real keyboard/mouse accessible main menu, graphics/settings and credits screens using original fog-theme nine-slice window, panel, menu-band and button sprites. In-game Escape returns to menu; F10 opens graphics. Graphics settings are live: view range, number of active local lights (0–8), and day/night simulation preview speed. Current controls do **not** claim dynamic mesh LOD, resolution scaling or server-side persistence.

The pack’s original **Ribbon 9-pixel bitmap font and atlas now render all menu and environmental HUD lettering**. Save-slot textures, inventory item icons, map markers, condition bars and ECG sprites are inventoried but **not yet attached to their gameplay screens**. Wire them to real character save data, inventory and map state before marking those tabs complete. Do not create inert buttons that suggest nonexistent multiplayer account features.

## GPU night, sky and streets acceptance

- `WorldDayNight` defines 24-hour daylight, dusk, sky colors, ambient light and lamp intensity over a configurable reference day of 24 real minutes; clock currently runs in the local GPU preview, not yet synced to the multiplayer host.
- `GpuSkybox` performs one GPU fullscreen draw with retro gradient sky, procedural stars and sun/moon glow.
- `UrbanLighting` indexes **actual Kenney light fixtures** from seeded city tiles; the GPU material shader uses up to eight nearest lamp sources with bounded distance attenuation and ambient daylight. This is real point-light *shading* without shadow maps; light halos/shadows are future quality gates.
- `UrbanTileAssembler` now surfaces streets from one-unit asphalt/pavement tiles with intersection-aware shoulders and street lamps. SAFE, RESOURCE, HAZARD and OPEN plots have coherent, plot-wide ground material rules and differentiated intact/collapsed roofs; prop families are composed by plot category. Colliders remain from authoritative tile topology, and newly introduced decorative objects are not yet fully collidable.
- Automated CI captures `gpu-kenney-city.png`, `gpu-kenney-night.png` and `gpu-psx-menu.png` from an actual OpenGL/Mesa context; these are render proofs, not aesthetic or high-concurrency acceptance.

## Still to do

Use original PSX bitmap font instead of stock libGDX font; integrate save slots with the authenticated persistent character selection, build responsive submenus for inventory, map, factions and settings. Add directional sun shading, shadow options and emissive lamp meshes. Validate built-road pavement continuity, mesh scale, doors and roofs from on-foot camera captures in more than one city district. Separate client visual settings from server clock authority, and persist graphics locally. Cross-zone environment LOD and culling continue as dedicated development gates.

## Verified CI evidence — 21 October 2026

[GPU day/night/UI execution run 39587066966](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39587066966) passed the Linux OpenGL/Mesa screenshot gate. Artifacts include 320×200 **day**, **midnight**, and **PSX main menu** captures. The menu screenshot uses the original fog-theme source sprites and Ribbon bitmap font, rather than a hand-drawn imitation. The GPU client continues to be a separately launched walkthrough; networked gameplay still runs in the Java2D client.

[Full game verification run 39586594612](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39586594612) accepted the revised Kenney plot and road assembly, with deterministic tile and collision tests on Linux/Windows Java 17 and 21. The clock and lighting registry have separate pure Java tests in the GPU pipeline. The successful screenshot gate is a renderer functional proof, **not** a complete aesthetic/collider acceptance.
