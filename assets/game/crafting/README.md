# Crafting and trade icon set — five consistent sprite sheets

**Status:** 150 fully sliced 128×128 RGBA inventory PNGs are committed in five ZIP archives, each with exactly 30 sprites; each group also has a visually matching sheet reference in WebP. Source artwork was generated for this project. **The WebP reference sheets are lossy previews** and are not guaranteed pixel-identical reconstructions of the original full-resolution image-generation PNGs. The complete original-resolution sheets, labeled-card slices and 150 small icons were also packaged separately for local authoring.

## Layout

| Pack | Economy | Items |
|---|---|---:|
| `shared_core` | wilderness materials, food, primary processing | 30 |
| `shared_equipment` | kits, industrial inputs and shared faction final goods | 30 |
| `unraveled_industry` | mechanical manufacturing, fuel, vehicles, electronics, repairs | 30 |
| `penitent_ritual` | offerings, reliquary manufacture, liturgy, synchronized rites | 30 |
| `begotten_biology` | nutrients, living cultures, tissues, growing components | 30 |

**Paths:** `assets/game/crafting/packs/<pack>_sprites_only.zip` contains `icons/<ID>_small.png` and `ids.txt`; `assets/game/crafting/packs/<pack>_source.webp` contains the visual sheet reference. `assets/game/crafting/icons_index.csv` is the **runtime authoritative** ID, display label, pack, ZIP entry, PNG hash manifest. `assets/game/crafting/icons_manifest.json` is the art-production manifest with pack-level checksums and all 150 IDs.

## Game integration

Use `CraftingIconPacks.load(Path.of("."))` and call `icon("TIMBER")`, `icon("MACHINE_PARTS")`, `icon("SORTED_ORE")`, `icon("RITUAL_CLOTH")`, `icon("CHITIN_SHARDS")`, etc. The class verifies PNG SHA-256 on access, refuses unknown IDs, checks 128×128 + alpha, and caches decoded images. It does not invent economy items or remove faction-specific recipe prerequisites. UI should draw the sprite and separately render localized item text instead of relying on the tiny text baked into the large reference sheet.

`CraftingIconPacksTest.main()` verifies all 150 decoded icons in a headless Java runtime. To expand actual PNGs for tooling or an engine import, run:

```bash
python tools/extract_crafting_icon_packs.py
```

This produces `build/generated/crafting-icons/<ID>.png`, validates every asset against `icons_index.csv`, and refuses malformed ZIP contents or mismatched hashes. Do not put loose generated icons under the source tree unless an actual renderer/packaging requirement calls for them.

## Naming and economic provenance

IDs are uppercase underscore-separated and **stable**. A single icon can be used by an ingredient stack, recipe step, convoy manifest, item tooltip, trade listing, storage ledger, or factory job. **An asset ID is not a manufacturing recipe**: new faction goods in these sheets must be implemented as real gated products in the appropriate server-side recipe and item registries before they become obtainable. Unraveled, Penitent and Begotten each retain their separately designed specialization and cooperation requirements. The 30 new material concepts per faction are generic scaffolding, not instant imports into live inventories.

## QA contract

All five original sheets were segmented from the generated 6×5 grid; each small icon has transparent corner pixels and sufficiently large nontransparent artwork. The locally packed reference set contained **five originals, 150 full-card previews, 150 128×128 small icons, one combined atlas, and a manifest**. GitHub keeps the production-efficient subset (five ZIPs, five compressed reference previews and index files); the complete full-fidelity authoring package is available as a separate downloadable artifact.
