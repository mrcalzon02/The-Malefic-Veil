# Functional arsenal and base progression

The approved primitive weapons are the working art. Modest bevels, shared textures and rigid component animation are sufficient for this phase; detailed kitbash replacement is deferred.

The first **eight** weapons (Vesper through Furnace Organ) may appear as world weapon loot. A player starts with Vesper. The remaining 24 are never random weapon drops: they require a nearby owned, living fabrication bench, its required upgrade tier, a faction recipe and scavenged materials/components. Ammunition and resources are host-owned.

**Canonical factions:** **The Unraveled** (salvage machinery, explosives, field artillery), **The Penitent** (ritualized beams, plasma and arc weapons), and **The Begotten** (disc/bounce, biological, cryogenic and gravitational instruments). The host's permanent faction enrollment alone controls advanced manufacturing. The old independent Foundry/Lumen/Veil selector was removed. The original distribution is 9 Unraveled / 9 Penitent / 6 Begotten. Basic world weapons are shared; research prerequisites remain owned/earned rather than consumed.

Raw resources: scrap, electronics, chemicals, alloy. Component recipes: machined frame, power cell, arc coil, warhead, anomaly core. Benches upgrade from tier 1 to 3. These choices establish an editable initial crafting tree, not final faction lore or balance.

All new fire, reload, selection, pickup, crafting and faction operations must be validated on the host. Reloads take time; switching/death cancels unfinished actions. Projectile lifetimes, bounce counts, chain counts and area damage are bounded; walls and barricades block effects; existing spawn and offline-base protection remain authoritative. The legacy two-weapon API remains for compatibility checks while the playable client uses the arsenal API.

Acceptance: all 32 weapons fire and reload through the host; alternate modes, chain/bounce/lob/beam behavior and crafting denials have deterministic tests; real clients replicate inventory and effects; the same assets load in a packaged JAR; animation and texture changes have rendered evidence. Exact balance and a physical multi-machine playtest remain later tuning work.

## Working controls and state

Hold LMB / Space for primary, RMB for alternate, R to reload, 1–8 for the basic weapon slots, and [ / ] to cycle owned weapons. K opens the **in-game** arsenal browser, with keyboard-operated selection, fabrication, equipment changes and bench upgrades; J requests a legal workbench placement. The browser displays the permanently enrolled faction and never offers a second faction choice. Components and ammunition use their existing authenticated host commands. A failed operation reports its reason and spends nothing. Fabrication shares the existing legal FIELD_BENCH station: crafting the station kit and placing it in wilderness is required. Placement cannot bypass the immutable city.

All 32 primary and alternate modes are implemented using bounded hitscan/pellet/beam/rail, electric chains, homing or ballistic projectiles, wall bounces, discs, grenades, lobbed shells, pools and fields. Orb detonation, sticky detonation and disc recall operate on owned live projectiles. These are playable prototype interpretations; the earlier prose damage/firing profiles remain design targets rather than exact promises of tuned behavior. Projectile elevation is simulated over the current 2D collision world. Visuals are lightweight software-rendered traces/arcs/volumes, not final particles or sound design.

Reload completes after the weapon timer. Switching cancels unfinished reload/charge actions; death cancels them, and active projectiles owned by a dead/disconnected player are removed. Heat limits sustained rotary/beam fire. Cryo applies a short bounded slow. Smoke obscures the local view; vortex fields affect nearby visible projectiles. The current realm snapshot v3 appends the original arsenal account inventory, ammo, research, bench tier and collected loot to the newer cargo-preserving v2 snapshot, while reading older v1/v2 worlds. Permanent faction identity remains in the game's host-owned FactionGroups registry. Solo remains a session game.

Runtime art uses one authored trim texture, modest beveled edges, four rigid clips in each GLB, and matching component poses in Java. The field-core suspension is intentional; conventional attachments retain their blocking mounts. Detailed kitbash replacement, final sound, power-ups and hunger/thirst are outside this run.

## Faction material pass

The original textured component meshes and GLB hierarchy are retained without reshaping. Runtime triangle material variants now use component roles and source luminance rather than flat global color: Unraveled uses patched graphite/oxidized teal/brass; Penitent uses ash charcoal/ceremonial brass/oxblood/ivory; Begotten uses bruised chitin/violet/bioluminescent organ cores. Source GLB binary materials are retained for later export work, and the new Java palette drives both software and GPU world/view meshes.
