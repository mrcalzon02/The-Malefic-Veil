# Source-model icon coverage and native menu artwork

## Existing assets: keep them

The 150 committed 128x128 crafting PNG icons in five original ZIPs remain authoritative for their stable inventory IDs. Their source hashes and original resolutions are verified by GpuCraftingIcons. They are appropriate for material stacks, workshop inputs, crafting, factory stock, trade listings and storage. Never substitute emoji, SVG stand-ins or arbitrary sprites.

There are also 32 actual arsenal GLBs and nine faction aircraft GLBs. These now have a separate source-model thumbnail pipeline. Icon artwork **does not** define items, unlock a recipe, change faction legality or create a buy/sell transaction.

## 3D item portrait pipeline

Run locally with Blender installed:

    blender --background --factory-startup --python tools/render_item_portraits.py

The script imports each existing GLB using Blender's glTF importer, including node transforms and real materials. It uses fixed orthographic three-quarter camera framing, soft studio lighting, transparent backgrounds, 256x256 RGBA PNGs and output source checksums. Outputs:

- \`build/generated/item-portraits/portraits/weapons/<id>.png\`
- \`build/generated/item-portraits/portraits/aircraft/<id>.png\`
- \`build/generated/item-portraits/portraits/index.csv\` with source/output SHA-256 values

Desktop release CI bakes before packaging. GPU CI bakes once in Linux then distributes the same output to Windows and Linux builds. Maven packages generated PNGs inside the standalone JAR. A released executable does not need Blender.

Runtime source priority: the original 150 crafting icons for their IDs; otherwise an explicitly registered, real model portrait; otherwise text and an honest unillustrated state. New GLBs are not auto-associated with economy items by filename guesswork.

## Native UI integration

- The unified frontend retains its genuine moving city backdrop and the original licensed PSX bitmap typeface/skin.
- A small informational armory panel cycles through the **actual** arsenal models and reports the existing faction assignment, without claiming that the preview is a store.
- Camp crate STORE/TAKE rows now reserve an icon slot and resolve the icon using the unchanged authoritative item ID, without changing host-side transfer behavior.
- The recipe encyclopedia supports both original crafting icons and source-model portraits for eligible IDs.

## Outstanding work, in order

1. Validate the generated GLB thumbnails by opening CI artifacts. Add per-model camera override only when warranted by silhouette, and verify image transparency, lighting, texture orientation and framing.
2. Audit actual item IDs used for weapons, pickups, throw-deployables, vehicles, components and factory outputs. Extend the registry to other proven source meshes; do not create fictional item art.
3. Apply the same resolver to gameplay inventory, loot, hauling manifests, equipment comparisons, faction stock ledgers, factory queues and **real** buy/sell lists. The store must show actual host-authoritative stock, prices, limitations, transfer receipts and errors.
4. Produce a separate approved game logo and OS app icon (Windows ICO, Linux PNG, optional macOS ICNS), with coherent splash/launch presentation based on the current PSX theme; do not repurpose licensed third-party logos as our brand.

**Not completed:** true buy/sell UI transactions, comprehensive inventory art coverage, OS launcher icons, or visual review of generated thumbnails. The 150 original icons and actual game functionality are preserved.
