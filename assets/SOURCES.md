# Google Drive source inventory — 14 root folders/archives and 19 verified individual GLBs

This is a **reference-only** register; the game does not yet render any Drive GLB or texture. All identifiers are verified by direct connected Drive metadata/folder listing on October 20, 2026. See the machine-readable [manifest.csv](manifest.csv).

## Main asset families

- [URBAN.zip](https://drive.google.com/file/d/19xJGiSa_Jlp-q_r2CpbBZ0PFovfigSpH/view): ~88.9 MB. Source geometry uninspected; not yet licensed for reuse in this project.
- [Kenney retro urban kit](https://drive.google.com/file/d/1T5k4KkqwWJ84rpl1WnMSbx5LxgKjpC11/view): ~2.16 MB, intended roads, street furniture and barricades. License file must be read from archive before distribution.
- [Characters PSX 1.2](https://drive.google.com/file/d/1Dd6RWVvV1qUGT8G0Q1rATlHYSn3doXgU/view): ~60.9 MB; candidate selectable playable character roster, rigs/materials not yet inventoried.
- [PSX style cars](https://drive.google.com/file/d/1TeD9Nwo_79ZxL_7mSTZ6euz1M7SjqsX-/view), [Exploration objects](https://drive.google.com/file/d/1K7bIJ-y5Nl6lkzpwtpaP8WJX0DJPvm-i/view), [Victorian Study](https://drive.google.com/file/d/1aueQw546grQVRxuADo1qwc8DFCVXMVOV/view): later props, tools and indoor environments.
- [PSX Mega Pack](https://drive.google.com/drive/folders/1a_Shb7jPYuD38gBXrHS2bSlQV-gSiTLr), [GLB models](https://drive.google.com/drive/folders/1PdyyLcXtbNKTEVUMPFzmrQzWFv9Zesyr), [textures](https://drive.google.com/drive/folders/1h8AiYe1Z6l_iz-yVfo30s2X7AKDFSk8w): purchased pack with owner-reported pack-wide commercial use rights, as recorded in Beckenburg [master index](https://docs.google.com/document/d/18G144Qd0-L5rL7NdqxMykqkXaBHtu8-ytr_yYPol6rI/edit). Do NOT copy the full original purchased pack into public GitHub.
- [Converted Runtime Assets](https://drive.google.com/drive/folders/1IibnT8YL3guVbFzGFnoMhkLpxuIpgsX-): roads, barriers, parked cars and handheld tools already present as individual GLBs, but **not** confirmed loaded by this project.

## Acquisition contract

Each selected asset must register: original Drive ID, original archive path, original license and attribution, file SHA-256, conversion tool/settings, output hash, repository/runtime path, visual proof and regression result. GLB file names are not enough to infer mesh or animation quality. The Java 17 Swing renderer cannot ingest arbitrary GLB without an explicit mesh/texture loading or conversion pipeline. Private paid archives stay in Drive.

Source archive rights vary. Beckenburg's PSX Mega Pack license note applies to that pack only, not to every file in the archive library. No automatic assumption that an asset's appearance in Drive means public redistribution is authorized.
