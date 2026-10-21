# Kenney modular city: source tiles and instancing contract

The source construction pieces for roads, corner pieces, pavements, doors, lower/upper wall sections, roofs, vegetation and street furniture come from the *actual* Kenney Retro Urban Kit 2.0 CC0 archive bundled at `assets/game/kenney/retro-urban-kit-2.0.zip`. They are world assembly **modules**, not loose decorative substitutes for the city.

## Current construction pipeline

`CityLayout` determines seeded connected public road graph, authoritative walkable/solid tiles and immutable plots. `UrbanTileAssembler` reads this output and places source Kenney GLB modules with deterministic transforms: road straight/corner/junction modules, plot pavement/ground, exposed wall module runs, matching open doorway frames, elevated second-storey walls, roof modules and category-appropriate scenery. `World` owns these placements; `UrbanScene` validates they exist in the licensed bundle and loads each GLB into one shared mesh cache. Game movement, spawn safety and projectiles remain bound to authoritative `CityLayout` collision; the additional four explicit urban obstacles have real authored radii. Visual road/facade piece geometry is **not yet a perfect per-triangle authoritative collider**—that remains a release gate.

The scene's spatial grid lists only nearby candidate instances. For view budgeting, the Java2D renderer sorts nearby candidates toward the camera and applies local prop-distance/model-count limits before triangle rasterization.

## Geometry instancing (important distinction)

`UrbanInstanceBatches` makes genuine mesh reuse explicit: one entry per unique *source mesh*, holding one `GlbMesh` reference and a packed six-float transform row per copy (X, Y, yaw, scale, opacity, height offset). All immutable placement buffers are defensive-copy protected. This is the immutable source geometry/instance-attribute contract for future GPU upload: load geometry once, issue `glDrawElementsInstanced` (or another suitable instanced API) per material, mesh and LOD, and move an instance buffer only when that zone's transforms change.

**The current Java2D renderer is NOT hardware instanced**. It still transforms and rasterizes visible triangles individually, and therefore cannot honestly claim the major GPU draw-call gains. Instancing-ready buffers are committed and validated, but full instanced rendering requires a separate GPU backend; avoid making it a fake Java2D batching claim. For animated monsters, skinned actor batches are a separate concern. Transparent objects need correct sorting or a suitable transparency technique.

## Release gate and performance

- KenneyCityAssemblyTest confirms real source mesh placements for plot ground, roads, wall runs, roof instances, open frames, source IDs, spawn safety and a 320×200 screenshot artifact.
- UrbanInstanceBatchesTest confirms that repeat instances share **one** source mesh, buffer lengths are consistent, data are finite/immutable, and repetitions greatly exceed unique geometry.
- Source kit compatibility tests accept valid **planar two-triangle road surfaces** and thin vertically oriented modules rather than wrongly rejecting them.
- Future GPU renderer must use indexed glTF vertex buffers and instanced attribute arrays, group draws by mesh/material/LOD and cull at zone/frustum level. Its correctness tests should compare identical placements with Java2D reference images.
- Performance benchmarks must measure actual draw calls, unique source meshes, instance counts, visible triangles, frame p95/p99, per-zone memory and GPU upload stalls. No runtime FPS improvements are claimed without measurement.
- A mesh naming match does not prove seams, door placement, roof alignment or collision parity; all require visual screenshot review and corrections.

The parallel *The Lost* NPC source-casting work remains separate from the Kenney world kit.


## Actual source dimensions and correction

The original CC0 Kenney GLBs have been inspected as geometry, not inferred by filenames. Road-asphalt-center, road-asphalt-pavement, grass and many facade modules occupy approximately **1×1 horizontal source units**; the asphalt straight is roughly **1×2** and the road corner **2×2**. Accordingly, the authoritative 6×6 city-layout macrocell now fills its footprint with 36 **individual 1×1 Kenney tile instances** (and adds an optional 1×2/2×2 route detail at its center). Plot ground and roof assemblies likewise use native-scale repeated source pieces. We removed the earlier 6× stretched squares and 5.2× roof wedges after real screenshot inspection showed oversized, implausible forms.

These replacements substantially increase instance count, which is exactly why shared immutable GLB geometry, packed instance buffers, spatial prefiltering, and a future true GPU instanced draw path are required. This is still a staging implementation: module edge seam alignment, roof and upper-storey accuracy, and source-matched collision need screenshot-based validation before declaring a city complete.
