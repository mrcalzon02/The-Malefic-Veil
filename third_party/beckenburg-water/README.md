# Beckenburg water module

Independent Java 17 module, no renderer/game dependency. `mvn -f water-module/pom.xml package` builds the library. Source and adaptation notices: [LICENSE](LICENSE), [upstream provenance](reference/provenance.json). The project and its other assets retain their own licences.

The MIT [PSX Style Water Surface](https://immaculate-lift-studio.itch.io/psx-style-water-surface) supplies the principles of independently scrolling texture layers, configurable blending, quantized UV samples, and noise-phase sine displacement. The downloaded shader is retained unchanged. Its Godot noise texture is replaced by deterministic coherent Java noise. The demo photograph is excluded; Hollow Creek uses original ripple textures. Source advertising mentions opacity/foam; the downloaded shader does not implement them, so these are original additions here.

`WaterModule` receives immutable world polygons, authored optical bathymetry, styles, dry hull profiles, and optional `WaterEffects`. Coordinates are metres, Y up. Opaque depth is camera-forward distance, not hardware depth or Euclidean range. Composite after opaque geometry and before above-water effects. Preserve camera and clock belonging to the actual completed GPU frame. The lighting callback applies real world and handheld lighting.

Implemented: two independently scrolling/pixelated layers; animated triangular wave surface with bounded first-hit grid traversal; real opaque-depth occlusion; depth absorption/alpha; refracted opaque sampling with foreground rejection; tinted Fresnel reflection; broken shallow foam and a depth-driven 5.5-second shoreline run-up/retreat; transparent bodies sorted far to near; dry actual-hull sections; explicit bed mesh and matching planar depth samples. Old constructors retain flat analytic behaviour for compatibility. Multiple overlapping bodies retain ordered alpha; screen refraction is intentionally disabled when a pixel has multiple water layers.

Hollow Creek adapter: 160mm wave amplitude,0.75m grid, original 64px ripple texture, quantization 128/192, two independent scroll rates, 60mm shore waves plus38 real inner-hull sections spaced 10mm, a new 12,000-triangle optical basin using the dedicated original silt/rounded-stone bed texture. Bathymetry is newly authored from actual bank control lines, not a measured bed. No player terrain/collision is changed. Lake and creek currently share a water style; authored creek-specific flow, interaction ripples, storm/wind presets, true reflected scene geometry, underwater camera transitions and other-map adaptation remain later iterations. The current Fresnel colour is a tuned dark environment tint, not a planar reflection capture.

Budgets: 64 bodies, 2MP target, 2048-point polygons, 2048 wave-cell steps per ray, 200k bed vertices, 1MB hull resource. Nearest-wave reference tests include grazing rays and negative coordinates. Water and boats must be reviewed together from beach, riverbanks and dock before another map is advanced.

Current Hollow Creek turbidity: minimum48%, max97%, absorption1.8/m; near-shore albedo detail remains visible while the deeper basin is obscured. Actual new clips use VP9 WebM with H264 MP4 fallback, explicit playback/time/error controls, and direct media links. The published HTTPS player must be tested through real browser interaction, not merely file metadata.

## Contact/3D-wave refinement

Texture scroll speeds stay unchanged. Hollow Creek now uses160mm base geometric waves with broad swell and shorter chop, a0.75m triangle grid, plus60mm shore run-up. Thirty-eight actual hull sections begin near the physical inner floor atY−2.0849 and end at−1.72; troughs below the cavity floor are occluded by the real hull. A full-cycle cockpit regression found and removed a1.6mm lower-profile gap.

`WaterContact` reconstructs world-space opaque points from the completed scene depth and the same camera. It detects wet solid contacts within0.28m, rejects lifted/distant objects, and uses depth-derived surface normals to reject shallow flat bed surfaces. Separate wave/shore foam handles shallow run-up. This pass covers visible solid geometry; it is not an offscreen world-collider bake. `WaterSpray` adds at most256 stable world-cell emitters and three small ballistic droplets each, with world lighting and real depth rejection. Both report sample counts through `Statistics`. Existing constructors and pure flat-layer behaviour remain compatible.

## Editor integration

Portable placement icon, live inspector, five real footprints, move/resize/rotation handles, multi-selection, straight/curved snapping, atomic documents and compatible seamless joins are included. See [EDITOR.md](EDITOR.md).
