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
