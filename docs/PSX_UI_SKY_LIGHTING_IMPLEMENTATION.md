# PSX Horror UI Free — The Malefic Veil attribution and integration

**Original artist:** heyheythere — https://heyheythere.itch.io/psx-horror-ui-free  
**Asset:** PSX Horror UI Free, acquired in the approved shared Drive intake folder `PSX Horror UI Free — heyheythere (source intake)`; source archive: `psx-horror-ui-free-assets.zip` (135,407 bytes).  
**License:** Creative Commons Attribution 4.0 International (CC BY 4.0); license file included inside original ZIP at `psx-horror-ui-free/LICENSE.txt`.  
**Required credit:** `PSX Horror UI Free by heyheythere - https://heyheythere.itch.io/psx-horror-ui-free - CC BY 4.0`

The **free pack** is available for modification and commercial distribution under CC BY 4.0 with attribution. This grant is **specific to the free pack** and must **not** be incorrectly applied to heyheythere's separately sold full PSX Horror UI product or other purchased assets.

Original asset bytes are stored intact in `gpu-client/src/main/resources/psx-ui/psx-horror-ui-free-assets.zip`. They are loaded from the JAR using Java `ZipFile` (with a temporary read-only ZIP copy to handle source STORE method descriptors) and decoded into nearest-neighbor GPU textures; they are not replaced with generated stand-ins.

## GPU UI coverage

`PsxHorrorMenu` implements real keyboard/mouse accessible main menu, graphics/settings and credits screens using original fog-theme nine-slice window, panel, menu-band and button sprites. In-game Escape returns to menu; F10 opens graphics. Graphics settings are live: view range, number of active local lights (0–8), and day/night simulation preview speed. Current controls do **not** claim dynamic mesh LOD, resolution scaling or server-side persistence.

Bundled bitmap font, save-slot textures, inventory items, maps, condition bars and ECG sprites have been inventoried but **not all deployed** in this initial menu pass. Wire them to real character save data, inventory and map state before marking those tabs complete. Do not create inert buttons that suggest nonexistent multiplayer account features.

## GPU night, sky and streets acceptance

- `WorldDayNight` defines 24-hour daylight, dusk, sky colors, ambient light and lamp intensity over a configurable reference day of 24 real minutes; clock currently runs in the local GPU preview, not yet synced to the multiplayer host.
- `GpuSkybox` performs one GPU fullscreen draw with retro gradient sky, procedural stars and sun/moon glow.
- `UrbanLighting` indexes **actual Kenney light fixtures** from seeded city tiles; the GPU material shader uses up to eight nearest lamp sources with bounded distance attenuation and ambient daylight. This is real point-light *shading* without shadow maps; light halos/shadows are future quality gates.
- `UrbanTileAssembler` now surfaces streets from one-unit asphalt/pavement tiles with intersection-aware shoulders and street lamps. SAFE, RESOURCE, HAZARD and OPEN plots have coherent, plot-wide ground material rules and differentiated intact/collapsed roofs; prop families are composed by plot category. Colliders remain from authoritative tile topology, and newly introduced decorative objects are not yet fully collidable.
- Automated CI captures `gpu-kenney-city.png`, `gpu-kenney-night.png` and `gpu-psx-menu.png` from an actual OpenGL/Mesa context; these are render proofs, not aesthetic or high-concurrency acceptance.

## Still to do

Use original PSX bitmap font instead of stock libGDX font; integrate save slots with the authenticated persistent character selection, build responsive submenus for inventory, map, factions and settings. Add directional sun shading, shadow options and emissive lamp meshes. Validate built-road pavement continuity, mesh scale, doors and roofs from on-foot camera captures in more than one city district. Separate client visual settings from server clock authority, and persist graphics locally. Cross-zone environment LOD and culling continue as dedicated development gates.
