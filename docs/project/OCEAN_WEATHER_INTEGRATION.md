# Ocean and Weather Modules — integration contract

## Source of truth

The original independent MIT-licensed Java 17 water SDK comes from
`mrcalzon02/The-Beckenburg-Project` at commit
`7f1515d03817e771d05604c136cbe2f5ed990d22`.
The 15 unmodified SDK classes live in
`src/main/java/dev/mrcalzon/water/`.
Its license, README and source provenance are retained at
`third_party/beckenburg-water/`.
Changes to this SDK must preserve the original license and be deliberately
reconciled with the upstream source, not silently substitute a game-specific fork.

`VeilOcean` is the Malefic-specific adapter. It makes one bounded
`WaterModule.Body` over the existing 224×224 seeded continent, without
modifying `ContinentLayout` or the server's collision map. The biome mask,
world-meter coordinates and city plates come from `World` / `ContinentVista`.
Only `Biome.WATER` parcels are wet. A local read-only optical shallowing at
real land/water transitions drives imported shoreline foam and run-up;
this does **not** alter server elevation or swimming physics.

`GpuOcean` creates immutable world-space mesh buffers at scene load
from `VeilOcean.sourceSeaParcels`, clipped to the actual source 6-metre water
parcels. The native **vertex shader** evaluates the independent SDK's two-phase
coherent-noise wave shape with host-weather amplitude; its **fragment shader**
reads the two original water texture layers, performs depth absorption, coarse
Fresnel reflection and broken shallow/shoreline foam. It draws depth-tested,
alpha-blended chunks before first-person equipment and HUD. It does **not**
invoke Java `WaterEffects.surface` or `VeilOcean.color` per vertex per frame,
and has **zero per-frame ocean VBO uploads**. Analytic GPU wave interpolation is
not bit-identical to the Java module's piecewise-planar collision/reference ray
mesh, and more fidelity validation remains required.

`VeilWeather` remains the sole Java 17 shared-weather adapter. Dedicated LAN
hosts own the preset/day clock; network clients follow those broadcasts.
`MaleficVeil.GpuFrame` carries that published state, rain, snow and flash
to the native GPU renderer. Storm presets increase the water SDK's actual
wave amplitude; GPU precipitation/flash passes use these same client weather
measurements. No second GPU weather scheduler or untrusted client override.

## Tests

`VeilOceanTest` verifies real land/sea parcel isolation, near-shore optical
depth, animated shared waves, storm response, color, and the original
`WaterModule.composite` producing water pixels from a world camera.

The original independent module test suites
`WaterModuleTest`, `WaterEffectsTest`, `WaterEditorModelTest`,
`WaterShapesSnapTest` are adapted to the `veil` test package and run in the
full host GitHub Actions matrix, along with `WeatherLanTest`. Root host
compilation, GPU desktop, visual regression, multiplayer soak and desktop
packaging include the new SDK source paths.

## Performance and real-ocean GPU diagnostics

The default City A camera contains no visible ocean and cannot establish
ocean frame rate or correctness. The native GPU workflow additionally runs
a real OpenGL **City C coastal** benchmark for seed `887766`, requiring a
nonempty source-ocean mesh, nonzero drawn triangles, and zero frame-by-frame
water vertex upload bytes. The diagnostic holds the identical actual camera
for two 180-frame phases: the city/terrain with ocean draws disabled, then the
same scene with GPU ocean enabled, excluding 30 warmup frames in EACH phase.
It runs uncapped without v-sync (production stays capped at 60 FPS),
reports average and 95th-percentile frame cadence, paired FPS estimates,
ocean draw calls and static GPU buffer bytes, and uses a camera centered
above a genuine water parcel to test visible ocean shading. It also records
matched no-water and ocean screenshots and checks that a substantial number
of sampled real framebuffer pixels change when the ocean is enabled. `OCEAN_GPU_PERF` additionally
logs CPU OpenGL submission milliseconds (which are NOT GPU execution time).
The report and `gpu-ocean-coastal-benchmark.png` screenshot are uploaded
as artifacts and the pass metric is included in the workflow summary.
The comparison estimates additional GPU ocean cost on software Mesa; it is
not an old CPU water vs new GPU water benchmark, and order/driver caches can
bias the result. A native-hardware comparison remains a separate gate.

**Reference CI measurement (2026-10-28, GPU workflow 40713768938,
uncapped pre-frustum-optimization):** on seed 887766 City C with the
same camera and scene, the 150-sample baseline averaged **11.737 ms**
(85.20 FPS), while enabling GPU water averaged **12.835 ms**
(77.91 FPS): an additional **1.098 ms**. P95 increased from **12.956 ms**
to **15.414 ms**. Water comprised 14 batches and 4,970 triangles,
298,200 static vertex bytes, and zero per-frame vertex buffer uploads.
These figures are software-Mesa CI diagnostics, not hardware-specific
performance promises. The reference is preserved to compare future
revisions and inform regression budgets.

## Still not completed

- Continental water is currently a **visual** surface; true streamed,
  authoritative traversable coastline, swimming, boat buoyancy and drowning
  are **not** implemented by this import.
- The native OpenGL ocean shares wave displacement, both scrolling layers
  and basic transparency with the SDK, but its complete depth-readback
  refraction, surface contact spray and fully shaded Fresnel/foam fidelity
  still require visual comparisons against the CPU compositor.
- The portable water editor classes have been imported; authored
  per-world placement, document revision handshake, and the actual native
  editor preview are not connected to gameplay yet.
- GPU wet/snow coating of individual 3D world materials and real
  roof-by-roof exposure rays remain incomplete; the Java2D/shared weather
  compositor has stronger coverage than the native GPU presentation.

Do not claim that a successful compile alone establishes ocean screenshot
quality, end-to-end sailing/swimming, or all weather-material interactions.
