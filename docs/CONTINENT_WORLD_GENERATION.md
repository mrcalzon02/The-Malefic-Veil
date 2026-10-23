# The Malefic Veil — continental world generation contract

## Intent

Generate a reproducible continent surrounded by a water plane, with an irregular coast, mountains, forests, plains, hills, marshes and beaches, containing multiple distinct cities. City boundaries are **templates**, not all squares: the reference sketch has A–G including angular/L-shaped settlements and a near-square City C. Build the road graph **inside each city** before tessellating its vacant ground into hazard/resource/safe/open plots. Join the cities by terrain-aware intercity highways, then populate transitional wilderness.

The implementation is layered so the existing fast multiplayer city renderer can remain functional while a larger streamed world is developed. **Do not confuse the new 224×224 parcel-scale continental blueprint with a fully traversable, rendered continent**; the current combat renderer still uses the 132×132-tile `World` sector.

## Deterministic order of generation

1. **Landmass:** Sample multi-octave, smoothly interpolated seeded value noise on a radial/elliptical elevation field. Multiple angular lobes distort the coastline; sea level clips the terrain to a surrounding water plane. Keep the center-connected continental landmass and discard detached artifacts. `ContinentLayout.SIZE=224` indexes coarse generation parcel cells, not final world meters.
2. **Climate and geography:** Derive beach/marsh bands close to sea level, elevated hill/mountain regions from height and ridge noise, and forest/plains from humidity noise. These are distinct seeded classification fields, not arbitrary random squares. Long-term: add rivers, coastal erosion, elevation-conditioned slopes, cliffs, wetland buffers and regional biome continuity metrics.
3. **City siting:** Apply A–G normalized settlement anchors based on the sketch, with bounded seed variation in location and footprint. Shapes: `L_WEST`, `SPIRE`, `SQUARE` (C), `L_SOUTH`, `IRREGULAR`, `COMPACT`, and `HORSESHOE`. Clip against actual land, remove disconnected boundary fragments, and maintain non-overlapping city IDs. Future city templates will come from a configurable shape library rather than seven baked anchors.
4. **Intercity roads:** Build a weighted graph over city centers, choose a minimum-cost spanning backbone plus up to two alternative links, and find a walkable tile route between each selected pair. Path costs discourage mountains, hills, marshes and crossing developed city interiors. Every generated highway belongs to the protected pre-existing world.
5. **Internal road network:** Starting at each center, connect seed-selected arterial endpoints within that site's footprint. The arterial roads remain connected to the intercity system and the city center. This macro network will later bind to the established `CityLayout.Road` tile grammar (straight, corner, T, crossroads, dead-end, branches, alleys).
6. **Plots:** Reserve roads first. Place preferred 2×2 lots in each city, covering all four HAZARD/RESOURCE/SAFE/OPEN categories, then fill leftovers with legal 1×1, 1×2, 2×1, 2×3, 3×2 and 3×3 lot footprints. Seeded assignment must cover each city cell exactly once, with no overlap or road collision. This is macro lot metadata; the existing city uses its 104-element template library for local collision geometries.
7. **Wilderness:** The cells outside cities/roads retain biome identities and ground availability. Scatter protected natural obstacles (rock outcrops/trees) sparsely rather than marking whole forests as unbuildable. Future passes should use explicit forest edges, foothill gradients, suburban frontage, industrial outskirts, rural holdings, streams and roadside points of interest with minimum separation rules.

## Construction ownership — non-negotiable

Every generated feature is owned by the **world**, not by an individual player. World-owned geometry is immutable to the player construction system unless an explicit later demolition/reclamation mechanic changes its ownership.

- **Forbidden:** **every part of every generated city**, including undeveloped/empty lots, parks, plazas, ruined sites, roofs, streets, courtyards and alleys; also intercity highways, world structures, protected spawn areas, natural obstacles and occupied ground. Walkable/open/empty does **not** confer building permission.
- **Allowed:** **wilderness only**—unoccupied suitable natural land outside all city footprint masks and generated roads, with full building-footprint and clearance checks, plus host authorization, resources, slope limits and route/spawn protection.
- **Server authority:** use the same deterministic masks for the host and clients. `World.worldStructureAt(x,y,radius)` checks the active arena's authored land/roads/structures/props. `ArenaSystems.reviewBuild` now returns `CITY_PROTECTED` for the current entire city-sector runtime before any payment or mutation. The continental planning validator returns `CITY_PROTECTED` for proposed city-overlapping footprints, even if a lot appears bare. Client presentation must not offer “buildable city” hints. Player-owned devices remain a separate mutable layer and are never baked into the world-generation map.
- **Planning authority:** `ContinentLayout.worldStructureAt` and `buildableGround` expose the analogous mask at macro parcel resolution. **This is not yet a fully-sized building-footprint test**: streamed chunks must translate the coarse protected mask to fine colliders, include height/floors and slopes, check every occupied cell of each rotated placement, and independently validate large building extents. Do not claim global large-structure placement implemented before those checks exist.

Within the existing **city-only** arena, `CityLayout` protects **every** parcel, including the `OPEN` / `empty-lot` variant. Therefore the server correctly refuses **all new player structures in the current city**. The continental blueprint already identifies suitable wilderness build parcels, but interactive, streamed, server-authoritative wilderness construction is **not yet implemented**. Until wilderness gameplay is connected, player construction remains unavailable rather than secretly allowing it inside cities. Existing saved city structures are handled as **legacy migration data**: on load, the server validates the checksummed snapshot, archives its exact bytes to a `realm-city-legacy-*.dat` file, **does not respawn the obsolete city structures**, and allows the next save to persist the sanitized world. Failure to archive aborts loading rather than discarding data. The archival snapshot remains available for later manual recovery or wilderness relocation. New saves refuse any active city-owned structure.

## Multiplayer, storage and performance

The host owns the continental seed, generation version, city template IDs, road edges and materialized chunk revisions. Clients may derive static geometry from the seed, but the host remains authoritative for collision, structures, pickups, NPCs, damage and per-chunk modifications. A later chunk scheme should keep sparse player buildings and destroyed/repaired structures as an overlay keyed by stable world+chunk+structure IDs. Use deterministic per-chunk random substreams and boundary contracts so streaming order never changes the map.

The current continental blueprint is generated **lazily** through `World.continent()`. The new `N` overview displays macro geography and city sites; `M` keeps the legacy local city map. The overview does **not** imply players can yet walk from City A to City G. Existing lane/host authority, actor positions and renderer limits are deliberately unchanged until chunk streaming and coordinate migration have acceptance tests.

## Deterministic population and macro building site pass

`ContinentPopulation` now derives immutable player spawn candidates in each of the seven generated cities (three per city), distributed road-accessible wilderness rally points, and bounded seeded hostile/scavenger group encounter markers. The same seed and generator version produce the same lists; site IDs remain stable for authoritative chunk materialization. Static encounter plans are **not** active NPCs, network spawn packets, or player traversal into continental sectors. Spawning living actors in a loaded cell remains a separate G2 host/client integration step.

`ContinentPopulation.reviewConstruction(x,y,width,height,clearance,rotated)` checks rotated rectangular parcel footprints and surrounding clearances against coast/water, mountains, world structures, and player-spawn protection zones. This is a **macro suitability check**; no player resources are charged here. Runtime builder authorization must additionally check exact fine colliders, live player buildings, slope, permission/ownership, obstruction, route connectivity and atomic inventory debit in the authoritative streamed chunk layer before construction is accepted.

**Immediate G2 integration sequence:** stable macro-to-fine cell coordinates; deterministic 3D terrain/chunk collision; generated roads and city geometry attached to each chunk; active spawn service consuming these plans; sector-local NPC AI and sleeping/unloading; structure overlay persistence keyed to immutable chunk/world IDs; shared host interest management and client scene loading; actual vehicle road traversal and seam tests from one city through wilderness to another.

## Release gates

- **G0 — deterministic blueprint (implemented):** Java 17 compile; six exact-invariant seeds plus 100 diverse seed-stress generations; same-seed exact geography/city/lot/route results; island surrounded by water; all seven biomes represented; all seven cities on land; City C square template; 6+ intercity links; road continuity between city centers; every city cell either road or exactly one plot; standard 2×2 categories present; protected roads/developed lots and significant buildable land.
- **G1 — site geometry and biome edges (population planning slice complete; visual edge and physical terrain pending):** Visual map artifact per seed, coast/hydrology checks, no accidental short bridges across water, city frontage orientation, transition band widths, city shape catalog expansion, actual intercity road tile models.
- **G2 — streamed playable land:** Convert macro parcels into hierarchical sectors of collision tiles; load/unload adjacent chunks with no seams; city-to-wilderness-to-city traversal; stable coordinates and host/client chunk version checks; benchmark traversal, spawn safety and memory.
- **G3 — full construction:** Rotated multiblock buildings, terrain grading, full 3D footprint checks, world-owned object registry, floors and vertical clearance, obstruction preview, persistence/rollback and multiplayer concurrency proof. Reject placement across protected footprints *before* deducting resources.
- **G4 — art and encounter realization:** License-verified forest/mountain/coast/road/city assets; sound, physics, materials, vegetation, transit encounters, hazards and worthwhile exploration between settlements.
- **G5 — long-session acceptance:** 20+ world seeds, pathfinding through all city sites, 2+ players, build/exploit/rejoin scenarios, reproducibility across Windows/Linux Java 17/21, real rendered playtest and frame/memory evidence.

Run locally: `javac --release 17 -d out src/main/java/veil/*.java`; `java -cp out veil.ContinentLayoutTest`; `java -cp out veil.CityLayoutTest`; `java -cp out veil.QualityGateTest`; `java -cp out veil.LanSmokeTest`. The existing verification workflow includes continent/city test gates; CI success must be established from a finished run, not presumed.

## G2 increment — sector partition and subscriptions (October 20, 2026)

`ContinentSectors` now provides deterministic 8×8 macro-parcel sectors (28×28 total for the 224×224 blueprint), containing immutable biome/elevation, city, road, world-structure, and buildability data. Player and NPC spawn *markers* are pre-indexed into a single owning sector; repeated subscriptions cannot duplicate marker IDs. Bounded `interest(x,y,radius)` produces a stable list of nearby sectors, clamped at map edges. The separate `ContinentSectorsTest` checks partition coverage, ownership masks, uniqueness, and boundary subscriptions across four seeds.

**Integration still required:** this sector model is not yet consulted by `LanServer` or `ArenaSystems`; its parcels are macro coordinates, not rendered terrain, and it does not create networked NPCs or construction entities. Next, implement immutable network sector descriptors (with generation version), actor subscription/delta reconciliation, and a macro-to-fine collision/materialization adapter. Only then move players/cars from single-city local coordinates to authoritative continental coordinates. Do not mark G2 complete until two-client crossing and restart tests demonstrate that traversal.

### G2 host protocol increment — read-only sector descriptors

`LanServer` now creates a deterministic `ContinentSectors` registry from the same seed as its arena. Clients can send `SECTOR|sx|sy` with integer sector indices in `[0,27]`. The host sends `SECTOR_BEGIN|sx|sy|parcelCount|playerSpawnCount|npcSpawnCount`, one `PARCEL` line per macro parcel (world x/y, biome, elevation, city code, road, immutable mask, buildability), zero or more `CONTINENT_SPAWN` and `CONTINENT_ENCOUNTER` marker lines, then `SECTOR_END|sx|sy`. Invalid indices return `SECTOR_DENIED|BOUNDS`. The request cannot mutate world state or choose arbitrary unbounded response sizes; existing dedicated-host AUTH restrictions still apply. `ContinentSectorNetworkTest` exercises this protocol over a real localhost socket.

**Important:** this is a read-only blueprint streaming protocol, not the final runtime delta stream, client terrain integration, production-safe network transport, or playable continental movement. Repeated requests retransmit descriptors and the client must ignore them until it implements parsing and scene materialization. Protocol generation-version negotiation, backpressure, subscriptions, caching limits, and precise fine-collider ownership are still required.


## Shared elevation and retaining-wall contract for every procedural city

The generated sites **A–G** now use `CityPlateTerrain` to establish one deterministic plate elevation per city from the final `ContinentLayout` footprint (after terrain clipping and disconnected-cell pruning). The mapping is the authoritative `World.PARCEL_METERS=6` grid, not the previous render-only three-metre projection. Each individual protected city cell contributes a plate-top mesh at its fixed grade; all exposed edges—including the recesses of L-shaped/irregular footprints—contribute vertical two-metre foundation walls. Natural terrain descends to those walls' feet, then blends back to the regional noise-driven elevations within a 24-metre band. City boundaries are never filled out to a rectangular bounding box.

The existing 132 × 132 City A district remains the detailed playable/source-kit scene, and its Kenney retaining blocks and two open, graded exit ramps take priority over the coarser City A macro-footprint. Generated city surface meshes B–G are **geometric plates/road palettes only**. They are not yet the complete source-kit building/plot instantiation of the future streamed cities, and this pass does not assert that client/server 3D collision or cross-city live streaming has been completed.

`CityPlateTerrainTest` checks every shape and foundation edge for six independent world seeds and checks actual 3D plateau/skirt vertices for B–G on the two seeds used for the GPU screenshot workflow. The screenshot gate also captures elevated and outside approaches to Cities B and C separately from City A. Assertions cover seam grade, plate-foot alignment, generation determinism, and visual renderability; archived images still require human art review.

## Interchangeable Java 17 level editor (authoring layer)

The game's **Level Editor (Dev)** menu option now hosts the **existing** external
`org.beckenburg.editor` module from *The Beckenburg Project*, published as
`LevelEditorKit-Java17.jar` in release `beckenburg-v0.1.31`. The pinned
binary SHA-256 is `5b75ada82ad71d128150b58f0ab74e0f0e18866968b216968c66940209e1252d`.
`tools/install-editor-kit.sh` fetches the independently published JAR, checks
its exact bytes, and installs it into Maven's local cache before building the
shaded GPU game. The Beckenburg editor implementation is **not copied or forked**.

`MaleficEditorProject implements EditorProject<Snapshot>` supplies game asset
identities, hierarchy, geometry, undo/redo snapshots, atomic save/reload, and
licensed source mesh validation; `MaleficEditorDock` uses the kit's actual
`EditorPalettePanel` and `EditorHistory` components, with a compact asset
filter, transform inspector, transactional Place/Apply/Delete, Undo/Redo,
Save/Reload and OpenGL viewport selection (Ctrl+click on an authored object).
The same instanced renderer, camera and seeded city geometry are visible while
editing. The editor can also launch with `java -jar MaleficVeil.jar --editor
--seed=20261020`. F11 focuses the editor dock, space/ctrl moves its fly camera,
and WASD/mouse turns and walks the shared GL viewport.

Authoring data is separate, in
`worlds/editor/seed-<seed>/city-A.properties`. It is reloaded automatically
as a **render-only** additional asset layer in ordinary GPU gameplay. This
document does *not* mutate the deterministic procedural city generator, host
collision, city protection, settlement construction, player stats, or durable
host saves. It currently supports City A objects and sourced models only;
brush-based terrain editing, walls that change collision, full map streaming
and cooperative authoring are not implemented. Treating editor-only props as
physical world structures would be a correctness/security defect until their
collisions are validated on the host.

A real GPU integration test `--editor-smoke` drives the imported history,
place, transform, undo/redo, atomic save/reload, GPU model upload, and a
captured framebuffer from the **packaged** game. The release workflow fails
if that path or the external dependency breaks.
