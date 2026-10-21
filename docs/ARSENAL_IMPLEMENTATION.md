# Functional arsenal and base progression

The approved primitive weapons are the working art. Modest bevels, shared textures and rigid component animation are sufficient for this phase; detailed kitbash replacement is deferred.

The first **eight** weapons (Vesper through Furnace Organ) may appear as world weapon loot. A player starts with Vesper. The remaining 24 are never random weapon drops: they require a nearby owned, living fabrication bench, its required upgrade tier, a faction recipe and scavenged materials/components. Ammunition and resources are host-owned.

Provisional factions are **Foundry** (heavy ballistic/explosive/salvage), **Lumen** (laser/plasma/arc) and **Veil** (disc/bounce/bio/cryo/gravity). Faction selection is separate from class ability, and locks when the first advanced weapon is fabricated. Basic recipes are shared. Higher recipes require an earlier crafted design as research, without consuming that weapon.

Raw resources: scrap, electronics, chemicals, alloy. Component recipes: machined frame, power cell, arc coil, warhead, anomaly core. Benches upgrade from tier 1 to 3. These choices establish an editable initial crafting tree, not final faction lore or balance.

All new fire, reload, selection, pickup, crafting and faction operations must be validated on the host. Reloads take time; switching/death cancels unfinished actions. Projectile lifetimes, bounce counts, chain counts and area damage are bounded; walls and barricades block effects; existing spawn and offline-base protection remain authoritative. The legacy two-weapon API remains for compatibility checks while the playable client uses the arsenal API.

Acceptance: all 32 weapons fire and reload through the host; alternate modes, chain/bounce/lob/beam behavior and crafting denials have deterministic tests; real clients replicate inventory and effects; the same assets load in a packaged JAR; animation and texture changes have rendered evidence. Exact balance and a physical multi-machine playtest remain later tuning work.

## Working controls and state

Hold LMB / Space for primary, RMB for alternate, R to reload, 1–8 for the basic weapon slots, and [ / ] to cycle owned weapons. K opens the inventory and fabrication browser; J builds the bench. The browser also equips crafted weapons, crafts components/ammunition, chooses a faction and upgrades the bench. A failed operation reports its reason and spends nothing. Benches cost $150 and 12 scrap and use the existing legal base-placement checks.

All 32 primary and alternate modes are implemented using bounded hitscan/pellet/beam/rail, electric chains, homing or ballistic projectiles, wall bounces, discs, grenades, lobbed shells, pools and fields. Orb detonation, sticky detonation and disc recall operate on owned live projectiles. These are playable prototype interpretations; the earlier prose damage/firing profiles remain design targets rather than exact promises of tuned behavior. Projectile elevation is simulated over the current 2D collision world. Visuals are lightweight software-rendered traces/arcs/volumes, not final particles or sound design.

Reload completes after the weapon timer. Switching cancels unfinished reload/charge actions; death cancels them, and active projectiles owned by a dead/disconnected player are removed. Heat limits sustained rotary/beam fire. Cryo applies a short bounded slow. Smoke obscures the local view; vortex fields affect nearby visible projectiles. Dedicated snapshot v2 stores arsenal inventory, research, faction, bench tier and loot state, retains checksum/backup recovery and accepts older v1 snapshots. Solo remains a session game.

Runtime art uses one authored trim texture, modest beveled edges, four rigid clips in each GLB, and matching component poses in Java. The field-core suspension is intentional; conventional attachments retain their blocking mounts. Detailed kitbash replacement, final sound, power-ups and hunger/thirst are outside this run.
