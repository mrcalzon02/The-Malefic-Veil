# Plot-first city generator (P2 foundation)

The hand-drawn layout reference is a network of streets whose leftover spaces are filled with categorized lots. The collision geometry is generated deterministically in Java 17; a later asset binding pass will turn each *template* into a distinctive licensed 3D environment.

## Current implementation

`CityLayout.generate(seed)` produces a 22x22 parcel grid (each parcel cell is 6x6 collision tiles) within the current 132x132 `World`. The **2x2 parcel** is the standard plot; footprint alternatives are 1x1, 1x2, 2x1, 2x3, 3x2 and 3x3. Placement is two-pass: reserve standard lots first, then fill irregular leftovers. No overlaps or unclaimed land.

The road network is an explicit connected graph. A seeded randomized spanning tree between major junctions receives additional loops, branches and cul-de-sacs. Each road cell carries a four-bit N/E/S/W connection mask, classified as a straight, corner, T-junction, crossroads or dead-end. A retained network spine connects all three player spawn sectors. Road cells and the adjoining plots both create collision geometry and share the same seed between LAN host and client.

Every plot has a stable template ID, size, category and geometry variant. The library currently defines **104 catalog entries**, including **8 distinct geometric layouts for each of the 4 standard 2x2 categories (32 definitions)**, and 3 definitions per category for each of the six other footprints (72 more). Eight procedural collision-layout motifs are currently supported for the standard footprint. This is a *definition library*, not 104 fully modeled/rendered 3D lots.

- **HAZARD:** Infested tenements, substations, chemical stores, collapses, burned clinics, occult sites, flood and contamination sites. Extra hard cover and priority hostile spawn regions.
- **RESOURCE:** Salvage yards, munitions stores, marketplaces, depots, workshops, pharmacies, scrapyards, terminals. Preferred cash, ammo and supplies placement.
- **SAFE:** Guard positions, shelters, medical facilities, protected garages, safehouses, ranger posts, defensive shops and operations centers. Preferred medical/health pickups.
- **OPEN:** Plazas, parking courts, vacant lots, parks, terminals, demolition sites and playgrounds. Reduced outer-wall density and long sightlines.

Placement preferences do not yet confer immunity, area healing, scripted traps, persistent loot crates or faction ownership. Those need host-authoritative gameplay implementation and dedicated acceptance tests. The renderer still uses the first five licensed Kenney GLBs; a catalog name is not evidence its corresponding unique environment mesh is already imported.

A final accessibility pass connects isolated walkable interiors using minimal doorway cuts without breaking the sealed outer border. The current minimap colors categories (hazard red, resource green, safe blue, open ochre; roads gray) and player visibility tests use actual road classification rather than fixed modulo-12 streets.

## Continental composition and player construction

The 132×132 active city is now one component of a larger 224×224 macro-parcel island generator. The continent is an independently seedable topology blueprint, not streamed gameplay yet; it places seven cities with varied footprints, road connections and categorized lots. See [CONTINENT_WORLD_GENERATION.md](CONTINENT_WORLD_GENERATION.md).

The city active in the current match treats generated roads and developed lots as immutable world structures. Only actual `OPEN / empty-lot` ground is eligible for player structures after host checks for footprint, props, navigation and costs. Player-owned structures are a separate layer. World-structure overlap rejects with `WORLD_STRUCTURE`; former tests that placed turrets on paved streets are replaced with vacant-site tests.

## Acceptance gates

1. **Foundation (implemented/tested locally):** deterministic seed; road graph fully connected; all parcel cells assigned exactly once; at least four 2x2 standard plots per tested seed, one of each category; closed outer border; connected playable floor; prior world spawn, pickup and NPC collision invariants. Run `javac --release 17 -d out src/main/java/veil/*.java`, then `java -cp out veil.CityLayoutTest` and `java -cp out veil.WorldSelfTest`.
2. **3D road tiles (next):** bind distinct authored/source-verified assets for straight, corner, T, crossroads, dead-end; verify orientation, continuity, scale and collider; support alleys and sidewalks; inspect rendered screenshots and performance, not just catalog IDs.
3. **Plot realization:** ingest legitimate selected urban assets per template (facades, floor plans, entrances, interior cover, sound, decals and lighting), build a meaningful large content library with model/texture provenance and per-template physical differences. Reject nonrendered placeholders.
4. **Gameplay:** host-authoritative hazard effects, actual resource spawn tables, defensive safe-zone utility, open sightline balancing, placement/repair of player structures, objective and accessibility tagging. Validate no unavoidable spawn damage or inaccessible objective.
5. **World scaling:** cross-plot buildings, bridges, floors, sector biome weighting, streamed larger maps, high-density stress tests, spawn fairness, LAN parity and 30-minute desktop traversal. Do not claim alpha R2 passed before manual testing.

The plot catalog and road graph are designed to be extended without changing `World` combat collision methods or multiplayer seed authority. Add new template dimensions and specific manifestations through a registry, then validate all map seeds and asset source licenses.
