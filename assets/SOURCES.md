# Google Drive source inventory — 14 root folders/archives and 19 verified individual GLBs

This is the **Drive source inventory**; the game now renders the separately licensed Kenney GLBs and uses original source PNG data. The remaining purchased/private entries are references only. All identifiers are verified by direct connected Drive metadata/folder listing on October 20, 2026. See the machine-readable [manifest.csv](manifest.csv) and [runtime asset implementation audit](../docs/ASSET_PIPELINE_AUDIT.md).

## Main asset families

- [URBAN.zip](https://drive.google.com/file/d/19xJGiSa_Jlp-q_r2CpbBZ0PFovfigSpH/view): ~88.9 MB. Source geometry uninspected; not yet licensed for reuse in this project.
- [Kenney retro urban kit](https://drive.google.com/file/d/1T5k4KkqwWJ84rpl1WnMSbx5LxgKjpC11/view): ~2.16 MB; License.txt verified CC0, archive now integrated with 124 GLBs and 22 PNG source images.
- [Characters PSX 1.2](https://drive.google.com/file/d/1Dd6RWVvV1qUGT8G0Q1rATlHYSn3doXgU/view): ~60.9 MB, directly inventoried 138 rigged + 138 unrigged FBX variants and 136 PNG files; redistribution/license still unverified, private originals not imported.
- [PSX style cars](https://drive.google.com/file/d/1TeD9Nwo_79ZxL_7mSTZ6euz1M7SjqsX-/view), [Exploration objects](https://drive.google.com/file/d/1K7bIJ-y5Nl6lkzpwtpaP8WJX0DJPvm-i/view), [Victorian Study](https://drive.google.com/file/d/1aueQw546grQVRxuADo1qwc8DFCVXMVOV/view): later props, tools and indoor environments.
- [PSX Mega Pack](https://drive.google.com/drive/folders/1a_Shb7jPYuD38gBXrHS2bSlQV-gSiTLr), [GLB models](https://drive.google.com/drive/folders/1PdyyLcXtbNKTEVUMPFzmrQzWFv9Zesyr), [textures](https://drive.google.com/drive/folders/1h8AiYe1Z6l_iz-yVfo30s2X7AKDFSk8w): purchased pack with owner-reported pack-wide commercial use rights, as recorded in Beckenburg [master index](https://docs.google.com/document/d/18G144Qd0-L5rL7NdqxMykqkXaBHtu8-ytr_yYPol6rI/edit). Do NOT copy the full original purchased pack into public GitHub.
- [Converted Runtime Assets](https://drive.google.com/drive/folders/1IibnT8YL3guVbFzGFnoMhkLpxuIpgsX-): roads, barriers, parked cars and handheld tools already present as individual GLBs, but **not** confirmed loaded by this project.

## Acquisition contract

Each selected asset must register: original Drive ID, original archive path, original license and attribution, file SHA-256, conversion tool/settings, output hash, repository/runtime path, visual proof and regression result. GLB file names are not enough to infer mesh or animation quality. The Java 17 Swing renderer cannot ingest arbitrary GLB without an explicit mesh/texture loading or conversion pipeline. Private paid archives stay in Drive.

Source archive rights vary. Beckenburg's PSX Mega Pack license note applies to that pack only, not to every file in the archive library. No automatic assumption that an asset's appearance in Drive means public redistribution is authorized.

## Expanded model/texture snapshot — October 20, 2026

Direct Drive folder-listing readback added **600 individually identified source files** to `manifest.csv`, beyond the initial 33 archive and converted-item entries. Those 600 additional purchased-pack records remain **source catalog entries only**, distinct from the CC0 Kenney kit now integrated into gameplay.

The connected folder read returns at most 100 direct children. Entries for folders reaching this cap are necessarily **partial**; do not interpret counts as the full archive. The inherited pack-wide purchased commercial-use license applies to the PSX Mega Pack categories only. The Beckenburg root has a mix of source terms; its other content is not automatically licensed.

| Folder | Observed entries | New cataloged | Potentially capped |
|---|---:|---:|---|
| PSX GLB / Electronics & Misc | 43 | 43 | No cap encountered |
| PSX GLB / Furniture | 100 | 100 | YES — more may exist |
| PSX GLB / Lighting | 44 | 44 | No cap encountered |
| PSX GLB / Items & Weapons | 100 | 100 | YES — more may exist |
| PSX GLB / Small Props | 60 | 60 | No cap encountered |
| PSX GLB / Large Props | 36 | 36 | No cap encountered |
| PSX GLB / Modular Structures | 100 | 100 | YES — more may exist |
| PSX GLB / Decals | 4 | 4 | No cap encountered |
| PSX Mega Pack Textures | 100 | 100 | YES — more may exist |
| Beckenburg root archives | 29 | 13 | No cap encountered |

Next intake pass should paginate exact source folders by Google Drive API or download archive manifests to enumerate missing contents before asserting archive-complete coverage. No arbitrary web images or unrelated assets should substitute for user-archived content.
