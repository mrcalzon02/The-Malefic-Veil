# Product-model audit inventory

This is an inventory, not a product-completion certificate. The deployable audit remains first priority.

Explicit rosters: **21 field deployables** and **32 weapons**. Stored GLBs are counted by checkout and may include alternate versions of the same asset. No model receives approval from its directory name.

| Family | Stored GLBs | Audit state |
|---|---|---|
| aircraft | 9 | Current-product audit pending |
| construction_components | 219 | Current-product audit pending |
| environment_props | 10 | Current-product audit pending |
| permanent_structures | 30 | Current-product audit pending |
| vegetation | 2 | Current-product audit pending |
| weapons | 136 | Current-product audit pending |

## Ordered work

1. Complete deployable geometry, articulation, packing, texture/organic-layer, purpose and runtime audits; repair or retain rejection.
2. Audit all 32 weapon IDs against their actual mechanisms, materials, animations and current renderer bindings; reconcile alternate authored candidates without overwriting them.
3. Audit permanent faction structures and construction components for usable placement, collision, materials and faction/function readability.
4. Audit procedural terrain, vegetation and scenery in actual gameplay scenes, including traversal, scale and visual continuity.
5. Audit remaining actors, pickups, medical/supply props, vehicles and aircraft against the functions implemented by the game.

Private reference archives are excluded. Detailed paths, hashes and GLB metadata are in `product-model-inventory.json`. The main runtime and the separate authoring checkout remain distinct; nothing is silently copied or promoted.
