# DEATH IS TOTAL — GRAVEYARD AND BODY RECOVERY CONTRACT
**The Malefic Veil — binding design v1.0 — 21 October 2026**

**DESIGN STAGE. Not live gameplay.** The old city shooter keeps ammunition/consumables in legacy integer fields and has not yet been migrated to the unified persistent item ledger. All details below define the survival game's required authoritative behavior; do not present them as existing feature functionality.

## Inviolable death rule

**Every physical object carried or equipped on the character is removed on death.** Weapons, weapon attachments, ammunition including chambered and loaded rounds, armor, equipment, tools, medical supplies, food, water, packs and bags, loose components, carried funds/currency tokens, quest objects that are genuinely physical, worn accessories and trophies—all are removed. No default weapon on respawn, no player-selectable 'keep inventory', no protected hotbar, no premium insurance, no class immunity, no items hidden in a 'secure pouch'. Unlocks, memories, appearance entitlements, recipes learned, owned crew membership, reputation and account identity are *not* physical objects and remain. Stationary base stores and externally parked truck cargo are **not** on the character and are not deleted by that character's death.

**The only exception to irreversible item destruction is a valid graveyard corpse escrow**, and even then the **respawn inventory is empty**. Recovery requires physically returning to the actual monumental graveyard, and no items are delivered at respawn or remotely by clicking a UI toggle. The graveyard protects *one last body ledger* and is a late-game, expensive settlement system rather than an affordable starter respawn station.

### Eligibility at the instant of death

At the authoritative death tick, the server checks: (1) there is a graveyard facility completed **before** this death and bound to this character's crew; (2) the site exists on valid wilderness territory, is intact and **OPERATIONAL** under the facility utility rules; (3) the character is an authorized crew member, not an unaffiliated visitor; (4) active monument permits a body escrow slot; (5) no ledger conflict exists for this character and epoch. If any fails, inventory is destroyed. **Constructing or repairing a graveyard after a death cannot retroactively protect it.** If a graveyard becomes unpowered later, existing unexpired escrow remains sealed but unclaimable until service is restored.

### Atomic inventory movement

A death has a unique server-generated `deathEventId` and a serializable item-inventory revision. Exactly one transaction removes **every** carry/equipment slot from character ownership. It either issues irreversible item-deletion journal records or changes ownership to a unique, sealed `CorpseEscrow(deathEventId,characterId,graveId,items,createdAt,expiresAt,status)`. These two paths are mutually exclusive. Only the server sees authoritative item instance IDs. Repeated damage/death packets, two simultaneous killers, client reconnect or a retry cannot create a second corpse or resurrect pre-death gear. On journal failure, the death transition cannot be reported as durably settled; recover or retry the **same** event ID, never fabricate a 'free' restored inventory.

On respawn, allocate the selected legitimate spawn, health and progression reset rules, and **an empty inventory**. Possession loss is identical across PvP, PvE, hazards, falls, vehicles, disease, thirst, starvation, and any remaining allowed death path. Disconnected bodies, AFK deaths and failed reconnect do not bypass inventory loss.

### One body per character, deliberately harsh

Default graveyard body ledger is **one outstanding escrow per character**. If a second death occurs while a body is already held, its previous escrow is irrevocably destroyed, whether the next death is protected or not. An eligible second death may produce a *new* escrow containing whatever the character actually carried at that later moment; this never merges the old body with new items. The player sees a conspicuous warning when an older escrow exists. Concurrent death processing enforces ascending death sequence rather than socket arrival order.

A monument has **four concurrent sealed body niches** initially, **eight** after a very expensive T4 expansion. Slots are occupied across its crew's characters, not unlimited for every faction member. At capacity, additional death cannot escrow and items are permanently lost. Authoritative reservation/expiry order is deterministic (oldest first for *display*, not auto-replacement). One monument per base; no chain of cheap spare graveyards to accumulate limitless life insurance.

### Recovery at the actual physical graveyard

The owner must have respawned and traveled through the dangerous open world to the original base; remote loading, map teleporting and automatic shipping are forbidden. Graveyard access requires a working entrance and server-owned crew rights, power/maintenance, no siege lock, 4-meter interaction range with LOS and uninterrupted **30-second** recovery animation/channel. The channel has an explicit cooldown and is interrupted by hostile damage, out-of-range movement, disabled facilities or siege. On completion, perform one transactional transfer from body ledger to owner inventory **or into a pre-authorized owned morgue locker**. If backpack weight/slot limits prevent complete return, do not perform a partial transfer that could orphan items. Instead, use the locker path with measured storage room; all-or-nothing item-instance transfer and single-spend receipt. The graveyard never spawns unattached loot copies. Retrieval is *not* a healing/revival ability.

**Time limit:** default one real-world **72-hour escrow lifetime** stored in UTC server time with monotonic reconciliation; time must survive server restarts, zone eviction and migrations. Clock skew is bounded, and recovery still requires an operational graveyard. On expiry the escrow is irrevocably deleted with audit and UI notice. An operator cannot set the clock backward to fabricate refunds.

### Destruction, raids and offline protections

During a declared valid raid vulnerability window, monuments are damageable structures. Damage that disables the graveyard stops recovery immediately, but an intact crypt retains the escrow. Full destruction starts a **12-hour last-repair grace** if the corpse's own 72-hour expiry has not passed. Repair during grace resumes the existing ownership-bound ledger. If the graveyard is not restored by the earlier of those deadlines, all associated escrow is destroyed, with audit records. No player can 'steal another character's soul' and directly loot their graveyard. Raiding a graveyard is nevertheless strategically meaningful: by destroying it, enemies can force permanent loss after the announced repair grace. The physical base's **ordinary resource storage** is a separate raidable system and must not be confused with private sealed body escrows.

Crew departure, betrayal, base transfer or charter deletion must not silently unlock private escrows to an enemy; specify a visible owner-bound expiry path and deny unauthorized claims. Server admins may use audited rollback of valid *existing* journal states only; they may not casually fabricate missing items. The active crew's vulnerability timer and siege damage must be displayed to affected members.

### Built to be enormous

All three faction monuments share a **minimum 24×24-meter** wilderness ground footprint, structurally supported vault, perimeter, gate, interior morgue stations and an unmistakable high silhouette. Dependencies: T3 command hall, research facility, infirmary, signal relay and powered/live utility services. Initial resource cost: **24 METAL_STOCK truck loads, 16 MACHINE_PARTS truck loads, 140 stone, 90 timber, 60 fiber, 30 herbs, 24 ore**, plus staged labor and maintenance. This represents **40 separate successful loaded-truck deliveries** before other building costs. Power/water/reagents and timed labor required for recovery. No faction gets a recipe discount that collapses the monument into a routine respawn upgrade.

The physical monuments must unmistakably match faction horror language:
- **Unraveled — Cemetery of Names:** salvaged shipping-containers and grave markers, irregular survivor memorial plaques, radio aerials and heavy shuttered mortuary vaults. Human, grieving, utilitarian; ledger tablets indicate the living owner's unique body.
- **Penitent — House of Last Confession:** a grim slasher-cult compound of cold ritual chapels, decrepit summer-camp beams, industrial boiler flues, butchered pews, inward-facing confessional cages and terror-harvesting apparatus. The architecture is constructed **by killers** to terrify anyone approaching; it is not a generic pretty gothic cathedral. Single body escrow rules unchanged.
- **Begotten — Ossuary of the Unmade:** towering corroded steel skeleton, enormous arching columns strangled in wet membranes, rib trusses and the silhouettes of absorbed bodies under translucent stretched flesh. The crypt opens like a heavy organ but remains a collision-valid interactable building. No free regenerative inventory on respawn.

## Death UX / clear QoL

On killing blow, emit explicit `INVENTORY LOST` and either `PERMANENT LOSS` or `BODY SEALED AT [MONUMENT NAME]` with remaining time and eligibility reason. The player's respawn-selection screen continues to show countdown and legitimate spawn choices, not an "equip previous loadout" loophole. If a grave is active, mark its physical map location (only if character has learned/authorized it), state, time remaining and visit route; if down or under siege, say so. UI must remain concise, PSX-styled and controller navigable. Do not suggest a body is guaranteed protected if slots are full, monument offline or crew access lost.

## Necessary tests and release gates

1. Every source of death exhaustively empties carry, armor, weapon, ammo and packed currency fields; stationary crew storage and externally parked trucks remain untouched. **All death paths, no exceptions**.
2. No monument / incomplete monument / expired or destroyed monument / out of slots / no crew affiliation ⇒ permanent item destruction.
3. Fully operational monumental graveyard created **before** death ⇒ one owner-bound escrow, empty respawn, timed physical visit, exact once-only return of unchanged item-instance IDs and quantities.
4. Second death destroys earlier escrow, including if second death is unprotected. Crash/retry/reconnect cannot replay an old corpse.
5. No owner spoofing, wrong faction, disband exploit, client-uploaded inventory, request replay, two simultaneous killers, cross-zone stale revision or time rewind can duplicate equipment.
6. Energy loss and siege damage freeze recovery; grace/expiry and UTC restart tests destroy escrows correctly. Body theft is impossible, but attacking monuments still meaningful.
7. Exact source-asset and PSX UI proof must be collected via actual compiled render and hosted simulation—not generated concept art marketed as in-engine.
