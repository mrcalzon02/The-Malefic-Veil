# The Lost — The Ruins Remember

[Home](Home.md) · [Lost Arsenal](Lost-Arsenal.md) · [World](World.md)

**Enemy faction, not a player class.** The Lost are former inhabitants and malformed entities drawn to fixed HAZARD structures. Their encounters should have a *source*, *weapon identity*, *tactics*, *environmental purpose* and readable weaknesses. They remain mechanically distinct from the three playable factions, though current hostile targeting treats Begotten players as allied with world monsters.

## What is live in the city

[LostSpawnSites.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostSpawnSites.java) assigns **45 persistent NPC slots** to up to **36 HAZARD ruin sources**, with site capacities, 45-second spawn cooldowns, maximum 45 live global creatures, max five inside a 12-unit neighborhood, and no new apparition near a living player within 10 world units. The source building ID and `lost-*` appearance persist on each NPC slot. The host pursues visible non-Begotten players, or audible moving loaded trucks from greater range, and attacks in melee for 8 HP and 8 sanity at a 1.2-second cooldown.

**Unfinished:** site-specific respawn cooldown persistence through full offline-clock restart semantics; model/animation source licensing and import; weapons below; elite behavior; loot tables; full-world NPC streaming.

## Five enemy families

### Drifters
Ruined civilians using scavenged objects. They move between cover and punish careless close-quarters looting; irregular aim and scarce ammunition distinguish them from trained soldiers.
**Equipment package:** [five designed weapon profiles](Lost-Arsenal.md#drifters); only the basic old melee logic is currently played. **Encounters:** street ruins, salvage offices, alley choke points. **Counterplay:** deny scavenged firing positions.

### Zealots
Human remnants organized around broken rites. They punish clustered squads and drain sanity; powerful casts are noisy and readable. Zealots are present in the source-candidate catalog, but current city site spawns do not select them.
**Equipment package:** [five designed weapon profiles](Lost-Arsenal.md#zealots); only the basic old melee logic is currently played. **Encounters:** chapels, bell towers and pilgrimage routes. **Counterplay:** interrupt ritual windups and separate groups.

### Stalkers
Predators exploiting occlusion and movement. Flank paths, pursuit marks and physical leaps matter more than raw HP. Their attacks must respect the same geometry and sound readability as player movement.
**Equipment package:** [five designed weapon profiles](Lost-Arsenal.md#stalkers); only the basic old melee logic is currently played. **Encounters:** forest edges, dark interior exits and marsh. **Counterplay:** watch flanks, preserve hearing and keep escape lanes.

### Brutes
Heavy obstacles that control doors and roads through displacement. Strong impacts require long tells, collision checks and large punish windows.
**Equipment package:** [five designed weapon profiles](Lost-Arsenal.md#brutes); only the basic old melee logic is currently played. **Encounters:** wrecked traffic corridors, scrapyards and collapsed works. **Counterplay:** draw committed attacks into empty space.

### Wailers
Fragile sanity specialists. Attacks are announced through visual/sound cues, with clear exit routes. A Wailer should not stack unrecoverable sanity drains.
**Equipment package:** [five designed weapon profiles](Lost-Arsenal.md#wailers); only the basic old melee logic is currently played. **Encounters:** burned clinics, flooded chambers and resonant urban basements. **Counterplay:** break line of sight and exit marked resonance zones.

## Encounter composition and ruin identity (new design)

| Ruin | Default mix | Signature complication | Tactical objective |
|---|---|---|---|
| Tenement / wrecked flats | Drifter + Stalker | Stairwell feints, scavenged bottlenecks | Retrieve evidence from occupied rooms |
| Cursed chapel / bell tower | Zealot + Wailer | Audible waves and ritual hazard circles | Interrupt a marked rite, protect retreat |
| Burned clinic | Wailer + Stalker | Limited safe sightlines, sanity erosion | Extract medicine without camping |
| Contaminated laboratory | Stalker + Brute | Line-of-sight traps and heavy guards | Restore power or reach a sample cache |
| Vehicle scrapyard | Drifter + Brute | Metal cover and loud loaded trucks | Escort a pallet truck across danger |
| Flooded basement / drainage | Wailer + Drifter | Echoing sound, slow movement zones | Follow light/noise, use elevated passages |

The encounter table is **proposed scenario design**, not currently wired site templates. No enemy should spawn in a player's face to satisfy a scripted objective. Cleared ruins obey site cooldown and population budget; completion and salvage reward are host-authoritative.

## Smarter rather than merely stronger

**Perception:** Different families weight sound, movement, visual contact and cargo noise differently. **Position:** Stalkers flank; Brutes establish threatening lanes; Wailers avoid close exposure; Drifters retreat to clutter; Zealots gather for rites. **Coordination:** A group has at most one designated support/area caster at once, with transparent warning effects. **Morale:** Drifters may break off after losses; zealots may defend a relic; predators may retreat into darkness. **Environment:** doors, floors, streets and real collision dictate attack success; no ghost projectiles through buildings. **Counter:** every effect must have a readable tell, positional answer and bounded duration.

**Progression:** Encounter rank governs decision options and equipment selection *separately* from bounded numeric levels. Weapon availability differs by family and rarity; see [full roster](Lost-Arsenal.md). Difficulty should grow by composition, decision-making, coordination and hazards, not identical enemies receiving +X% damage per level.

**Loot integration:** Proposed family drops should be thematic and finite. Drifters: worn mechanisms, scrap; Zealots: seals and ritual remnants; Stalkers: sinew and spines; Brutes: dense plating and construction remnants; Wailers: resonance fragments. These are **not** added to `economy/items.csv`, and cannot be counted as craftable until item IDs, icons, economy costs and server drop validation exist.

**Appearance registry:** [LostNpcCatalog.java](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/src/main/java/veil/LostNpcCatalog.java) references 15 static FBX candidate source variants for continent encounter plans; one is flagged texture-unverified and those FBX assets are *not* runtime animation meshes. Actual current city site appearances are `lost-drifter`, `lost-stalker`, `lost-wailer`, and `lost-brute`; zealots currently lack a city-site selection path.
