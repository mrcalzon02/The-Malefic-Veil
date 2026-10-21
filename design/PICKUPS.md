# Pickups, power-ups and inventory contract

Prototype art and proposed mechanics. Existing health/ammo/supplies systems remain authoritative. New pickup effects below are not wired into solo/LAN combat yet.

## Power-ups

| ID | Effect proposal | Duration | Visual language / restriction |
|---|---|---|---|
| overdrive | 1.75x direct damage, no multiplier to damage over time | 20 s | amber toothed reactor; cannot stack |
| aegis | absorbs 100 incoming damage | 25 s maximum | blue shield plates; no invulnerability |
| haste | +20% movement, +15% reload speed | 20 s | twin cyan fins; preserve movement collision |
| blood-pact | heal for 20% direct damage dealt, capped 50 total | 18 s | red heart capsule; no turret/DoT farming |
| phase-step | one short directional dash every 4 s | 20 s | violet offset rings; no wall clipping |
| storm-crown | next 8 hits chain at 30% damage to one visible nearby enemy | 20 s | purple fork crown; one secondary jump |
| coolant-heart | heat buildup halved, overheat recovery accelerated | 25 s | frosted blue twin canister; no infinite beam |
| scavenger | doubles ordinary ammo pickup yield within capacity | 30 s | lime magnet fork; excludes cores/quest loot |
| iron-saint | +75 temporary armor, bleeds away after 10 s | 30 s maximum | gold ribbed armor crest; no stacking |
| echo-shot | every third primary shot repeats at 35% damage | 18 s | three white vanes; excludes superweapon |
| veil-sight | silhouettes visible enemies for 0.6 s after their shot | 20 s | pink lens; no through-wall tracking |
| annihilator | one rare core and 10 s weapon-switch bonus | 10 s | black reactor with white hazard ring; broadcast spawn cue |

Mutually exclusive offensive buffs: overdrive, blood-pact, storm-crown, echo-shot. Pickup the new one to replace, not extend, the old. Timers and ammo bonuses are host-owned; death clears timed buffs. Individual sound/stencil/shape cues must supplement color.

## Health, armor, ammunition and resources

| Family | Prototype variants | Proposed behavior |
|---|---|---|
| health | shard +5, field kit +25, trauma case +75, overheal +50 | ordinary caps at 100; overheal decays from 150 |
| armor | scrap plate +10, vest +50, heavy shell +100 | capacity and absorption rules separate from health |
| ammo | light, heavy, shells, belt, cells, slugs, cores, charges, rockets, missiles, grenades, mortar, flak, discs, orbs, scrap, gel, coolant | shaped container per family; partial pickups leave remainder; core is rare |
| resources | scrap, electronics, chemicals, weapon parts | inventory stack limits 100 / 50 / 30 / 20; scrap is also a weapon resource |
| salvage | damaged weapon | explicit dismantle action produces parts; never auto-consume equipped items |
| consumables | food ration, sealed water | reserve inventory IDs/stacks only; no hunger/thirst simulation in this phase |

Inventory categories: weapons, ammo, medical, armor, resources, consumables, quest. Proposed 8 weapon slots, per-family ammo caps, 24 general slots; no pickup silently deletes inventory. Full inventory prompts a deliberate swap. Power-up timer and armor overlays must fit existing concise HUD. World pickups need bob/spin, identifiable geometry, local audio, respawn timing and server-owned collection. These are separate from weapon animation clips.
