# Complete Item Production and Consumption Index

[Home](Home.md) · [Items](Items.md) · [Recipes](Recipes.md) · [Structures](Structures.md)

This page gives **every** direct producer and every direct ingredient/structure-cost consumer of the 157 item IDs currently exported from the registry. Nothing is hidden after a "+more" summary. Ingredient quantities are per batch, output quantities per completed recipe, and structure quantities per built blueprint. These are *declared formulas*; see [Source and Status](Source-and-Status.md) before assuming that industrial outputs or full physical construction are playable.

Compiled from [item IDs](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/items.csv), [hand recipes](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/hand_recipes.csv), [industrial recipes](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/industrial_recipes.csv), [recipe inputs](https://github.com/mrcalzon02/The-Malefic-Veil/tree/main/economy) and [facility costs](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/economy/facility_costs.csv). The exported CSVs follow the Java registry.

## Timber (`TIMBER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (47):** [hand:campfire](Recipes.md#handcampfire) — needs 3, makes 1 × `CAMPFIRE`; [hand:tent](Recipes.md#handtent) — needs 2, makes 1 × `TENT_KIT`; [hand:crate](Recipes.md#handcrate) — needs 5, makes 1 × `CRATE_KIT`; [hand:fence](Recipes.md#handfence) — needs 3, makes 2 × `FENCE_SECTION`; [hand:stake-trap](Recipes.md#handstake-trap) — needs 2, makes 1 × `STAKE_TRAP`; [hand:noise-alarm](Recipes.md#handnoise-alarm) — needs 1, makes 1 × `NOISE_ALARM`; [hand:workbench](Recipes.md#handworkbench) — needs 6, makes 1 × `WORKBENCH`; [hand:shelter-kit](Recipes.md#handshelter-kit) — needs 8, makes 1 × `SHELTER_KIT`; [hand:split-kindling](Recipes.md#handsplit-kindling) — needs 2, makes 2 × `FIREWOOD`; [hand:pit-charcoal](Recipes.md#handpit-charcoal) — needs 5, makes 1 × `CHARCOAL`; [industry:split-firewood](Recipes.md#industrialsplit-firewood) — needs 2, makes 3 × `FIREWOOD`; [industry:charcoal-burn](Recipes.md#industrialcharcoal-burn) — needs 4, makes 2 × `CHARCOAL`; [facility:camp-stake](Structures.md#camp-stake) — build cost 1 (STRANDED, ALL); [facility:claim-heart](Structures.md#claim-heart) — build cost 12 (ENCAMPMENT, ALL); [facility:hand-workshop](Structures.md#hand-workshop) — build cost 10 (ENCAMPMENT, ALL); [facility:dry-store](Structures.md#dry-store) — build cost 12 (ENCAMPMENT, ALL); [facility:cooking-hearth](Structures.md#cooking-hearth) — build cost 10 (ENCAMPMENT, ALL); [facility:rain-catcher](Structures.md#rain-catcher) — build cost 8 (ENCAMPMENT, ALL); [facility:cargo-laydown](Structures.md#cargo-laydown) — build cost 12 (ENCAMPMENT, ALL); [facility:field-shelter](Structures.md#field-shelter) — build cost 10 (ENCAMPMENT, ALL); [facility:forager-garden](Structures.md#forager-garden) — build cost 12 (ENCAMPMENT, ALL); [facility:seed-nursery](Structures.md#seed-nursery) — build cost 12 (ENCAMPMENT, ALL); [facility:compost-yard](Structures.md#compost-yard) — build cost 10 (ENCAMPMENT, ALL); [facility:herb-plot](Structures.md#herb-plot) — build cost 8 (ENCAMPMENT, ALL); [facility:root-cellar](Structures.md#root-cellar) — build cost 18 (ENCAMPMENT, ALL); [facility:smokehouse](Structures.md#smokehouse) — build cost 16 (ENCAMPMENT, ALL); [facility:fiber-loom](Structures.md#fiber-loom) — build cost 14 (ENCAMPMENT, ALL); [facility:watch-post](Structures.md#watch-post) — build cost 14 (ENCAMPMENT, ALL); [facility:palisade-section](Structures.md#palisade-section) — build cost 12 (ENCAMPMENT, ALL); [facility:charcoal-kiln](Structures.md#charcoal-kiln) — build cost 14 (SALVAGE, ALL); [facility:stone-furnace](Structures.md#stone-furnace) — build cost 12 (SALVAGE, ALL); [facility:freight-bay](Structures.md#freight-bay) — build cost 16 (SALVAGE, ALL); [facility:grain-mill](Structures.md#grain-mill) — build cost 18 (SALVAGE, ALL); [facility:animal-pen](Structures.md#animal-pen) — build cost 30 (SALVAGE, ALL); [facility:materials-recycler](Structures.md#materials-recycler) — build cost 10 (SALVAGE, ALL); [facility:mechanical-shaft](Structures.md#mechanical-shaft) — build cost 14 (STEAM, ALL); [facility:conveyor-line](Structures.md#conveyor-line) — build cost 12 (STEAM, ALL); [facility:controlled-greenhouse](Structures.md#controlled-greenhouse) — build cost 18 (ELECTRIC, ALL); [facility:command-hall](Structures.md#command-hall) — build cost 24 (AUTOMATION, ALL); [facility:infirmary](Structures.md#infirmary) — build cost 16 (AUTOMATION, ALL); [facility:wall-gate-system](Structures.md#wall-gate-system) — build cost 18 (ENCAMPMENT, ALL); [facility:cold-store](Structures.md#cold-store) — build cost 22 (SALVAGE, ALL); [facility:barracks](Structures.md#barracks) — build cost 28 (SALVAGE, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 90 (INCURSION, ALL); [facility:unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) — build cost 12 (ENCAMPMENT, UNRAVELED); [facility:penitent-votive-altar](Structures.md#penitent-votive-altar) — build cost 10 (ENCAMPMENT, PENITENT); [facility:begotten-brood-nest](Structures.md#begotten-brood-nest) — build cost 12 (ENCAMPMENT, BEGOTTEN)

## Field Stone (`FIELD_STONE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (52):** [hand:campfire](Recipes.md#handcampfire) — needs 2, makes 1 × `CAMPFIRE`; [hand:water-filter](Recipes.md#handwater-filter) — needs 2, makes 1 × `FILTER`; [hand:workbench](Recipes.md#handworkbench) — needs 4, makes 1 × `WORKBENCH`; [hand:camp-firebrick](Recipes.md#handcamp-firebrick) — needs 5, makes 1 × `FIRED_BRICKS`; [industry:mill-crops](Recipes.md#industrialmill-crops) — needs 1, makes 2 × `MEAL_SACK`; [industry:firebrick-batch](Recipes.md#industrialfirebrick-batch) — needs 4, makes 2 × `FIRED_BRICKS`; [facility:claim-heart](Structures.md#claim-heart) — build cost 12 (ENCAMPMENT, ALL); [facility:hand-workshop](Structures.md#hand-workshop) — build cost 6 (ENCAMPMENT, ALL); [facility:dry-store](Structures.md#dry-store) — build cost 8 (ENCAMPMENT, ALL); [facility:cooking-hearth](Structures.md#cooking-hearth) — build cost 12 (ENCAMPMENT, ALL); [facility:rain-catcher](Structures.md#rain-catcher) — build cost 6 (ENCAMPMENT, ALL); [facility:cargo-laydown](Structures.md#cargo-laydown) — build cost 10 (ENCAMPMENT, ALL); [facility:field-shelter](Structures.md#field-shelter) — build cost 4 (ENCAMPMENT, ALL); [facility:forager-garden](Structures.md#forager-garden) — build cost 6 (ENCAMPMENT, ALL); [facility:seed-nursery](Structures.md#seed-nursery) — build cost 5 (ENCAMPMENT, ALL); [facility:compost-yard](Structures.md#compost-yard) — build cost 7 (ENCAMPMENT, ALL); [facility:herb-plot](Structures.md#herb-plot) — build cost 5 (ENCAMPMENT, ALL); [facility:root-cellar](Structures.md#root-cellar) — build cost 12 (ENCAMPMENT, ALL); [facility:smokehouse](Structures.md#smokehouse) — build cost 10 (ENCAMPMENT, ALL); [facility:watch-post](Structures.md#watch-post) — build cost 8 (ENCAMPMENT, ALL); [facility:palisade-section](Structures.md#palisade-section) — build cost 3 (ENCAMPMENT, ALL); [facility:charcoal-kiln](Structures.md#charcoal-kiln) — build cost 16 (SALVAGE, ALL); [facility:stone-furnace](Structures.md#stone-furnace) — build cost 30 (SALVAGE, ALL); [facility:forge](Structures.md#forge) — build cost 20 (SALVAGE, ALL); [facility:freight-bay](Structures.md#freight-bay) — build cost 24 (SALVAGE, ALL); [facility:grain-mill](Structures.md#grain-mill) — build cost 16 (SALVAGE, ALL); [facility:irrigation-works](Structures.md#irrigation-works) — build cost 16 (SALVAGE, ALL); [facility:animal-pen](Structures.md#animal-pen) — build cost 8 (SALVAGE, ALL); [facility:steam-boiler](Structures.md#steam-boiler) — build cost 28 (STEAM, ALL); [facility:metal-press](Structures.md#metal-press) — build cost 16 (STEAM, ALL); [facility:electric-bus](Structures.md#electric-bus) — build cost 12 (ELECTRIC, ALL); [facility:automatic-sorter](Structures.md#automatic-sorter) — build cost 12 (ELECTRIC, ALL); [facility:water-works](Structures.md#water-works) — build cost 18 (ELECTRIC, ALL); [facility:industrial-workshop](Structures.md#industrial-workshop) — build cost 28 (AUTOMATION, ALL); [facility:motor-pool](Structures.md#motor-pool) — build cost 30 (AUTOMATION, ALL); [facility:assembly-line](Structures.md#assembly-line) — build cost 35 (AUTOMATION, ALL); [facility:resilience-grid](Structures.md#resilience-grid) — build cost 60 (INCURSION, UNRAVELED); [facility:dread-works](Structures.md#dread-works) — build cost 60 (INCURSION, PENITENT); [facility:incursion-anchor](Structures.md#incursion-anchor) — build cost 60 (INCURSION, BEGOTTEN); [facility:wall-gate-system](Structures.md#wall-gate-system) — build cost 12 (ENCAMPMENT, ALL); [facility:cold-store](Structures.md#cold-store) — build cost 18 (SALVAGE, ALL); [facility:barracks](Structures.md#barracks) — build cost 20 (SALVAGE, ALL); [facility:purification-works](Structures.md#purification-works) — build cost 24 (ELECTRIC, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 140 (INCURSION, ALL); [facility:unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) — build cost 8 (ENCAMPMENT, UNRAVELED); [facility:unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) — build cost 12 (SALVAGE, UNRAVELED); [facility:penitent-votive-altar](Structures.md#penitent-votive-altar) — build cost 10 (ENCAMPMENT, PENITENT); [facility:penitent-reliquary](Structures.md#penitent-reliquary) — build cost 16 (SALVAGE, PENITENT); [facility:penitent-cantor-dais](Structures.md#penitent-cantor-dais) — build cost 18 (STEAM, PENITENT); [facility:penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) — build cost 24 (AUTOMATION, PENITENT); [facility:begotten-spore-basin](Structures.md#begotten-spore-basin) — build cost 16 (SALVAGE, BEGOTTEN); [facility:begotten-chitin-vat](Structures.md#begotten-chitin-vat) — build cost 18 (STEAM, BEGOTTEN)

## Fiber (`FIBER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (41):** [hand:bandage](Recipes.md#handbandage) — needs 2, makes 1 × `BANDAGE`; [hand:water-filter](Recipes.md#handwater-filter) — needs 3, makes 1 × `FILTER`; [hand:tent](Recipes.md#handtent) — needs 4, makes 1 × `TENT_KIT`; [hand:crate](Recipes.md#handcrate) — needs 2, makes 1 × `CRATE_KIT`; [hand:fence](Recipes.md#handfence) — needs 1, makes 2 × `FENCE_SECTION`; [hand:stake-trap](Recipes.md#handstake-trap) — needs 1, makes 1 × `STAKE_TRAP`; [hand:noise-alarm](Recipes.md#handnoise-alarm) — needs 2, makes 1 × `NOISE_ALARM`; [hand:workbench](Recipes.md#handworkbench) — needs 2, makes 1 × `WORKBENCH`; [hand:shelter-kit](Recipes.md#handshelter-kit) — needs 5, makes 1 × `SHELTER_KIT`; [hand:field-dressing](Recipes.md#handfield-dressing) — needs 3, makes 2 × `BANDAGE`; [hand:rough-compost](Recipes.md#handrough-compost) — needs 3, makes 1 × `COMPOST`; [hand:hand-woven-cloth](Recipes.md#handhand-woven-cloth) — needs 8, makes 1 × `CLOTH_ROLL`; [hand:bunch-herbs](Recipes.md#handbunch-herbs) — needs 2, makes 1 × `HERBAL_BUNDLE`; [industry:compost-ferment](Recipes.md#industrialcompost-ferment) — needs 2, makes 2 × `COMPOST`; [industry:weave-cloth](Recipes.md#industrialweave-cloth) — needs 4, makes 2 × `CLOTH_ROLL`; [industry:cellar-vegetables](Recipes.md#industrialcellar-vegetables) — needs 1, makes 2 × `CELLARED_CROPS`; [facility:claim-heart](Structures.md#claim-heart) — build cost 6 (ENCAMPMENT, ALL); [facility:hand-workshop](Structures.md#hand-workshop) — build cost 4 (ENCAMPMENT, ALL); [facility:dry-store](Structures.md#dry-store) — build cost 6 (ENCAMPMENT, ALL); [facility:cooking-hearth](Structures.md#cooking-hearth) — build cost 3 (ENCAMPMENT, ALL); [facility:rain-catcher](Structures.md#rain-catcher) — build cost 8 (ENCAMPMENT, ALL); [facility:cargo-laydown](Structures.md#cargo-laydown) — build cost 6 (ENCAMPMENT, ALL); [facility:field-shelter](Structures.md#field-shelter) — build cost 8 (ENCAMPMENT, ALL); [facility:forager-garden](Structures.md#forager-garden) — build cost 6 (ENCAMPMENT, ALL); [facility:seed-nursery](Structures.md#seed-nursery) — build cost 8 (ENCAMPMENT, ALL); [facility:compost-yard](Structures.md#compost-yard) — build cost 5 (ENCAMPMENT, ALL); [facility:herb-plot](Structures.md#herb-plot) — build cost 6 (ENCAMPMENT, ALL); [facility:root-cellar](Structures.md#root-cellar) — build cost 4 (ENCAMPMENT, ALL); [facility:smokehouse](Structures.md#smokehouse) — build cost 5 (ENCAMPMENT, ALL); [facility:fiber-loom](Structures.md#fiber-loom) — build cost 12 (ENCAMPMENT, ALL); [facility:watch-post](Structures.md#watch-post) — build cost 6 (ENCAMPMENT, ALL); [facility:palisade-section](Structures.md#palisade-section) — build cost 3 (ENCAMPMENT, ALL); [facility:charcoal-kiln](Structures.md#charcoal-kiln) — build cost 3 (SALVAGE, ALL); [facility:grain-mill](Structures.md#grain-mill) — build cost 6 (SALVAGE, ALL); [facility:animal-pen](Structures.md#animal-pen) — build cost 12 (SALVAGE, ALL); [facility:wall-gate-system](Structures.md#wall-gate-system) — build cost 8 (ENCAMPMENT, ALL); [facility:barracks](Structures.md#barracks) — build cost 16 (SALVAGE, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 60 (INCURSION, ALL); [facility:unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) — build cost 4 (ENCAMPMENT, UNRAVELED); [facility:penitent-votive-altar](Structures.md#penitent-votive-altar) — build cost 8 (ENCAMPMENT, PENITENT); [facility:begotten-brood-nest](Structures.md#begotten-brood-nest) — build cost 9 (ENCAMPMENT, BEGOTTEN)

## Edibles (`EDIBLES`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (16):** [hand:camp-ration](Recipes.md#handcamp-ration) — needs 3, makes 1 × `RATIONS`; [hand:rough-compost](Recipes.md#handrough-compost) — needs 4, makes 1 × `COMPOST`; [hand:smoke-ration](Recipes.md#handsmoke-ration) — needs 5, makes 1 × `SMOKED_RATIONS`; [industry:preserve-rations](Recipes.md#industrialpreserve-rations) — needs 3, makes 2 × `RATIONS`; [industry:compost-ferment](Recipes.md#industrialcompost-ferment) — needs 2, makes 2 × `COMPOST`; [industry:grow-field-crops](Recipes.md#industrialgrow-field-crops) — needs 2, makes 4 × `FIELD_CROPS`; [industry:smoke-rations](Recipes.md#industrialsmoke-rations) — needs 4, makes 2 × `SMOKED_RATIONS`; [industry:biomass-feed](Recipes.md#industrialbiomass-feed) — needs 5, makes 1 × `NUTRIENT_STOCK`; [industry:growth-feed](Recipes.md#industrialgrowth-feed) — needs 3, makes 2 × `GROWTH_FEED`; [industry:begotten-mix-slurry](Recipes.md#industrialbegotten-mix-slurry) — needs 4, makes 2 × `NUTRIENT_SLURRY`; [facility:nutrient-digestor](Structures.md#nutrient-digestor) — build cost 24 (AUTOMATION, BEGOTTEN); [facility:begotten-brood-nest](Structures.md#begotten-brood-nest) — build cost 12 (ENCAMPMENT, BEGOTTEN); [facility:begotten-spore-basin](Structures.md#begotten-spore-basin) — build cost 18 (SALVAGE, BEGOTTEN); [facility:begotten-chitin-vat](Structures.md#begotten-chitin-vat) — build cost 25 (STEAM, BEGOTTEN); [facility:begotten-membrane-lab](Structures.md#begotten-membrane-lab) — build cost 30 (ELECTRIC, BEGOTTEN); [facility:begotten-organ-lattice](Structures.md#begotten-organ-lattice) — build cost 40 (AUTOMATION, BEGOTTEN)

## Medicinal Herbs (`MEDICINAL_HERBS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (16):** [hand:bandage](Recipes.md#handbandage) — needs 1, makes 1 × `BANDAGE`; [hand:field-dressing](Recipes.md#handfield-dressing) — needs 2, makes 2 × `BANDAGE`; [hand:bunch-herbs](Recipes.md#handbunch-herbs) — needs 3, makes 1 × `HERBAL_BUNDLE`; [industry:grow-medicinals](Recipes.md#industrialgrow-medicinals) — needs 1, makes 2 × `HERBAL_BUNDLE`; [industry:votive-incense](Recipes.md#industrialvotive-incense) — needs 2, makes 2 × `VOTIVE_INCENSE`; [industry:viable-spores](Recipes.md#industrialviable-spores) — needs 2, makes 2 × `VIABLE_SPORES`; [industry:penitent-weave-ritual](Recipes.md#industrialpenitent-weave-ritual) — needs 1, makes 2 × `RITUAL_CLOTH`; [industry:begotten-culture-spores](Recipes.md#industrialbegotten-culture-spores) — needs 2, makes 2 × `SPORE_POD`; [facility:research-archive](Structures.md#research-archive) — build cost 20 (AUTOMATION, ALL); [facility:infirmary](Structures.md#infirmary) — build cost 14 (AUTOMATION, ALL); [facility:terror-collector](Structures.md#terror-collector) — build cost 24 (AUTOMATION, PENITENT); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 30 (INCURSION, ALL); [facility:penitent-reliquary](Structures.md#penitent-reliquary) — build cost 10 (SALVAGE, PENITENT); [facility:penitent-choir-relay](Structures.md#penitent-choir-relay) — build cost 16 (ELECTRIC, PENITENT); [facility:begotten-spore-basin](Structures.md#begotten-spore-basin) — build cost 8 (SALVAGE, BEGOTTEN); [facility:begotten-membrane-lab](Structures.md#begotten-membrane-lab) — build cost 12 (ELECTRIC, BEGOTTEN)

## Raw Ore (`RAW_ORE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (14):** [industry:iron-bloom](Recipes.md#industrialiron-bloom) — needs 4, makes 1 × `IRON_BLOOM`; [industry:copper-wire](Recipes.md#industrialcopper-wire) — needs 3, makes 2 × `CONDUCTOR_WIRE`; [industry:sort-mineral-feed](Recipes.md#industrialsort-mineral-feed) — needs 4, makes 2 × `SORTED_ORE`; [industry:unraveled-recover-scrap](Recipes.md#industrialunraveled-recover-scrap) — needs 2, makes 2 × `SCRAP_METAL`; [industry:begotten-harden-chitin](Recipes.md#industrialbegotten-harden-chitin) — needs 1, makes 2 × `CHITIN_SHARDS`; [facility:stone-furnace](Structures.md#stone-furnace) — build cost 4 (SALVAGE, ALL); [facility:forge](Structures.md#forge) — build cost 8 (SALVAGE, ALL); [facility:steam-boiler](Structures.md#steam-boiler) — build cost 8 (STEAM, ALL); [facility:generator](Structures.md#generator) — build cost 10 (ELECTRIC, ALL); [facility:precision-lathe](Structures.md#precision-lathe) — build cost 8 (ELECTRIC, ALL); [facility:fuel-refinery](Structures.md#fuel-refinery) — build cost 12 (ELECTRIC, ALL); [facility:signal-relay](Structures.md#signal-relay) — build cost 8 (AUTOMATION, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 24 (INCURSION, ALL); [facility:unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) — build cost 8 (STEAM, UNRAVELED)

## Dirty Water (`DIRTY_WATER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (4):** [hand:boil-water](Recipes.md#handboil-water) — needs 1, makes 1 × `CLEAN_WATER`; [industry:boil-water](Recipes.md#industrialboil-water) — needs 1, makes 1 × `CLEAN_WATER`; [industry:treat-contaminated-water](Recipes.md#industrialtreat-contaminated-water) — needs 6, makes 4 × `STERILE_WATER`; [industry:begotten-mix-slurry](Recipes.md#industrialbegotten-mix-slurry) — needs 2, makes 2 × `NUTRIENT_SLURRY`

## Clean Water (`CLEAN_WATER`)

**Produced by:** [hand:boil-water](Recipes.md#handboil-water) — 1 × per craft at FIRE; [industry:boil-water](Recipes.md#industrialboil-water) — 1 × at [cooking-hearth](Structures.md#cooking-hearth) (ALL)

**Consumed by (8):** [hand:camp-ration](Recipes.md#handcamp-ration) — needs 1, makes 1 × `RATIONS`; [industry:preserve-rations](Recipes.md#industrialpreserve-rations) — needs 1, makes 2 × `RATIONS`; [industry:grow-field-crops](Recipes.md#industrialgrow-field-crops) — needs 3, makes 4 × `FIELD_CROPS`; [industry:grow-medicinals](Recipes.md#industrialgrow-medicinals) — needs 2, makes 2 × `HERBAL_BUNDLE`; [industry:biomass-feed](Recipes.md#industrialbiomass-feed) — needs 3, makes 1 × `NUTRIENT_STOCK`; [industry:growth-feed](Recipes.md#industrialgrowth-feed) — needs 1, makes 2 × `GROWTH_FEED`; [industry:cold-preservation](Recipes.md#industrialcold-preservation) — needs 1, makes 3 × `PRESERVED_RATIONS`; [industry:begotten-grow-membrane](Recipes.md#industrialbegotten-grow-membrane) — needs 2, makes 1 × `LIVING_MEMBRANE`

## Firewood (`FIREWOOD`)

**Produced by:** [hand:split-kindling](Recipes.md#handsplit-kindling) — 2 × per craft at HAND; [industry:split-firewood](Recipes.md#industrialsplit-firewood) — 3 × at [cooking-hearth](Structures.md#cooking-hearth) (ALL)

**Consumed by (9):** [hand:camp-ration](Recipes.md#handcamp-ration) — needs 1, makes 1 × `RATIONS`; [hand:pit-charcoal](Recipes.md#handpit-charcoal) — needs 2, makes 1 × `CHARCOAL`; [hand:smoke-ration](Recipes.md#handsmoke-ration) — needs 3, makes 1 × `SMOKED_RATIONS`; [hand:camp-firebrick](Recipes.md#handcamp-firebrick) — needs 4, makes 1 × `FIRED_BRICKS`; [industry:boil-water](Recipes.md#industrialboil-water) — needs 1, makes 1 × `CLEAN_WATER`; [industry:smoke-rations](Recipes.md#industrialsmoke-rations) — needs 2, makes 2 × `SMOKED_RATIONS`; [industry:charcoal-burn](Recipes.md#industrialcharcoal-burn) — needs 1, makes 2 × `CHARCOAL`; [industry:votive-incense](Recipes.md#industrialvotive-incense) — needs 2, makes 2 × `VOTIVE_INCENSE`; [industry:unraveled-recover-scrap](Recipes.md#industrialunraveled-recover-scrap) — needs 1, makes 2 × `SCRAP_METAL`

## Charcoal (`CHARCOAL`)

**Produced by:** [hand:pit-charcoal](Recipes.md#handpit-charcoal) — 1 × per craft at FIRE; [industry:charcoal-burn](Recipes.md#industrialcharcoal-burn) — 2 × at [charcoal-kiln](Structures.md#charcoal-kiln) (ALL)

**Consumed by (6):** [industry:firebrick-batch](Recipes.md#industrialfirebrick-batch) — needs 1, makes 2 × `FIRED_BRICKS`; [industry:iron-bloom](Recipes.md#industrialiron-bloom) — needs 3, makes 1 × `IRON_BLOOM`; [industry:steel-billet](Recipes.md#industrialsteel-billet) — needs 1, makes 1 × `METAL_BILLET`; [industry:sort-mineral-feed](Recipes.md#industrialsort-mineral-feed) — needs 1, makes 2 × `SORTED_ORE`; [industry:temper-alloy](Recipes.md#industrialtemper-alloy) — needs 2, makes 1 × `HARDENED_ALLOY`; [industry:treat-contaminated-water](Recipes.md#industrialtreat-contaminated-water) — needs 2, makes 4 × `STERILE_WATER`

## Fired Bricks (`FIRED_BRICKS`)

**Produced by:** [hand:camp-firebrick](Recipes.md#handcamp-firebrick) — 1 × per craft at FIRE; [industry:firebrick-batch](Recipes.md#industrialfirebrick-batch) — 2 × at [stone-furnace](Structures.md#stone-furnace) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Iron Bloom (`IRON_BLOOM`)

**Produced by:** [industry:iron-bloom](Recipes.md#industrialiron-bloom) — 1 × at [forge](Structures.md#forge) (ALL)

**Consumed by (1):** [industry:steel-billet](Recipes.md#industrialsteel-billet) — needs 2, makes 1 × `METAL_BILLET`

## Metal Billet (`METAL_BILLET`)

**Produced by:** [industry:steel-billet](Recipes.md#industrialsteel-billet) — 1 × at [forge](Structures.md#forge) (ALL)

**Consumed by (9):** [industry:fasteners](Recipes.md#industrialfasteners) — needs 1, makes 4 × `FASTENERS`; [industry:driveshaft](Recipes.md#industrialdriveshaft) — needs 3, makes 1 × `DRIVE_SHAFT`; [industry:gear-train](Recipes.md#industrialgear-train) — needs 2, makes 1 × `GEAR_ASSEMBLY`; [industry:copper-wire](Recipes.md#industrialcopper-wire) — needs 1, makes 2 × `CONDUCTOR_WIRE`; [industry:precision-fit](Recipes.md#industrialprecision-fit) — needs 3, makes 1 × `PRECISION_MODULE`; [industry:temper-alloy](Recipes.md#industrialtemper-alloy) — needs 3, makes 1 × `HARDENED_ALLOY`; [industry:witness-seal](Recipes.md#industrialwitness-seal) — needs 1, makes 2 × `WITNESS_SEAL`; [industry:unraveled-pressure-vessel](Recipes.md#industrialunraveled-pressure-vessel) — needs 2, makes 1 × `PRESSURE_TANK`; [industry:penitent-engrave-reliquary](Recipes.md#industrialpenitent-engrave-reliquary) — needs 2, makes 1 × `RELIQUARY_PLATE`

## Fasteners (`FASTENERS`)

**Produced by:** [industry:fasteners](Recipes.md#industrialfasteners) — 4 × at [forge](Structures.md#forge) (ALL)

**Consumed by (5):** [industry:driveshaft](Recipes.md#industrialdriveshaft) — needs 2, makes 1 × `DRIVE_SHAFT`; [industry:gear-train](Recipes.md#industrialgear-train) — needs 2, makes 1 × `GEAR_ASSEMBLY`; [industry:ballistic-resupply](Recipes.md#industrialballistic-resupply) — needs 4, makes 2 × `BALLISTIC_SUPPLY`; [industry:field-maintenance](Recipes.md#industrialfield-maintenance) — needs 3, makes 1 × `VEHICLE_REPAIR_KIT`; [industry:unraveled-make-bearings](Recipes.md#industrialunraveled-make-bearings) — needs 1, makes 2 × `BEARING_SET`

## Drive Shaft (`DRIVE_SHAFT`)

**Produced by:** [industry:driveshaft](Recipes.md#industrialdriveshaft) — 1 × at [metal-press](Structures.md#metal-press) (ALL)

**Consumed by (2):** [industry:steam-pump-head](Recipes.md#industrialsteam-pump-head) — needs 1, makes 1 × `PUMP_HEAD`; [industry:precision-feedlines](Recipes.md#industrialprecision-feedlines) — needs 1, makes 2 × `PRECISION_FEEDLINE`

## Gear Assembly (`GEAR_ASSEMBLY`)

**Produced by:** [industry:gear-train](Recipes.md#industrialgear-train) — 1 × at [metal-press](Structures.md#metal-press) (ALL)

**Consumed by (2):** [industry:steam-pump-head](Recipes.md#industrialsteam-pump-head) — needs 1, makes 1 × `PUMP_HEAD`; [industry:precision-fit](Recipes.md#industrialprecision-fit) — needs 2, makes 1 × `PRECISION_MODULE`

## Pump Head (`PUMP_HEAD`)

**Produced by:** [industry:steam-pump-head](Recipes.md#industrialsteam-pump-head) — 1 × at [metal-press](Structures.md#metal-press) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Conductor Wire (`CONDUCTOR_WIRE`)

**Produced by:** [industry:copper-wire](Recipes.md#industrialcopper-wire) — 2 × at [precision-lathe](Structures.md#precision-lathe) (ALL)

**Consumed by (6):** [industry:controller-board](Recipes.md#industrialcontroller-board) — needs 3, makes 1 × `CONTROL_BOARD`; [industry:tracking-electronics](Recipes.md#industrialtracking-electronics) — needs 2, makes 1 × `TRACKING_CIRCUIT`; [industry:precision-feedlines](Recipes.md#industrialprecision-feedlines) — needs 3, makes 2 × `PRECISION_FEEDLINE`; [industry:unraveled-calibrate-sensors](Recipes.md#industrialunraveled-calibrate-sensors) — needs 3, makes 2 × `SENSOR_HEAD`; [facility:security-grid](Structures.md#security-grid) — build cost 8 (AUTOMATION, ALL); [facility:unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) — build cost 4 (ELECTRIC, UNRAVELED)

## Control Board (`CONTROL_BOARD`)

**Produced by:** [industry:controller-board](Recipes.md#industrialcontroller-board) — 1 × at [precision-lathe](Structures.md#precision-lathe) (ALL)

**Consumed by (6):** [industry:production-controller](Recipes.md#industrialproduction-controller) — needs 2, makes 1 × `ASSEMBLY_CONTROLLER`; [industry:tracking-electronics](Recipes.md#industrialtracking-electronics) — needs 1, makes 1 × `TRACKING_CIRCUIT`; [industry:chorus-resonance](Recipes.md#industrialchorus-resonance) — needs 1, makes 1 × `CHORUS_RESONANCE`; [industry:unraveled-calibrate-sensors](Recipes.md#industrialunraveled-calibrate-sensors) — needs 2, makes 2 × `SENSOR_HEAD`; [industry:penitent-score-hymn](Recipes.md#industrialpenitent-score-hymn) — needs 1, makes 1 × `CHOIR_SCORE`; [facility:unraveled-defense-works](Structures.md#unraveled-defense-works) — build cost 3 (AUTOMATION, UNRAVELED)

## Precision Module (`PRECISION_MODULE`)

**Produced by:** [industry:precision-fit](Recipes.md#industrialprecision-fit) — 1 × at [precision-lathe](Structures.md#precision-lathe) (ALL)

**Consumed by (1):** [industry:production-controller](Recipes.md#industrialproduction-controller) — needs 2, makes 1 × `ASSEMBLY_CONTROLLER`

## Assembly Controller (`ASSEMBLY_CONTROLLER`)

**Produced by:** [industry:production-controller](Recipes.md#industrialproduction-controller) — 1 × at [assembly-line](Structures.md#assembly-line) (ALL)

**Consumed by (3):** [industry:resilience-core](Recipes.md#industrialresilience-core) — needs 2, makes 1 × `RESILIENCE_CORE`; [industry:dread-ward](Recipes.md#industrialdread-ward) — needs 2, makes 1 × `DREAD_WARD`; [industry:breach-seed](Recipes.md#industrialbreach-seed) — needs 3, makes 1 × `BREACH_SEED`

## Compost (`COMPOST`)

**Produced by:** [hand:rough-compost](Recipes.md#handrough-compost) — 1 × per craft at HAND; [industry:compost-ferment](Recipes.md#industrialcompost-ferment) — 2 × at [compost-yard](Structures.md#compost-yard) (ALL)

**Consumed by (2):** [industry:grow-field-crops](Recipes.md#industrialgrow-field-crops) — needs 1, makes 4 × `FIELD_CROPS`; [industry:grow-medicinals](Recipes.md#industrialgrow-medicinals) — needs 1, makes 2 × `HERBAL_BUNDLE`

## Field Crops (`FIELD_CROPS`)

**Produced by:** [industry:grow-field-crops](Recipes.md#industrialgrow-field-crops) — 4 × at [forager-garden](Structures.md#forager-garden) (ALL)

**Consumed by (4):** [hand:coarse-meal](Recipes.md#handcoarse-meal) — needs 6, makes 1 × `MEAL_SACK`; [industry:cellar-vegetables](Recipes.md#industrialcellar-vegetables) — needs 3, makes 2 × `CELLARED_CROPS`; [industry:mill-crops](Recipes.md#industrialmill-crops) — needs 4, makes 2 × `MEAL_SACK`; [industry:grown-chitin](Recipes.md#industrialgrown-chitin) — needs 3, makes 2 × `GROWN_CHITIN`

## Herbal Bundle (`HERBAL_BUNDLE`)

**Produced by:** [hand:bunch-herbs](Recipes.md#handbunch-herbs) — 1 × per craft at WORKBENCH; [industry:grow-medicinals](Recipes.md#industrialgrow-medicinals) — 2 × at [herb-plot](Structures.md#herb-plot) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Cloth Roll (`CLOTH_ROLL`)

**Produced by:** [hand:hand-woven-cloth](Recipes.md#handhand-woven-cloth) — 1 × per craft at WORKBENCH; [industry:weave-cloth](Recipes.md#industrialweave-cloth) — 2 × at [fiber-loom](Structures.md#fiber-loom) (ALL)

**Consumed by (3):** [industry:cold-preservation](Recipes.md#industrialcold-preservation) — needs 1, makes 3 × `PRESERVED_RATIONS`; [industry:guard-provisions](Recipes.md#industrialguard-provisions) — needs 1, makes 2 × `GUARD_PROVISIONS`; [industry:penitent-weave-ritual](Recipes.md#industrialpenitent-weave-ritual) — needs 1, makes 2 × `RITUAL_CLOTH`

## Smoked Rations (`SMOKED_RATIONS`)

**Produced by:** [hand:smoke-ration](Recipes.md#handsmoke-ration) — 1 × per craft at FIRE; [industry:smoke-rations](Recipes.md#industrialsmoke-rations) — 2 × at [smokehouse](Structures.md#smokehouse) (ALL)

**Consumed by (1):** [industry:cold-preservation](Recipes.md#industrialcold-preservation) — needs 2, makes 3 × `PRESERVED_RATIONS`

## Cellared Crops (`CELLARED_CROPS`)

**Produced by:** [industry:cellar-vegetables](Recipes.md#industrialcellar-vegetables) — 2 × at [root-cellar](Structures.md#root-cellar) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Meal Sack (`MEAL_SACK`)

**Produced by:** [hand:coarse-meal](Recipes.md#handcoarse-meal) — 1 × per craft at WORKBENCH; [industry:mill-crops](Recipes.md#industrialmill-crops) — 2 × at [grain-mill](Structures.md#grain-mill) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Rations (`RATIONS`)

**Produced by:** [hand:camp-ration](Recipes.md#handcamp-ration) — 1 × per craft at FIRE; [industry:preserve-rations](Recipes.md#industrialpreserve-rations) — 2 × at [cooking-hearth](Structures.md#cooking-hearth) (ALL)

**Consumed by (1):** [industry:guard-provisions](Recipes.md#industrialguard-provisions) — needs 3, makes 2 × `GUARD_PROVISIONS`

## Nutrient Stock (`NUTRIENT_STOCK`)

**Produced by:** [industry:biomass-feed](Recipes.md#industrialbiomass-feed) — 1 × at [nutrient-digestor](Structures.md#nutrient-digestor) (BEGOTTEN)

**Consumed by (3):** [industry:breach-seed](Recipes.md#industrialbreach-seed) — needs 8, makes 1 × `BREACH_SEED`; [industry:binding-resin](Recipes.md#industrialbinding-resin) — needs 1, makes 1 × `BINDING_RESIN`; [industry:begotten-pulse-organ](Recipes.md#industrialbegotten-pulse-organ) — needs 2, makes 1 × `ORGAN_CORE`

## Metal Stock (`METAL_STOCK`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (47):** [hand:shelter-kit](Recipes.md#handshelter-kit) — needs 1, makes 1 × `SHELTER_KIT`; [hand:vehicle-repair](Recipes.md#handvehicle-repair) — needs 1, makes 1 × `VEHICLE_REPAIR_KIT`; [industry:resilience-core](Recipes.md#industrialresilience-core) — needs 2, makes 1 × `RESILIENCE_CORE`; [industry:dread-ward](Recipes.md#industrialdread-ward) — needs 2, makes 1 × `DREAD_WARD`; [industry:breach-seed](Recipes.md#industrialbreach-seed) — needs 4, makes 1 × `BREACH_SEED`; [facility:forge](Structures.md#forge) — build cost 3 (SALVAGE, ALL); [facility:freight-bay](Structures.md#freight-bay) — build cost 4 (SALVAGE, ALL); [facility:irrigation-works](Structures.md#irrigation-works) — build cost 1 (SALVAGE, ALL); [facility:materials-recycler](Structures.md#materials-recycler) — build cost 2 (SALVAGE, ALL); [facility:steam-boiler](Structures.md#steam-boiler) — build cost 4 (STEAM, ALL); [facility:mechanical-shaft](Structures.md#mechanical-shaft) — build cost 2 (STEAM, ALL); [facility:metal-press](Structures.md#metal-press) — build cost 3 (STEAM, ALL); [facility:conveyor-line](Structures.md#conveyor-line) — build cost 2 (STEAM, ALL); [facility:generator](Structures.md#generator) — build cost 4 (ELECTRIC, ALL); [facility:electric-bus](Structures.md#electric-bus) — build cost 3 (ELECTRIC, ALL); [facility:precision-lathe](Structures.md#precision-lathe) — build cost 5 (ELECTRIC, ALL); [facility:automatic-sorter](Structures.md#automatic-sorter) — build cost 3 (ELECTRIC, ALL); [facility:water-works](Structures.md#water-works) — build cost 2 (ELECTRIC, ALL); [facility:fuel-refinery](Structures.md#fuel-refinery) — build cost 5 (ELECTRIC, ALL); [facility:controlled-greenhouse](Structures.md#controlled-greenhouse) — build cost 3 (ELECTRIC, ALL); [facility:industrial-workshop](Structures.md#industrial-workshop) — build cost 6 (AUTOMATION, ALL); [facility:motor-pool](Structures.md#motor-pool) — build cost 6 (AUTOMATION, ALL); [facility:assembly-line](Structures.md#assembly-line) — build cost 10 (AUTOMATION, ALL); [facility:command-hall](Structures.md#command-hall) — build cost 8 (AUTOMATION, ALL); [facility:research-archive](Structures.md#research-archive) — build cost 8 (AUTOMATION, ALL); [facility:infirmary](Structures.md#infirmary) — build cost 2 (AUTOMATION, ALL); [facility:signal-relay](Structures.md#signal-relay) — build cost 3 (AUTOMATION, ALL); [facility:nutrient-digestor](Structures.md#nutrient-digestor) — build cost 6 (AUTOMATION, BEGOTTEN); [facility:terror-collector](Structures.md#terror-collector) — build cost 6 (AUTOMATION, PENITENT); [facility:resilience-grid](Structures.md#resilience-grid) — build cost 16 (INCURSION, UNRAVELED); [facility:dread-works](Structures.md#dread-works) — build cost 16 (INCURSION, PENITENT); [facility:incursion-anchor](Structures.md#incursion-anchor) — build cost 16 (INCURSION, BEGOTTEN); [facility:cold-store](Structures.md#cold-store) — build cost 2 (SALVAGE, ALL); [facility:barracks](Structures.md#barracks) — build cost 1 (SALVAGE, ALL); [facility:purification-works](Structures.md#purification-works) — build cost 4 (ELECTRIC, ALL); [facility:security-grid](Structures.md#security-grid) — build cost 7 (AUTOMATION, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 24 (INCURSION, ALL); [facility:unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) — build cost 2 (SALVAGE, UNRAVELED); [facility:unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) — build cost 4 (STEAM, UNRAVELED); [facility:unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) — build cost 4 (ELECTRIC, UNRAVELED); [facility:unraveled-defense-works](Structures.md#unraveled-defense-works) — build cost 8 (AUTOMATION, UNRAVELED); [facility:penitent-reliquary](Structures.md#penitent-reliquary) — build cost 2 (SALVAGE, PENITENT); [facility:penitent-cantor-dais](Structures.md#penitent-cantor-dais) — build cost 3 (STEAM, PENITENT); [facility:penitent-choir-relay](Structures.md#penitent-choir-relay) — build cost 5 (ELECTRIC, PENITENT); [facility:penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) — build cost 8 (AUTOMATION, PENITENT); [facility:begotten-chitin-vat](Structures.md#begotten-chitin-vat) — build cost 2 (STEAM, BEGOTTEN); [facility:begotten-organ-lattice](Structures.md#begotten-organ-lattice) — build cost 6 (AUTOMATION, BEGOTTEN)

## Machine Parts (`MACHINE_PARTS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (48):** [hand:vehicle-repair](Recipes.md#handvehicle-repair) — needs 1, makes 1 × `VEHICLE_REPAIR_KIT`; [industry:steam-pump-head](Recipes.md#industrialsteam-pump-head) — needs 1, makes 1 × `PUMP_HEAD`; [industry:controller-board](Recipes.md#industrialcontroller-board) — needs 1, makes 1 × `CONTROL_BOARD`; [industry:precision-fit](Recipes.md#industrialprecision-fit) — needs 1, makes 1 × `PRECISION_MODULE`; [industry:production-controller](Recipes.md#industrialproduction-controller) — needs 2, makes 1 × `ASSEMBLY_CONTROLLER`; [industry:resilience-core](Recipes.md#industrialresilience-core) — needs 2, makes 1 × `RESILIENCE_CORE`; [industry:breach-seed](Recipes.md#industrialbreach-seed) — needs 3, makes 1 × `BREACH_SEED`; [industry:field-maintenance](Recipes.md#industrialfield-maintenance) — needs 2, makes 1 × `VEHICLE_REPAIR_KIT`; [industry:unraveled-defense-scanner](Recipes.md#industrialunraveled-defense-scanner) — needs 3, makes 1 × `RELAY_BOX`; [facility:forge](Structures.md#forge) — build cost 1 (SALVAGE, ALL); [facility:freight-bay](Structures.md#freight-bay) — build cost 3 (SALVAGE, ALL); [facility:irrigation-works](Structures.md#irrigation-works) — build cost 1 (SALVAGE, ALL); [facility:materials-recycler](Structures.md#materials-recycler) — build cost 3 (SALVAGE, ALL); [facility:steam-boiler](Structures.md#steam-boiler) — build cost 2 (STEAM, ALL); [facility:mechanical-shaft](Structures.md#mechanical-shaft) — build cost 1 (STEAM, ALL); [facility:metal-press](Structures.md#metal-press) — build cost 2 (STEAM, ALL); [facility:conveyor-line](Structures.md#conveyor-line) — build cost 3 (STEAM, ALL); [facility:generator](Structures.md#generator) — build cost 5 (ELECTRIC, ALL); [facility:electric-bus](Structures.md#electric-bus) — build cost 3 (ELECTRIC, ALL); [facility:precision-lathe](Structures.md#precision-lathe) — build cost 4 (ELECTRIC, ALL); [facility:automatic-sorter](Structures.md#automatic-sorter) — build cost 5 (ELECTRIC, ALL); [facility:water-works](Structures.md#water-works) — build cost 2 (ELECTRIC, ALL); [facility:fuel-refinery](Structures.md#fuel-refinery) — build cost 6 (ELECTRIC, ALL); [facility:controlled-greenhouse](Structures.md#controlled-greenhouse) — build cost 2 (ELECTRIC, ALL); [facility:industrial-workshop](Structures.md#industrial-workshop) — build cost 6 (AUTOMATION, ALL); [facility:motor-pool](Structures.md#motor-pool) — build cost 5 (AUTOMATION, ALL); [facility:assembly-line](Structures.md#assembly-line) — build cost 9 (AUTOMATION, ALL); [facility:command-hall](Structures.md#command-hall) — build cost 6 (AUTOMATION, ALL); [facility:research-archive](Structures.md#research-archive) — build cost 8 (AUTOMATION, ALL); [facility:infirmary](Structures.md#infirmary) — build cost 2 (AUTOMATION, ALL); [facility:signal-relay](Structures.md#signal-relay) — build cost 5 (AUTOMATION, ALL); [facility:nutrient-digestor](Structures.md#nutrient-digestor) — build cost 6 (AUTOMATION, BEGOTTEN); [facility:terror-collector](Structures.md#terror-collector) — build cost 6 (AUTOMATION, PENITENT); [facility:resilience-grid](Structures.md#resilience-grid) — build cost 12 (INCURSION, UNRAVELED); [facility:dread-works](Structures.md#dread-works) — build cost 12 (INCURSION, PENITENT); [facility:incursion-anchor](Structures.md#incursion-anchor) — build cost 12 (INCURSION, BEGOTTEN); [facility:purification-works](Structures.md#purification-works) — build cost 3 (ELECTRIC, ALL); [facility:security-grid](Structures.md#security-grid) — build cost 6 (AUTOMATION, ALL); [facility:monument-graveyard](Structures.md#monument-graveyard) — build cost 16 (INCURSION, ALL); [facility:unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) — build cost 2 (SALVAGE, UNRAVELED); [facility:unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) — build cost 3 (STEAM, UNRAVELED); [facility:unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) — build cost 5 (ELECTRIC, UNRAVELED); [facility:unraveled-defense-works](Structures.md#unraveled-defense-works) — build cost 8 (AUTOMATION, UNRAVELED); [facility:penitent-cantor-dais](Structures.md#penitent-cantor-dais) — build cost 2 (STEAM, PENITENT); [facility:penitent-choir-relay](Structures.md#penitent-choir-relay) — build cost 4 (ELECTRIC, PENITENT); [facility:penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) — build cost 7 (AUTOMATION, PENITENT); [facility:begotten-membrane-lab](Structures.md#begotten-membrane-lab) — build cost 4 (ELECTRIC, BEGOTTEN); [facility:begotten-organ-lattice](Structures.md#begotten-organ-lattice) — build cost 6 (AUTOMATION, BEGOTTEN)

## Shelter Kit (`SHELTER_KIT`)

**Produced by:** [hand:shelter-kit](Recipes.md#handshelter-kit) — 1 × per craft at WORKBENCH

**Consumed by (0):** No current declared recipe or build cost.

## Vehicle Repair Kit (`VEHICLE_REPAIR_KIT`)

**Produced by:** [hand:vehicle-repair](Recipes.md#handvehicle-repair) — 1 × per craft at WORKBENCH; [industry:field-maintenance](Recipes.md#industrialfield-maintenance) — 1 × at [motor-pool](Structures.md#motor-pool) (UNRAVELED)

**Consumed by (0):** No current declared recipe or build cost.

## Tent Kit (`TENT_KIT`)

**Produced by:** [hand:tent](Recipes.md#handtent) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Crate Kit (`CRATE_KIT`)

**Produced by:** [hand:crate](Recipes.md#handcrate) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Fence Section (`FENCE_SECTION`)

**Produced by:** [hand:fence](Recipes.md#handfence) — 2 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Stake Trap (`STAKE_TRAP`)

**Produced by:** [hand:stake-trap](Recipes.md#handstake-trap) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Noise Alarm (`NOISE_ALARM`)

**Produced by:** [hand:noise-alarm](Recipes.md#handnoise-alarm) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Campfire (`CAMPFIRE`)

**Produced by:** [hand:campfire](Recipes.md#handcampfire) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Filter (`FILTER`)

**Produced by:** [hand:water-filter](Recipes.md#handwater-filter) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Dread Imprint (`DREAD_IMPRINT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (3):** [industry:dread-ward](Recipes.md#industrialdread-ward) — needs 3, makes 1 × `DREAD_WARD`; [industry:bound-liturgy](Recipes.md#industrialbound-liturgy) — needs 1, makes 1 × `BOUND_LITURGY`; [industry:penitent-sanctify-shell](Recipes.md#industrialpenitent-sanctify-shell) — needs 3, makes 1 × `CONSECRATED_SHELL`

## Resilience Core (`RESILIENCE_CORE`)

**Produced by:** [industry:resilience-core](Recipes.md#industrialresilience-core) — 1 × at [resilience-grid](Structures.md#resilience-grid) (UNRAVELED)

**Consumed by (0):** No current declared recipe or build cost.

## Dread Ward (`DREAD_WARD`)

**Produced by:** [industry:dread-ward](Recipes.md#industrialdread-ward) — 1 × at [dread-works](Structures.md#dread-works) (PENITENT)

**Consumed by (1):** [industry:sacred-supply](Recipes.md#industrialsacred-supply) — needs 1, makes 1 × `SACRED_SUPPLY`

## Breach Seed (`BREACH_SEED`)

**Produced by:** [industry:breach-seed](Recipes.md#industrialbreach-seed) — 1 × at [incursion-anchor](Structures.md#incursion-anchor) (BEGOTTEN)

**Consumed by (1):** [industry:spine-supply](Recipes.md#industrialspine-supply) — needs 1, makes 1 × `SPINE_SUPPLY`

## Sorted Ore (`SORTED_ORE`)

**Produced by:** [industry:sort-mineral-feed](Recipes.md#industrialsort-mineral-feed) — 2 × at [materials-recycler](Structures.md#materials-recycler) (ALL)

**Consumed by (1):** [industry:tracking-electronics](Recipes.md#industrialtracking-electronics) — needs 1, makes 1 × `TRACKING_CIRCUIT`

## Hardened Alloy (`HARDENED_ALLOY`)

**Produced by:** [industry:temper-alloy](Recipes.md#industrialtemper-alloy) — 1 × at [forge](Structures.md#forge) (ALL)

**Consumed by (2):** [industry:ballistic-resupply](Recipes.md#industrialballistic-resupply) — needs 2, makes 2 × `BALLISTIC_SUPPLY`; [industry:field-maintenance](Recipes.md#industrialfield-maintenance) — needs 1, makes 1 × `VEHICLE_REPAIR_KIT`

## Tracking Circuit (`TRACKING_CIRCUIT`)

**Produced by:** [industry:tracking-electronics](Recipes.md#industrialtracking-electronics) — 1 × at [precision-lathe](Structures.md#precision-lathe) (ALL)

**Consumed by (3):** [industry:ballistic-resupply](Recipes.md#industrialballistic-resupply) — needs 1, makes 2 × `BALLISTIC_SUPPLY`; [industry:secure-ammo-cache](Recipes.md#industrialsecure-ammo-cache) — needs 1, makes 2 × `DEFENSE_CACHE`; [industry:unraveled-defense-scanner](Recipes.md#industrialunraveled-defense-scanner) — needs 1, makes 1 × `RELAY_BOX`

## Precision Feedline (`PRECISION_FEEDLINE`)

**Produced by:** [industry:precision-feedlines](Recipes.md#industrialprecision-feedlines) — 2 × at [industrial-workshop](Structures.md#industrial-workshop) (ALL)

**Consumed by (1):** [industry:ballistic-resupply](Recipes.md#industrialballistic-resupply) — needs 1, makes 2 × `BALLISTIC_SUPPLY`

## Ballistic Supply (`BALLISTIC_SUPPLY`)

**Produced by:** [industry:ballistic-resupply](Recipes.md#industrialballistic-resupply) — 2 × at [industrial-workshop](Structures.md#industrial-workshop) (UNRAVELED)

**Consumed by (1):** [industry:secure-ammo-cache](Recipes.md#industrialsecure-ammo-cache) — needs 2, makes 2 × `DEFENSE_CACHE`

## Votive Incense (`VOTIVE_INCENSE`)

**Produced by:** [industry:votive-incense](Recipes.md#industrialvotive-incense) — 2 × at [cooking-hearth](Structures.md#cooking-hearth) (PENITENT)

**Consumed by (1):** [industry:witness-seal](Recipes.md#industrialwitness-seal) — needs 2, makes 2 × `WITNESS_SEAL`

## Witness Seal (`WITNESS_SEAL`)

**Produced by:** [industry:witness-seal](Recipes.md#industrialwitness-seal) — 2 × at [forge](Structures.md#forge) (PENITENT)

**Consumed by (2):** [industry:chorus-resonance](Recipes.md#industrialchorus-resonance) — needs 2, makes 1 × `CHORUS_RESONANCE`; [industry:penitent-cast-censer](Recipes.md#industrialpenitent-cast-censer) — needs 1, makes 1 × `BRONZE_CENSER`

## Chorus Resonance (`CHORUS_RESONANCE`)

**Produced by:** [industry:chorus-resonance](Recipes.md#industrialchorus-resonance) — 1 × at [research-archive](Structures.md#research-archive) (PENITENT)

**Consumed by (1):** [industry:bound-liturgy](Recipes.md#industrialbound-liturgy) — needs 1, makes 1 × `BOUND_LITURGY`

## Bound Liturgy (`BOUND_LITURGY`)

**Produced by:** [industry:bound-liturgy](Recipes.md#industrialbound-liturgy) — 1 × at [terror-collector](Structures.md#terror-collector) (PENITENT)

**Consumed by (2):** [industry:sacred-supply](Recipes.md#industrialsacred-supply) — needs 2, makes 1 × `SACRED_SUPPLY`; [industry:penitent-sanctify-shell](Recipes.md#industrialpenitent-sanctify-shell) — needs 1, makes 1 × `CONSECRATED_SHELL`

## Sacred Supply (`SACRED_SUPPLY`)

**Produced by:** [industry:sacred-supply](Recipes.md#industrialsacred-supply) — 1 × at [dread-works](Structures.md#dread-works) (PENITENT)

**Consumed by (0):** No current declared recipe or build cost.

## Growth Feed (`GROWTH_FEED`)

**Produced by:** [industry:growth-feed](Recipes.md#industrialgrowth-feed) — 2 × at [cooking-hearth](Structures.md#cooking-hearth) (BEGOTTEN)

**Consumed by (2):** [industry:viable-spores](Recipes.md#industrialviable-spores) — needs 2, makes 2 × `VIABLE_SPORES`; [industry:begotten-pulse-organ](Recipes.md#industrialbegotten-pulse-organ) — needs 2, makes 1 × `ORGAN_CORE`

## Viable Spores (`VIABLE_SPORES`)

**Produced by:** [industry:viable-spores](Recipes.md#industrialviable-spores) — 2 × at [materials-recycler](Structures.md#materials-recycler) (BEGOTTEN)

**Consumed by (1):** [industry:grown-chitin](Recipes.md#industrialgrown-chitin) — needs 2, makes 2 × `GROWN_CHITIN`

## Grown Chitin (`GROWN_CHITIN`)

**Produced by:** [industry:grown-chitin](Recipes.md#industrialgrown-chitin) — 2 × at [controlled-greenhouse](Structures.md#controlled-greenhouse) (BEGOTTEN)

**Consumed by (1):** [industry:binding-resin](Recipes.md#industrialbinding-resin) — needs 2, makes 1 × `BINDING_RESIN`

## Binding Resin (`BINDING_RESIN`)

**Produced by:** [industry:binding-resin](Recipes.md#industrialbinding-resin) — 1 × at [nutrient-digestor](Structures.md#nutrient-digestor) (BEGOTTEN)

**Consumed by (1):** [industry:spine-supply](Recipes.md#industrialspine-supply) — needs 2, makes 1 × `SPINE_SUPPLY`

## Spine Supply (`SPINE_SUPPLY`)

**Produced by:** [industry:spine-supply](Recipes.md#industrialspine-supply) — 1 × at [incursion-anchor](Structures.md#incursion-anchor) (BEGOTTEN)

**Consumed by (0):** No current declared recipe or build cost.

## Scrap Metal (`SCRAP_METAL`)

**Produced by:** [industry:unraveled-recover-scrap](Recipes.md#industrialunraveled-recover-scrap) — 2 × at [unraveled-scrap-outpost](Structures.md#unraveled-scrap-outpost) (UNRAVELED)

**Consumed by (1):** [industry:unraveled-make-bearings](Recipes.md#industrialunraveled-make-bearings) — needs 3, makes 2 × `BEARING_SET`

## Salvaged Wire (`SALVAGED_WIRE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bearing Set (`BEARING_SET`)

**Produced by:** [industry:unraveled-make-bearings](Recipes.md#industrialunraveled-make-bearings) — 2 × at [unraveled-machinist-bay](Structures.md#unraveled-machinist-bay) (UNRAVELED)

**Consumed by (1):** [industry:unraveled-pressure-vessel](Recipes.md#industrialunraveled-pressure-vessel) — needs 2, makes 1 × `PRESSURE_TANK`

## Valve Body (`VALVE_BODY`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Piston Head (`PISTON_HEAD`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Rubber Seals (`RUBBER_SEALS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Fuel Can (`FUEL_CAN`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Lubricant (`LUBRICANT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Pressure Tank (`PRESSURE_TANK`)

**Produced by:** [industry:unraveled-pressure-vessel](Recipes.md#industrialunraveled-pressure-vessel) — 1 × at [unraveled-pressure-shop](Structures.md#unraveled-pressure-shop) (UNRAVELED)

**Consumed by (1):** [industry:unraveled-calibrate-sensors](Recipes.md#industrialunraveled-calibrate-sensors) — needs 1, makes 2 × `SENSOR_HEAD`

## Sheet Metal (`SHEET_METAL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Welded Frame (`WELDED_FRAME`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Coil Spring (`COIL_SPRING`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Pulley Set (`PULLEY_SET`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Tool Roll (`TOOL_ROLL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Power Cell (`POWER_CELL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Sensor Head (`SENSOR_HEAD`)

**Produced by:** [industry:unraveled-calibrate-sensors](Recipes.md#industrialunraveled-calibrate-sensors) — 2 × at [unraveled-sensor-lab](Structures.md#unraveled-sensor-lab) (UNRAVELED)

**Consumed by (1):** [industry:unraveled-defense-scanner](Recipes.md#industrialunraveled-defense-scanner) — needs 2, makes 1 × `RELAY_BOX`

## Relay Box (`RELAY_BOX`)

**Produced by:** [industry:unraveled-defense-scanner](Recipes.md#industrialunraveled-defense-scanner) — 1 × at [unraveled-defense-works](Structures.md#unraveled-defense-works) (UNRAVELED)

**Consumed by (0):** No current declared recipe or build cost.

## Actuator (`ACTUATOR`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Filter Media (`FILTER_MEDIA`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Coolant Jug (`COOLANT_JUG`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Drive Chain (`DRIVE_CHAIN`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Circuit Housing (`CIRCUIT_HOUSING`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Composite Panel (`COMPOSITE_PANEL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Cargo Net (`CARGO_NET`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Fastener Box (`FASTENER_BOX`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Processed Fuel (`PROCESSED_FUEL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Repair Compound (`REPAIR_COMPOUND`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Precision Lens (`PRECISION_LENS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Calibration Kit (`CALIBRATION_KIT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Industrial Solvent (`INDUSTRIAL_SOLVENT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Ritual Cloth (`RITUAL_CLOTH`)

**Produced by:** [industry:penitent-weave-ritual](Recipes.md#industrialpenitent-weave-ritual) — 2 × at [penitent-votive-altar](Structures.md#penitent-votive-altar) (PENITENT)

**Consumed by (1):** [industry:penitent-engrave-reliquary](Recipes.md#industrialpenitent-engrave-reliquary) — needs 1, makes 1 × `RELIQUARY_PLATE`

## Wax Candles (`WAX_CANDLES`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Ash Salt (`ASH_SALT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Sacred Oil (`SACRED_OIL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Offering Bowl (`OFFERING_BOWL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Prayer Beads (`PRAYER_BEADS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Ceremonial Ink (`CEREMONIAL_INK`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bone Tokens (`BONE_TOKENS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Blessed Thread (`BLESSED_THREAD`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Seal Wax (`SEAL_WAX`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bronze Censer (`BRONZE_CENSER`)

**Produced by:** [industry:penitent-cast-censer](Recipes.md#industrialpenitent-cast-censer) — 1 × at [penitent-cantor-dais](Structures.md#penitent-cantor-dais) (PENITENT)

**Consumed by (1):** [industry:penitent-score-hymn](Recipes.md#industrialpenitent-score-hymn) — needs 1, makes 1 × `CHOIR_SCORE`

## Ritual Ash (`RITUAL_ASH`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Votive Powder (`VOTIVE_POWDER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Sanctified Water (`SANCTIFIED_WATER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Reliquary Plate (`RELIQUARY_PLATE`)

**Produced by:** [industry:penitent-engrave-reliquary](Recipes.md#industrialpenitent-engrave-reliquary) — 1 × at [penitent-reliquary](Structures.md#penitent-reliquary) (PENITENT)

**Consumed by (1):** [industry:penitent-cast-censer](Recipes.md#industrialpenitent-cast-censer) — needs 2, makes 1 × `BRONZE_CENSER`

## Scripture Leaf (`SCRIPTURE_LEAF`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Copper Chimes (`COPPER_CHIMES`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Chorus Mask (`CHORUS_MASK`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Oath Ribbon (`OATH_RIBBON`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Consecrated Resin (`CONSECRATED_RESIN`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Ember Charms (`EMBER_CHARMS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Ward Nails (`WARD_NAILS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Processional Chain (`PROCESSIONAL_CHAIN`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Hallowed Sand (`HALLOWED_SAND`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Choir Score (`CHOIR_SCORE`)

**Produced by:** [industry:penitent-score-hymn](Recipes.md#industrialpenitent-score-hymn) — 1 × at [penitent-choir-relay](Structures.md#penitent-choir-relay) (PENITENT)

**Consumed by (1):** [industry:penitent-sanctify-shell](Recipes.md#industrialpenitent-sanctify-shell) — needs 2, makes 1 × `CONSECRATED_SHELL`

## Shrine Stone (`SHRINE_STONE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Pilgrim Pack (`PILGRIM_PACK`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Purity Salve (`PURITY_SALVE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Binding Cord (`BINDING_CORD`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Consecrated Shell (`CONSECRATED_SHELL`)

**Produced by:** [industry:penitent-sanctify-shell](Recipes.md#industrialpenitent-sanctify-shell) — 1 × at [penitent-ward-sanctum](Structures.md#penitent-ward-sanctum) (PENITENT)

**Consumed by (0):** No current declared recipe or build cost.

## Nutrient Slurry (`NUTRIENT_SLURRY`)

**Produced by:** [industry:begotten-mix-slurry](Recipes.md#industrialbegotten-mix-slurry) — 2 × at [begotten-brood-nest](Structures.md#begotten-brood-nest) (BEGOTTEN)

**Consumed by (1):** [industry:begotten-culture-spores](Recipes.md#industrialbegotten-culture-spores) — needs 2, makes 2 × `SPORE_POD`

## Spore Pod (`SPORE_POD`)

**Produced by:** [industry:begotten-culture-spores](Recipes.md#industrialbegotten-culture-spores) — 2 × at [begotten-spore-basin](Structures.md#begotten-spore-basin) (BEGOTTEN)

**Consumed by (1):** [industry:begotten-harden-chitin](Recipes.md#industrialbegotten-harden-chitin) — needs 2, makes 2 × `CHITIN_SHARDS`

## Chitin Shards (`CHITIN_SHARDS`)

**Produced by:** [industry:begotten-harden-chitin](Recipes.md#industrialbegotten-harden-chitin) — 2 × at [begotten-chitin-vat](Structures.md#begotten-chitin-vat) (BEGOTTEN)

**Consumed by (1):** [industry:begotten-grow-membrane](Recipes.md#industrialbegotten-grow-membrane) — needs 3, makes 1 × `LIVING_MEMBRANE`

## Tendon Fiber (`TENDON_FIBER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Resin Glob (`RESIN_GLOB`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bone Meal (`BONE_MEAL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Growth Medium (`GROWTH_MEDIUM`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Brood Gel (`BROOD_GEL`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Carapace Plate (`CARAPACE_PLATE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Maw Seeds (`MAW_SEEDS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Pale Marrow (`PALE_MARROW`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Mutagen Sap (`MUTAGEN_SAP`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Spine Cluster (`SPINE_CLUSTER`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Incubator Slime (`INCUBATOR_SLIME`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Fungal Mat (`FUNGAL_MAT`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Living Membrane (`LIVING_MEMBRANE`)

**Produced by:** [industry:begotten-grow-membrane](Recipes.md#industrialbegotten-grow-membrane) — 1 × at [begotten-membrane-lab](Structures.md#begotten-membrane-lab) (BEGOTTEN)

**Consumed by (1):** [industry:begotten-pulse-organ](Recipes.md#industrialbegotten-pulse-organ) — needs 2, makes 1 × `ORGAN_CORE`

## Root Bulb (`ROOT_BULB`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Vein Tubing (`VEIN_TUBING`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Scent Gland (`SCENT_GLAND`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Larval Feed (`LARVAL_FEED`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Husk Fragments (`HUSK_FRAGMENTS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Mycelial Mass (`MYCELIAL_MASS`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Biolume Fluid (`BIOLUME_FLUID`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Organ Core (`ORGAN_CORE`)

**Produced by:** [industry:begotten-pulse-organ](Recipes.md#industrialbegotten-pulse-organ) — 1 × at [begotten-organ-lattice](Structures.md#begotten-organ-lattice) (BEGOTTEN)

**Consumed by (0):** No current declared recipe or build cost.

## Tangle Vine (`TANGLE_VINE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bile Sac (`BILE_SAC`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Calcified Nodules (`CALCIFIED_NODULES`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Stem Bundle (`STEM_BUNDLE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Thorn Bundle (`THORN_BUNDLE`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Pulse Organ (`PULSE_ORGAN`)

**Produced by:** No recipe in the exported registries; source acquisition/trade or later implementation required.

**Consumed by (0):** No current declared recipe or build cost.

## Bandage (`BANDAGE`)

**Produced by:** [hand:bandage](Recipes.md#handbandage) — 1 × per craft at HAND; [hand:field-dressing](Recipes.md#handfield-dressing) — 2 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Workbench (`WORKBENCH`)

**Produced by:** [hand:workbench](Recipes.md#handworkbench) — 1 × per craft at HAND

**Consumed by (0):** No current declared recipe or build cost.

## Preserved Rations (`PRESERVED_RATIONS`)

**Produced by:** [industry:cold-preservation](Recipes.md#industrialcold-preservation) — 3 × at [cold-store](Structures.md#cold-store) (ALL)

**Consumed by (1):** [industry:fortified-rations](Recipes.md#industrialfortified-rations) — needs 2, makes 3 × `FIELD_MRE`

## Guard Provisions (`GUARD_PROVISIONS`)

**Produced by:** [industry:guard-provisions](Recipes.md#industrialguard-provisions) — 2 × at [barracks](Structures.md#barracks) (ALL)

**Consumed by (1):** [industry:fortified-rations](Recipes.md#industrialfortified-rations) — needs 2, makes 3 × `FIELD_MRE`

## Sterile Water (`STERILE_WATER`)

**Produced by:** [industry:treat-contaminated-water](Recipes.md#industrialtreat-contaminated-water) — 4 × at [purification-works](Structures.md#purification-works) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

## Defense Cache (`DEFENSE_CACHE`)

**Produced by:** [industry:secure-ammo-cache](Recipes.md#industrialsecure-ammo-cache) — 2 × at [security-grid](Structures.md#security-grid) (UNRAVELED)

**Consumed by (0):** No current declared recipe or build cost.

## Field Mre (`FIELD_MRE`)

**Produced by:** [industry:fortified-rations](Recipes.md#industrialfortified-rations) — 3 × at [industrial-workshop](Structures.md#industrial-workshop) (ALL)

**Consumed by (0):** No current declared recipe or build cost.

