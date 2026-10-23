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
