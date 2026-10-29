# HARDCORE HOSTING, RPG PROGRESSION AND FACTION-WIDE ALLIANCE RULES
**The Malefic Veil — authoritative feature contract • 21 October 2026**

## Three mutually hostile playable civilizations

- **The Unraveled:** civilian/survivor population and salvage bases.
- **The Penitent:** murderous occultists, slasher cults and terror industry.
- **The Begotten:** playable horror monsters with steel/flesh industry.
- **The Lost:** separate hostile *NPC* spawner population, not a fourth player-character allegiance.

**NONNEGOTIABLE: ALL crews within each playable faction are irrevocably allied.** No group-versus-group combat inside a faction, no war declaration against same-faction parties, no faction switching to betray a friendly base, no toggling a friendly-fire override in hardcore, no clan admin with an exception. Player bullets striking another member of the same faction have zero effect on that player and cannot travel through the friendly body to hit an enemy beyond it. The host rejects deliberate damage to legacy structures owned by other accounts of the same faction and excludes friendly players/turrets from hostile spawn scores. Existing unclaimed trucks have no owner record, so shared-vehicle friendly-fire/repair ACL remains a needed follow-up: do not claim protection for owned vehicles until a real vehicle-ownership ledger exists.

**Crew membership is private administration, not global faction diplomacy.** Several independent Unraveled crews may retain their own buildings, resources, ranks and charters, yet are ALWAYS allies in PvP. Likewise every Penitent crew and every Begotten crew. Distinct factions can fight, with the existing raid window, permanent construction protection and authentication rules.

`FactionGroups` implements one-time account-bound faction enrollment, crew charter creation, invitation/acceptance, officer promotion, member removal, founder transfer and disband. Faction is keyed by the authenticated persistent account and does not change on leaving a crew or surviving hardcore permadeath. Crews cannot recruit another faction, elevate ordinary members to founder without explicit transfer, expel their founder or claim temporary guest sessions as durable property owners. Initial caps: 32 accounts per crew, 2,048 crews per world, 256 invitations and 5-minute pending invite windows. These are bounded back-end limits, not fully functioning world base ACLs: wilderness facilities and claim ownership are still separate work.

The server sends `FACTION_STATE` on authentication and after crew commands. LAN client exposes `FACTION_JOIN`, `CREW_CREATE`, `CREW_INVITE`, `CREW_ACCEPT`, `CREW_LEAVE`, `CREW_TRANSFER` and `CREW_DISBAND`. The current Java2D desktop uses a compact **O** management prompt to call those actual server commands. `I` shows current survival carry inventory, and `K` shows skill ranks, alive streak and hydration. All requests are authorized by the server, and client-supplied alleged allegiance is never used for hit or ownership checks. Penitent/Begotten character/asset integration is incomplete in the current playable Java2D character selection even though the authoritative faction/group schema supports them.

## Normal versus hardcore — a PER-WORLD host option

Use the dedicated server launcher with positional arguments and optional `--hardcore` anywhere, e.g.

```sh
java -Dveil.hardcore=true -cp build/classes veil.LanServer 7777 20261020 server-data
# or: java -cp build/classes veil.LanServer --hardcore 7777 20261020 server-data
```

Normal (default) death: **destroy all physical carried items, ammo, currency, survival materials and equipment**, reset living streak bonus to 1.0x, leave earned permanent RPG skills and levels intact. Hardcore death: **destroy the same complete physical inventory AND reset every earned skill XP/level, personal frags and alive streak to starting zero**. The authenticated account identity, selected faction and legitimate crew membership survive; otherwise permadeath could strand and orphan an entire faction's shared base. No free starter gun or ammo spawns with a dead character. A completed monumental graveyard is the only planned item-recovery escrow under either mode; it never preserves skill XP in hardcore. Graveyard itself is still DESIGN and not operational.

`HostRules` writes a CRC-checked `host-rules.dat` on first world creation and rejects mode flips on subsequent startups; enabling hardcore on a legacy NORMAL world without an explicit audited migration is forbidden. `AccountDirectory` stores CRC-verified, backed-up account profiles, skill snapshots, immutable factions and crew memberships. `DurableRealm` remains the separate authoritative world state. A single crash-atomic transaction spanning both separate files, including a kill exactly during a storage write, remains an explicit durability gate; the server must not present the two independently safe files as one globally atomic journal.

## Character advancement and world threats

`CharacterProgression` supplies MARKSMANSHIP, ATHLETICS, ENGINEERING, SCAVENGING and SURVIVAL, each with cubic XP thresholds up to level 50. Overall level averages all five to discourage single-task AFK specialization. The longer a living character survives, the greater the server-only XP reward multiplier: +10% per 30 minutes survived, capped at 2.5x after 7.5 hours. Death immediately resets that **life streak** in both modes; hardcore additionally deletes accumulated skill experience. This is a slow-growth RPG target, not a subscription/faster-leveling mechanic.

Current combat hooks include damage bonus for Marksmanship (maximum +20%), movement bonus for Athletics (maximum +15%), reload cooldown modifier for Marksmanship (down to 80%) and bounded Engineering craft-time modifier (down to 75%). Client data displays current rank, XP, level threshold, alive time, survival needs and active server protection. Further host-verified grant hooks for high-tier crafting, construction completion, survival feats and actual intercity traversal must arrive with their corresponding playable mechanics. XP must never be awarded from a client's claimed movement, item quantity or invented monster kill.

NPC threat labels are based on **server-ranked monster level minus player level**:
- More than **five levels below**: pale light-blue name and health bar, hollow icon.
- Within approximately ±5 levels: green name, diamond and health bar.
- Between **1–5 levels above**: orange danger exclamation and health bar.
- More than **five levels above**: red skull-and-crossbones, name and health bar.

The Swing game renders badges above visible PSX enemies from server rank snapshots, not manufactured screenshot annotations. Fog/occlusion and a seven-label cap avoid UI spam.

## Host-owned protection

- On a *real socket connection*, the host starts a bounded world-loading shield (up to 10 seconds, with explicit ready acknowledgment and a 15-second handshake deadline). This is not a generic initial shield in standalone unit arenas.
- On respawn, the host draws a cryptographically seeded random **5–8 seconds of nonoffensive spawn grace**. Movement/escape remains possible while shielded. An actual shot, consumable pickup, medkit action, vehicle entry or class ability cancels protection **before** producing an effect. Empty trigger pulls don't cancel protection because after mandatory item loss there may be no ammunition.
- Shielding is invulnerability only, not unrestricted offensive immunity: no firing safely at opposing crews. Expired protection never silently renews from repeated scene-ready commands or disconnected/reconnected scripted packets.
- Spawn selection still shows its risk and a genuine server-owned delay. A dead combatant respawns with no carried goods in either mode.

## Proof and still-open gates

Code: `ArenaSystems`, `FactionGroups`, `CharacterProgression`, `AccountDirectory`, `HostRules`, `LanServer`, `NetLink`, `MaleficVeil`; regression: `FactionGroupHardcoreTest`, existing combat and multiplayer soak, world saves and graphical input. New behavior must pass Linux/Windows Java 17/21, two connected clients in opposing factions, two different same-faction crews in the same PvP corridor, a base damage request and a hardcore death followed by world restart.

Not yet complete: authenticated character-level multi-slot selection, all nine playable class models, persistent survival inventory in the dedicated account file, server-authoritative construction/crafting XP from live wilderness facilities, full loot-on-death escrow/graveyard and same-faction vehicle/cargo ownership. Distinguish developed core behavior from these unmet release gates.
