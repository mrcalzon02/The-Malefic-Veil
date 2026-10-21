# Kenney Retro Urban Kit 2.0 — curated project assets

**Source:** Google Drive archive `kenney_retro-urban-kit.zip`, ID `1T5k4KkqwWJ84rpl1WnMSbx5LxgKjpC11`, original `Models/GLB format/*.glb`. The original archive's `License.txt` was read on 2026-10-20: Creative Commons Zero, CC0, personal and commercial use permitted. Source: Kenney (kenney.nl). Attribution appreciated though not required.

**Provenance:** The independently converted Drive file `kenney_road_asphalt_straight.glb` is byte-identical to the exact file in the archive, SHA-256 `33d5a25df63fdbfd696564972326de0dc7bdcf100649b8beb7bcd4dffec19c4e`. Remaining selections come from the same expanded/converted Kenney family; verify their content digest against the matching ZIP entries in the next acceptance gate before treating provenance as finalized.

**Selected distributable CC0 meshes:**
- `road-asphalt-straight.glb` — Drive 1uZijCbSepMkIlKL3orUgmUtkgocO02Up
- `detail-barrier-type-a.glb` — Drive 1QmVqIjabITMXXh9rdcwSVsy_TZOOpYS5
- `detail-bench.glb` — Drive 1PZg-6x8Bp6StvXNrFS1UQzJQWtolsRzZ
- `detail-dumpster-closed.glb` — Drive 1lMMOnVkTDB1Tk-mxHv4SzCLNLisqR6Fp
- `detail-light-single.glb` — Drive 1ntuMj886up94iPLHvAQuSexFWQsD7qz8

**Runtime:** The `GlbMesh` Java 17 GLB parser consumes source vertex positions, triangle indices, and simple material references. `PropRenderer` projects 3D mesh geometry over the raycaster, without UV texture sampling/skinning. `UrbanScene` places seeded street props. The JAR packages only five curated CC0 GLBs. No paid PSX Mega Pack binary is bundled.

**Open visual gates:** source image textures, per-pixel occlusion and lighting, physics collision, correct road tile transforms, full building geometry, approved character animations and side-by-side reference screenshots. Do not represent this initial geometry pass as complete textured integration.
