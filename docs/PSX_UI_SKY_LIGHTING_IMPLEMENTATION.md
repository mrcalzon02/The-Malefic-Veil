# PSX Horror UI Free — The Malefic Veil attribution and integration

**Original artist:** heyheythere — https://heyheythere.itch.io/psx-horror-ui-free  
**Asset:** PSX Horror UI Free, acquired in the approved shared Drive intake folder `PSX Horror UI Free — heyheythere (source intake)`; source archive: `psx-horror-ui-free-assets.zip` (135,407 bytes).  
**License:** Creative Commons Attribution 4.0 International (CC BY 4.0); license file included inside original ZIP at `psx-horror-ui-free/LICENSE.txt`.  
**Required credit:** `PSX Horror UI Free by heyheythere - https://heyheythere.itch.io/psx-horror-ui-free - CC BY 4.0`

The **free pack** is available for modification and commercial distribution under CC BY 4.0 with attribution. This grant is **specific to the free pack** and must **not** be incorrectly applied to heyheythere's separately sold full PSX Horror UI product or other purchased assets.

Original asset bytes are stored intact in `gpu-client/src/main/resources/psx-ui/psx-horror-ui-free-assets.zip`. They are loaded from the JAR using Java `ZipFile` (with a temporary read-only ZIP copy to handle source STORE method descriptors) and decoded into nearest-neighbor GPU textures; they are not replaced with generated stand-ins.

## GPU UI coverage

`PsxHorrorMenu` implements real keyboard/mouse accessible main menu, graphics/settings and credits screens using original fog-theme nine-slice window, panel, menu-band and button sprites. In-game Escape returns to menu; F10 opens graphics. Graphics settings are live: view range, number of active local lights (0–8), and day/night simulation preview speed. Current controls do **not** claim dynamic mesh LOD, resolution scaling or server-side persistence.

The pack’s original **Ribbon bitmap font and atlas** render menu, game and GPU editor overlay lettering. In the playable GPU client, `PsxHorrorMenu` now loads **all 53 original fog-theme PNGs**, and draws sourced nine-slice HUD/pause panels, weapon/ammo and character status displays, actual `progress_bg/progress_fill` health and sanity meters, `slot` inventory backgrounds, `mark_you` map/crosshair markers, `tooltip` event notices and `menu_band` lower bar. The actual city/continent generated map images remain gameplay data, framed by original theme widgets.

`PsxHorrorSwingSkin` reads exactly the same licensed ZIP and renders original `button_normal`, `button_hover`, `button_pressed`, `window`, `panel` and `line_edit` art in the separately windowed native editor dock. Shared editor behavior still comes from the external LevelEditorKit. This styling does **not** claim that every Swing world-creation dialog or every gameplay transaction UI is already converted.

## GPU night, sky and streets acceptance

- `WorldDayNight` defines 24-hour daylight, dusk, sky colors, ambient light and lamp intensity over a configurable reference day of 24 real minutes; clock currently runs in the local GPU preview, not yet synced to the multiplayer host.
- `GpuSkybox` performs one GPU fullscreen draw with retro gradient sky, procedural stars and sun/moon glow.
- `UrbanLighting` indexes **actual Kenney light fixtures** from seeded city tiles; the GPU material shader uses up to eight nearest lamp sources with bounded distance attenuation and ambient daylight. This is real point-light *shading* without shadow maps; light halos/shadows are future quality gates.
- `UrbanTileAssembler` now surfaces streets from one-unit asphalt/pavement tiles with intersection-aware shoulders and street lamps. SAFE, RESOURCE, HAZARD and OPEN plots have coherent, plot-wide ground material rules and differentiated intact/collapsed roofs; prop families are composed by plot category. Colliders remain from authoritative tile topology, and newly introduced decorative objects are not yet fully collidable.
- Automated CI captures `gpu-kenney-city.png`, `gpu-kenney-night.png` and `gpu-psx-menu.png` from an actual OpenGL/Mesa context; these are render proofs, not aesthetic or high-concurrency acceptance.

## Still to do

Integrate original source save-slot textures with the authenticated persistent character selection, complete responsive submenus for factions and transactions, and skin the separate Swing world-creation dialogues. Add directional sun shading, shadow options and emissive lamp meshes. Validate built-road pavement continuity, mesh scale, doors and roofs from on-foot camera captures in more than one city district. Separate client visual settings from server clock authority, and persist graphics locally. Cross-zone environment LOD and culling continue as dedicated development gates.

## Verified CI evidence — 21 October 2026

[GPU day/night/UI execution run 39587066966](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39587066966) passed the Linux OpenGL/Mesa screenshot gate. Artifacts include 320×200 **day**, **midnight**, and **PSX main menu** captures. The menu screenshot uses the original fog-theme source sprites and Ribbon bitmap font, rather than a hand-drawn imitation. The GPU client continues to be a separately launched walkthrough; networked gameplay still runs in the Java2D client.

[Full game verification run 39586594612](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39586594612) accepted the revised Kenney plot and road assembly, with deterministic tile and collision tests on Linux/Windows Java 17 and 21. The clock and lighting registry have separate pure Java tests in the GPU pipeline. The successful screenshot gate is a renderer functional proof, **not** a complete aesthetic/collider acceptance.


## Source intake readback and current acceptance (22 October 2026)

The exact approved source intake ZIP is already committed at
`gpu-client/src/main/resources/psx-ui/psx-horror-ui-free-assets.zip`.
It remains embedded intact inside the Maven-shaded **actual installer game
JAR**, not copied from a second, unverified download at launch. Independent
release readback of that JAR confirms the source archive is 135,407 bytes,
SHA-256 `47193ece2bcc9b5f8193dfee6ff064e291cbbc0ae1582104a1d1fa8132535350`,
containing all **53 fog-theme sprites**, the original font files, and the
artist's original `LICENSE.txt` whose CC BY 4.0 line is preserved.

`PsxHorrorUiSource.verifiedCopy()` checks that SHA, the archive's contents,
the source license and required widgets **before** generating GPU textures or
the Swing skin. No silent generic fallback is permitted for missing assets.
The packed game UI now centers the original 320×200 menu artwork on the
640×400 framebuffer and uses corresponding mouse hit coordinates.

GPU CI checks original source/attribution, editor Swing skin loading, real
source-backed menu screenshot, plus the real editor viewport; desktop release
CI separately verifies both **shipped gameplay HUD and editor** emit source
render proofs from the **packaged** `MaleficVeil.jar`. These are execution
checks rather than a substitute for human interface readability review.

Retain the original artist credit:
`PSX Horror UI Free by heyheythere - https://heyheythere.itch.io/psx-horror-ui-free - CC BY 4.0`
