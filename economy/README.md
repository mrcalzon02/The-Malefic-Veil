# Industrial economy CSV catalog

Recipe and facility quantities are validated against authoritative Java registries by CraftingEconomyCsvTest; do not infer executable factories from their presence in CSV. The Unraveled, Penitent and Begotten facility spines and process ownership are distinct at every era. Shared infrastructure remains labeled ALL. Foreign faction ingredients require explicit trade/cooperation support, not an implicit shared production privilege.
This directory contains item, recipe, input, facility, construction-cost, storage, and skill tables generated from the current registries and icon index.

Existing authoritative definitions: 62 industrial recipes, 69 facilities, 34 field recipes (including 12 physical weapon recipes), ingredients and construction costs. Items include 150 source icon records plus explicit missing-art entries; eight scavenged weapon types are world pickups. A physical weapon is not yet an executable attack until host-owned equip/fire rules and viewmodels are completed. BANDAGE and WORKBENCH were added explicitly with ICON_REQUIRED because their icon entries were absent from the supplied icon index.

Balancing proposals, not yet runtime rules: per-item stack caps, storage capacities, item prices, skill requirements and multipliers. These columns must be validated and adopted by the host before affecting gameplay.

New weapon item art is marked ICON_REQUIRED in items.csv; the eight world pickups have authored interim 3D proxies. The present workstation limit is 512 units/64 stacks. The present survival backpack has weight cap 200. Existing trucks carry one abstract pallet and do not yet deliver a quantified material payout. Physical storage capacity is distinct from carried weight and workstation buffers.

Run `python3 tools/validate_economy_csv.py` to check references. Server-side crafting transactions must remain atomic, power-checked, faction-aware and capacity-limited. Ground items expire after 20 minutes, but stored items should not inherit that expiry.
