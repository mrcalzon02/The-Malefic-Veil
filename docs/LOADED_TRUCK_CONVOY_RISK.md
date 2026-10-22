# Loaded truck convoy risk

Bulk truck cargo should be an economically meaningful shipment, never the equivalent of one ordinary inventory pickup. The existing cargo system treats a loaded pallet as one abstract **METAL_STOCK** or **MACHINE_PARTS** cargo unit, with physical loading, damage ejection, and recovery. **A delivery-to-settlement material conversion is not yet wired**, so no numeric shipment yield is claimed as implemented.

## Current host rules
- Trucks carrying cargo are capped at 3.2 world units/second forward and 1.5 reverse; empty vehicles retain the previous 8 forward and 3 reverse limits.
- When a driven, loaded truck moves faster than 0.35 units/second, hostile Lost NPCs within 26 world units can acquire its driver by engine sound even without a visual sightline. The regular faction exemption for Begotten is retained.
- NPCs continue to use existing authoritative movement collision and melee attacks. Sound is a host-side detection rule, not merely an audio effect.
- Existing truck HP, fuel depletion, cargo damage ejection, cargo recovery and protected spawn-path construction rules remain.
- A loaded truck must be valuable enough to justify escorting. **Proposed subsequent delivery balance**: each pallet should credit a substantial batch of industrial inputs at a legitimate powered/unloading facility, with exact quantities tuned to facility recipes, salvage rates, capacity, and faction economies. Do not assign an arbitrary high quantity before implementing and testing delivery.
- Future checks: actual delivery and unloading, reward conservation through damage and recovery, client warning that the convoy attracts enemies, audible positional engine sound, additional faction-specific AI investigations, traffic path viability across city/wilderness, and difficulty scaling by shipment value.

Java regression: `LoadedConvoyRiskTest` checks the loaded movement cap and preservation of cargo while moving; the full host/AI behavior still requires live test verification.
