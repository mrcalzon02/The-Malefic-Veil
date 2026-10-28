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

`GpuOcean` renders source-driven geometry from `WaterEffects.surface` and
the two independent scrolling water-texture layers. It owns reusable
depth-tested, alpha-blended world chunks and is drawn in the native OpenGL
game and editor viewport, before first-person equipment and HUD.

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
