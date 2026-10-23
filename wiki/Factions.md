# The Three Playable Factions

[Home](Home.md) · [Classes](Classes.md) · [Structures](Structures.md)

**The Lost are not a fourth playable faction.** They are hostile world NPCs with site-owned lifetimes and an independent proposed weapons ecosystem: [The Lost](The-Lost.md).

## The Unraveled — people against the impossible

The Unraveled salvage broken infrastructure, practice emergency medicine and defend improvised settlements. **Visual identity:** broken halo, oxidized teal, scavenged coats, radio noise, patched armor. **Combat:** Warden / Stitcher / Hollow. **Economy:** scavenging, metalworks, precision machinery, grids, defensive towers and convoy logistics. **Strength:** recovery and engineering. **Trade dependency:** specialist occult/biological goods should not be produced by copy-pasting the other factions' recipe chains.

## The Penitent — congregation of the open wound

Penitent survivors interpret the Veil as an ordeal of revelation. **Visual identity:** weeping eye, candle gold, crimson, masks, bells, veils and ceremonial processions. **Combat:** Confessor / Flagellant / Votary. **Economy:** ritual textiles, sanctification, offerings, dread containment, confession structures and energy rites. **Strength:** composure, crowd control and ritual coordination. **Trade dependency:** structured exchanges with industrial and biological factions should be expensive, staged and voluntary, not monolithic universal conversion.

## The Begotten — altered bodies and alien adaptation

The Begotten include transformed humans and intruding organisms, bound in unstable broods rather than a single empire. **Visual identity:** split chrysalis, dark violet, pearlescent tissue, asymmetric silhouettes. **Combat:** Harrower / Devourer / Mourner. **Economy:** nutrient cultivation, organo-mechanical production, brood dens, biological processing and living fortifications. **Strength:** biological resilience, predatory movement and territory corruption. **Trade dependency:** requires distinctive biology/technology interfaces and contested inputs, not cheap rebranded Unraveled smelters.

## Hard faction rules

Same-faction players and crews are permanently allied within the current faction system; a crew is a **separate group**, not a faction, and current code caps it at three members. A player can belong to one faction. The Lost ally with **Begotten for monster targeting** under current host combat logic; they do not become Begotten-owned characters, build sites or economic institutions.

The intended economy is **three distinct production trees with complementary trade**, not one shared settlement hierarchy. The facility CSV includes `ALL` for genuinely common infrastructure and names faction-only branches independently. Recipe `faction` flags must survive server validation.

Source: [faction lore](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/FACTION_LORE_AND_ABILITY_CODEX.md) · [faction architecture](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/FACTION_HORROR_ARCHITECTURE.md) · [industrial progression](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/CRAFTING_INDUSTRY_INCURSION_PROGRESSION.md) · [alliance code](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/FactionGroups.java).

## Registered faction-exclusive infrastructure and processes

These entries are **not interchangeable reskins**. They are the separate faction branches currently exported from [facilities.csv](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/facilities.csv) and [industrial_recipes.csv](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/industrial_recipes.csv). Common `ALL` infrastructure remains shared, but each specialist branch retains its own station, ingredient chain and output. Production descriptors are not evidence of live factories.

### The Unraveled: 6 exclusive facilities and 9 processes

| Facility | Tier | Prerequisites | Linked processes |
|---|---|---|---|
| [resilience-grid](Structures.md#resilience-grid) | INCURSION | assembly-line;signal-relay | [resilience-core](Recipes.md#industrial-resilience-core) |
| [unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) | ENCAMPMENT | claim-heart;hand-workshop | [unraveled-recover-scrap](Recipes.md#industrial-unraveled-recover-scrap) |
| [unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) | SALVAGE | unraveled-scrap-outpost;materials-recycler | [unraveled-make-bearings](Recipes.md#industrial-unraveled-make-bearings) |
| [unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) | STEAM | unraveled-machinist-bay;mechanical-shaft | [unraveled-pressure-vessel](Recipes.md#industrial-unraveled-pressure-vessel) |
| [unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) | ELECTRIC | unraveled-pressure-shop;electric-bus | [unraveled-calibrate-sensors](Recipes.md#industrial-unraveled-calibrate-sensors) |
| [unraveled-defense-works](Structures.md#unraveled-defense-works) | AUTOMATION | unraveled-sensor-lab;security-grid | [unraveled-defense-scanner](Recipes.md#industrial-unraveled-defense-scanner) |

| Process | Product | Era | Required station |
|---|---|---|---|
| [resilience-core](Recipes.md#industrial-resilience-core) | `RESILIENCE_CORE` × 1 | INCURSION | [resilience-grid](Structures.md#resilience-grid) |
| [ballistic-resupply](Recipes.md#industrial-ballistic-resupply) | `BALLISTIC_SUPPLY` × 2 | AUTOMATION | [industrial-workshop](Structures.md#industrial-workshop) |
| [field-maintenance](Recipes.md#industrial-field-maintenance) | `VEHICLE_REPAIR_KIT` × 1 | AUTOMATION | [motor-pool](Structures.md#motor-pool) |
| [secure-ammo-cache](Recipes.md#industrial-secure-ammo-cache) | `DEFENSE_CACHE` × 2 | AUTOMATION | [security-grid](Structures.md#security-grid) |
| [unraveled-recover-scrap](Recipes.md#industrial-unraveled-recover-scrap) | `SCRAP_METAL` × 2 | ENCAMPMENT | [unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) |
| [unraveled-make-bearings](Recipes.md#industrial-unraveled-make-bearings) | `BEARING_SET` × 2 | SALVAGE | [unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) |
| [unraveled-pressure-vessel](Recipes.md#industrial-unraveled-pressure-vessel) | `PRESSURE_TANK` × 1 | STEAM | [unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) |
| [unraveled-calibrate-sensors](Recipes.md#industrial-unraveled-calibrate-sensors) | `SENSOR_HEAD` × 2 | ELECTRIC | [unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) |
| [unraveled-defense-scanner](Recipes.md#industrial-unraveled-defense-scanner) | `RELAY_BOX` × 1 | AUTOMATION | [unraveled-defense-works](Structures.md#unraveled-defense-works) |

### The Penitent: 7 exclusive facilities and 11 processes

| Facility | Tier | Prerequisites | Linked processes |
|---|---|---|---|
| [terror-collector](Structures.md#terror-collector) | AUTOMATION | research-archive;signal-relay | [bound-liturgy](Recipes.md#industrial-bound-liturgy) |
| [dread-works](Structures.md#dread-works) | INCURSION | terror-collector;assembly-line | [dread-ward](Recipes.md#industrial-dread-ward), [sacred-supply](Recipes.md#industrial-sacred-supply) |
| [penitent-votive-altar](Structures.md#penitent-votive-altar) | ENCAMPMENT | claim-heart;herb-plot | [penitent-weave-ritual](Recipes.md#industrial-penitent-weave-ritual) |
| [penitent-reliquary](Structures.md#penitent-reliquary) | SALVAGE | penitent-votive-altar;forge | [penitent-engrave-reliquary](Recipes.md#industrial-penitent-engrave-reliquary) |
| [penitent-cantor-dais](Structures.md#penitent-cantor-dais) | STEAM | penitent-reliquary;mechanical-shaft | [penitent-cast-censer](Recipes.md#industrial-penitent-cast-censer) |
| [penitent-choir-relay](Structures.md#penitent-choir-relay) | ELECTRIC | penitent-cantor-dais;electric-bus | [penitent-score-hymn](Recipes.md#industrial-penitent-score-hymn) |
| [penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) | AUTOMATION | penitent-choir-relay;terror-collector | [penitent-sanctify-shell](Recipes.md#industrial-penitent-sanctify-shell) |

| Process | Product | Era | Required station |
|---|---|---|---|
| [dread-ward](Recipes.md#industrial-dread-ward) | `DREAD_WARD` × 1 | INCURSION | [dread-works](Structures.md#dread-works) |
| [votive-incense](Recipes.md#industrial-votive-incense) | `VOTIVE_INCENSE` × 2 | ENCAMPMENT | [cooking-hearth](Structures.md#cooking-hearth) |
| [witness-seal](Recipes.md#industrial-witness-seal) | `WITNESS_SEAL` × 2 | SALVAGE | [forge](Structures.md#forge) |
| [chorus-resonance](Recipes.md#industrial-chorus-resonance) | `CHORUS_RESONANCE` × 1 | AUTOMATION | [research-archive](Structures.md#research-archive) |
| [bound-liturgy](Recipes.md#industrial-bound-liturgy) | `BOUND_LITURGY` × 1 | AUTOMATION | [terror-collector](Structures.md#terror-collector) |
| [sacred-supply](Recipes.md#industrial-sacred-supply) | `SACRED_SUPPLY` × 1 | INCURSION | [dread-works](Structures.md#dread-works) |
| [penitent-weave-ritual](Recipes.md#industrial-penitent-weave-ritual) | `RITUAL_CLOTH` × 2 | ENCAMPMENT | [penitent-votive-altar](Structures.md#penitent-votive-altar) |
| [penitent-engrave-reliquary](Recipes.md#industrial-penitent-engrave-reliquary) | `RELIQUARY_PLATE` × 1 | SALVAGE | [penitent-reliquary](Structures.md#penitent-reliquary) |
| [penitent-cast-censer](Recipes.md#industrial-penitent-cast-censer) | `BRONZE_CENSER` × 1 | STEAM | [penitent-cantor-dais](Structures.md#penitent-cantor-dais) |
| [penitent-score-hymn](Recipes.md#industrial-penitent-score-hymn) | `CHOIR_SCORE` × 1 | ELECTRIC | [penitent-choir-relay](Structures.md#penitent-choir-relay) |
| [penitent-sanctify-shell](Recipes.md#industrial-penitent-sanctify-shell) | `CONSECRATED_SHELL` × 1 | AUTOMATION | [penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) |

### The Begotten: 7 exclusive facilities and 12 processes

| Facility | Tier | Prerequisites | Linked processes |
|---|---|---|---|
| [nutrient-digestor](Structures.md#nutrient-digestor) | AUTOMATION | water-works;industrial-workshop | [biomass-feed](Recipes.md#industrial-biomass-feed), [binding-resin](Recipes.md#industrial-binding-resin) |
| [incursion-anchor](Structures.md#incursion-anchor) | INCURSION | nutrient-digestor;assembly-line | [breach-seed](Recipes.md#industrial-breach-seed), [spine-supply](Recipes.md#industrial-spine-supply) |
| [begotten-brood-nest](Structures.md#begotten-brood-nest) | ENCAMPMENT | claim-heart;forager-garden | [begotten-mix-slurry](Recipes.md#industrial-begotten-mix-slurry) |
| [begotten-spore-basin](Structures.md#begotten-spore-basin) | SALVAGE | begotten-brood-nest;charcoal-kiln | [begotten-culture-spores](Recipes.md#industrial-begotten-culture-spores) |
| [begotten-chitin-vat](Structures.md#begotten-chitin-vat) | STEAM | begotten-spore-basin;steam-boiler | [begotten-harden-chitin](Recipes.md#industrial-begotten-harden-chitin) |
| [begotten-membrane-lab](Structures.md#begotten-membrane-lab) | ELECTRIC | begotten-chitin-vat;water-works | [begotten-grow-membrane](Recipes.md#industrial-begotten-grow-membrane) |
| [begotten-organ-lattice](Structures.md#begotten-organ-lattice) | AUTOMATION | begotten-membrane-lab;nutrient-digestor | [begotten-pulse-organ](Recipes.md#industrial-begotten-pulse-organ) |

| Process | Product | Era | Required station |
|---|---|---|---|
| [biomass-feed](Recipes.md#industrial-biomass-feed) | `NUTRIENT_STOCK` × 1 | AUTOMATION | [nutrient-digestor](Structures.md#nutrient-digestor) |
| [breach-seed](Recipes.md#industrial-breach-seed) | `BREACH_SEED` × 1 | INCURSION | [incursion-anchor](Structures.md#incursion-anchor) |
| [growth-feed](Recipes.md#industrial-growth-feed) | `GROWTH_FEED` × 2 | ENCAMPMENT | [cooking-hearth](Structures.md#cooking-hearth) |
| [viable-spores](Recipes.md#industrial-viable-spores) | `VIABLE_SPORES` × 2 | SALVAGE | [materials-recycler](Structures.md#materials-recycler) |
| [grown-chitin](Recipes.md#industrial-grown-chitin) | `GROWN_CHITIN` × 2 | ELECTRIC | [controlled-greenhouse](Structures.md#controlled-greenhouse) |
| [binding-resin](Recipes.md#industrial-binding-resin) | `BINDING_RESIN` × 1 | AUTOMATION | [nutrient-digestor](Structures.md#nutrient-digestor) |
| [spine-supply](Recipes.md#industrial-spine-supply) | `SPINE_SUPPLY` × 1 | INCURSION | [incursion-anchor](Structures.md#incursion-anchor) |
| [begotten-mix-slurry](Recipes.md#industrial-begotten-mix-slurry) | `NUTRIENT_SLURRY` × 2 | ENCAMPMENT | [begotten-brood-nest](Structures.md#begotten-brood-nest) |
| [begotten-culture-spores](Recipes.md#industrial-begotten-culture-spores) | `SPORE_POD` × 2 | SALVAGE | [begotten-spore-basin](Structures.md#begotten-spore-basin) |
| [begotten-harden-chitin](Recipes.md#industrial-begotten-harden-chitin) | `CHITIN_SHARDS` × 2 | STEAM | [begotten-chitin-vat](Structures.md#begotten-chitin-vat) |
| [begotten-grow-membrane](Recipes.md#industrial-begotten-grow-membrane) | `LIVING_MEMBRANE` × 1 | ELECTRIC | [begotten-membrane-lab](Structures.md#begotten-membrane-lab) |
| [begotten-pulse-organ](Recipes.md#industrial-begotten-pulse-organ) | `ORGAN_CORE` × 1 | AUTOMATION | [begotten-organ-lattice](Structures.md#begotten-organ-lattice) |

Distinct production is constrained by [the faction industry and trade specification](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/CITY_MIASMA_SETTLEMENT_ECONOMY_AND_TRADE.md); no faction can skip staged cooperation by exploiting a universal factory recipe.
