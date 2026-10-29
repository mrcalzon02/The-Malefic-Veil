# Malefic Veil — Drive-sourced audio library (October 29, 2026)

Selected from the connected owner's **Sounds** / **sound/ui** folders before looking at public libraries. Filenames and Drive file IDs identify exact source files; byte hashes can be recovered from the committed Git blobs. Unlike extracted game music from another title, these selections were furnished as project sound material by the user. A public-repository commit at the user's direction is not an independent legal finding of ownership: preserve license/consent evidence before distributing a release.

| In-game file | Original Drive ID | Role | Status |
| --- | --- | --- | --- |
| Single_click.wav | `1C97hmPSJaghdMfV2F8C-XRnihvnf7O6d` | menu activate | bundled, loaded |
| Cathonk.wav | `10rDodOAoNdOq9KtSWk-GHOhZb241E7fk` | success/accept | bundled, loaded |
| Metal_wonk.wav | `192BOJs4D5zjckcjeVGoWJFit2eEcXOHg` | rejection/failure | bundled, loaded |
| Book_snap.wav | `14nOOYZ8Xso7JbdivVGNddPG2xBTPag2p` | panel open/close | bundled, loaded |
| Shot.mp3 | `1vsbX_TWm1OLyBTdWb88aSgz1lwkEfJrW` | subdued ballistic source layer | bundled, loaded |
| LAS.mp3 | `1I12l0PvjCwVL8amxM-M8nIwV63KDBFxW` | energy source layer | bundled, loaded |
| Lightning-crackle.mp3 | `1MYeRqRBmrMawnndRquVo7SD3lr88eKD3` | electric source layer | bundled, loaded |
| FLAME.mp3 | `1T4wN8wPhVqrBl4DTK7hKjZF6mnk5RjDc` | explosive/propellant source layer | bundled, loaded |
| Plas.mp3 | `1F-F_Sh6_kRCSAat5J1Xqmr5Bs9u8Vqrj` | cryo source layer (provisional) | bundled, loaded |
| RELOAD.mp3 | `1bR4aczSliEitM6Cu-gXTNHMFdqbkfcQ3` | subdued layered reload cue | bundled, authoritative reload action |
| Coins.wav | `1KwYATDrau5t2-P0TbyeMb-YjNg-SsfI9` | confirmed cash increase | bundled, loaded |
| Drawer.wav | `1-6Rc4Cz0JTHvsBWSXOqNtfQNKRZ7Ehi9` | inventory panel opening | bundled, loaded |
| Shifting_cloth.wav | `159kexDFReIWbytRsZAUVzAMIFR0sFmBE` | increased carried-item stock | bundled, loaded |
| Granade.wav | `1-mskBupR-Wd_VEaqd5bTezrXVH0IA5iz` | provisional thrown-deployable acknowledgement | bundled, loaded |
| Bash.mp3 | `1QKJP7i_gpJLNP3UPXrEB4nc4PYN3SLKe` | damage/death impact | bundled, loaded |

Original audio under `gpu-client/src/main/resources/audio/cold-unresolved-drone.mp3.part00..07` is **separately** verified byte-for-byte against the approved Google Drive menu-music recording; see `../PROVENANCE.md`. Never repurpose these track pieces as effects or replace their checksum.

The stored source audio is unmodified; no automatic destructive resampling or format conversion. Each weapon additionally gets a generated individual profile from `GpuAudioSynthesis`, not merely one of the shared recordings. If a decoder cannot load a source recording, the audio system logs the failure and retains synthetic sound where available.

## Source-rights boundary

The owner-managed archive contains several distinct categories: original recordings, project-local compositions, purchased sound packs, and soundtrack files from other games. **Access is not a universal commercial redistribution license.** The NOX Sound packs and other game soundtrack files remain **candidate/reference sources only** until their specific licenses have been checked; no such files were copied here.

## Runtime locations

- Library: `gpu-client/src/main/resources/audio/library/*`
- Audio bus: `gpu-client/src/main/java/veil/gpu/GpuAudioBus.java`
- Procedural cues and distinct weapon fallbacks: `gpu-client/src/main/java/veil/gpu/GpuAudioSynthesis.java`
- Settings: `GpuClientSettings` (separate master/music/effects/ambience/UI/voice controls)
- Gameplay hooks: `GpuKenneyPreview`
- Menus: `GpuFrontEnd`, `GpuOverlayMenu`
