# Weapon refinement sweep

Work one weapon at a time: targeted mechanical geometry, exported clay review, shared PBR bake, final exported review, gated promotion and GitHub checkpoint.

Vesper revision 3 is a reviewed **refined prototype**, supplied as an animated GLB, editable Blender scene and 1024 albedo/ORM maps. It is staged under `assets/game/arsenal-refined-approved/`. The current Java `.vmesh` runtime still uses the existing working arsenal; this art pass has not replaced that runtime asset or changed world-spawn/crafting rules. First eight remain world loot; the other 24 remain base/faction crafting.

Vesper fixes: measured +X barrel/bore, raked magazine tunnel with closed rear wall, complete removable magazine, curved trigger within a continuous guard, connected sights, profiled slide, recessed cocking grooves/ejection port, seated fasteners, non-mirrored receiver markings and separated material response. Reload sweeps sample ten poses, not exhaustive hand/weapon collision. Four named component animation clips are retained.

## Efficient review loop

Reuse appropriate measured mechanical components and the material atlas; preserve distinct weapon silhouettes and family-specific loading methods. Do not add a pistol magazine to a revolver or apply generic axis assumptions to rotated source cylinders. Use one clay sheet to catch geometry defects before baking, then one final sheet. Regenerate only when geometry/materials actually change.

`python tools/weapon_review_pipeline.py --id <candidate-id>` runs exported inspection and rendering concurrently, creates a six-view sheet and private comparison, and runs the gate. A matching hash cache skips unchanged expensive work. An art-review hold is not approval; inspect actual images and write a reasoned hash-bound review before promotion. Vesper has additional reload/wall/muzzle checks; other families need their own interface checks.

Rebuild Vesper seed with Blender running `tools/refine_weapon_roughs.py -- --ids w01-vesper --revision v3`, then `tools/refine_vesper.py -- --attempt 3`. Generate stencil sources using a Pillow-enabled Python with `tools/make_weapon_finish_sources.py`, then run the same Blender script with `--attempt 3 --texture`. Build products remain in `build/candidates/` until reviewed. Calibration archives and comparison panels remain private.

## Queue

| Weapon | Current art sweep |
|---|---|
| Vesper | Refined prototype r3, exported review passed |
| Ossuary | Rejected after 3 attempts; coordinated guard/frame redesign required |
| Needle Choir | Refined prototype r3, exported mechanical and art checks passed |
| Rivet Saint | Refined prototype r2, exported mechanical and art checks passed |
| Grave Bell | Refined prototype r3, matched body/breech and exported reload checks passed |
| Cerberus | Refined prototype r1, triple break-action checks passed |
| Reliquary | Refined prototype r3, pump and bottom-loading checks passed |
| Furnace Organ | Refined prototype r3, rotor/drum checks passed |
| Hailstorm | Pending individual mechanical review |
| Cautery | Pending individual mechanical review |
| Prism Writ | Pending individual mechanical review |
| Longwake | Pending individual mechanical review |
| Ion Psalm | Pending individual mechanical review |
| Sun Eater | Pending individual mechanical review |
| Storm Rosary | Pending individual mechanical review |
| Forked Benediction | Pending individual mechanical review |
| Ball Lightning | Pending individual mechanical review |
| Thunder Loom | Pending individual mechanical review |
| Pilgrim | Pending individual mechanical review |
| Widow Choir | Pending individual mechanical review |
| Basilisk | Pending individual mechanical review |
| Censer | Pending individual mechanical review |
| Vigil Mortar | Pending individual mechanical review |
| Cathedral | Pending individual mechanical review |
| Shrapnel Mass | Pending individual mechanical review |
| Ricochet Gospel | Pending individual mechanical review |
| Bankshot | Pending individual mechanical review |
| Scrap Cyclone | Pending individual mechanical review |
| Miasma | Pending individual mechanical review |
| Winter Verdict | Pending individual mechanical review |
| Eventide | Pending individual mechanical review |
| Veilbreaker | Pending individual mechanical review |
