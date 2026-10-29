# Working Arsenal — 32 Source-Verified Weapons

[Home](Home.md) · [Factions](Factions.md) · [Items](Items.md) · [Recipes](Recipes.md) · [Gameplay](Gameplay.md)

These are the **original 32 models, IDs and prototype combat profiles recovered from [arsenal-prototypes](https://github.com/mrcalzon02/The-Malefic-Veil/tree/arsenal-prototypes)**, not replacements. Eight weapons are world-scavenged, and 24 weapons are manufactured at host-owned fabrication benches. All 32 original articulated `vmesh` files and GLBs are retained; rendered body, trim, grip and energized-component colors now reflect the three actual game factions.

Manufacture follows permanent account faction registration. Existing prototype owner index 0 maps to **Unraveled**, 1 to **Penitent**, and 2 to **Begotten**; the obsolete Foundry/Lumen/Veil *faction names* are not gameplay groups. The game's Unraveled, Penitent, and Begotten economies stay separate. An already-owned/scavenged weapon does not grant another faction production rights.

## Eight shared world weapons

| Weapon ID | Name | Combat family | Primary | Alternate |
|---|---|---|---|---|
| `w01-vesper` | **Vesper** | slide | HIT (24 × 1) | PELLET (15 × 3) |
| `w02-ossuary` | **Ossuary** | cylinder | HIT (68 × 1) | RAIL (95 × 1) |
| `w03-needle-choir` | **Needle Choir** | bolt | HIT (7 × 1) | HIT (11 × 6) |
| `w04-rivet-saint` | **Rivet Saint** | bolt | BOLT (32 × 1) | BOLT (50 × 1) |
| `w05-grave-bell` | **Grave Bell** | hinge | PELLET (11 × 10) | PELLET (9 × 20) |
| `w06-cerberus` | **Cerberus** | hinge | PELLET (10 × 8) | PELLET (9 × 24) |
| `w07-reliquary` | **Reliquary** | pump | PELLET (9 × 12) | HIT (85 × 1) |
| `w08-furnace-organ` | **Furnace Organ** | rotor | HIT (8 × 1) | PELLET (9 × 12) |

## Faction manufacturing by authentic tier

The faction distribution in the original implementation is **9 Unraveled / 9 Penitent / 6 Begotten**, not four each, and is preserved pending explicit game-design rebalance. Tiers 1–3 are bench upgrade gates. Research/owned prerequisites remain active: `-` means none, and references to the eight shared weapons require physical ownership rather than free research.

### The Unraveled

Salvage-forged metalwork, recovered heavy machinery, explosive engineering and field artillery. **Finish:** oxidized teal, repaired dark gunmetal, warm brass/hazard markings, black industrial grips.

| Tier | Weapon | Prerequisite | Primary / alternate | Production costs |
|---|---|---|---|---|
| 2 | **Hailstorm** (`w09-hailstorm`) | Furnace Organ (`w08`) | HIT 9×2; HIT 13×2 | 6× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame |
| 1 | **Pilgrim** (`w19-pilgrim`) | — | ROCKET 105×1; ROCKET 90×3 | 8× Scrap; 2× Electronics; 2× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Widow Choir** (`w20-widow-choir`) | Pilgrim (`w19`) | MISSILE 32×4; MISSILE 40×4 | 8× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Basilisk** (`w21-basilisk`) | Pilgrim (`w19`) | ROCKET 150×1; ROCKET 95×1 | 9× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Censer** (`w22-censer`) | Pilgrim (`w19`) | GRENADE 100×1; STICKY 80×1 | 9× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Vigil Mortar** (`w23-vigil-mortar`) | Censer (`w22`) | MORTAR 130×1; MORTAR 38×5 | 9× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Warhead |
| 3 | **Cathedral** (`w24-cathedral`) | Vigil Mortar (`w23`) | MORTAR 220×1; SMOKE 0×1 | 9× Scrap; 4× Electronics; 4× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Shrapnel Mass** (`w25-shrapnel-mass`) | Censer (`w22`) | FLAK 10×14; GRENADE 90×1 | 10× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Warhead |
| 2 | **Scrap Cyclone** (`w28-scrap-cyclone`) | Shrapnel Mass (`w25`) | SCRAP 8×18; RAIL 115×1 | 10× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame |

### The Penitent

Consecrated energy instruments, brilliant arc systems and reliquary-tech beam weapons. **Finish:** brass/gold liturgical trim, ash charcoal, oxblood enamel, aged ivory grips and luminous cores.

| Tier | Weapon | Prerequisite | Primary / alternate | Production costs |
|---|---|---|---|---|
| 1 | **Cautery** (`w10-cautery`) | — | BEAM 22×1; BEAM 9×1 | 6× Scrap; 2× Electronics; 2× Alloy; 1× Machined frame; 1× Power cell |
| 2 | **Prism Writ** (`w11-prism-writ`) | Cautery (`w10`) | BEAM 22×3; BEAM 100×1 | 6× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell |
| 2 | **Longwake** (`w12-longwake`) | Cautery (`w10`) | RAIL 125×1; HIT 78×1 | 6× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell |
| 2 | **Ion Psalm** (`w13-ion-psalm`) | Cautery (`w10`) | BOLT 38×1; ORB 110×1 | 7× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell |
| 3 | **Sun Eater** (`w14-sun-eater`) | Ion Psalm (`w13`) | BEAM 30×1; BEAM 200×1 | 7× Scrap; 4× Electronics; 4× Alloy; 1× Machined frame; 1× Power cell |
| 2 | **Storm Rosary** (`w15-storm-rosary`) | Cautery (`w10`) | CHAIN 48×1; ORB 65×1 | 7× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell; 1× Arc coil |
| 2 | **Forked Benediction** (`w16-forked-benediction`) | Storm Rosary (`w15`) | CHAIN 28×2; CHAIN 45×3 | 7× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell; 1× Arc coil |
| 2 | **Ball Lightning** (`w17-ball-lightning`) | Storm Rosary (`w15`) | ORB 75×1; DETONATE 110×1 | 8× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame; 1× Power cell; 1× Arc coil |
| 3 | **Thunder Loom** (`w18-thunder-loom`) | Forked Benediction (`w16`) | CHAIN 24×1; PULSE 80×1 | 8× Scrap; 4× Electronics; 4× Alloy; 1× Machined frame; 1× Power cell; 1× Arc coil |

### The Begotten

Living anomalous instruments, thrown/returning weapons, corrosion, cryogenic transformation and gravity manipulation. **Finish:** bruised violet chitin, bioluminescent green-teal organ cores, dark tissue grips and organically patinated plates.

| Tier | Weapon | Prerequisite | Primary / alternate | Production costs |
|---|---|---|---|---|
| 1 | **Ricochet Gospel** (`w26-ricochet-gospel`) | — | DISC 65×1; RECALL 45×1 | 10× Scrap; 2× Electronics; 2× Alloy; 1× Machined frame |
| 2 | **Bankshot** (`w27-bankshot`) | Ricochet Gospel (`w26`) | BOUNCE 50×1; BOUNCE 32×3 | 10× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame |
| 1 | **Miasma** (`w29-miasma`) | — | BIO 28×1; BIO 15×1 | 11× Scrap; 2× Electronics; 2× Alloy; 1× Machined frame |
| 2 | **Winter Verdict** (`w30-winter-verdict`) | Miasma (`w29`) | CRYO 12×1; BOLT 80×1 | 11× Scrap; 3× Electronics; 3× Alloy; 1× Machined frame |
| 3 | **Eventide** (`w31-eventide`) | Bankshot (`w27`) | GRAVITY 60×1; VORTEX 20×1 | 11× Scrap; 4× Electronics; 4× Alloy; 1× Machined frame; 1× Arc coil; 1× Anomaly core |
| 3 | **Veilbreaker** (`w32-veilbreaker`) | Eventide (`w31`) | SINGULARITY 240×1; BEAM 180×1 | 11× Scrap; 4× Electronics; 4× Alloy; 1× Machined frame; 1× Arc coil; 1× Anomaly core |

## Component and ammunition industry

The original fabrication engine includes scrap, electronics, chemicals, alloy, machined frames, power cells, arc coils, warheads, anomaly cores and ammunition crafting. Only a living *owned* bench within reach satisfies production access. Advanced weapon manufacture validates bench tier, authenticated faction and previous design research/ownership, then atomically consumes materials and yields a weapon. Benches upgrade from tier 1 to 3. Manufacturing permission is distinct from faction cooperation, item possession and legal world construction.

## Art, motion and release gates

The 32 original `assets/game/arsenal/*.vmesh` and `*.glb` assets plus `working-arsenal.blend` are copied unchanged from their verified GitHub blobs. [FactionWeaponMaterials.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionWeaponMaterials.java) applies semantic, component-specific finishes in the Java renderer while preserving source light/dark variation and all rigid motion. The source GLB files retain their original shared material until a dedicated GLB re-export pass. **Geometry boxiness is deliberately not addressed in this phase.**

Host combat supports the arsenal's 32 primary and alternate prototypes (including timed reload, projectile/area modes and network messages). Full balance and audiovisual polish remain separate gates. The old survival resource recipe registry and the working bench arsenal are **distinct domains**, not permission to mint these 24 weapons with neutral handcraft requests. Do not confuse this recovered player arsenal with the separate [Lost enemy arsenal](Lost-Arsenal.md), which remains a design-only NPC catalog.

Authoritative source: [ArsenalCatalog.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArsenalCatalog.java) · [ArsenalSystems.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArsenalSystems.java) · [runtime JSON](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/data/runtime-arsenal.json) · [component meshes](https://github.com/mrcalzon02/The-Malefic-Veil/tree/main/assets/game/arsenal) · [tests](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/ArsenalIntegrationTest.java).
