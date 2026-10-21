# Kenney Retro Urban Kit 2.0 — curated project assets

**Source:** Google Drive archive `kenney_retro-urban-kit.zip`, ID `1T5k4KkqwWJ84rpl1WnMSbx5LxgKjpC11`, original `Models/GLB format/*.glb`. The original archive's `License.txt` was read on 2026-10-20: Creative Commons Zero, CC0, personal and commercial use permitted. Source: Kenney (kenney.nl). Attribution appreciated though not required.

**Provenance:** The independently converted Drive file `kenney_road_asphalt_straight.glb` is byte-identical to the exact file in the archive, SHA-256 `33d5a25df63fdbfd696564972326de0dc7bdcf100649b8beb7bcd4dffec19c4e`. Each remaining selected GLB has now been compared byte-for-byte against its matching `Models/GLB format/` entry in the archived source ZIP. All five are **exact matches** with SHA-256 values below.

**Selected distributable CC0 meshes:**
- `road-asphalt-straight.glb` — Drive 1uZijCbSepMkIlKL3orUgmUtkgocO02Up
- `detail-barrier-type-a.glb` — Drive 1QmVqIjabITMXXh9rdcwSVsy_TZOOpYS5
- `detail-bench.glb` — Drive 1PZg-6x8Bp6StvXNrFS1UQzJQWtolsRzZ
- `detail-dumpster-closed.glb` — Drive 1lMMOnVkTDB1Tk-mxHv4SzCLNLisqR6Fp
- `detail-light-single.glb` — Drive 1ntuMj886up94iPLHvAQuSexFWQsD7qz8

**Runtime:** The Java 17 `KenneyAssetPack` reader opens the committed complete CC0 ZIP (124 source GLBs and 22 source PNGs). `GlbMesh` consumes source vertex positions, triangle indices, material references and source UV coordinates. Material color for a triangle comes from its **nearest PNG texel at UV centroid**, not a fully rasterized texture. `UrbanScene` places 42 curated families as deterministic props. The JAR contains the source ZIP and the five original individually curated GLBs. No purchased PSX Mega Pack binary is bundled.

**Open visual gates:** per-pixel texture interpolation, depth-buffer occlusion and lighting, geometric physics collision, correct road tile transforms, full building geometry, approved character rigs/animations and side-by-side reference screenshots. Do not represent this initial geometry pass as complete textured integration.

## ZIP-vs-Drive source hashes (verified 2026-10-20)

| Mesh | SHA-256 | Match |
|---|---|---|
| `road-asphalt-straight.glb` | `33d5a25df63fdbfd696564972326de0dc7bdcf100649b8beb7bcd4dffec19c4e` | byte-for-byte |
| `detail-barrier-type-a.glb` | `8ad30f159654cccd2cb176b84852275c375a2d4cb868b6969b184474d3d765ef` | byte-for-byte |
| `detail-bench.glb` | `ccf6f0a95b04db1720c9a7040404ca0d676a0f850815aee90de5edc33b96c47c` | byte-for-byte |
| `detail-dumpster-closed.glb` | `c2b54e446c95fa413929793f3d89cb0176077fa29e4a449dc03cada558bd07d9` | byte-for-byte |
| `detail-light-single.glb` | `aac24987fa7651f8e892f4e661b3bb67c6866edb726e0ab092e9c747f0a69151` | byte-for-byte |
