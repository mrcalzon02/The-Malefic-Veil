# Arsenal checkpoint — 21 October 2026

**Historical blockout checkpoint.** The subsequent functional run now uses these primitives as working textured/animated weapons. See [implementation status and controls](ARSENAL_IMPLEMENTATION.md) and the `assets/game/arsenal/` working assets. The notes below describe the earlier blockout review stage.

## Ready for review

- [32-weapon roster and primary/alternate fire proposals](../design/ARSENAL.md).
- [Blockout review 1: sidearms, rifles, shotguns and rotary gun](../assets/game/arsenal-blockouts/arsenal-review-1.png).
- [Blockout review 2: heavy rotary, laser, rail, plasma and electric](../assets/game/arsenal-blockouts/arsenal-review-2.png).
- [Blockout review 3: orb, arc rotary, missiles, grenades and artillery](../assets/game/arsenal-blockouts/arsenal-review-3.png).
- [Blockout review 4: flak, disc, bounce, scrap, bio, cryo and anomaly](../assets/game/arsenal-blockouts/arsenal-review-4.png).
- [All 32 together](../assets/game/arsenal-blockouts/arsenal-overview.png).
- [Editable Blender master](../assets/game/arsenal-blockouts/arsenal-blockouts.blend), with named collections, primitive replacement roles, articulation pivots and sockets.
- 32 hierarchical GLBs and 32 static transform-baked GLBs in the same folder. The static companions are compatible with the existing game's static mesh reader.

Review sheets automatically frame each weapon; they do not depict all weapons at the same scale. The master uses metres and preserves size relationships. Grey = receiver; light grey = mechanism; dark = hand-contact volume; ochre = ammo volume. Colored emitters identify families but are not completed firing effects.

## Workflow correction

The first envelope-scaled assemblies were rejected for distortion. A subsequent mostly newly modeled weapon did not satisfy the request to assemble the supplied parts. A full-detail sourced Vesper experiment followed, but its ergonomic placement and trigger bracket needed revision. None of these attempts count as a finished arsenal.

The active workflow is now **all primitive silhouettes first, then reusable source-module replacement**. The 32 blockouts contain no purchased geometry. The source experiment uses actual Body67 receiver geometry, Body242 grip geometry and full-detail machined barrel components; its meshes stay in ignored local output. No decimation is used in that source experiment.

## Attachment checks

Stocks must reach receivers; sights need mounts; feed drums need feed bridges; shoulder rests need saddles; carry handles need posts; artillery braces need a crossbar; field rings need a supporting spine. The generator records a conservative connected-volume check at a 12 mm proximity threshold in `reports/arsenal_blockouts.json`. It catches detached bounding volumes, but cannot establish exact surface contact, physical fit or animation clearance. Suspended anomaly cores are intentional, explicitly listed exceptions. Side-view renders accompany the angled views locally.

## Remaining work

Review proportions and hand clearance, replace major primitive roles with the supplied modules, then apply materials/textures and rigid animation. Preserve high detail, bake detail maps before simplification and keep moving seams separate. Authoritative projectile behavior, chain/bounce/area rules, alternate-fire input, damage balance, inventory entries, timed power-ups, and multiplayer replication are subsequent implementation work. This checkpoint does not claim those systems exist.

## Rebuild

With Blender 5 installed, run `tools/blockout_arsenal.py` through Blender in background mode. It creates the original blockouts and local angled/side renders. `tools/compose_blockouts.py` uses Pillow to compose the sheets. `tools/validate_blockouts.py` validates the exported hierarchy and the connection report. Compile Java 17 sources and run `veil.ArsenalBlockoutTest` to check all 32 static files through the actual game importer.

Detailed source intake is separate: `tools/catalog_mech.py` reads the local model library and creates the ignored normalized-parts cache. `tools/build_source_prototype.py` creates the local full-detail sourced experiment. Neither path modifies the originals or places purchased meshes in the public source tree.
