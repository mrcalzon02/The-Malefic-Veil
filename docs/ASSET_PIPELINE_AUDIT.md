# The Malefic Veil — Asset Pipeline Audit and Release Gates
**Updated: 20 October 2026. Source of truth: actual Java runtime, GitHub repository, user-approved shared Google Drive archives.**

## Executive assessment

The game originally held **633 Drive references**, but referenced files are not rendered assets. The build now includes a **licensed CC0 city kit with 124 GLBs and 22 PNG source textures**, 42 urban model families selected for procedural placement, and **18 distinctly modeled original PSX gameplay assets**. The runtime uses the real GLB vertices, indexed triangles and texture-image data. All four playable classes and nearby enemies, pickups and constructed devices are rendered as low-poly meshes instead of billboard rectangles; the actual handgun and shotgun meshes replace the flat first-person weapon placeholders. The multiplayer protocol now sends remote class identity alongside player position and authoritative visibility alpha.

**This is not the final rigged-character integration.** The character archive is cataloged for rights/format assessment only; no third-party character FBX mesh, animation, or purchased-archive original is distributed in the public project.

## Actual asset classes (implementation versus gate)

| Family | Verified source | Integrated now | Still required |
|---|---|---|---|
| Urban buildings, streets and furniture | [Kenney Retro Urban Kit 2.0](https://drive.google.com/file/d/1T5k4KkqwWJ84rpl1WnMSbx5LxgKjpC11/view): license file states CC0; 124 source GLBs / 22 textures | 42 selected model IDs in deterministic world layout; 124 GLBs parse as real triangle meshes; source PNG colors sampled at triangle UV centroids; matching colliders use explicitly authored radii | road tile grammar, facade attachment, floors/interiors, geometric collision bounds, per-fragment perspective-correct textures, draw-depth buffer, stress profiling |
| Player classes | [Characters PSX 1.2](https://drive.google.com/file/d/1Dd6RWVvV1qUGT8G0Q1rATlHYSn3doXgU/view): 276 FBX total (138 rig / 138 no-rig); 136 PNG | Four **project-authored interim** meshes (Warden, Medic, Shade, Occultist), distinct armor/medical/hood/occult silhouettes; networked class ID and character alpha | archive redistribution/license verification, offline FBX→GLB skinned conversion, idle/run/crouch/fire/reload/death/revive animations, inverse bind matrices, per-bone pose blending, hitbox/animation synchronization |
| Hostiles | Source character/monster libraries in archived Drive, licensing varied | One original low-poly red-eyed humanoid NPC mesh replacing flat enemy billboard | multiple species, animation state machine, hit reactions, LOD and animation blending |
| Handgun and shotgun | Game's existing server weapon rules, plus authored 3D assets | Two original fully triangulated PSX gun models, shown as perspective first-person viewmodels; world visual IDs supplied | muzzle/ejection/slide/pump/reload animations, separate world-drop meshes, magazine props, muzzle-light/test coverage |
| Healing/sanity/ammo/money/supplies/medkit pickups | Existing seven authoritative pickup kinds | Seven original differentiated world-space 3D pickup meshes linked to actual kind IDs | timed special power-up families (shield/sprint/veil) require explicit authoritative behavior and distinct UI durations; no fake pickups |
| Turret, healing, revival and barricade | Four existing host-owned construction kinds | Four distinct original 3D devices matched to the authoritative device indices | animated states, damaged meshes, repair state, physical part-by-part assembly, placement silhouette vs authoritative collider parity |
| Purchased PSX Mega Pack | [Private Drive master folder](https://drive.google.com/drive/folders/1a_Shb7jPYuD38gBXrHS2bSlQV-gSiTLr) and inherited Beckenburg provenance | Source **references only** | selective licensed conversion and distributable derivative review; raw originals remain private |

## Kenney source archive verification

The complete original ZIP (`assets/game/kenney/retro-urban-kit-2.0.zip`) is bundled as an approximately 2.16 MB **CC0 source archive** to make the runtime independent from Drive. The Java `KenneyAssetPack` loader whitelists 124 GLBs and 22 PNGs from `Models/GLB format/`, checks for duplicate entries and oversized content, and rejects mismatched counts. Source GLBs, not rewritten placeholders, are parsed by `GlbMesh`. The original five individually imported GLBs remain for provenance and compatibility.

`UrbanScene` resolves only model IDs that `World` actually places; the full set remains available for additional lots, street pieces, roofs, walls, trees, damaged structures, trucks and furniture. Models still use simplified authored placement radii, so *visual surface triangles and competitive physics are not yet equal to each other*.

**Material rendering precision:** Source texture coordinates (TEXCOORD_0) now select **one nearest source pixel at each triangle's UV centroid**, using source PNGs and material color factors. This provides recognizable source texture coloring while preserving chunky PSX faces. It is **not** full per-pixel UV rasterization, lightmapping, normal mapping or a complete glTF node/animation implementation.

## Character source audit and safe conversion path

Directly inspected `Characters_psx_1.2.zip` from Drive:
- 424 ZIP entries, including directories; **138 rigged FBX** and **138 unrigged FBX**, paired model families. Of each rig condition there are 59 male, 58 female and 21 killer models.
- **136 PNG** textures and no embedded plain-text/readme license statement observed. Sample rigged FBX files are binary Kaydara FBX with `Deformer`, `Skin`, `Cluster` and `AnimationStack` markers. These markers are **not evidence of functional animation clips** in the finished game.
- The Java GLB parser handles **static positions, indices, material texture references** but not inverse bind matrices, joints/weights, skeleton traversal or interpolated animation channels. Adding the raw FBX files to the JAR would do nothing useful and could violate redistribution terms.

**Required import pipeline:** (1) identify vendor and commercial redistribution/derivative rights; (2) select one rigged hero source at a time; (3) convert through approved offline FBX→GLB path (e.g., Blender) while preserving legal provenance, texture references and bone names; (4) establish a validated skeleton and animation clip manifest; (5) implement skinning and character action mapping in the Java renderer or a specialized runtime; (6) host-authoritative stance/hitbox/state replication; (7) screenshot and two-client animation QA; (8) release only permitted derived runtime assets. Remain with project-authored interim models until all eight steps pass.

## Gameplay representation and engineering limits

`GameplayMeshes` produces original triangulated low-poly geometry (4 distinct heroes + 1 hostile + 2 weapons + 7 pickups + 4 devices = **18 mesh IDs**). `PropRenderer` takes both static source-model and gameplay-model instances, transforms their triangles into the software camera and applies per-instance alpha. Enemy/player hit registration remains host controlled; visuals must not become a client-authoritative hitbox. `WeaponViewRenderer` projects the same authored weapon geometry into first person. Remote class identity is sent in **LAN protocol v3**, so Shade's actual character mesh receives Shade's host-computed opacity.

**Remaining renderer gaps:** near-plane triangle clipping, per-pixel Z testing, fully correct glass/translucency order, correct glTF scene-node transforms/rotation, true texture UV interpolation and bilinear/nearest sampling options, authored animation states and performance budgets for hundreds of props, NPCs and other players. Do not mark “finished PSX render pipeline” until these are tested.

## Acceptance gates

**A1 — Source/license (passed for Kenney only).** Original ZIP license verified CC0, all 124 GLBs and 22 PNGs present. Five previously curated individual GLBs match the archived original hashes. Do not generalize the CC0 license to any other archive.

**A2 — Geometry/packaging (automated).** Java 17 parser loads and verifies all 124 GLBs with source materials; 42 selected model types can appear in deterministic worlds; valid source resources resolve inside a JAR without Drive. Source artifacts, actual shader output and malformed-GLB rejection are checked in CI.

**A3 — Gameplay mesh assignment (automated).** 18 authored game meshes have nonzero bounds and indexed triangles; 25% stealth alpha is visibly different from 100% and 0%; class identity matches server state; real weapon polygons are visible in screenshot; pickups match gameplay kind IDs.

**A4 — Visual correctness (not yet passed).** Screenshot-to-3D source comparison, UV/pixel accuracy, no mesh through walls/cover, correct dimension/rotation/floor attachment, no clipping or missing surface under near-camera traversal, 1%-low FPS under maximum props.

**A5 — Rig/animation (blocked on private asset license and conversion).** Four playable characters with source skeletons, ≥6 working actions per character, server-synchronized look/stance/fire/death, no incorrectly deformed bones, peer visibility/opacity tests.

**A6 — Physical multiplayer playtest (not yet passed).** Two real machines on network, class/weapon item swaps, PvP visibility contrast/occlusion, door/prop collision, medkit/healing/turret/revive interaction, >30 minutes and bug logs with evidence.

**Next execution order:** finish A4 texture-depth and collision parity; source/rights check for rigged characters; develop one full animation pipeline and asset validation gate; expand road and building layouts with the newly available CC0 model IDs. No deployment release tag based solely on passing a model-count test.
