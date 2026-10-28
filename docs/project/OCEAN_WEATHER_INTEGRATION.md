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
an OpenGL **City E coastal** gallery for seed `20261020`, requiring a
nonempty source-ocean mesh, nonzero drawn triangles, and zero frame-by-frame
water vertex upload bytes. `OCEAN_GPU_PERF` logs record visible chunk count,
draw calls, triangles, static buffer bytes, CPU OpenGL submission milliseconds,
and measured viewport FPS. These are published in the GPU workflow summary
and uploaded `gpu-ocean-performance.log` artifact. CPU submission time is
not GPU execution time, and software-rendered CI FPS should not be treated as
a native hardware benchmark. Full frame-time comparison with the old
CPU-updated ocean remains a separate performance acceptance task.

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
