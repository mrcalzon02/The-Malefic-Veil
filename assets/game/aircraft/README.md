# The Malefic Veil — Original PSX Air Power Geometry

Nine original **actual static glTF 2.0 binary GLB models** (three per faction), built as individually selectable mesh assets for the game. These are generated geometry, not planes cut out of the concept images, not purchased kit assets, and not declared flyable game entities.

**Visual intent from the approved 2026-10-21 concept work:**

- **Unraveled:** Needlewing (civilian pusher-wing / A-10-style aggressive nose), Harrier (refitted multipurpose improvised attack craft), Leviathan (slow armored salvage freight gunship).
- **Penitent:** Shrike (tractor/dozer-cab light flying chipper), Censer (combine-harvester rig with pipes, threshing intakes and hostile propulsion), Cathedral (working legacy model ID; visually an immense levitating combine/industrial abattoir, **NOT** a gothic cathedral, white cross aircraft or church-shaped fighter). Outboard wing spinning-death-wheel trope intentionally avoided. The centerline engine inlets are serrated shredders.
- **Begotten:** Gnat (hunched mange-infested bat, crude back harness, two leg-replacement machine guns), Mantis (four-legged vulture/lion with ragged wing membranes, hooked beak, long talons), Behemoth (huge skinless whale with shark-tooth maw, exactly two pairs of massive dragon wings and grotesque hanging sacs).

All models face negative **Z** in glTF's X/right, Y/up, Z/back coordinate system. Coordinates are baked: there are no per-node transforms that a limited glTF reader could ignore. Models have flat color material swatches and a **128x128 embedded nearest-neighbor mottled noise tile** for PSX appearance; the Java2D renderer will display flat base material colors until per-fragment texture rendering is available. The three `*_palette_128.png` images are editable palette/reference textures, not substitutes for geometry.

`aircraft_manifest.json` includes model names, faction, triangle counts, file sizes, SHA-256 and AABBs. The aircraft are neither rigged nor animated; engines, mouths, wings, fins, hardpoints and landing wheels are part of the current single static mesh. Follow-on passes need named animation submeshes/pivots, physical shape colliders, LOD, materials and UV tuning, sound/FX, and flight physics before game actors can fly them. Existing city miasma, anti-air counterplay, and 50-level progression remain independent gameplay requirements.

Rebuild deterministically with `python tools/build_malefic_aircraft.py` (Python 3, NumPy, Pillow and trimesh used only for build/validation). Run Java verification from repo root via `java -cp out veil.AirPowerMeshesTest` after compiling source. `AirPowerMeshes.load(Path.of("."))` checks all nine SHA hashes, GLB parser acceptance and bounding boxes. The `GameplayMeshes` authoring registry can load these models under their IDs for later instantiation; do not conflate presence in the catalog with working aircraft combat or AI.
