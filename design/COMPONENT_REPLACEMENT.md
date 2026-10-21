# Block first, replace by role

The 32-weapon roster is in [ARSENAL.md](ARSENAL.md). Primitive models establish the silhouette and hand positions before any further detailed kitbash work. They are not final textured weapons or new playable combat modes.

## Reusable replacement library

These are **candidates**, not automatic accepted fits. All part IDs resolve through `reports/mech_inventory.json` to the user's actual source files. Source parts remain full detail. The current Vesper source prototype is a separate experiment, not the canonical blockout.

| Primitive role | Source candidates | Reuse / fitting rule |
|---|---|---|
| plated receiver | G058 / Body67, G107 / Body3, G159 / Body206 | use a suitable receiver region; cut away unused mounting geometry; retain original panel detail |
| tubular power housing | G131 / Body253, G138, G141 | orient original cylinder axis onto muzzle axis, then scale uniformly |
| skeletal accelerator housing | G198, G238 / Body107 | orient long axis along the rail block; use a separate lower grip |
| grip | G132 / Body242, G144 / Body222 | trim mounting flange and test hand clearance against trigger block |
| barrel sleeve | M025 / Body87, G169 / Body185 | repeat sections end-to-end for length; never stretch a short fitting into a long barrel |
| muzzle/nozzle | M019 / Body185 | use real bore axis and verify from muzzle and side views |
| collar / field ring | G013 / Object200, G003 | original ring normal is Y: rotate about Z by 90 degrees to face X; never rotate blindly about Y |
| cylinder, drum, reactor | M031, G016, G135 | use as functional ammo/energy assembly, with its own removal or rotation pivot |
| electrode / support arm | G238, G085 / Body33 | actual structural parts replace prong or brace solids |
| cooling system | G046 / Body80, G051 | secondary details only after main body, grip, barrel and magazine are sourced |
| trigger / thin guard | purpose-built simple connective geometry | use ergonomic clearance; do not force an unrelated oversized mech bracket into this role |

## Fitting contract

1. Review all blockout silhouettes together. Keep side views as well as angled views. Check trigger reach, grip rake, support-hand room, ammunition removal path, moving-part sweep and shoulder contact.
2. Select a small source module set per family: receiver, grip, barrel, magazine/drum and mechanism. Reuse the fit across related weapons; create variants by the established blockout arrangement.
3. Replace a complete functional volume, not a few cosmetic bolts. Keep each source part's proportions unless a deliberate, visually checked edit calls for changing them.
4. Use connected collars, posts, saddles, hinges or brackets at every interface. Named parenting alone does not make geometry touch. Suspended anomaly cores are an explicit exception; the supporting field rings still need mounts.
5. Check the result in side, muzzle, rear and first-person views. Use the attachment graph to catch candidates; it checks bounding volumes and cannot prove exact surface contact.
6. Keep high-detail assemblies and separate articulation groups. Only after shape approval: UVs and materials, simple rigid fire/reload clips, then LODs. Bake normal/AO/detail maps before removing fine geometry. Do not blindly decimate the details used to justify the kitbash.

## Current artifact boundary

`assets/game/arsenal-blockouts/` contains original primitive geometry and review sheets, safe to commit to this public project. The master `.blend` preserves 32 named collections, replacement roles, movement pivots and sockets. Hierarchical `.glb` files preserve those groups; `-static.glb` files bake transforms for the existing Java static reader. The actual game still has its existing two weapon behaviors.

Detailed supplied geometry and source-derived prototype files remain in ignored `assets/private/` and `assets/runtime/` under the repository's existing asset policy. Original source files in Documents/Models remain untouched. The earlier distorted batch and mostly newly modeled hero have been rejected and isolated locally; they are not counted as delivered arsenal assets.
