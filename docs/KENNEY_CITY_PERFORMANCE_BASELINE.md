# Kenney City Performance Baseline — 21 October 2026

## Scope

Measurements from the **real Kenney Retro Urban Kit 2.0** tile-assembled Java 17 city, not the separate URBAN.zip and not the unimplemented continental streaming world. City area is 132×132 gameplay tiles with roads, lots, walls, doors, modular rooftops and district details. The licensed mesh objects are shared, and `UrbanInstanceBatches` stores transform rows for repeated tile models. **There is no active GPU instanced drawing:** the current software Java2D renderer rasterizes meshes individually; actual instanced hardware draw calls need a GPU-backed renderer.

### Source-backed changes

- `GlbMesh.asset` caches immutable parsed Kenney GLBs once per process: repeated worlds and scenes reference the same mesh objects rather than reparsing all model geometry.
- `UrbanScene.nearby` spatially indexes the 29,072 placed tiles and filters candidates by actual requested radius before sorting.
- `PropRenderer` precomputes its per-column depth thresholds once per renderer call and coalesces contiguous visible screen-column clipping spans. This replaces allocation of one `java.awt.geom.Area` **per column of every submitted triangle**. Separate limits bound models and submitted faces in the software fallback.
- The city retains the optimized `physicalProps` index: only actual extra obstacles enter movement and projectile LOS loops. Immutable `CityLayout` walls remain collision-authoritative.
- Both `UrbanPerformanceTest` and `FullFramePerformanceTest` are executed in the existing Linux/Windows Java 17/21 verification matrix, and their CSV reports are included in CI artifacts.

## Verified baseline

Evidence: [full verification run 39582104582](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39582104582) — successful across all four matrix cells; collected as **headless microbenchmarks on GitHub-hosted virtual machines**. Timing is NOT an observed live multiplayer FPS and varies by runner.

| Test metric | Linux Java 17 | Linux Java 21 |
|---|---:|---:|
| World generation | 100.48 ms | 99.84 ms |
| Scene model import | 363.43 ms | 327.27 ms |
| Nearby spatial query p95 | 2.88 ms | 2.24 ms |
| Isolated Kenney prop render p95 | 4.44 ms | 4.46 ms |
| 10,000 movement + 500 LOS queries | 7.60 ms | 9.28 ms |

| Actual complete Swing panel paint p95 | Measurement |
|---|---:|
| Linux Java 17 | 9.72 ms |
| Linux Java 21 | 13.02 ms |
| Windows Java 17 | 13.50 ms |
| Windows Java 21 | 7.36 ms |

The city constructs **29,072 instances** from **48 loaded mesh types**. Of those, the models actually placed contain **1,864 unique source triangles**, against ~**342,872 triangles** if identical model geometry were duplicated per placement. This quantifies source geometry reuse and the *potential* of real hardware instancing; Java2D still draws visible copies separately.

Headless full-frame test includes raycasts, environmental scene, nearby pickups/NPCs, weapons, HUD and nearest-neighbor 960×600 presentation at three existing spawn areas and multiple headings. It captures a 12-frame warmed sample and reports p50/p95. Tests are deliberately **not** allowed to declare platform minimum hardware requirements or real FPS; they verify geometry and algorithmic bounds and record timings.

## Remaining performance work and acceptance

1. Physically verify Kenney modular scale, roof visibility, doorway passability, wall/mask parity and PSX presentation in screenshots from the CI artifacts. Draw-call batching alone cannot fix bad tile topology.
2. Build real client *GPU instancing*: shared vertex/index buffers uploaded once per licensed GLB, frustum-/cell-culled instance transform buffers and one material/mesh/LOD instanced submission. Software Java2D has no hardware instancing API; do not mislabel the current data batching.
3. Add bounded zone/sector streaming: avoid loading the full future continent and all its scenery when only 1–3 sectors are active. Cache and evict by memory budget, prefetch near zone boundaries.
4. Profile real device CPU/GPU, resolution, active NPC population, multiplayer packet rates and high-speed car traversal. Report frame p50/p95/p99, stalls, GC pauses, upload spikes, memory peaks and 30-minute driving loop stability.
5. Tune software render model and triangle budgets against worst-case *dense downtown* sightlines, not only sampled existing spawn positions. Never optimize by removing authoritative collision or silently reducing gameplay actors.

## Proof contract

A `PASS` above means source code compiles and specific checks pass on the named CI runners. It does **not** mean that the entire island is streamed, that terrain geometry is hardware-instanced, or that the game is production-ready for 24/7 public multiplayer. The CI artifacts provide metric CSVs and actual rendered screenshots for inspection.
