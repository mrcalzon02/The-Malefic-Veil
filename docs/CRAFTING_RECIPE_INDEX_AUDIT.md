# Crafting system: source-of-truth audit

The playable game now has **22 survival recipes** defined in
`SurvivalCrafting.recipes()`, including ten costly emergency alternatives to
better later production. It also carries **42 industrial process descriptors**
and an interconnected construction prerequisite graph in
`CraftingEraRegistry`. The latter are plans, not yet manufactured
by running staffed machines.

The existing economy CSV exports were inconsistent with code: the
`boil-water` row incorrectly claimed to make a `WORKBENCH`, another
`boil-water` row absorbed the `workbench` cost, and the station's
actual ingredient list was inaccurate. All four recipe CSV exports
were regenerated here directly from authoritative registry definitions.
Avoid editing them as a separate source of gameplay truth.

The new `CraftingRecipeIndex` derives all output quantities,
ingredient costs, where-used reverse edges, era/faction/station
requirements, power/time metadata, and production availability
directly from source registries.

The GPU recipe book draws source sprites from the original five
icon packs: **150 original PNGs**. Items that have artwork but
no implemented recipe are explicitly catalogued as concepts.
`BANDAGE` and `WORKBENCH` do not yet have dedicated original
source icons, and building plans do not have architecture icons.
They are labeled, rather than misrepresented by a different image.

Production implementation priorities: authorize and persist
industrial machine stock/energy, unlock stations and full faction
processes, implement physical facilities with correct collisions,
and connect crafted goods to real uses/trade/consumption. The
registry cannot produce stock simply by describing a formula.

Retain host-owned ingredient consumption and inventory weight
validation. Future icon additions must retain the original stable
item IDs and verified sprite hashes.
