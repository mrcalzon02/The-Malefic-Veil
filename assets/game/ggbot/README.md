# GGBot PSX city cars (CC0)

Source: [GGBotNet — PSX Style Cars](https://ggbot.itch.io/psx-style-cars), authored Blender, OBJ, MTL and original 128×128 PNG texture atlases. This is separate from the Kenney Retro Urban Kit. The existing Kenney trucks and their cargo/vehicle gameplay are intentionally retained.

The **current city** uses GGBot cars as deterministic, collidable **parked civilian vehicles, rare police/taxi vehicles, and wrecks on hazard lots**. They are static world structures, **not** replacements for the six host-controlled, drivable Kenney cargo trucks. This preserves all existing cargo rules, networking and physics.

## Source integrity and release packaging

The existing desktop and GPU build pipelines fetch the original 2023 CC0 source archive from https://opengameart.org/sites/default/files/psx_style_cars_by_ggbot_august2023.zip and verify SHA-256 `db67b0b0fbaa02454a5d000dc9ec1cc53f360e8f41ab44f3fb1c7e8f71e699e4`. Release packaging includes it as `assets/game/ggbot/psx-style-cars.zip` and the corresponding classpath resource. An unverified or changed download fails packaging; game clients do not silently substitute arbitrary textures. The runtime parses the original OBJ face corners and PNG atlases directly, preserving authored UV assignments and separate police/taxi source geometry. No guesswork remapping, global texture projection or recoloring is used.

For local source-tree development, place the same verified ZIP at `assets/game/ggbot/psx-style-cars.zip` before running `java -cp out veil.GGBotCarPackTest`. Without the optional source ZIP, local development builds keep Kenney trucks and omit GGBot decorative cars rather than crash. Published development releases require the archive and verify all 39 appearances, including source-UV rasterization and city placements.

The GPU renderer draws the original source UV atlases with nearest-neighbor sampling. The Java2D gameplay renderer applies the original PNG atlas to each projected car triangle using its preserved UV corner coordinates (affine PSX texturing), instead of sampling one color at the triangle center. Asset and performance tests must approve both paths.

The source artist notes that the source car scales vary. The in-game **scenery-only** car scale is deliberately normalized to 0.52 and uses explicit host collision radii. Exact body-shaped colliders and civilian vehicle drivability would be separate gameplay work, not something imported implicitly with these models.
