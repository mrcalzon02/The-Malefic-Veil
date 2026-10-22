# Faction settlement sustenance — third high-level upkeep pressure

A high-level base consumes a faction-specific resource in addition to power and munitions. The objective is ongoing expeditions, not a punitive chore that requires a full team online.

| Faction | Sustenance | Acquisition |
|---|---|---|
| The Unraveled | Resonance shards | Scattered urban anomalies and Resource/Hazard plots |
| The Penitent | Penitent relics | Scattered urban cache sites |
| The Begotten | Lost souls | Killing hostile Lost NPCs |

The seeded city node placement excludes SAFE plots and the initial spawn clearance zones. Existing resource/hazard plots receive higher cache probabilities than OPEN areas. Additional wilderness sources and loot-from-Lost variants can be added without turning SAFE buildings into loot sites.

## Upkeep

Base level 1–9: free. Level 10–17: 1/hour; 18–25: 2/hour; 26–33: 3/hour; 34–41: 4/hour; 42–49: 5/hour; level 50: 6/hour. Costs are assessed hourly at the base account, not per individual turret. Depleted sustenance suspends advanced base operation without destroying structures or claims. Ordinary structures and access routes remain intact.

Current city-cache base yields are 2–5 units. Scavenging skill increases yield gradually up to 2× at skill level 50. An eligible Begotten kill of an actual Lost NPC yields a base of 2 souls and up to 4 at maximum skill. In nominal conditions a skilled solo player can cover peak upkeep through a handful of recoveries or kills per hour, although travel and enemy pressure remain relevant.

## Implementation boundary

`SettlementSustenance` implements a deterministic host-side node/stock/upkeep ledger and an isolated test. It is not yet attached to the persistent `SettlementAuthority` charter store or to the combat NPC death event, and it is not yet wired to network pickup interactions or facility enable/disable gates. These must use authenticated accounts and server-verified faction ownership. Don't award souls for an alive NPC hit, the same NPC death twice, a dummy death, or an AI-spawn/death replay. Do not use faction-agnostic trade deposits to bypass the restriction. Record sustenance balance and harvested site IDs in durable world saves before enabling ongoing upkeep on persistent hosts. An offline scheduling policy and grace reserves still require an explicit release decision. Source-level commits do not constitute an end-to-end playtest.

Data: `economy/faction_sustenance.csv` and `economy/settlement_upkeep_levels.csv`; regressions: `SettlementSustenanceTest`.
