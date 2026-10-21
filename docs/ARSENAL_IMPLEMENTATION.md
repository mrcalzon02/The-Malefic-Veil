# Functional arsenal and base progression

The approved primitive weapons are the working art. Modest bevels, shared textures and rigid component animation are sufficient for this phase; detailed kitbash replacement is deferred.

The first **eight** weapons (Vesper through Furnace Organ) may appear as world weapon loot. A player starts with Vesper. The remaining 24 are never random weapon drops: they require a nearby owned, living fabrication bench, its required upgrade tier, a faction recipe and scavenged materials/components. Ammunition and resources are host-owned.

Provisional factions are **Foundry** (heavy ballistic/explosive/salvage), **Lumen** (laser/plasma/arc) and **Veil** (disc/bounce/bio/cryo/gravity). Faction selection is separate from class ability, and locks when the first advanced weapon is fabricated. Basic recipes are shared. Higher recipes require an earlier crafted design as research, without consuming that weapon.

Raw resources: scrap, electronics, chemicals, alloy. Component recipes: machined frame, power cell, arc coil, warhead, anomaly core. Benches upgrade from tier 1 to 3. These choices establish an editable initial crafting tree, not final faction lore or balance.

All new fire, reload, selection, pickup, crafting and faction operations must be validated on the host. Reloads take time; switching/death cancels unfinished actions. Projectile lifetimes, bounce counts, chain counts and area damage are bounded; walls and barricades block effects; existing spawn and offline-base protection remain authoritative. The legacy two-weapon API remains for compatibility checks while the playable client uses the arsenal API.

Acceptance: all 32 weapons fire and reload through the host; alternate modes, chain/bounce/lob/beam behavior and crafting denials have deterministic tests; real clients replicate inventory and effects; the same assets load in a packaged JAR; animation and texture changes have rendered evidence. Exact balance and a physical multi-machine playtest remain later tuning work.
