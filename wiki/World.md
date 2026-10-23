# World Generation and Construction Authority

[Home](Home.md) · [Gameplay](Gameplay.md) · [Structures](Structures.md)

**Generation objective.** A seeded island/continent bounded by sea, differentiated forest/plains/mountain/wetland regions, a connected road plan and seven city sites. The detailed playable city sector is a 132 × 132 arena; continental terrain, roads, city sites and resource markers have additional deterministic planners/rendering work but are **not** all simultaneously playable walkable 3D cities.

**City plots.** Roads form a reusable modular graph; plots are categorized SAFE, RESOURCE, HAZARD and OPEN, including multiple variants. HAZARD buildings seed location-owned Lost populations. RESOURCE plots include physical pallet yards. City-owned layouts are world structures, not buildable player lots, whether a plot is empty or decorated.

**Miasma.** Each city reserves a purple/black high-altitude air-denial field to preserve ground salvage gameplay. The geometry/policy registry exists; aircraft flight and ordnance systems are pending live integration. No aerial attack or supply shortcut should bypass protected city resource access.

**Wilderness.** Irregular resource clusters and vegetation are based on seed and biome. The domain layer models harvesting, depletion and replenishment. Streaming, authority, persistence, navigable roads and permanent wilderness claim construction are separate integration gates.

**Settlement target.** Permanent wilderness bases support faction-exclusive production and defense, utilities, sustained upkeep and upgrades through level 50. This is a design/catalog program, not present fully placed architecture.

Sources: [continent generation](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/CONTINENT_WORLD_GENERATION.md) · [plots](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/PLOT_GENERATION.md) · [continuous-world status](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/ONE_CONTINUOUS_WORLD_AND_HARVESTING.md) · [miasma](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md).
