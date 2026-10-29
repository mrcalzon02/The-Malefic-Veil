# Three factions × three exclusive classes

Faction, character appearance, and mechanical class are distinct. **A class belongs to exactly one faction**; it cannot be selected by another. The playable prototype still transmits only legacy ability IDs 0–3, so faction membership is not yet independently replicated or persisted on the server. The new launcher faction choice is therefore a **selection-preview gate**, not authoritative faction ownership.

| Faction | Class | Gameplay identity | Current status |
|---|---|---|---|
| The Unraveled | Warden | Defensive devices and hardpoints | Existing Warden ability ID 0 |
| The Unraveled | Stitcher | Improvised recovery and healing | Existing Medic ability ID 1 |
| The Unraveled | Hollow | Invisibility / concealment | Existing Shade ability ID 2 |
| The Penitent | Confessor | Sanity and psychic repulsion | Existing Occultist ability ID 3, interim Occultist model |
| The Penitent | Flagellant | Pain-for-power risk and melee pressure | Concept only |
| The Penitent | Votary | Group rites and ritual support | Concept only |
| The Begotten | Harrower | Stalking, fear and prey isolation | Concept only |
| The Begotten | Devourer | Durability, feeding and brutal assault | Concept only |
| The Begotten | Mourner | Dread fields and corrupted ground | Concept only |

The user-facing launcher first selects faction and then presents only that faction's three classes. Unimplemented roles are labeled **COMING SOON** and reject play rather than silently granting another faction's ability. Existing 4 abilities remain untouched. No unlicensed Characters PSX FBX is imported or packaged.

## Non-negotiable multiplayer completion gate

Add stable faction ID, class ID and appearance ID to server-owned saved character records and versioned authentication/selection packets. A session must send a class *registry ID*, not just the legacy numeric ability ID. Server validates that the selected class belongs to the character's permitted faction, has a real implementation and uses a correctly licensed available appearance. The renderer resolves appearance IDs separately from powers. Clients must never dictate their own faction through local visuals or a spoofed peer packet. Preserve the original four legacy IDs in versioned migration only; no save conversion may silently turn Warden into a monster.

The Begotten may need different physical scale, locomotion and interaction rules. Do not fake parity or grant unusual collision privileges by skin selection. Host validation, PvP silhouette recognition, class cooldowns, unique audio cues and faction spawn rules require their own implementation and test gates. The archive's Killer family is a *candidate source*, not proven usable eldritch creatures.

## Acceptance

C1 registry and launcher preview: implemented, runtime execution pending. C2 actual FBX selection, licensed conversion and skeletal animation: open. C3 five additional unique class mechanics: open. C4 host-authoritative faction/class/appearance profile and transport: open. C5 independent three-faction spawn, animation, balance and network tests: open.
