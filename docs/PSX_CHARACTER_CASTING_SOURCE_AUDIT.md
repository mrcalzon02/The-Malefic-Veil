# Characters PSX 1.2 — verified source casting pass
Source: connected Google Drive `Characters_psx_1.2.zip`, file ID `1Dd6RWVvV1qUGT8G0Q1rATlHYSn3doXgU`, read directly 21 October 2026.

**Distinguish three facts:** the FBX **file exists** (verified against the ZIP), the file **looks right** (NOT YET verified by 3D render), and the file **can legally and technically ship** (NOT YET verified). Nothing in this document claims conversion, rig function, rights clearance, game deployment, or a canonical finished cast.

## Source-backed initial casting shortlist

All names below are literal filenames inside `Characters_psx_1.2/Characters_psx_02/Models/No-Rig/`; rigged counterparts are present in the archive category, but must be matched, skeleton-tested and visually rendered before promotion. The assignment is a **provisional casting target**, not a visual approval.

| Faction | Class | Exact available source path suffix | Why inspect |
|---|---|---|---|
| Unraveled | Warden | `Male/Character_17_Police.fbx` | Law-enforcement uniform lends to survivor defense |
| Unraveled | Stitcher | `Female/Character_23_Female_Doctor.fbx` | Doctor silhouette and medical identity |
| Unraveled | Hollow | `Male/Character_31.fbx` | Anonymous civilian base; assess garments/hood requirement |
| Penitent | Confessor | `Killer_M/Character_Killer_03.fbx` | Horror costume candidate; must inspect visual suitability |
| Penitent | Flagellant | `Killer_M/Character_Killer_06.fbx` | Horror costume candidate, not necessarily a cultist |
| Penitent | Votary | `Killer_M/Character_Killer_09.fbx` | Horror costume candidate; may require authorized custom garment |
| Begotten | Harrower | `Killer_M/Character_Monster_02.fbx` | Explicit monster model filename |
| Begotten | Devourer | `Killer_M/Character_Monster_05.fbx` | Explicit monster model filename |
| Begotten | Mourner | `Killer_M/Character_Monster_08.fbx` | Explicit monster model filename |

**Alternate actual human sources to review:** `Female/Character_17_Female_Police.fbx`, `Female/Character_21_Female_Firefighter.fbx`, `Male/Character_23_Firefighter.fbx`, `Male/Character_25_Doctor.fbx`, `Male/Character_Sheriff_N.fbx`. **Alternate monster sources:** `Killer_M/Character_Monster.fbx`, `Killer_M/Character_Monster_01.fbx` through `Character_Monster_08.fbx`. Do not promise that the Penitent can be populated convincingly from the killer catalog without visual inspection; they may require licensed custom remeshing or entirely separate owned models.

## Actual source hierarchy and PSX delivery gates

The archive contains `Models/No-Rig/Female`, `Models/No-Rig/Male`, `Models/No-Rig/Killer_M` and corresponding rigged categories, plus `Textures/*.png`. Asset catalog counts from prior audit: 138 model families (59 male, 58 female, 21 killer), mirrored rigged and unrigged and 136 PNG textures. The exact source paths above were independently checked from the original ZIP, unlike prior conceptual lineup artwork.

1. Render **actual model contact sheets** from the FBX meshes with their PNG textures, flat PSX lighting, front/side/back camera and 320×200 thumbnail. Do **not** use image-generated fictional replacement cast as proof.
2. Evaluate silhouettes, faces, texture availability, scale and recognizability for all nine candidates, then promote or reject individually.
3. Locate archive creator/license and confirm commercial runtime redistribution and derivative rights. Package only permitted converted assets; retain purchased originals privately.
4. Convert selected rigged FBX through approved Blender/headless FBX pipeline. Preserve source-to-runtime hashes, transforms, animation verification and LOD budgets; fallback to low-poly static meshes when animation is unverified.
5. Bind approved runtime `meshId` to `CharacterRoster` and validate with real game screenshots, remote multiplayer appearance sync and server-owned faction ID.
6. Commit a real in-game PSX selection screen; no speculative high-fantasy marketing artwork, no lavish painted concept renders substituting for actual 3D assets.

**Visual target:** jagged silhouettes, visibly low triangle counts, compact/dithered textures, hard vertex shading, fog-limited visibility, dark environmental readability. For HUD icons prefer 16×16–64×64 single-color insignia; no ornamental modern fantasy scrollwork.
