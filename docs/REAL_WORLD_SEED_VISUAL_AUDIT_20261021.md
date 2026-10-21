# Real GPU world seed visual audit — 21 October 2026

**Evidence:** GitHub Actions GPU workflow [run 39634154771](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39634154771), commit `cf235abc0dbcdf601fd21c022cd6f163327e844a`. Windows JDK 17 build and Linux JDK 17/Mesa OpenGL 3.3 rendering both passed. Download the Linux `malefic-gpu-ubuntu-latest-jdk17` artifact for the actual original captured PNG pixels and per-seed stdout logs. No generative image process contributed to these captures.

## Reproducible gallery

`.github/workflows/gpu-client.yml` runs `MaleficVeil-GPU.jar --gallery --seed=20261020`, `--seed=42`, and `--seed=887766`. Each creates six **real 320×200 OpenGL frame captures** in `build/test-visuals/gallery-seed-<seed>-*.png`: city street (noon), seeded road intersection (16:30), city roofs at 43 units (18:12), city overhead at 115 units (noon), woodland/terrain boundary at 38 units (10:00), and night city at 40 units (midnight). Logs supply the actual camera coordinates, terrain chunk counts, mesh instance counts and draw calls. All 18 captures are required and included in the GitHub artifact. Initial run 39633211637 exposed clipped aerial views; corrected camera far-plane in commit `eaa8013a` and corrected altitude/fog range in `cf235abc`.

**Important boundary:** This is one playable **132×132 city sector** and its render-only continent vista, regenerated under three distinct seeds. The *entire* multi-city continent is not a continuous playable world yet. These screenshots cannot be presented as proof of true intercity gameplay or whole-island geography.

## Verified defects in current images

1. **GALLERY-WORLD-001 — conspicuous city/continent seam (critical):** High aerial captures have a pale rectangular/square-looking terrain gap/mismatch adjoining the modeled city. The ground collision city footprint and surrounding `ContinentVista` patches are not rendered with a visually continuous material or elevation transition. Must connect city-edge height, material and texture sampling to surrounding landscape; no exposure of empty/flat fog skirt from above.
2. **GALLERY-WORLD-002 — circular city visibility boundary (critical):** Overhead city covers only a disc/arc inside a larger square visual boundary. The neighborhood geometry is clipped by the `UrbanScene.nearby` fixed 100-unit lookup, `MAX_VISIBLE_MODELS=10000` cap, or both. Elevated camera requires 3D frustum/sector coverage and coherent LOD, not the ground-player's XY radius nor arbitrary first 10,000 transforms. Prove complete city overhead with no visible draw-radius silhouette.
3. **GALLERY-WORLD-003 — monotonous facade massing (major):** Street level repeats near-identical Kenney wall/roof modules in many rectangular blocks. Build deterministic district-specific lot composition, varied setbacks, roofs, floor counts, corner treatments and density *from real licensed kit parts*. Preserve city-wide construction protection and actual collision/entrance rules.
4. **GALLERY-WORLD-004 — road-to-wilderness transition (major):** Terrain beyond city appears as basic colored undulating mesh with scattered Kenney foliage and hard road/ground transitions. Biome terrain visuals need patch material atlas or source texture, coastal and wilderness transition bands, and grounded silhouettes. Recheck native 320×200 detail readability.
5. **GALLERY-WORLD-005 — midnight readability (major):** Night aerial is extremely dark outside immediate local light pools. Add controllable ambient fog/light floor, distance-appropriate lights with aggressive budget, luminous point cues and accessible brightness control without erasing the horror identity. Prove footage with real PSX UI and readable enemy silhouettes.
6. **GALLERY-WORLD-006 — performance/visual budget cliff (major):** 10,000 visible-instance hard cap is reached in most downtown/aerial frames, not a true spatial LOD solution. Replace source-order cap with camera-frustum-aware screen-size prioritization, occlusion, sector mesh batching and LOD; collect p95/p99 real rendered frame times at the worst elevated views.

## Release gates

- A seeded 6-shot gallery must be produced *from actual OpenGL framebuffer* for 3 distinct worlds on every relevant world/rendering release gate; source seed and camera transforms saved with screenshots.
- Aerial image must show the actual city footprint **and its surroundings with no blank square and no circular clip boundary**. Overhead camera height must not automatically consume fog distance.
- Street view must demonstrate Kenney-based varied lots and recognizable road junctions and walkable doors, with no screenshot-only fake geometry.
- Live server mask is unchanged: **all city lots and generated city structures are immutable; players may build only on future playable wilderness**.
- User-visible presentation must always distinguish actual GitHub Actions screenshot artifacts from AI-generated visual references.

This audit is visual evidence and problem classification, not a claim the six defects have been repaired.
