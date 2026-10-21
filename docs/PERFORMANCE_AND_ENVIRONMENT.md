# Performance and environmental asset gates

## Current executable controls

Press **F10** in the Java desktop client for a compact Swing graphics dialog. `PerformanceSettings` persists settings with Java Preferences independently of account/character records. View distance (4–64 game units), high-detail mesh distance (2–32, clamped to view distance), and visible-model budget (8–500) alter static GLB prop rendering. Past the high-detail distance, a deterministic 1-in-4 triangle subset is drawn. This is a temporary mesh LOD approximation, not a proper authored lower-poly mesh; test visual holes, silhouette changes and occlusion artifacts before release.

A 15–30 FPS render cap drops alternate repaints at reduced settings but **does not** alter the 33ms simulation timer. This client currently cannot produce 60 FPS in the existing 30Hz loop; do not advertise 60 FPS support without separating game simulation and rendering. The model count budget is applied separately per renderer call; it is not yet a global all-category GPU/CPU frame budget. There is no automatic performance tuner, asynchronous asset decode or mesh-draw-call batching yet.

Existing **M** local map and **N** continental overview stay functional. Future options: map zoom/pan, independently adjustable distant terrain, vegetation, dynamic lights and NPC detail, model density, texture resolution, object shadow toggle, accessibility scale, window resolution, render target scale and measured frame-time diagnostics. Server rules, interest range and collision must not be reduced just because a client selects lower visual detail.

## Environmental source inventory and production intake

**Approved public redistribution path:** the project's existing Kenney Retro Urban Kit 2.0 ZIP and selected GLBs, whose CC0 `License.txt` is verified and recorded in `assets/game/kenney/PROVENANCE.md`. Reuse its actual geometry for urban roads, barriers, benches, lighting, dumpsters and modular city scenery; use LOD variants or measured proxy models created from licensed originals. Do not attribute non-Kenney assets to the Kenney license.

**Candidate sources, not approved to redistribute yet:** the Drive-indexed URBAN.zip, PSX Mega Pack, PSX Cars, and environment packs listed in `assets/SOURCES.md` and `assets/manifest.csv`. Audit source-specific license text, content hashes and renderer compatibility before selectively importing each into the runtime. Preserve purchased archives in the source vault and avoid exposing their raw packages in public repo/releases without confirmed rights.

**Environment needed for the continent:** coastal water and shoreline meshes; irregular forests with tree/rock/undergrowth variations; mountains and hill cliffs; plains grasses and scattered rocks; roads including intersections, verges, bridges and breakdowns; city edge transitions; marsh vegetation, pools and deadwood; horizon silhouettes. Start with deterministic project-generated terrain heights and optimized procedurally authored mesh variants where licensed sources are insufficient. Shared asset IDs plus deterministic placement and biome tables are preferable to serializing duplicate geometry per zone.

## Loading architecture and frame budgets

1. Zone prewarm triggers before crossing a zone boundary. The client prioritizes the current visible sector, then movement-direction neighbor, then background ring.
2. Limit **decode milliseconds**, mesh uploads and new instance creation per frame separately; a hard cap on model count does not cap triangle count or draw time.
3. Generate collision data independently of visual LOD; adjacent sectors use identical edge height samples and a mesh stitching contract.
4. Cache immutable mesh templates keyed by asset + LOD + version, and world sector geometry keyed by (seed, generator version, sector, LOD, material revision). Persist hashes; evict by memory budget using LRU without evicting currently referenced meshes.
5. Stage changes off the render thread; publish an immutable snapshot only after all geometry dependencies are ready. Never stall on disk/network inside the Swing paint loop.
6. Use occlusion and view-frustum culling, maximum draw-distance bands, instanced repeated props, and triangle-budget reporting. Missing content should use a visually clear low-poly proxy rather than freezing gameplay.
7. Test with 30-minute driving loops across the entire continent and track worst-case frame time, p95/p99, GC pauses, asset cache misses, mesh stalls and bytes streamed; validate on low-end target machines.

### Release gates

- P1: Client controls persist, do not change authoritative physics, and reject out-of-range values.
- P2: Mesh caches are bounded and deterministic; level seams identical across generation order and LOD.
- P3: Environment models are licensed, hashed, converted, displayed and collision-verified with actual runtime assets.
- P4: No perceptible sector handoff stall during full-speed vehicle travel under simulated latency.
- P5: 24h host and client soak with memory, zone loading, frame time and network traffic telemetry. Numerical CPU/GPU hardware minima follow benchmark evidence, not guesses.

This file deliberately does not claim that P2–P5 are implemented.
