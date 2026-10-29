# The Malefic Veil — Universal Audio Audit and Staged Implementation
Observed against `main` on 2026-10-29. This is a **coverage audit**, not a blanket claim that every audio event has been implemented or listening-tested.

## Initial findings
- The GPU launcher originally had one verified source track, one generic synthesized click and one generic synthesized gunshot.
- The `ambience` slider existed but was not attached to a dedicated ambient stream; the same soundtrack was merely turned down during play using ambience gain.
- Every one of the 32 implemented arsenal entries played the same generated gun sound from GPU mouse input.
- No distinct success/failure feedback, footsteps, UI navigation, context-specific music, per-weapon reloads, environmental emitters, weather loops, creature voices, or spatial occlusion were established in the GPU client.
- Weather files `assets/game/weather/audio_00.zip` through `audio_05.zip` exist in the repository, but their presence does **not** establish decoded playback or weather synchronization.
- System settings already support master, music, effects, ambience, UI, voice independently; preserve these.

## Sourcing hierarchy (mandatory)
1. **Owner-provided Drive** (original recordings, game-produced audio, available project sources). Inventory exact folder, name, size, hash, rights, suitability, and intended event; do not use name alone as proof of quality.
2. **Existing repositories** (ReverieVR, Beckenburg, HB-TTRPG tools, other original works) with provenance and redistribution rights. ReverieVR has `red_ledger_click.wav`, `red_ledger_book.wav`, `red_ledger_coins.wav`, `red_ledger_door.wav` and test-chamber impacts; names and byte sizes corroborate some Drive sources. Avoid silently replacing approved game themes.
3. **Public, explicitly licensed** material for identified gaps only: Kenney UI Audio (CC0, https://kenney.nl/assets/ui-audio), Kenney Interface Sounds (CC0, https://opengameart.org/content/interface-sounds), and individually CC0-tagged OpenGameArt sound effects (https://opengameart.org/content/cc0-sound-effects). Preserve creator/source URL, exact asset, license, attribution (when applicable), and output hashes.

## Stage 1 — GPU sound coverage (source-code implementation in this batch)
- [x] Keep the SHA-256 verified, original Drive menu track intact.
- [x] Import ten selected source recordings (four WAVs and six MP3s) into the GPU resource classpath with a provenance table.
- [x] Give the GPU runtime **32 separately synthesized** weapon firing identities keyed by the authoritative arsenal index (0–31) and mixed with lower-level Drive source layers by family.
- [x] Distinct UI navigation, click, accept, reject, open-panel, empty-magazine and footsteps fallback cues.
- [x] Wire selection, activation, panel-change, acknowledgement/failure sounds in the integrated launcher and in-world overlay.
- [x] Wire movement-distance footsteps and weapon-index firing from the GPU world (without making combat client-authoritative).
- [x] Separate looping industrial/city ambience from music; ambience gain now controls ambience, not music.
- [x] Add headless deterministic audio-bank integrity check to existing GPU CI; verify selected classpath source files are packed.
- [ ] Physical listening QA on Windows/Linux, volume normalization, no clipping and menu transition regression: not yet observed.
- [ ] Dedicated reload event hook and reload-source mixing: asset imported but not event-wired.
- [ ] Visual/audio proof capture in CI: smoke tests check render and code paths but do not capture sound devices.

### Arsenal identities and acoustic direction
| Indices | Weapons | Signature target |
| --- | --- | --- |
| 0–8 | Vesper, Ossuary, Needle Choir, Rivet Saint, Grave Bell, Cerberus, Reliquary, Furnace Organ, Hailstorm | crisp pistol, heavy cylinder crack, microdart ticks, rivet thump, double-barrel blast, triple recoil, pump report, rotating barrels and heavy rotary respectively |
| 9–13 | Cautery, Prism Writ, Longwake, Ion Psalm, Sun Eater | sharp laser pulse, prismatic split, charged rail spike, wet plasma discharge, extended beam furnace |
| 14–17 | Storm Rosary, Forked Benediction, Ball Lightning, Thunder Loom | chain arc, twin crackle, seeking electrical orb, rotary electrocution |
| 18–24 | Pilgrim, Widow Choir, Basilisk, Censer, Vigil Mortar, Cathedral, Shrapnel Mass | propellant rockets, clustered missiles, heavy launcher, grenadier, mortar arcs and metal shrapnel |
| 25–27 | Ricochet Gospel, Bankshot, Scrap Cyclone | blade/disc, ricochet snap, irregular metallic storm |
| 28–31 | Miasma, Winter Verdict, Eventide, Veilbreaker | biological hiss, cracking pressure frost, gravitic resonance, singularity pulse |

**Caveat:** The first-batch weapon signatures are unique deterministic *provisional* SFX, not yet 32 individually commissioned/mastered recordings. The authentic weapon identities require ear tests and bespoke layered design, especially alternate fire, charge, continuous fire, reload and impact variants.

## Stage 2 — Interaction, gameplay and feedback
Wire player feedback to authoritative gameplay outcomes rather than merely to clicks:
- success vs rejected action, insufficient resources, invalid item, crafting completion, purchase/sale, pickup, equipped item, damage, low health, death, revival, healing, hunger, thirst, skills and levels
- distinct primary/alternate fire, dry fire, magazine/cylinder/shell reload, charge-up/down, sustain loop, overheat, projectile whoosh/impact, ricochet, material hits and explosion falloff for **all 32** weapons
- thrown deployables: throw, landing, mechanical/ritual/biological deployment, active loop, expiration and safe decay explosion; distinct Unraveled/Penitent/Begotten treatments
- factions: Unraveled machinery/engines, Penitent reliquary/chant/censer, Begotten chitin/viscera/organ sounds. Do not flatten these into one sound economy.

## Stage 3 — Environment and music
- Event-driven biome/zone beds (city, ruins, interiors, forest, coast, subterranean and faction-held territory) with crossfading layers.
- Wind, rain, thunder, surf, moving water, fire, miasma, power plants, traffic, aircraft, electrical systems, construction, drones, footsteps by material, creature calls and distant conflict.
- Use weather module state and ocean water states to activate exact loops at correct intensities (not arbitrary random ambience).
- Music states: title, peaceful exploration, increasing threat, combat, victory, critical health, death, faction hub and night; independent sliders, loop points, transition fades and ducking.
- Sources from Drive: dedicated *Nature_Essentials_NOX_SOUND* collection has 18 named loops (subject to license), local *Sounds* includes 43 immediate entries, owner-maintained music folders contain many MP3/WAV choices. Game soundtrack collections must not be repurposed without grants.

## Stage 4 — Engine, performance, and delivery QA
- Audio event registry (event -> file -> rights -> mixer bus -> source/recipient -> variation/cooldown -> coverage).
- Camera-relative spatial stereo, distance falloff, occlusion, voice concurrency caps, pooling, fade/duck logic, crowd mix and deterministic audio diagnostics.
- Verify source recording loudness peaks, silence, sample rate, clipping, loudness target, normalized playback and seam continuity; do not normalize originals destructively.
- Verify no hundreds-of-thousands-of-plays per tick, device failure degrades gracefully, and server/headless runs never require an audio device.
- CI compile and manifest integrity tests; human ears/device checks are separate.


## Recorded source-file quality inspection (2026-10-29)

Inspected exact imported original recordings with ffprobe and ffmpeg's `volumedetect`. These measurements describe **whole-file signal averages and decoded sample peaks**, not perceived loudness (LUFS) or listening-test approval. No destructive normalization of original files was performed.

| Source | Sampling / channels | Duration | Mean dBFS | Peak dBFS | Concern |
| --- | --- | ---: | ---: | ---: | --- |
| Single_click.wav | 44.1 kHz stereo | 0.360 s | -36.3 | -8.3 | Quiet relative to other UI sounds |
| Cathonk.wav | 44.1 kHz stereo | 0.360 s | -28.7 | -2.8 | Moderate |
| Metal_wonk.wav | 44.1 kHz stereo | 0.325 s | -22.9 | -3.1 | Relatively prominent |
| Book_snap.wav | 44.1 kHz stereo | 0.569 s | -15.3 | -0.9 | Loud UI transient; reduce mix gain |
| Shot.mp3 | 24 kHz stereo | 1.224 s | -17.7 | 0.0 | At decoded full scale; investigate distortion |
| LAS.mp3 | 24 kHz stereo | 0.624 s | -17.6 | -4.7 | Low bandwidth, acceptable provisional layer |
| Plas.mp3 | 48 kHz mono | 0.456 s | -17.7 | -5.3 | Provisional weapon association |
| Lightning-crackle.mp3 | 8 kHz mono | 1.224 s | -19.5 | -1.5 | Telephone bandwidth; prioritize source replacement |
| FLAME.mp3 | 44.1 kHz stereo | 1.332 s | -16.9 | -2.2 | Provisional propellant layer |
| RELOAD.mp3 | 44.1 kHz stereo | 0.261 s | -22.1 | 0.0 | Peak at full scale, not yet event-wired |

**Finding:** the original UI sources differ by about 21 dB in whole-file mean level, from `Single_click.wav` to `Book_snap.wav`. In commit `60c85a3ee17cd9ab2a989f7993502dd1e5fbf16f`, runtime gain was adjusted to boost click playback and substantially reduce the panel/rejection sounds. These are provisional mix corrections, **not perceptual loudness-normalization certification**. Preserve source originals.

## Validation status

- GitHub commit and remote `main` readback verified.
- GPU and desktop release GitHub Actions were **in progress** at time of last inspection, not yet passing.
- Actual speaker/headphone listening, decoder behavior on both Windows/Linux, and device-level volume measurements are outstanding.


## Stage 2 — Authoritative audio and responsive weather (2026-10-29)

**Implemented in this second GPU iteration, subject to CI verification:**
- Audio now follows the *authoritative* `ArsenalSystems.View.actionAt()` edge rather than treating a local mouse-down request as a confirmed hit or gunshot.
- Distinct `PRIMARY` vs `ALTERNATE` for each of 32 weapons, separate handling/reload profiles, charge signal and equip feedback. Successful reload events come from the authoritative action snapshot; rejected reloads do not play the reload sound.
- Stale/repeated action timestamps never retrigger a report. A first snapshot is baseline-only and does not replay combat from before scene connection.
- Projectile-capacity rejection now happens **before** weapon action/ammunition mutation, fixing a host-side false-confirmation issue.
- Three provisional procedural weather beds (rain, wind, snow) are blended from host-synchronized weather values. Thunder transient follows the existing lightning flash with a cooldown, not an independent random weather simulation.
- GPU audio owns its weather mix; the legacy Java Sound weather player is disabled for GPU presentation only, while the traditional Swing player retains its module audio.
- Separate music, ambience, UI and effects gains remain intact; no server/headless audio dependency is added.

**Not yet complete:** high-quality sourced environment loops (NOX Sound licensing still unverified), per-biome beds, indoor occlusion, material-specific footsteps, enemy/civilian sounds, environmental and machine emitters, intentional dynamic music state machine, combat impacts, deployables, and device-level listening/mix QA. Alternate fire signatures are separately synthesized but are not yet individually sound-designed/recorded for production.

**Verification contract:** `GpuAudioEventGate.main` asserts no replay, first-snapshot silence, primary/alternate/reload/charge/equip transitions and invalid weapon rejection; `GpuAudioSynthesis.main` asserts all 32 primary, alternate and handling waveforms are valid/distinct and weather beds exist. Existing GPU Linux/Windows workflows run both. A CI pass validates generation and packaging, **not** whether each sound is artistically suitable or audible on every device.


## Stage 3 — Confirmed world events and five more Drive assets

- Added `GpuWorldAudioGate` to compare immutable host snapshots: injury, healing, death, increased cash, added inventory items and inventory-panel opening. The first frame establishes a baseline and does not play old events.
- Triggers success, failure, and deployed-item cues from changed host notices, not merely the button that submitted a request.
- Imported five more user-archived audio files: `Coins.wav`, `Drawer.wav`, `Shifting_cloth.wav`, `Granade.wav`, `Bash.mp3`. Asset rights remain a separate verification gate documented in the audio library provenance.
- Removed premature success chimes from the in-game UI when it had only requested host action, not received host acceptance.
- `GpuWorldAudioGate.main` is a device-free regression for event changes and historical-state suppression, included in the GPU CI.
- Still pending: precise world-position/occlusion for doors, faction machinery, vehicle engines, biome soundscapes, sound mastering, dynamic music, realistic deployable mechanical transitions and material-specific footsteps.

## GPU input continuity — primary/alternate fire

The GPU client previously sent only a one-shot primary mouse press. Held firing was not forwarded to the real `MaleficVeil` simulation, and right-mouse alternate fire had no GPU entry point. The new `gpuHoldFire(primary,alternate)` path forwards **state transitions** to the existing Swing-EDT host gameplay input, keeping continuous fire, charge, spin-up and release under the same `ArsenalSystems` authoritative cooldown/ammunition handling as traditional play. Right-click sends `gpuFire(true)`; all sound still comes from confirmed `ArsenalSystems.View.actionAt`, never raw mouse input. Menu opening sends release to avoid firing behind overlays. GPU live smoke now exercises held primary and alternate host requests.

Do not describe this as fully listening-tested until the GPU CI and real device pass. Host-side ballistic/hit rules are unchanged.

## Stage 4 — Authoritative combat impacts, environmental fire and tension scoring

- `GpuFrame.combatEffects` is an immutable presentation snapshot from the already-existing `ArsenalSystems.effects()` / `NetLink.arsenalEffects()` sources. This changes **no host firing, damage, ammunition or network protocol**.
- `GpuCombatAudioGate` scans positive, monotonically numbered host combat effect IDs, ignoring transient negative-ID field/projectile render objects. It establishes a first-frame baseline, ignores already-observed effects and caps bursts at eight one-shots per presentation frame. CI covers all branches.
- Actual `HIT`, `IMPACT`, `BOUNCE`, `ARC`, `BLAST` produce different original procedural sound profiles. `MUZZLE` and tracer FX cannot masquerade as an impact; no source-derived effect is triggered by just pressing fire.
- Effect playback uses camera-relative stereo panning, distance attenuation to 36 metres and conservative attenuation when a ray through existing world collision says the source is blocked. It does not yet simulate physical diffraction/reverb.
- Original eight-second synthesized, nonmelodic combat tension layer crossfades behind the retained, checksum-verified menu/exploration source music. Threat conditions are authoritative locally visible enemy models, confirmed combat activity or critical health. No borrowed soundtrack was introduced.
- An ambient campfire layer becomes audible when a real `device-field-fire` model exists nearby in the immutable host frame. It is silent when no such device is present; **not** a generic loop emitted from every idle machine.
- Original WAVs are created at runtime, honoring separate music/ambience/effects gains. All music and sound objects are disposed with the GPU audio bus and no headless server depends on them.

**Remaining:** physical mastering/listening QA, other facility-specific active operating-state emitters, terrain-footstep materials, rain shelter/occlusion reverb, vehicle movement sounds, field deployable start/decay states, authored long-form adaptive score and public CC0 asset intake where original recordings cannot cover a verified gap.

## Stage 5 — Terrain-aware footsteps (October 29, 2026)

The prior two synthesized footstep clicks were replaced with **eight grounded surface identities**, with two deterministic samples per material. The GPU now samples the same `World` road grid and `ContinentLayout` biome used for game movement; weather snow and heavy rain modify the chosen material without any additional invented terrain simulation.

- Detailed-city road cells: asphalt. Other traversable city cells: stone/concrete.
- Wilderness: grass, forest leaf litter, gravel in hills/mountains, sand on beaches, marsh mud; heavy rain makes open plains muddy; snow changes the exposed walking sound to snow crunch.
- Actual travel distance still gates footsteps and large coordinate corrections remain silent. A step is never triggered solely by camera motion.
- `GpuFootstepSurface.main` validates city, road, biome, wet and snow classifications; `GpuAudioSynthesis.main` verifies all sixteen individual WAV waveforms are distinct. No additional external asset license is needed.

**Outstanding sound work:** actual material probing underneath placed prefabs, interior reverb/occlusion, biome-specific wild animal ambience, operating factory/vehicle loops, cinematic music and release-grade loudness QA.

## Stage 6 — PCM quality and loop seam integrity gate

- `GpuAudioQualityGate` decodes and checks **132 generated WAV files**, with no sound-card or OpenAL requirement. Validates RIFF sizes, mono 16-bit PCM, 22,050 Hz rate, bounded peaks, nonzero RMS and sample-level endpoints for the six continuous ambient/music loops.
- Corrected a real discontinuity in the low-frequency noise lane of `weatherBed`: its coarse noise grid previously jumped to zero when a 4-second loop wrapped. Now both the fine-noise and slow-noise lanes use circular interpolation.
- The generator intentionally limits output to 23,000 / 32,767 peak amplitude, leaving digital headroom.
- CI remains a **technical wave integrity** pass, not an equal-loudness/mastering or perceptual-quality signoff. Sourced Drive recordings remain unmodified; they require individual listening and dynamic-range inspection.
