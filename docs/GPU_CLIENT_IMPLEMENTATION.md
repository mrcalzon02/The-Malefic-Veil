# Optional libGDX/OpenGL 3.3 GPU client — phase G1

The Malefic Veil now has a **separate opt-in libGDX 1.14.2 + LWJGL3 desktop client** in `gpu-client/`. This is a real OpenGL 3.3 rendering backend for the seeded **Kenney Retro Urban Kit** city. It is NOT an implementation of the entire existing multiplayer gameplay UI. `LanServer` and the original `src/main/java/veil/MaleficVeil.java` remain independent and continue building with JDK 17 without libGDX.

## How it draws

1. Initialize the **existing** seeded `World`, `CityLayout`, `UrbanTileAssembler` and `UrbanScene`—no second/random substitute world.
2. Load the existing CC0 `kenney/retro-urban-kit-2.0.zip` via the proven `KenneyAssetPack` and `GlbMesh` source parsers. Source RGB per triangle uses actual source texture sampling already supported by that parser.
3. Convert each distinct placed GLB source mesh into one GPU vertex buffer with vertex position and source face color. Each model is **uploaded only once**.
4. Use `UrbanScene.nearby` for spatial filtering and a coarse camera-direction test; group visible transforms by mesh ID. Repack each group into a compact instance array `[x, y, yaw, scale, alpha, zOffset]`.
5. `Mesh.enableInstancedRendering()` and `Mesh.render()` issue **one actual `glDrawArraysInstanced` call per visible mesh type** through libGDX's GL30 mesh path. Upload transforms as per-instance vertex attributes. Hardware depth buffering naturally occludes 3D tile modules, replacing the legacy wall-texture overlay in this preview.
6. Draw into a GPU framebuffer at **320×200** with nearest-neighbor upscale, PSX fog and a compact diagnostic overlay. No modern high-poly replacements are generated.

Controls: WASD movement, arrow keys turn, Left Shift sprint, plus/minus adjust the visual range (8–80 world units), Escape exit. Movement respects the existing `World.canWalk` collider; it is a **local walkthrough**, not authoritative network movement. 60 FPS is a GPU presentation target; gameplay fidelity and stability still require measurement on actual player hardware.

## Build and test

Maven 3.9+ and JDK 17+ are required to build; Java 17 bytecode is used. The native platform libraries are resolved transitively by the LWJGL3 backend. No manual OpenGL/JNI implementation or new custom rendering engine is required.

From the repository root:

```bash
mvn -B -ntp -f gpu-client/pom.xml -DskipTests package
java -jar gpu-client/target/MaleficVeil-GPU.jar
```

A self-contained shaded JAR is produced; it includes the CC0 Kenney source ZIP plus libGDX dependencies. The existing Java2D launcher and dedicated host use their previous paths unchanged. On Linux, an OpenGL 3.3 capable driver (Mesa hardware/software path) and a graphical display are required. To capture automated proof:

```bash
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a java -jar gpu-client/target/MaleficVeil-GPU.jar --smoke
```

The smoke run captures a **real OpenGL framebuffer** to `build/test-visuals/gpu-kenney-city.png`; if a usable image, instanced meshes and multiple draw groups cannot be produced, the process fails. CI at `.github/workflows/gpu-client.yml` builds Windows and Linux JARs and runs the Mesa/Xvfb smoke on Ubuntu. The artifact includes the JAR and screenshot. Keep failing workflow conclusions visible; never describe an unverified smoke as success.

## Non-goals / unfinished gates

- No replicated player characters, multiplayer authentication, authoritative combat, weapons, or HUD interactions yet; those remain in the existing client and server. Do not replace existing `play.bat`/`play.sh` defaults until those features migrate.
- Static Kenney source triangles are instanced. Animated character meshes, dynamic lights, multiple LOD mesh geometries, world streaming, and material-texture GPU atlas uploads are later work.
- Kenney models use existing per-triangle RGB source-pixel UV sampling, not full texture mapping. PSX-style pixelated textures need a proper material/UV shader pass.
- Transparent meshes need verified alpha sorting or another suitable transparency strategy. Current source placement uses opaque assets in the first prototype.
- World layout and terrain are still the 132×132 city; the large continent remains read-only macro-sector planning until operational streaming is implemented.
- Full source geometry licensing is CC0 for the bundled Kenney set. libGDX licensing is Apache 2.0; retain notices and verify the final distributable's dependency/license inventory.
- To replace the old wall-shaded raycaster in actual gameplay, migrate gameplay rendering, input binding, UI, and network replicates to this backend, then compare screenshots and PvP collider parity. The GPU client does not silently bypass server checks.

**Proof gates**: G1 Maven compile (Linux/Windows); G2 OpenGL screenshot under Mesa; G3 all Kenney buildings visibly and spatially correct; G4 30-minute traversal benchmark and LOD/hot-load; G5 real two-client network combat parity; G6 preferred launcher switch. G1/G2 are only considered passed after readback of successful GitHub Actions logs and screenshot artifacts.
